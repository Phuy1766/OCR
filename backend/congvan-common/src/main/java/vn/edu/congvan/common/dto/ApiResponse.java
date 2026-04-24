package vn.edu.congvan.common.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Wrapper chuẩn cho mọi response REST API.
 *
 * @param success {@code true} nếu request xử lý thành công
 * @param data    payload trả về (null khi lỗi)
 * @param errors  danh sách lỗi chi tiết (null khi thành công)
 * @param meta    metadata bổ sung (phân trang, tracing)
 * @param timestamp thời điểm server trả response (theo giờ Asia/Ho_Chi_Minh)
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        List<ApiError> errors,
        Meta meta,
        OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data, Meta meta) {
        return new ApiResponse<>(true, data, null, meta, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> fail(List<ApiError> errors) {
        return new ApiResponse<>(false, null, errors, null, OffsetDateTime.now());
    }

    public record ApiError(String code, String field, String message) {}

    public record Meta(Integer page, Integer size, Long totalElements, Integer totalPages) {}
}
