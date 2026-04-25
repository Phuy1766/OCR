package vn.edu.congvan.outbound.entity;

/** Trạng thái 1 phiên bản trong document_versions. */
public enum VersionStatus {
    /** Đang sửa. */
    DRAFT,
    /** Đã gửi duyệt. */
    SUBMITTED,
    /** Đã được duyệt cuối — IMMUTABLE (BR-07). */
    APPROVED,
    /** Đã có version mới hơn thay thế. */
    SUPERSEDED,
    /** Bị từ chối (đã có version mới sau đó). */
    REJECTED
}
