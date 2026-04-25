package vn.edu.congvan.outbound.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Mỗi update tạo version mới — fields null = giữ nguyên giá trị cũ. */
public record UpdateOutboundDraftRequest(
        UUID documentTypeId,
        UUID confidentialityLevelId,
        UUID priorityLevelId,
        @Size(min = 5, max = 1000) String subject,
        @Size(max = 5000) String summary,
        UUID departmentId,
        LocalDate dueDate) {}
