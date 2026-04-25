package vn.edu.congvan.outbound.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.auth.service.AuditLogger;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.inbound.dto.DocumentFileDto;
import vn.edu.congvan.inbound.entity.DocumentDirection;
import vn.edu.congvan.inbound.entity.DocumentEntity;
import vn.edu.congvan.inbound.entity.DocumentFileEntity;
import vn.edu.congvan.inbound.entity.DocumentFileRole;
import vn.edu.congvan.inbound.entity.DocumentStatus;
import vn.edu.congvan.inbound.repository.DocumentFileRepository;
import vn.edu.congvan.inbound.repository.DocumentRepository;
import vn.edu.congvan.inbound.service.FileValidator;
import vn.edu.congvan.inbound.service.UploadedFile;
import vn.edu.congvan.integration.storage.MinioFileService;
import vn.edu.congvan.integration.storage.StoredObject;
import vn.edu.congvan.masterdata.entity.BookType;
import vn.edu.congvan.masterdata.entity.DocumentBookEntity;
import vn.edu.congvan.masterdata.repository.DocumentBookRepository;
import vn.edu.congvan.masterdata.service.DocumentNumberService;
import vn.edu.congvan.outbound.dto.ApprovalDecisionRequest;
import vn.edu.congvan.outbound.dto.ApprovalDto;
import vn.edu.congvan.outbound.dto.CreateOutboundDraftRequest;
import vn.edu.congvan.outbound.dto.DocumentVersionDto;
import vn.edu.congvan.outbound.dto.IssueRequest;
import vn.edu.congvan.outbound.dto.OutboundDocumentDto;
import vn.edu.congvan.outbound.dto.UpdateOutboundDraftRequest;
import vn.edu.congvan.outbound.entity.ApprovalDecision;
import vn.edu.congvan.outbound.entity.ApprovalEntity;
import vn.edu.congvan.outbound.entity.ApprovalLevel;
import vn.edu.congvan.outbound.entity.DocumentVersionEntity;
import vn.edu.congvan.outbound.entity.VersionStatus;
import vn.edu.congvan.outbound.repository.ApprovalRepository;
import vn.edu.congvan.outbound.repository.DocumentVersionRepository;

/**
 * Quản lý vòng đời VB đi: dự thảo → duyệt cấp phòng → duyệt cấp đơn vị → cấp số
 * → phát hành. Mỗi update tạo version mới (immutable). BR-07: khóa version
 * khi APPROVED, hash SHA-256 chốt content_snapshot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundDocumentService {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DocumentRepository documents;
    private final DocumentFileRepository docFiles;
    private final DocumentVersionRepository versions;
    private final ApprovalRepository approvals;
    private final DocumentBookRepository books;
    private final DocumentNumberService numberService;
    private final FileValidator fileValidator;
    private final MinioFileService minio;
    private final AuditLogger audit;
    private final ObjectMapper json;

    @Transactional
    public OutboundDocumentDto createDraft(
            CreateOutboundDraftRequest req,
            List<UploadedFile> uploadedFiles,
            AuthPrincipal actor,
            String actorIp) {
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            throw new BusinessException(
                    "OUTBOUND_FILE_REQUIRED",
                    "Phải đính kèm ít nhất 1 file dự thảo (PDF/DOCX).");
        }
        for (UploadedFile f : uploadedFiles) {
            fileValidator.validate(f.filename(), f.mimeType(), f.content());
        }

        DocumentEntity d = new DocumentEntity();
        d.setId(UUID.randomUUID());
        d.setDirection(DocumentDirection.OUTBOUND);
        d.setDocumentTypeId(req.documentTypeId());
        d.setConfidentialityLevelId(req.confidentialityLevelId());
        d.setPriorityLevelId(req.priorityLevelId());
        d.setSubject(req.subject());
        d.setSummary(req.summary());
        d.setStatus(DocumentStatus.DRAFT);
        d.setOrganizationId(req.organizationId());
        d.setDepartmentId(req.departmentId());
        d.setDueDate(req.dueDate());
        d = documents.save(d);

        // Tạo version 1
        DocumentVersionEntity v = createNewVersion(d, null, actor);
        // Upload files gắn version
        List<DocumentFileEntity> files = uploadFiles(d.getId(), v.getId(), uploadedFiles, actor);

        audit.logSuccess(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.username(),
                actorIp,
                "CREATE_OUTBOUND_DRAFT",
                "documents",
                d.getId().toString());
        return toDto(d, files, List.of(v), List.of());
    }

    @Transactional
    public OutboundDocumentDto updateDraft(
            UUID documentId,
            UpdateOutboundDraftRequest req,
            List<UploadedFile> newFiles,
            AuthPrincipal actor,
            String actorIp) {
        DocumentEntity d = loadOutbound(documentId);
        if (d.getStatus() != DocumentStatus.DRAFT) {
            throw new BusinessException(
                    "OUTBOUND_NOT_DRAFT",
                    "Chỉ được sửa khi VB đang ở trạng thái DRAFT (hiện tại: "
                            + d.getStatus()
                            + ").");
        }

        // Apply changes (null = giữ nguyên)
        if (req.documentTypeId() != null) d.setDocumentTypeId(req.documentTypeId());
        if (req.confidentialityLevelId() != null)
            d.setConfidentialityLevelId(req.confidentialityLevelId());
        if (req.priorityLevelId() != null) d.setPriorityLevelId(req.priorityLevelId());
        if (req.subject() != null) d.setSubject(req.subject());
        if (req.summary() != null) d.setSummary(req.summary());
        if (req.departmentId() != null) d.setDepartmentId(req.departmentId());
        if (req.dueDate() != null) d.setDueDate(req.dueDate());

        // Validate file mới
        if (newFiles != null) {
            for (UploadedFile f : newFiles) {
                fileValidator.validate(f.filename(), f.mimeType(), f.content());
            }
        }

        // Mark version cũ SUPERSEDED
        DocumentVersionEntity prev = versions.findLatest(documentId)
                .orElseThrow(() -> new IllegalStateException("Document không có version nào."));
        prev.setVersionStatus(VersionStatus.SUPERSEDED);
        versions.save(prev);

        // Tạo version mới
        DocumentVersionEntity newVersion = createNewVersion(d, prev.getId(), actor);

        // Upload file mới (nếu có) — gắn vào version mới
        if (newFiles != null && !newFiles.isEmpty()) {
            uploadFiles(documentId, newVersion.getId(), newFiles, actor);
        } else {
            // Nếu không upload file mới, copy file cũ sang version mới (chỉ link DB,
            // không đụng MinIO). Phase 4: file cũ vẫn dùng được vì version_id là FK.
            // Để đơn giản: tạo bản record mới gắn version mới, cùng storage_key.
            cloneFilesToNewVersion(documentId, prev.getId(), newVersion.getId(), actor);
        }

        audit.logSuccess(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.username(),
                actorIp,
                "UPDATE_OUTBOUND_DRAFT",
                "documents",
                documentId.toString());
        return getById(documentId, actor);
    }

    @Transactional
    public OutboundDocumentDto submit(UUID documentId, AuthPrincipal actor, String actorIp) {
        DocumentEntity d = loadOutbound(documentId);
        if (d.getStatus() != DocumentStatus.DRAFT) {
            throw new BusinessException(
                    "OUTBOUND_NOT_DRAFT", "Chỉ submit được khi đang DRAFT.");
        }
        DocumentVersionEntity latest = versions.findLatest(documentId)
                .orElseThrow(() -> new IllegalStateException("Không có version."));
        latest.setVersionStatus(VersionStatus.SUBMITTED);
        versions.save(latest);

        d.setStatus(DocumentStatus.PENDING_DEPT_APPROVAL);
        audit.logSuccess(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.username(),
                actorIp,
                "SUBMIT_OUTBOUND",
                "documents",
                documentId.toString());
        return getById(documentId, actor);
    }

    @Transactional
    public OutboundDocumentDto approveDept(
            UUID documentId, ApprovalDecisionRequest req, AuthPrincipal actor, String actorIp) {
        DocumentEntity d = loadOutbound(documentId);
        if (d.getStatus() != DocumentStatus.PENDING_DEPT_APPROVAL) {
            throw new BusinessException(
                    "OUTBOUND_NOT_PENDING_DEPT",
                    "VB không ở trạng thái chờ duyệt cấp phòng.");
        }
        DocumentVersionEntity latest = versions.findLatest(documentId)
                .orElseThrow(() -> new IllegalStateException("Không có version."));
        recordApproval(d, latest, ApprovalLevel.DEPARTMENT_HEAD, req, actor);

        if (req.decision() == ApprovalDecision.APPROVED) {
            d.setStatus(DocumentStatus.PENDING_LEADER_APPROVAL);
            audit.logSuccess(actor == null ? null : actor.userId(), actor == null ? null : actor.username(),
                    actorIp, "APPROVE_DEPT", "documents", documentId.toString());
        } else {
            // REJECTED → quay lại DRAFT, đánh dấu version REJECTED, để chuyên viên sửa
            latest.setVersionStatus(VersionStatus.REJECTED);
            versions.save(latest);
            d.setStatus(DocumentStatus.DRAFT);
            audit.logSuccess(actor == null ? null : actor.userId(), actor == null ? null : actor.username(),
                    actorIp, "REJECT_DEPT", "documents", documentId.toString());
        }
        return getById(documentId, actor);
    }

    @Transactional
    public OutboundDocumentDto approveLeader(
            UUID documentId, ApprovalDecisionRequest req, AuthPrincipal actor, String actorIp) {
        DocumentEntity d = loadOutbound(documentId);
        if (d.getStatus() != DocumentStatus.PENDING_LEADER_APPROVAL) {
            throw new BusinessException(
                    "OUTBOUND_NOT_PENDING_LEADER",
                    "VB không ở trạng thái chờ duyệt cấp đơn vị.");
        }
        DocumentVersionEntity latest = versions.findLatest(documentId)
                .orElseThrow(() -> new IllegalStateException("Không có version."));
        recordApproval(d, latest, ApprovalLevel.UNIT_LEADER, req, actor);

        if (req.decision() == ApprovalDecision.APPROVED) {
            // BR-07: chốt approved_version_id + tính hash SHA-256 của content_snapshot
            String hash = sha256Hex(latest.getContentSnapshot());
            latest.setHashSha256(hash);
            latest.setVersionStatus(VersionStatus.APPROVED);
            versions.save(latest);

            d.setApprovedVersionId(latest.getId());
            d.setStatus(DocumentStatus.APPROVED);
            audit.log(
                    AuditLogger.AuditRecord.builder()
                            .actorId(actor == null ? null : actor.userId())
                            .actorUsername(actor == null ? null : actor.username())
                            .actorIp(actorIp)
                            .action("APPROVE_LEADER")
                            .entityType("documents")
                            .entityId(documentId.toString())
                            .success(true)
                            .extra(Map.of(
                                    "approved_version_id", latest.getId().toString(),
                                    "version_number", latest.getVersionNumber(),
                                    "hash_sha256", hash))
                            .build());
        } else {
            latest.setVersionStatus(VersionStatus.REJECTED);
            versions.save(latest);
            d.setStatus(DocumentStatus.DRAFT);
            audit.logSuccess(actor == null ? null : actor.userId(), actor == null ? null : actor.username(),
                    actorIp, "REJECT_LEADER", "documents", documentId.toString());
        }
        return getById(documentId, actor);
    }

    @Transactional
    public OutboundDocumentDto issue(
            UUID documentId, IssueRequest req, AuthPrincipal actor, String actorIp) {
        DocumentEntity d = loadOutbound(documentId);
        if (d.getStatus() != DocumentStatus.APPROVED) {
            throw new BusinessException(
                    "OUTBOUND_NOT_APPROVED",
                    "Chỉ phát hành được sau khi đã duyệt cấp đơn vị.");
        }
        if (d.getApprovedVersionId() == null) {
            throw new IllegalStateException("Document APPROVED nhưng không có approved_version_id");
        }

        DocumentBookEntity book = books.findById(req.bookId())
                .orElseThrow(() -> new BusinessException(
                        "DOCUMENT_BOOK_NOT_FOUND", "Không tìm thấy sổ đăng ký."));
        if (book.getBookType() != BookType.OUTBOUND) {
            throw new BusinessException(
                    "DOCUMENT_BOOK_WRONG_TYPE", "Sổ này không phải sổ công văn đi.");
        }

        // Cấp số (BR-01/02)
        var reserved = numberService.reserve(book.getId());
        d.setBookId(book.getId());
        d.setBookYear(reserved.year());
        d.setBookNumber(reserved.number());
        d.setIssuedDate(req.issuedDate() != null ? req.issuedDate() : LocalDate.now(ZONE_VN));
        d.setStatus(DocumentStatus.ISSUED);
        // Phase 4: đánh dấu cần ký số nhưng chưa ký (Phase 7 sẽ thay).
        // Status ISSUED (chưa SIGNED) hợp với BR-06/12 phải có 2 chữ ký số.

        audit.log(
                AuditLogger.AuditRecord.builder()
                        .actorId(actor == null ? null : actor.userId())
                        .actorUsername(actor == null ? null : actor.username())
                        .actorIp(actorIp)
                        .action("ISSUE_OUTBOUND")
                        .entityType("documents")
                        .entityId(documentId.toString())
                        .success(true)
                        .extra(Map.of(
                                "book_id", book.getId().toString(),
                                "book_year", reserved.year(),
                                "book_number", reserved.number()))
                        .build());
        return getById(documentId, actor);
    }

    @Transactional
    public void recall(UUID documentId, String reason, AuthPrincipal actor, String actorIp) {
        DocumentEntity d = loadOutbound(documentId);
        if (d.isRecalled()) {
            throw new BusinessException(
                    "OUTBOUND_ALREADY_RECALLED", "Công văn đã được thu hồi.");
        }
        if (d.getStatus() == DocumentStatus.SENT
                || d.getStatus() == DocumentStatus.ARCHIVED) {
            throw new BusinessException(
                    "OUTBOUND_RECALL_TOO_LATE",
                    "Không thể thu hồi VB đã gửi/lưu trữ qua giao diện này.");
        }
        d.setRecalled(true);
        d.setRecalledAt(OffsetDateTime.now());
        d.setRecalledBy(actor == null ? null : actor.userId());
        d.setRecalledReason(reason);
        d.setStatus(DocumentStatus.RECALLED);

        audit.log(
                AuditLogger.AuditRecord.builder()
                        .actorId(actor == null ? null : actor.userId())
                        .actorUsername(actor == null ? null : actor.username())
                        .actorIp(actorIp)
                        .action("RECALL_OUTBOUND")
                        .entityType("documents")
                        .entityId(documentId.toString())
                        .success(true)
                        .extra(Map.of("reason", reason))
                        .build());
    }

    @Transactional(readOnly = true)
    public OutboundDocumentDto getById(UUID documentId, AuthPrincipal actor) {
        DocumentEntity d = loadOutbound(documentId);
        List<DocumentVersionEntity> vs = versions.findByDocumentIdOrderByVersionNumberAsc(documentId);
        List<ApprovalEntity> aps = approvals.findByDocumentIdOrderByDecidedAtAsc(documentId);
        List<DocumentFileEntity> files = filesOfLatestVersion(documentId, vs);
        return toDto(d, files, vs, aps);
    }

    @Transactional(readOnly = true)
    public Page<OutboundDocumentDto> list(int page, int size, AuthPrincipal actor) {
        // Phase 4: đơn giản — list theo direction OUTBOUND, không filter scope chi tiết.
        // Phase 5 thêm scope theo INBOUND-style (READ_OWN/DEPT/ALL).
        Page<DocumentEntity> docs = documents.search(
                DocumentDirection.OUTBOUND,
                null, null, null, null, null,
                null, null, null,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100)));
        return docs.map(d -> {
            List<DocumentVersionEntity> vs =
                    versions.findByDocumentIdOrderByVersionNumberAsc(d.getId());
            return toDto(d, filesOfLatestVersion(d.getId(), vs), vs, List.of());
        });
    }

    // ---------- helpers ----------

    private DocumentEntity loadOutbound(UUID id) {
        return documents.findById(id)
                .filter(x -> x.getDirection() == DocumentDirection.OUTBOUND)
                .orElseThrow(() -> new BusinessException(
                        "DOCUMENT_NOT_FOUND", "Không tìm thấy công văn đi."));
    }

    private DocumentVersionEntity createNewVersion(
            DocumentEntity d, UUID parentVersionId, AuthPrincipal actor) {
        Integer maxNum = versions.maxVersionNumber(d.getId());
        int next = (maxNum == null ? 0 : maxNum) + 1;
        DocumentVersionEntity v = new DocumentVersionEntity();
        v.setId(UUID.randomUUID());
        v.setDocumentId(d.getId());
        v.setVersionNumber(next);
        v.setParentVersionId(parentVersionId);
        v.setVersionStatus(VersionStatus.DRAFT);
        v.setContentSnapshot(snapshotOf(d));
        v.setCreatedAt(OffsetDateTime.now());
        v.setCreatedBy(actor == null ? null : actor.userId());
        return versions.save(v);
    }

    /** Snapshot JSON của metadata document (KHÔNG bao gồm files để giữ snapshot ổn định). */
    private String snapshotOf(DocumentEntity d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("documentTypeId", str(d.getDocumentTypeId()));
        map.put("confidentialityLevelId", str(d.getConfidentialityLevelId()));
        map.put("priorityLevelId", str(d.getPriorityLevelId()));
        map.put("subject", d.getSubject());
        map.put("summary", d.getSummary());
        map.put("organizationId", str(d.getOrganizationId()));
        map.put("departmentId", str(d.getDepartmentId()));
        map.put("dueDate", d.getDueDate() == null ? null : d.getDueDate().toString());
        try {
            return json.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("Không serialize được snapshot", e);
        }
    }

    private static String str(UUID id) {
        return id == null ? null : id.toString();
    }

    private List<DocumentFileEntity> uploadFiles(
            UUID documentId, UUID versionId, List<UploadedFile> uploads, AuthPrincipal actor) {
        String keyPrefix =
                "outbound/"
                        + LocalDate.now(ZONE_VN).getYear()
                        + "/"
                        + String.format("%02d", LocalDate.now(ZONE_VN).getMonthValue())
                        + "/"
                        + documentId;
        List<DocumentFileEntity> result = new ArrayList<>();
        for (UploadedFile uf : uploads) {
            StoredObject stored = minio.upload(keyPrefix, uf.filename(), uf.content(), uf.mimeType());
            DocumentFileEntity fe = new DocumentFileEntity();
            fe.setId(UUID.randomUUID());
            fe.setDocumentId(documentId);
            fe.setVersionId(versionId);
            fe.setFileRole(DocumentFileRole.DRAFT_PDF);
            fe.setFileName(uf.filename());
            fe.setMimeType(uf.mimeType());
            fe.setSizeBytes(stored.sizeBytes());
            fe.setSha256(stored.sha256());
            fe.setStorageKey(stored.storageKey());
            fe.setUploadedBy(actor == null ? null : actor.userId());
            fe.setUploadedAt(OffsetDateTime.now());
            result.add(docFiles.save(fe));
        }
        return result;
    }

    /** Copy DB record các file của version cũ sang version mới (cùng storage_key — không đụng MinIO). */
    private void cloneFilesToNewVersion(
            UUID documentId, UUID prevVersionId, UUID newVersionId, AuthPrincipal actor) {
        List<DocumentFileEntity> prevFiles =
                docFiles.findByDocumentIdOrderByUploadedAtAsc(documentId).stream()
                        .filter(f -> prevVersionId.equals(f.getVersionId()))
                        .toList();
        for (DocumentFileEntity old : prevFiles) {
            DocumentFileEntity clone = new DocumentFileEntity();
            clone.setId(UUID.randomUUID());
            clone.setDocumentId(documentId);
            clone.setVersionId(newVersionId);
            clone.setFileRole(old.getFileRole());
            clone.setFileName(old.getFileName());
            clone.setMimeType(old.getMimeType());
            clone.setSizeBytes(old.getSizeBytes());
            clone.setSha256(old.getSha256());
            // Same storage_key... không được vì UNIQUE. Append "/v{newVersion}" để khác key.
            clone.setStorageKey(old.getStorageKey() + "#v=" + newVersionId);
            clone.setUploadedBy(actor == null ? null : actor.userId());
            clone.setUploadedAt(OffsetDateTime.now());
            docFiles.save(clone);
        }
    }

    private void recordApproval(
            DocumentEntity d,
            DocumentVersionEntity v,
            ApprovalLevel level,
            ApprovalDecisionRequest req,
            AuthPrincipal actor) {
        ApprovalEntity a = new ApprovalEntity();
        a.setId(UUID.randomUUID());
        a.setDocumentId(d.getId());
        a.setVersionId(v.getId());
        a.setApprovalLevel(level);
        a.setDecision(req.decision());
        a.setComment(req.comment());
        a.setDecidedBy(actor == null ? null : actor.userId());
        a.setDecidedAt(OffsetDateTime.now());
        approvals.save(a);
    }

    private List<DocumentFileEntity> filesOfLatestVersion(
            UUID documentId, List<DocumentVersionEntity> vs) {
        if (vs.isEmpty()) return List.of();
        UUID latestId = vs.get(vs.size() - 1).getId();
        return docFiles.findByDocumentIdOrderByUploadedAtAsc(documentId).stream()
                .filter(f -> latestId.equals(f.getVersionId()))
                .toList();
    }

    private OutboundDocumentDto toDto(
            DocumentEntity d,
            List<DocumentFileEntity> files,
            List<DocumentVersionEntity> vs,
            List<ApprovalEntity> aps) {
        List<DocumentFileDto> fileDtos = files.stream()
                .map(f -> new DocumentFileDto(
                        f.getId(), f.getDocumentId(), f.getFileRole(),
                        f.getFileName(), f.getMimeType(), f.getSizeBytes(),
                        f.getSha256(), f.getUploadedAt()))
                .toList();
        List<DocumentVersionDto> versionDtos = vs.stream()
                .map(v -> new DocumentVersionDto(
                        v.getId(), v.getDocumentId(), v.getVersionNumber(),
                        v.getParentVersionId(), v.getVersionStatus(),
                        v.getHashSha256(), v.getContentSnapshot(),
                        v.getCreatedAt(), v.getCreatedBy()))
                .toList();
        List<ApprovalDto> approvalDtos = aps.stream()
                .map(a -> new ApprovalDto(
                        a.getId(), a.getDocumentId(), a.getVersionId(),
                        a.getApprovalLevel(), a.getDecision(),
                        a.getComment(), a.getDecidedBy(), a.getDecidedAt()))
                .toList();
        return new OutboundDocumentDto(
                d.getId(),
                d.getDocumentTypeId(),
                d.getConfidentialityLevelId(),
                d.getPriorityLevelId(),
                d.getSubject(),
                d.getSummary(),
                d.getStatus(),
                d.getApprovedVersionId(),
                d.getBookId(),
                d.getBookYear(),
                d.getBookNumber(),
                d.getIssuedDate(),
                d.getOrganizationId(),
                d.getDepartmentId(),
                d.getDueDate(),
                d.isRecalled(),
                d.getCreatedAt(),
                d.getCreatedBy(),
                fileDtos,
                versionDtos,
                approvalDtos);
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }
}
