package vn.edu.congvan.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateDepartmentRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(min = 2, max = 50) String code,
        @NotBlank @Size(min = 2, max = 255) String name,
        UUID parentId,
        UUID headUserId) {}
