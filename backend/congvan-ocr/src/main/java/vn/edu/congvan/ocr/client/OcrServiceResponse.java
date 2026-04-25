package vn.edu.congvan.ocr.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

/** Response từ FastAPI OCR service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OcrServiceResponse(
        String rawText,
        BigDecimal confidenceAvg,
        Integer processingMs,
        Integer pageCount,
        String engineVersion,
        List<ExtractedField> fields) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtractedField(
            String fieldName,
            String fieldValue,
            BigDecimal confidence,
            BoundingBox bbox,
            Integer pageNumber) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BoundingBox(int x, int y, int w, int h) {}
}
