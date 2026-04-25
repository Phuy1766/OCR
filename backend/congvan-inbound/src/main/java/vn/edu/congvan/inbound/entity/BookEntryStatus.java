package vn.edu.congvan.inbound.entity;

/**
 * Trạng thái bản ghi trong sổ đăng ký.
 * RESERVED — đã giữ số nhưng chưa chính thức
 * OFFICIAL — đã đóng dấu chính thức trong sổ
 * CANCELLED — hủy (số không tái sử dụng — vẫn xuất hiện trong in sổ)
 */
public enum BookEntryStatus {
    RESERVED,
    OFFICIAL,
    CANCELLED
}
