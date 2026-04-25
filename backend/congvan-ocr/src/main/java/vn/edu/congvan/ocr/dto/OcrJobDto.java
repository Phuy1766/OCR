package vn.edu.congvan.ocr.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import vn.edu.congvan.ocr.entity.OcrJobStatus;

public record OcrJobDto(
        UUID jobId,
        UUID documentId,
        UUID fileId,
        OcrJobStatus status,
        int retryCount,
        String errorMessage,
        OffsetDateTime enqueuedAt,
        OffsetDateTime completedAt,
        OcrResultDto result) {

    public record OcrResultDto(
            UUID id,
            String rawText,
            BigDecimal confidenceAvg,
            Integer processingMs,
            String engineVersion,
            Integer pageCount,
            boolean accepted,
            OffsetDateTime acceptedAt,
            UUID acceptedBy,
            List<ExtractedFieldDto> fields) {}

    public record ExtractedFieldDto(
            UUID id,
            String fieldName,
            String fieldValue,
            BigDecimal confidence,
            String bbox,
            Integer pageNumber) {}
}
