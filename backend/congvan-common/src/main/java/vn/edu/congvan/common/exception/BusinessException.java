package vn.edu.congvan.common.exception;

/**
 * Ngoại lệ nghiệp vụ — mã lỗi dạng chuỗi enum để frontend có thể i18n.
 * Không dùng cho lỗi validation field (ConstraintViolationException xử lý riêng).
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final Object[] args;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.args = new Object[0];
    }

    public BusinessException(String code, String message, Object... args) {
        super(message);
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}
