package vn.edu.congvan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Vui lòng nhập tên đăng nhập.")
                @Size(min = 3, max = 100, message = "Tên đăng nhập phải 3–100 ký tự.")
                String username,
        @NotBlank(message = "Vui lòng nhập mật khẩu.")
                @Size(min = 1, max = 200)
                String password) {}
