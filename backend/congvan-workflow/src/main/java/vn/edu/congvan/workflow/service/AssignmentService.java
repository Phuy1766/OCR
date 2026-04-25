package vn.edu.congvan.workflow.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.entity.UserEntity;
import vn.edu.congvan.auth.repository.UserRepository;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.auth.service.AuditLogger;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.inbound.entity.DocumentEntity;
import vn.edu.congvan.inbound.entity.DocumentStatus;
import vn.edu.congvan.inbound.repository.DocumentRepository;
import vn.edu.congvan.integration.outbox.OutboxRecorder;
import vn.edu.congvan.workflow.dto.AssignRequest;
import vn.edu.congvan.workflow.dto.AssignmentDto;
import vn.edu.congvan.workflow.dto.CompleteAssignmentRequest;
import vn.edu.congvan.workflow.entity.AssignmentEntity;
import vn.edu.congvan.workflow.entity.AssignmentStatus;
import vn.edu.congvan.workflow.entity.NotificationType;
import vn.edu.congvan.workflow.entity.WorkflowInstanceEntity;
import vn.edu.congvan.workflow.entity.WorkflowState;
import vn.edu.congvan.workflow.entity.WorkflowStepEntity;
import vn.edu.congvan.workflow.entity.WorkflowStepType;
import vn.edu.congvan.workflow.repository.AssignmentRepository;
import vn.edu.congvan.workflow.repository.WorkflowInstanceRepository;
import vn.edu.congvan.workflow.repository.WorkflowStepRepository;

/**
 * Phân công xử lý VB. Phase 5: 1-step (TP/VanThu gán cho 1 chuyên viên).
 *
 * <p>Mỗi action đều: cập nhật state, ghi {@code workflow_steps} (append-only),
 * tạo notification, ghi outbox event, audit log — tất cả trong cùng 1
 * transaction nghiệp vụ (atomic).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final DocumentRepository documents;
    private final UserRepository users;
    private final WorkflowInstanceRepository workflows;
    private final WorkflowStepRepository workflowSteps;
    private final AssignmentRepository assignments;
    private final NotificationService notifications;
    private final OutboxRecorder outbox;
    private final AuditLogger audit;

    @Transactional
    public AssignmentDto assign(
            UUID documentId, AssignRequest req, AuthPrincipal actor, String actorIp) {
        DocumentEntity d = loadDocument(documentId);

        if (d.getStatus() == DocumentStatus.RECALLED
                || d.getStatus() == DocumentStatus.COMPLETED
                || d.getStatus() == DocumentStatus.ARCHIVED) {
            throw new BusinessException(
                    "WORKFLOW_DOCUMENT_CLOSED",
                    "Không thể phân công VB ở trạng thái " + d.getStatus() + ".");
        }
        // Đảm bảo có 1 active assignment tối đa
        var existingActive = assignments.findActive(documentId, AssignmentStatus.ACTIVE);
        if (existingActive.isPresent()) {
            throw new BusinessException(
                    "WORKFLOW_ALREADY_ASSIGNED",
                    "Công văn đã có người được giao xử lý. Dùng REASSIGN nếu muốn đổi.");
        }

        // Validate user tồn tại + active
        UserEntity assignee = users.findById(req.assignedToUserId())
                .filter(UserEntity::isLoginAllowed)
                .orElseThrow(() -> new BusinessException(
                        "USER_NOT_FOUND_OR_INACTIVE",
                        "Người được giao không tồn tại hoặc đã khóa."));

        // Tạo / lấy workflow instance
        WorkflowInstanceEntity wf = ensureWorkflow(documentId);

        AssignmentEntity a = new AssignmentEntity();
        a.setId(UUID.randomUUID());
        a.setDocumentId(documentId);
        a.setWorkflowId(wf.getId());
        a.setAssignedToUserId(assignee.getId());
        a.setAssignedToDeptId(
                req.assignedToDeptId() != null ? req.assignedToDeptId() : assignee.getDepartmentId());
        a.setAssignedBy(actor == null ? null : actor.userId());
        a.setAssignedAt(OffsetDateTime.now());
        a.setDueDate(req.dueDate());
        a.setStatus(AssignmentStatus.ACTIVE);
        a.setNote(req.note());
        a = assignments.save(a);

        // Cập nhật document + workflow
        d.setStatus(DocumentStatus.ASSIGNED);
        d.setCurrentHandlerUserId(assignee.getId());
        d.setCurrentHandlerDeptId(a.getAssignedToDeptId());
        if (req.dueDate() != null) d.setDueDate(req.dueDate());

        wf.setState(WorkflowState.ASSIGNED);
        workflows.save(wf);

        recordStep(wf.getId(), WorkflowStepType.ASSIGN, actor, assignee.getId(),
                a.getAssignedToDeptId(), req.note());

        // Notification + outbox + audit
        notifications.notify(
                assignee.getId(),
                NotificationType.ASSIGNMENT,
                "Bạn được giao xử lý: " + d.getSubject(),
                req.note(),
                "documents",
                documentId.toString(),
                outbox.map(
                        "documentId", documentId.toString(),
                        "assignmentId", a.getId().toString(),
                        "dueDate", req.dueDate() == null ? null : req.dueDate().toString()));

        outbox.record(outbox.event("documents", documentId.toString(), "DocumentAssigned")
                .routingKey("document.inbound.assigned")
                .payload(outbox.map(
                        "documentId", documentId.toString(),
                        "assigneeId", assignee.getId().toString(),
                        "assignedBy", actor == null ? null : actor.userId().toString(),
                        "dueDate", req.dueDate() == null ? null : req.dueDate().toString()))
                .build());

        audit.logSuccess(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.username(),
                actorIp,
                "ASSIGN_DOCUMENT",
                "documents",
                documentId.toString());

        return toDto(a);
    }

    @Transactional
    public AssignmentDto reassign(
            UUID assignmentId, AssignRequest req, AuthPrincipal actor, String actorIp) {
        AssignmentEntity old = assignments.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(
                        "ASSIGNMENT_NOT_FOUND", "Không tìm thấy assignment."));
        if (old.getStatus() != AssignmentStatus.ACTIVE) {
            throw new BusinessException(
                    "ASSIGNMENT_NOT_ACTIVE",
                    "Chỉ reassign được assignment đang ACTIVE.");
        }
        // Mark old REASSIGNED
        old.setStatus(AssignmentStatus.REASSIGNED);
        assignments.save(old);

        // Tạo assignment mới (logic giống assign)
        return assign(old.getDocumentId(), req, actor, actorIp);
    }

    @Transactional
    public AssignmentDto complete(
            UUID assignmentId,
            CompleteAssignmentRequest req,
            AuthPrincipal actor,
            String actorIp) {
        AssignmentEntity a = assignments.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(
                        "ASSIGNMENT_NOT_FOUND", "Không tìm thấy assignment."));

        // Chỉ assignee mới complete được
        UUID actorId = actor == null ? null : actor.userId();
        if (!a.getAssignedToUserId().equals(actorId)) {
            throw new BusinessException(
                    "AUTH_FORBIDDEN",
                    "Chỉ người được giao mới được hoàn thành assignment này.");
        }
        if (a.getStatus() != AssignmentStatus.ACTIVE) {
            throw new BusinessException(
                    "ASSIGNMENT_NOT_ACTIVE",
                    "Assignment không ở trạng thái ACTIVE (hiện tại: " + a.getStatus() + ").");
        }

        a.setStatus(AssignmentStatus.COMPLETED);
        a.setCompletedAt(OffsetDateTime.now());
        a.setCompletedBy(actorId);
        a.setResultSummary(req.resultSummary());

        // Cập nhật document → COMPLETED
        DocumentEntity d = loadDocument(a.getDocumentId());
        d.setStatus(DocumentStatus.COMPLETED);

        WorkflowInstanceEntity wf = workflows.findById(a.getWorkflowId())
                .orElseThrow(() -> new IllegalStateException("Workflow không tồn tại"));
        wf.setState(WorkflowState.COMPLETED);
        wf.setCompletedAt(OffsetDateTime.now());
        workflows.save(wf);

        recordStep(wf.getId(), WorkflowStepType.COMPLETE, actor, null, null,
                req.resultSummary());

        // Notify người gán
        notifications.notify(
                a.getAssignedBy(),
                NotificationType.STATUS_CHANGE,
                "Đã hoàn tất xử lý: " + d.getSubject(),
                req.resultSummary(),
                "documents",
                d.getId().toString(),
                outbox.map(
                        "documentId", d.getId().toString(),
                        "completedBy", actorId == null ? null : actorId.toString()));

        outbox.record(outbox.event("documents", d.getId().toString(), "AssignmentCompleted")
                .routingKey("document.inbound.completed")
                .payload(outbox.map(
                        "documentId", d.getId().toString(),
                        "assignmentId", a.getId().toString(),
                        "completedBy", actorId == null ? null : actorId.toString()))
                .build());

        audit.logSuccess(actorId, actor == null ? null : actor.username(), actorIp,
                "COMPLETE_ASSIGNMENT", "assignments", a.getId().toString());

        return toDto(a);
    }

    @Transactional(readOnly = true)
    public Page<AssignmentDto> myAssignments(
            UUID userId, AssignmentStatus status, int page, int size) {
        return assignments
                .findByUser(
                        userId,
                        status,
                        PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100)))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<AssignmentDto> historyOfDocument(UUID documentId) {
        return assignments.findByDocumentIdOrderByAssignedAtAsc(documentId).stream()
                .map(this::toDto)
                .toList();
    }

    // ---------- helpers ----------

    private DocumentEntity loadDocument(UUID id) {
        return documents.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "DOCUMENT_NOT_FOUND", "Không tìm thấy công văn."));
    }

    private WorkflowInstanceEntity ensureWorkflow(UUID documentId) {
        return workflows.findByDocumentId(documentId).orElseGet(() -> {
            WorkflowInstanceEntity wf = new WorkflowInstanceEntity();
            wf.setId(UUID.randomUUID());
            wf.setDocumentId(documentId);
            wf.setTemplateCode("STANDARD_INBOUND");
            wf.setState(WorkflowState.INITIAL);
            wf.setStartedAt(OffsetDateTime.now());
            return workflows.save(wf);
        });
    }

    private void recordStep(
            UUID workflowId,
            WorkflowStepType type,
            AuthPrincipal actor,
            UUID targetUserId,
            UUID targetDeptId,
            String note) {
        WorkflowStepEntity s = new WorkflowStepEntity();
        s.setId(UUID.randomUUID());
        s.setWorkflowId(workflowId);
        s.setStepType(type);
        s.setActorId(actor == null ? null : actor.userId());
        s.setTargetUserId(targetUserId);
        s.setTargetDeptId(targetDeptId);
        s.setNote(note);
        s.setOccurredAt(OffsetDateTime.now());
        workflowSteps.save(s);
    }

    private AssignmentDto toDto(AssignmentEntity a) {
        return new AssignmentDto(
                a.getId(),
                a.getDocumentId(),
                a.getWorkflowId(),
                a.getAssignedToUserId(),
                a.getAssignedToDeptId(),
                a.getAssignedBy(),
                a.getAssignedAt(),
                a.getDueDate(),
                a.getStatus(),
                a.getNote(),
                a.getCompletedAt(),
                a.getCompletedBy(),
                a.getResultSummary());
    }

    /** Helper exposed cho LocalDate import. */
    @SuppressWarnings("unused")
    private static LocalDate noop() {
        return null;
    }
}
