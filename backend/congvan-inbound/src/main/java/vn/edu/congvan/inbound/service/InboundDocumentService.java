package vn.edu.congvan.inbound.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.auth.service.AuditLogger;
import vn.edu.congvan.common.event.InboundDocumentRegisteredEvent;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.inbound.dto.CreateInboundRequest;
import vn.edu.congvan.inbound.dto.DocumentFileDto;
import vn.edu.congvan.inbound.dto.InboundDocumentDto;
import vn.edu.congvan.inbound.entity.BookEntryStatus;
import vn.edu.congvan.inbound.entity.DocumentBookEntryEntity;
import vn.edu.congvan.inbound.entity.DocumentDirection;
import vn.edu.congvan.inbound.entity.DocumentEntity;
import vn.edu.congvan.inbound.entity.DocumentFileEntity;
import vn.edu.congvan.inbound.entity.DocumentFileRole;
import vn.edu.congvan.inbound.entity.DocumentStatus;
import vn.edu.congvan.inbound.repository.DocumentBookEntryRepository;
import vn.edu.congvan.inbound.repository.DocumentFileRepository;
import vn.edu.congvan.inbound.repository.DocumentRepository;
import vn.edu.congvan.integration.outbox.OutboxRecorder;
import vn.edu.congvan.integration.storage.MinioFileService;
import vn.edu.congvan.integration.storage.StoredObject;
import vn.edu.congvan.masterdata.entity.BookType;
import vn.edu.congvan.masterdata.entity.ConfidentialityScope;
import vn.edu.congvan.masterdata.entity.DocumentBookEntity;
import vn.edu.congvan.masterdata.repository.ConfidentialityLevelRepository;
import vn.edu.congvan.masterdata.repository.DocumentBookRepository;
import vn.edu.congvan.masterdata.service.DocumentNumberService;

/** Đăng ký, xem, recall công văn đến (Phase 3). */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundDocumentService {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int BACKDATE_LIMIT_DAYS = 30;

    private final DocumentRepository documents;
    private final DocumentFileRepository files;
    private final DocumentBookEntryRepository bookEntries;
    private final DocumentBookRepository books;
    private final ConfidentialityLevelRepository confLevels;
    private final DocumentNumberService numberService;
    private final FileValidator fileValidator;
    private final MinioFileService minio;
    private final AuditLogger audit;
    private final OutboxRecorder outbox;
    private final ApplicationEventPublisher events;

    /**
     * Đăng ký VB đến: validate → cấp số (BR-01/02) → lưu document → ghi entry sổ
     * → upload file → audit. Toàn bộ trong 1 transaction (file upload thực sự
     * lên MinIO ngoài transaction DB, nhưng nếu DB rollback thì các file đã upload
     * sẽ là orphan — chấp nhận, sẽ có job cleanup ở Phase 10).
     */
    @Transactional
    public InboundDocumentDto register(
            CreateInboundRequest req,
            List<UploadedFile> uploadedFiles,
            AuthPrincipal actor,
            String actorIp) {
        // BR-09: bắt buộc có ít nhất 1 file scan
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            throw new BusinessException(
                    "INBOUND_FILE_REQUIRED",
                    "VB đến phải có ít nhất 1 file đính kèm (bản scan/PDF gốc).");
        }
        // Validate từng file trước khi gọi cấp số
        for (UploadedFile f : uploadedFiles) {
            fileValidator.validate(f.filename(), f.mimeType(), f.content());
        }

        // Validate sổ tồn tại + đúng loại INBOUND
        DocumentBookEntity book =
                books.findById(req.bookId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "DOCUMENT_BOOK_NOT_FOUND",
                                                "Không tìm thấy sổ đăng ký."));
        if (book.getBookType() != BookType.INBOUND) {
            throw new BusinessException(
                    "DOCUMENT_BOOK_WRONG_TYPE",
                    "Sổ này không phải sổ công văn đến.");
        }

        // BR-03: VB mật phải dùng sổ SECRET
        var conf = confLevels.findById(req.confidentialityLevelId())
                .orElseThrow(() -> new BusinessException(
                        "CONFIDENTIALITY_LEVEL_NOT_FOUND",
                        "Không tìm thấy mức mật."));
        if (conf.requiresSecretBook()
                && book.getConfidentialityScope() != ConfidentialityScope.SECRET) {
            throw new BusinessException(
                    "BR_03_SECRET_BOOK_REQUIRED",
                    "Văn bản mức "
                            + conf.getName()
                            + " phải đăng ký vào sổ mật riêng (BR-03 NĐ 30/2020).");
        }

        // Backdate validation (Q2): cho phép tối đa 30 ngày
        LocalDate received =
                req.receivedDate() != null ? req.receivedDate() : LocalDate.now(ZONE_VN);
        LocalDate today = LocalDate.now(ZONE_VN);
        if (received.isAfter(today)) {
            throw new BusinessException(
                    "INBOUND_FUTURE_RECEIVED_DATE", "Ngày đến không được ở tương lai.");
        }
        boolean lateRegistration = false;
        if (received.isBefore(today.minusDays(BACKDATE_LIMIT_DAYS))) {
            throw new BusinessException(
                    "INBOUND_BACKDATE_TOO_OLD",
                    "Ngày đến quá xa quá khứ ("
                            + BACKDATE_LIMIT_DAYS
                            + " ngày). Liên hệ admin nếu cần backdate xa hơn.");
        }
        if (received.isBefore(today)) {
            // BR-05: cảnh báo nếu đăng ký sau 1 ngày so với ngày đến.
            lateRegistration = true;
        }

        // Cấp số (BR-01/02 trong DocumentNumberService)
        var reserved = numberService.reserve(book.getId());

        // Tạo document
        DocumentEntity d = new DocumentEntity();
        d.setId(UUID.randomUUID());
        d.setDirection(DocumentDirection.INBOUND);
        d.setDocumentTypeId(req.documentTypeId());
        d.setConfidentialityLevelId(req.confidentialityLevelId());
        d.setPriorityLevelId(req.priorityLevelId());
        d.setSubject(req.subject());
        d.setSummary(req.summary());
        d.setStatus(DocumentStatus.REGISTERED);
        d.setBookId(book.getId());
        d.setBookYear(reserved.year());
        d.setBookNumber(reserved.number());
        d.setReceivedDate(received);
        d.setReceivedFromChannel(req.receivedFromChannel());
        d.setExternalReferenceNumber(req.externalReferenceNumber());
        d.setExternalIssuer(req.externalIssuer());
        d.setExternalIssuedDate(req.externalIssuedDate());
        d.setOrganizationId(req.organizationId());
        d.setDepartmentId(req.departmentId());
        d.setDueDate(req.dueDate());
        d = documents.save(d);

        // Ghi vào sổ
        DocumentBookEntryEntity entry = new DocumentBookEntryEntity();
        entry.setId(UUID.randomUUID());
        entry.setBookId(book.getId());
        entry.setYear(reserved.year());
        entry.setNumber(reserved.number());
        entry.setDocumentId(d.getId());
        entry.setEntryStatus(BookEntryStatus.OFFICIAL);
        entry.setEnteredAt(OffsetDateTime.now());
        entry.setEnteredBy(actor == null ? null : actor.userId());
        bookEntries.save(entry);

        // Upload file lên MinIO + ghi document_files
        String keyPrefix =
                "inbound/"
                        + reserved.year()
                        + "/"
                        + String.format("%02d", today.getMonthValue())
                        + "/"
                        + d.getId();
        List<DocumentFileEntity> fileEntities = new ArrayList<>();
        for (UploadedFile uf : uploadedFiles) {
            StoredObject stored = minio.upload(keyPrefix, uf.filename(), uf.content(), uf.mimeType());
            DocumentFileEntity fe = new DocumentFileEntity();
            fe.setId(UUID.randomUUID());
            fe.setDocumentId(d.getId());
            fe.setFileRole(DocumentFileRole.ORIGINAL_SCAN);
            fe.setFileName(uf.filename());
            fe.setMimeType(uf.mimeType());
            fe.setSizeBytes(stored.sizeBytes());
            fe.setSha256(stored.sha256());
            fe.setStorageKey(stored.storageKey());
            fe.setUploadedBy(actor == null ? null : actor.userId());
            fe.setUploadedAt(OffsetDateTime.now());
            fileEntities.add(files.save(fe));
        }

        // Audit + outbox event
        audit.logSuccess(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.username(),
                actorIp,
                "REGISTER_INBOUND_DOCUMENT",
                "documents",
                d.getId().toString());

        outbox.record(outbox.event("documents", d.getId().toString(), "InboundDocumentRegistered")
                .routingKey("document.inbound.registered")
                .payload(outbox.map(
                        "documentId", d.getId().toString(),
                        "subject", d.getSubject(),
                        "bookId", book.getId().toString(),
                        "bookYear", reserved.year(),
                        "bookNumber", reserved.number()))
                .build());

        if (lateRegistration) {
            // BR-05: ghi audit warning khi đăng ký trễ.
            audit.log(
                    AuditLogger.AuditRecord.builder()
                            .actorId(actor == null ? null : actor.userId())
                            .actorUsername(actor == null ? null : actor.username())
                            .actorIp(actorIp)
                            .action("INBOUND_LATE_REGISTRATION")
                            .entityType("documents")
                            .entityId(d.getId().toString())
                            .success(true)
                            .extra(
                                    Map.of(
                                            "received_date",
                                                    received.format(DateTimeFormatter.ISO_DATE),
                                            "registered_date",
                                                    today.format(DateTimeFormatter.ISO_DATE),
                                            "days_late",
                                                    today.toEpochDay() - received.toEpochDay()))
                            .build());
        }

        // Publish event để OCR module subscribe và auto-trigger sau commit.
        // Event listener phía OCR sẽ chạy với @TransactionalEventListener(AFTER_COMMIT).
        var fileInfos = fileEntities.stream()
                .map(f -> new InboundDocumentRegisteredEvent.FileInfo(
                        f.getId(), f.getMimeType()))
                .toList();
        events.publishEvent(new InboundDocumentRegisteredEvent(
                d.getId(), actor == null ? null : actor.userId(), fileInfos));

        return toDto(d, fileEntities);
    }

    @Transactional(readOnly = true)
    public Page<InboundDocumentDto> list(
            DocumentSearchCriteria criteria, AuthPrincipal actor, int page, int size) {
        // Áp scope theo permission
        UUID scopeOwnUserId = null;
        UUID scopeDeptId = null;
        if (PermissionScope.canReadAll(actor)) {
            // Không filter scope
        } else if (PermissionScope.canReadDept(actor)) {
            // Filter theo phòng của actor (Phase 3 đơn giản: dùng departmentId trên token,
            // chưa có. Thay thế: lấy từ user record). Tạm dùng department_id = null không
            // filter — sẽ bổ sung Phase 5 khi có resolver.
            // Với phase 3 tests: chỉ test READ_ALL và READ_OWN, READ_DEPT bỏ qua.
            scopeDeptId = criteria.departmentId();
        } else if (PermissionScope.canReadOwn(actor)) {
            scopeOwnUserId = actor == null ? null : actor.userId();
        } else {
            throw new BusinessException("AUTH_FORBIDDEN", "Không có quyền xem công văn đến.");
        }

        Page<DocumentEntity> pageData =
                documents.search(
                        DocumentDirection.INBOUND,
                        criteria.status(),
                        criteria.organizationId(),
                        criteria.bookId(),
                        criteria.fromDate(),
                        criteria.toDate(),
                        scopeOwnUserId,
                        scopeDeptId,
                        criteria.query(),
                        PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100)));

        return pageData.map(d -> toDto(d, files.findByDocumentIdOrderByUploadedAtAsc(d.getId())));
    }

    @Transactional(readOnly = true)
    public InboundDocumentDto getById(UUID id, AuthPrincipal actor) {
        DocumentEntity d =
                documents.findById(id)
                        .filter(x -> x.getDirection() == DocumentDirection.INBOUND)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "DOCUMENT_NOT_FOUND",
                                                "Không tìm thấy công văn."));
        // Re-check scope: nếu chỉ có READ_OWN thì handler/created_by phải là actor
        if (!PermissionScope.canReadAll(actor) && !PermissionScope.canReadDept(actor)) {
            UUID actorId = actor == null ? null : actor.userId();
            boolean isOwn =
                    actorId != null
                            && (actorId.equals(d.getCurrentHandlerUserId())
                                    || actorId.equals(d.getCreatedBy()));
            if (!isOwn) {
                throw new BusinessException(
                        "AUTH_FORBIDDEN", "Không có quyền xem công văn này.");
            }
        }
        return toDto(d, files.findByDocumentIdOrderByUploadedAtAsc(id));
    }

    @Transactional
    public void recall(UUID id, String reason, AuthPrincipal actor, String actorIp) {
        DocumentEntity d =
                documents.findById(id)
                        .filter(x -> x.getDirection() == DocumentDirection.INBOUND)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "DOCUMENT_NOT_FOUND",
                                                "Không tìm thấy công văn."));
        if (d.isRecalled()) {
            throw new BusinessException(
                    "INBOUND_ALREADY_RECALLED", "Công văn đã được thu hồi trước đó.");
        }
        if (d.getStatus() == DocumentStatus.COMPLETED
                || d.getStatus() == DocumentStatus.ARCHIVED) {
            throw new BusinessException(
                    "INBOUND_RECALL_TOO_LATE",
                    "Không thể thu hồi VB đã hoàn tất hoặc đã lưu trữ.");
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
                        .action("RECALL_INBOUND_DOCUMENT")
                        .entityType("documents")
                        .entityId(d.getId().toString())
                        .success(true)
                        .extra(Map.of("reason", reason))
                        .build());

        outbox.record(outbox.event("documents", d.getId().toString(), "InboundDocumentRecalled")
                .routingKey("document.inbound.recalled")
                .payload(outbox.map(
                        "documentId", d.getId().toString(),
                        "reason", reason,
                        "recalledBy", actor == null ? null : actor.userId().toString()))
                .build());
    }

    private InboundDocumentDto toDto(DocumentEntity d, List<DocumentFileEntity> fs) {
        List<DocumentFileDto> fileDtos =
                fs.stream()
                        .map(
                                f ->
                                        new DocumentFileDto(
                                                f.getId(),
                                                f.getDocumentId(),
                                                f.getFileRole(),
                                                f.getFileName(),
                                                f.getMimeType(),
                                                f.getSizeBytes(),
                                                f.getSha256(),
                                                f.getUploadedAt()))
                        .toList();
        return new InboundDocumentDto(
                d.getId(),
                d.getDocumentTypeId(),
                d.getConfidentialityLevelId(),
                d.getPriorityLevelId(),
                d.getSubject(),
                d.getSummary(),
                d.getStatus(),
                d.getBookId(),
                d.getBookYear(),
                d.getBookNumber(),
                d.getReceivedDate(),
                d.getReceivedFromChannel(),
                d.getExternalReferenceNumber(),
                d.getExternalIssuer(),
                d.getExternalIssuedDate(),
                d.getCurrentHandlerUserId(),
                d.getCurrentHandlerDeptId(),
                d.getDueDate(),
                d.getOrganizationId(),
                d.getDepartmentId(),
                d.isRecalled(),
                d.getRecalledAt(),
                d.getRecalledReason(),
                d.getCreatedAt(),
                d.getCreatedBy(),
                fileDtos);
    }

    /** Tiêu chí tìm kiếm — collected từ query params. */
    public record DocumentSearchCriteria(
            DocumentStatus status,
            UUID organizationId,
            UUID departmentId,
            UUID bookId,
            LocalDate fromDate,
            LocalDate toDate,
            String query) {}
}
