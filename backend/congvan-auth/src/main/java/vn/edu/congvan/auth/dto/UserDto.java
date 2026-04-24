package vn.edu.congvan.auth.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String email,
        String fullName,
        String phone,
        UUID organizationId,
        UUID departmentId,
        String positionTitle,
        boolean active,
        boolean locked,
        boolean mustChangePassword,
        OffsetDateTime lastLoginAt,
        Set<String> roles,
        Set<String> permissions) {}
