package vn.edu.congvan.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record UpdateUserRequest(
        @Email String email,
        @Size(min = 2, max = 255) String fullName,
        @Size(max = 30) String phone,
        UUID organizationId,
        UUID departmentId,
        @Size(max = 255) String positionTitle,
        Boolean active,
        Set<String> roleCodes) {}
