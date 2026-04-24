package vn.edu.congvan.auth.exception;

import vn.edu.congvan.common.exception.BusinessException;

/** Ngoại lệ xác thực — mã lỗi xác định để frontend i18n. */
public class AuthException extends BusinessException {

    public static final String INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String ACCOUNT_LOCKED = "AUTH_ACCOUNT_LOCKED";
    public static final String ACCOUNT_INACTIVE = "AUTH_ACCOUNT_INACTIVE";
    public static final String REFRESH_TOKEN_INVALID = "AUTH_REFRESH_INVALID";
    public static final String REFRESH_TOKEN_REUSED = "AUTH_REFRESH_REUSED";
    public static final String MUST_CHANGE_PASSWORD = "AUTH_MUST_CHANGE_PASSWORD";

    public AuthException(String code, String message) {
        super(code, message);
    }
}
