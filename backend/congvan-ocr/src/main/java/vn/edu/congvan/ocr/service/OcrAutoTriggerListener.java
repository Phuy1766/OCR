package vn.edu.congvan.ocr.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.edu.congvan.common.event.InboundDocumentRegisteredEvent;
import vn.edu.congvan.ocr.entity.OcrJobEntity;

/**
 * Subscribe sự kiện {@link InboundDocumentRegisteredEvent} từ Inbound module.
 * Sau khi register transaction commit, tự động tạo OCR job cho file đầu tiên
 * có MIME thuộc PDF/JPEG/PNG và trigger {@code processJob} async.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrAutoTriggerListener {

    private static final Set<String> OCR_SUPPORTED_MIME = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    private final OcrService ocrService;

    @Value("${app.ocr.auto-trigger:true}")
    private boolean autoTrigger;

    /**
     * Sau khi register transaction commit:
     *   1. submit() — mở tx mới, save job PENDING, commit
     *   2. trigger processJob async — dispatch sang executor task-N
     *
     * <p>Bản thân handler được đánh dấu {@code @Async} để toàn bộ flow chạy
     * trên thread riêng, không block HTTP response của register. Bên trong
     * gọi {@code processJob} sync (REQUIRES_NEW tự lo session) — vì đã ở thread
     * async rồi không cần dispatch tiếp.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInboundDocumentRegistered(InboundDocumentRegisteredEvent event) {
        if (!autoTrigger) {
            log.debug("OCR auto-trigger disabled, skip document {}", event.documentId());
            return;
        }
        var supportedFile = event.files().stream()
                .filter(f -> OCR_SUPPORTED_MIME.contains(f.mimeType()))
                .findFirst()
                .orElse(null);
        if (supportedFile == null) {
            log.debug("Document {} không có file hỗ trợ OCR, skip", event.documentId());
            return;
        }
        OcrJobEntity job = ocrService.submit(event.documentId(), supportedFile.fileId(), null);
        // Sync: cùng async thread, submit's tx đã commit, fetch trong processJob
        // sẽ thấy row. Không dùng @Async trên processJob nữa để tránh race.
        ocrService.processJob(job.getId());
    }
}
