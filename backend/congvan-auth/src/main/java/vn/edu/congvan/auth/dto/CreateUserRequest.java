package vn.edu.congvan.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank
                @Size(min = 3, max = 100)
                @Pattern(
                        regexp = "^[a-z0-9][a-z0-9._-]*$",
                        message =
                                "Tên đăng nhập chỉ được chứa chữ thường, số, dấu ., _, -"
                                        + " và bắt đầu bằng chữ/số.")
                String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 2, max = 255) String fullName,
        @Size(max = 30) String phone,
        UUID organizationId,
        UUID departmentId,
        @Size(max = 255) String positionTitle,
        @NotEmpty Set<String> roleCodes,
        @NotBlank
                @Size(min = 10, max = 200, message = "Mật khẩu phải từ 10–200 ký tự.")
                String initialPassword) {}
