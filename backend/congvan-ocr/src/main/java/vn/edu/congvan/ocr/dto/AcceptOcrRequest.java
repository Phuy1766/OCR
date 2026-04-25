package vn.edu.congvan.ocr.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request chấp nhận kết quả OCR — văn thư có thể override các field
 * trước khi áp vào documents.
 */
public record AcceptOcrRequest(
        @Size(max = 100) String externalReferenceNumber,
        @Size(max = 500) String externalIssuer,
        LocalDate externalIssuedDate,
        @Size(min = 5, max = 1000) String subject,
        @Size(max = 5000) String summary) {}
