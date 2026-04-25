package vn.edu.congvan.common.event;

import java.util.List;
import java.util.UUID;

/**
 * Event nội bộ phát sau khi {@code InboundDocumentService.register()} commit.
 * OCR module subscribe để tự động trigger OCR job cho file đầu tiên có mime
 * thuộc PDF/JPEG/PNG. Tách qua event để tránh cycle dependency Inbound→OCR.
 */
public record InboundDocumentRegisteredEvent(
        UUID documentId, UUID createdBy, List<FileInfo> files) {

    public record FileInfo(UUID fileId, String mimeType) {}
}
