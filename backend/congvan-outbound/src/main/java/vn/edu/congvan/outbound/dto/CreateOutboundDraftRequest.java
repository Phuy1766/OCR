package vn.edu.congvan.outbound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Request tạo dự thảo VB đi. Files dự thảo trong phần multipart riêng. */
public record CreateOutboundDraftRequest(
        @NotNull UUID documentTypeId,
        @NotNull UUID confidentialityLevelId,
        @NotNull UUID priorityLevelId,
        @NotBlank @Size(min = 5, max = 1000) String subject,
        @Size(max = 5000) String summary,
        @NotNull UUID organizationId,
        UUID departmentId,
        LocalDate dueDate) {}
