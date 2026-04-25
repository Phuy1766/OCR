package vn.edu.congvan.inbound.entity;

/**
 * State machine của công văn (đến + đi).
 *
 * <p><b>Inbound flow (Phase 3 + Phase 5):</b><br>
 * RECEIVED → REGISTERED → OCR_PENDING → OCR_COMPLETED → ASSIGNED →
 * IN_PROGRESS → COMPLETED → ARCHIVED, có thể nhảy sang RECALLED bất kỳ lúc nào trước COMPLETED.
 *
 * <p>Phase 3 chỉ implement RECEIVED → REGISTERED → OCR_PENDING (chờ Phase 6).
 */
public enum DocumentStatus {
    /** Vừa upload, chưa cấp số. */
    RECEIVED,
    /** Đã đăng ký vào sổ, có số. */
    REGISTERED,
    /** Đang chờ OCR (Phase 6). */
    OCR_PENDING,
    /** OCR xong, người dùng có thể accept/reject (Phase 6). */
    OCR_COMPLETED,
    /** Đã phân công xử lý (Phase 5). */
    ASSIGNED,
    /** Người được giao đang xử lý. */
    IN_PROGRESS,
    /** Đã hoàn tất xử lý. */
    COMPLETED,
    /** Đã lưu trữ (BR-13/14). */
    ARCHIVED,
    /** Đã thu hồi (BR-11) — không xóa. */
    RECALLED,

    // ---- Outbound flow (Phase 4) ----
    DRAFT,
    PENDING_DEPT_APPROVAL,
    PENDING_LEADER_APPROVAL,
    APPROVED,
    PENDING_SIGN,
    SIGNED,
    ISSUED,
    SENT,
    REJECTED
}
