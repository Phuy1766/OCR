package vn.edu.congvan.workflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record AssignRequest(
        @NotNull UUID assignedToUserId,
        UUID assignedToDeptId,
        LocalDate dueDate,
        @Size(max = 2000) String note) {}
