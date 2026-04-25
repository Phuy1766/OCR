package vn.edu.congvan.workflow.entity;

/** Trạng thái workflow của 1 VB. */
public enum WorkflowState {
    /** Vừa khởi tạo (sau register). */
    INITIAL,
    /** Đã có assignment ACTIVE. */
    ASSIGNED,
    /** Chuyên viên đang xử lý. */
    IN_PROGRESS,
    /** Đã hoàn tất. */
    COMPLETED,
    /** Đóng workflow (đã lưu trữ hoặc thu hồi). */
    CLOSED,
    /** Hủy workflow. */
    CANCELLED
}
