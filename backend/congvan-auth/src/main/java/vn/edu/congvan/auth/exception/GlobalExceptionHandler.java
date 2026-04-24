package vn.edu.congvan.auth.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.common.dto.ApiResponse.ApiError;
import vn.edu.congvan.common.exception.BusinessException;

/** Chuyển exception thành {@link ApiResponse} JSON chuẩn. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthException ex) {
        HttpStatus status =
                switch (ex.getCode()) {
                    case AuthException.ACCOUNT_LOCKED,
                            AuthException.ACCOUNT_INACTIVE -> HttpStatus.FORBIDDEN;
                    default -> HttpStatus.UNAUTHORIZED;
                };
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(List.of(new ApiError(ex.getCode(), null, ex.getMessage()))));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(List.of(new ApiError(ex.getCode(), null, ex.getMessage()))));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError> errors = new ArrayList<>();
        for (FieldError f : ex.getBindingResult().getFieldErrors()) {
            errors.add(new ApiError("VALIDATION_FAILED", f.getField(), f.getDefaultMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        List<ApiError> errors = new ArrayList<>();
        ex.getConstraintViolations()
                .forEach(
                        v ->
                                errors.add(
                                        new ApiError(
                                                "VALIDATION_FAILED",
                                                v.getPropertyPath().toString(),
                                                v.getMessage())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformed(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiResponse.fail(
                                List.of(
                                        new ApiError(
                                                "REQUEST_MALFORMED",
                                                null,
                                                "Nội dung request không đọc được."))));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        ApiResponse.fail(
                                List.of(
                                        new ApiError(
                                                "AUTH_FORBIDDEN",
                                                null,
                                                "Không đủ quyền thực hiện thao tác này."))));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiResponse.fail(
                                List.of(
                                        new ApiError(
                                                "INTERNAL_ERROR",
                                                null,
                                                "Đã xảy ra lỗi hệ thống. Vui lòng thử lại."))));
    }
}
