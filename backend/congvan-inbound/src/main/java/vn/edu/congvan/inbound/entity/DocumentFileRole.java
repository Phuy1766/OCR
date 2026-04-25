package vn.edu.congvan.inbound.entity;

/** Vai trò file trong VB. */
public enum DocumentFileRole {
    /** Bản scan/PDF gốc của VB đến. BR-09: bắt buộc có ít nhất 1. */
    ORIGINAL_SCAN,
    /** Phụ lục đính kèm. */
    ATTACHMENT,
    /** Dự thảo PDF chưa ký (Phase 4). */
    DRAFT_PDF,
    /** Bản chính chưa ký số (sau khi duyệt cuối). */
    FINAL_PDF,
    /** Bản đã ký số (Phase 7). */
    SIGNED
}
