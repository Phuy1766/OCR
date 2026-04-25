package vn.edu.congvan.ocr.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.auth.service.AuditLogger;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.inbound.entity.DocumentEntity;
import vn.edu.congvan.inbound.entity.DocumentFileEntity;
import vn.edu.congvan.inbound.entity.DocumentStatus;
import vn.edu.congvan.inbound.repository.DocumentFileRepository;
import vn.edu.congvan.inbound.repository.DocumentRepository;
import vn.edu.congvan.integration.storage.MinioFileService;
import vn.edu.congvan.ocr.client.OcrClient;
import vn.edu.congvan.ocr.client.OcrServiceResponse;
import vn.edu.congvan.ocr.dto.AcceptOcrRequest;
import vn.edu.congvan.ocr.dto.OcrJobDto;
import vn.edu.congvan.ocr.dto.OcrJobDto.ExtractedFieldDto;
import vn.edu.congvan.ocr.dto.OcrJobDto.OcrResultDto;
import vn.edu.congvan.ocr.entity.OcrExtractedFieldEntity;
import vn.edu.congvan.ocr.entity.OcrJobEntity;
import vn.edu.congvan.ocr.entity.OcrJobStatus;
import vn.edu.congvan.ocr.entity.OcrResultEntity;
import vn.edu.congvan.ocr.repository.OcrExtractedFieldRepository;
import vn.edu.congvan.ocr.repository.OcrJobRepository;
import vn.edu.congvan.ocr.repository.OcrResultRepository;

/**
 * Quản lý OCR jobs cho VB đến.
 *
 * <p>Flow: {@link #submit(UUID, UUID, AuthPrincipal)} tạo job PENDING + trigger
 * {@link #processJob(UUID)} async. Worker gọi {@link OcrClient} đến FastAPI,
 * lưu result + extracted_fields. Văn thư xem rồi {@link #accept(UUID, ...)}
 * để áp metadata lên document.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private final OcrJobRepository jobs;
    private final OcrResultRepository results;
    private final OcrExtractedFieldRepository fields;
    private final DocumentRepository documents;
    private final DocumentFileRepository docFiles;
    private final MinioFileService minio;
    private final OcrClient ocrClient;
    private final AuditLogger audit;

    @Value("${app.ocr.auto-trigger:true}")
    private boolean autoTrigger;

    /** Tạo job PENDING + trigger async. Caller (vd Inbound register) gọi sau register tx. */
    @Transactional
    public OcrJobEntity submit(UUID documentId, UUID fileId, AuthPrincipal actor) {
        OcrJobEntity job = new OcrJobEntity();
        job.setId(UUID.randomUUID());
        job.setDocumentId(documentId);
        job.setFileId(fileId);
        job.setStatus(OcrJobStatus.PENDING);
        job.setEnqueuedAt(OffsetDateTime.now());
        job.setCreatedBy(actor == null ? null : actor.userId());
        job = jobs.save(job);

        // Cập nhật document status (nếu hiện tại REGISTERED → OCR_PENDING)
        documents.findById(documentId).ifPresent(d -> {
            if (d.getStatus() == DocumentStatus.REGISTERED) {
                d.setStatus(DocumentStatus.OCR_PENDING);
            }
        });

        log.info("OCR job created: {} for document {} file {}", job.getId(), documentId, fileId);
        return job;
    }

    /**
     * Chạy OCR. Mở transaction RIÊNG (REQUIRES_NEW) để không block transaction
     * caller; có thể gọi từ thread @Async (vd OcrAutoTriggerListener) hoặc
     * trực tiếp khi user trigger thủ công.
     * Failure không rethrow — chỉ ghi vào job.errorMessage.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processJob(UUID jobId) {
        OcrJobEntity job = jobs.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("OCR job {} không tồn tại — skip", jobId);
            return;
        }
        if (job.getStatus() != OcrJobStatus.PENDING) {
            log.debug("OCR job {} đã chuyển trạng thái {}, skip", jobId, job.getStatus());
            return;
        }

        job.setStatus(OcrJobStatus.PROCESSING);
        job.setStartedAt(OffsetDateTime.now());
        jobs.save(job);

        try {
            DocumentFileEntity file = docFiles.findById(job.getFileId())
                    .orElseThrow(() -> new BusinessException(
                            "OCR_FILE_NOT_FOUND", "File không tồn tại"));

            byte[] content = minio.download(file.getStorageKey());
            OcrServiceResponse response = ocrClient.process(
                    file.getFileName(), file.getMimeType(), content);

            persistResult(job, response);
            job.setStatus(OcrJobStatus.COMPLETED);
            job.setCompletedAt(OffsetDateTime.now());

            documents.findById(job.getDocumentId()).ifPresent(d -> {
                if (d.getStatus() == DocumentStatus.OCR_PENDING) {
                    d.setStatus(DocumentStatus.OCR_COMPLETED);
                }
            });

            audit.logSuccess(null, "ocr-worker", null, "OCR_COMPLETED",
                    "ocr_jobs", jobId.toString());
            log.info("OCR job {} completed in {}ms", jobId, response.processingMs());

        } catch (BusinessException be) {
            job.setStatus(determineFailureStatus(be.getCode()));
            job.setErrorMessage(be.getMessage());
            job.setCompletedAt(OffsetDateTime.now());
            log.warn("OCR job {} failed: {} ({})", jobId, be.getMessage(), be.getCode());
        } catch (Exception e) {
            job.setStatus(OcrJobStatus.FAILED);
            job.setErrorMessage(truncate(e.getMessage(), 5000));
            job.setCompletedAt(OffsetDateTime.now());
            log.warn("OCR job {} unexpected error: {}", jobId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public OcrJobDto getJobByDocument(UUID documentId) {
        OcrJobEntity job = jobs.findFirstByDocumentIdOrderByEnqueuedAtDesc(documentId)
                .orElse(null);
        if (job == null) return null;
        return toDto(job);
    }

    @Transactional
    public OcrJobDto accept(UUID jobId, AcceptOcrRequest req, AuthPrincipal actor, String actorIp) {
        OcrJobEntity job = jobs.findById(jobId)
                .orElseThrow(() -> new BusinessException(
                        "OCR_JOB_NOT_FOUND", "Không tìm thấy OCR job."));
        if (job.getStatus() != OcrJobStatus.COMPLETED) {
            throw new BusinessException(
                    "OCR_JOB_NOT_COMPLETED",
                    "Chỉ chấp nhận được kết quả khi job đã COMPLETED.");
        }
        OcrResultEntity result = results.findByJobId(jobId)
                .orElseThrow(() -> new BusinessException(
                        "OCR_RESULT_NOT_FOUND", "Không có kết quả OCR để chấp nhận."));
        result.setAccepted(true);
        result.setAcceptedAt(OffsetDateTime.now());
        result.setAcceptedBy(actor == null ? null : actor.userId());

        // Áp metadata vào document — ưu tiên giá trị user override, fallback OCR field
        DocumentEntity d = documents.findById(job.getDocumentId())
                .orElseThrow(() -> new BusinessException(
                        "DOCUMENT_NOT_FOUND", "Không tìm thấy công văn."));
        Map<String, OcrExtractedFieldEntity> ocrByName = fields
                .findByResultIdOrderByFieldNameAsc(result.getId()).stream()
                .collect(java.util.stream.Collectors.toMap(
                        OcrExtractedFieldEntity::getFieldName, x -> x));

        applyField(d::setExternalReferenceNumber, req.externalReferenceNumber(),
                ocrByName.get("external_reference_number"));
        applyField(d::setExternalIssuer, req.externalIssuer(),
                ocrByName.get("external_issuer"));
        applyDate(d::setExternalIssuedDate, req.externalIssuedDate(),
                ocrByName.get("external_issued_date"));
        applyField(d::setSubject, req.subject(), ocrByName.get("subject"));
        applyField(d::setSummary, req.summary(), ocrByName.get("summary"));

        // Document quay về REGISTERED (sau OCR_COMPLETED, sẵn sàng phân công)
        if (d.getStatus() == DocumentStatus.OCR_COMPLETED) {
            d.setStatus(DocumentStatus.REGISTERED);
        }

        audit.log(AuditLogger.AuditRecord.builder()
                .actorId(actor == null ? null : actor.userId())
                .actorUsername(actor == null ? null : actor.username())
                .actorIp(actorIp)
                .action("ACCEPT_OCR_RESULT")
                .entityType("ocr_results")
                .entityId(result.getId().toString())
                .success(true)
                .extra(Map.of("documentId", d.getId().toString()))
                .build());

        return toDto(job);
    }

    private void applyField(
            java.util.function.Consumer<String> setter,
            String userValue,
            OcrExtractedFieldEntity ocrField) {
        if (userValue != null && !userValue.isBlank()) {
            setter.accept(userValue);
        } else if (ocrField != null && ocrField.getFieldValue() != null) {
            setter.accept(ocrField.getFieldValue());
        }
    }

    private void applyDate(
            java.util.function.Consumer<LocalDate> setter,
            LocalDate userValue,
            OcrExtractedFieldEntity ocrField) {
        if (userValue != null) {
            setter.accept(userValue);
        } else if (ocrField != null && ocrField.getFieldValue() != null) {
            try {
                setter.accept(LocalDate.parse(
                        ocrField.getFieldValue(), DateTimeFormatter.ISO_DATE));
            } catch (Exception ignored) {
                // OCR date không parse được — bỏ qua, user phải tự nhập
            }
        }
    }

    private void persistResult(OcrJobEntity job, OcrServiceResponse response) {
        OcrResultEntity r = new OcrResultEntity();
        r.setId(UUID.randomUUID());
        r.setJobId(job.getId());
        r.setRawText(response.rawText());
        r.setConfidenceAvg(response.confidenceAvg());
        r.setProcessingMs(response.processingMs());
        r.setEngineVersion(response.engineVersion());
        r.setPageCount(response.pageCount());
        r.setAccepted(false);
        r.setCreatedAt(OffsetDateTime.now());
        r = results.save(r);

        if (response.fields() != null) {
            for (var f : response.fields()) {
                OcrExtractedFieldEntity fe = new OcrExtractedFieldEntity();
                fe.setId(UUID.randomUUID());
                fe.setResultId(r.getId());
                fe.setFieldName(f.fieldName());
                fe.setFieldValue(f.fieldValue());
                fe.setConfidence(f.confidence());
                fe.setPageNumber(f.pageNumber());
                fe.setCreatedAt(OffsetDateTime.now());
                fields.save(fe);
            }
        }
    }

    private OcrJobStatus determineFailureStatus(String code) {
        return switch (code) {
            case "OCR_SERVICE_ERROR" -> OcrJobStatus.SERVICE_UNAVAILABLE;
            default -> OcrJobStatus.FAILED;
        };
    }

    private OcrJobDto toDto(OcrJobEntity job) {
        OcrResultEntity result = results.findByJobId(job.getId()).orElse(null);
        OcrResultDto resultDto = null;
        if (result != null) {
            List<ExtractedFieldDto> fieldDtos = fields
                    .findByResultIdOrderByFieldNameAsc(result.getId()).stream()
                    .map(f -> new ExtractedFieldDto(
                            f.getId(), f.getFieldName(), f.getFieldValue(),
                            f.getConfidence(), f.getBbox(), f.getPageNumber()))
                    .toList();
            resultDto = new OcrResultDto(
                    result.getId(),
                    result.getRawText(),
                    result.getConfidenceAvg(),
                    result.getProcessingMs(),
                    result.getEngineVersion(),
                    result.getPageCount(),
                    result.isAccepted(),
                    result.getAcceptedAt(),
                    result.getAcceptedBy(),
                    fieldDtos);
        }
        return new OcrJobDto(
                job.getId(),
                job.getDocumentId(),
                job.getFileId(),
                job.getStatus(),
                job.getRetryCount(),
                job.getErrorMessage(),
                job.getEnqueuedAt(),
                job.getCompletedAt(),
                resultDto);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    @SuppressWarnings("unused")
    private static BigDecimal unused() {
        return null;
    }
}
