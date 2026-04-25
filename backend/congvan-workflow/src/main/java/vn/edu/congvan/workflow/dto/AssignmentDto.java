package vn.edu.congvan.workflow.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.congvan.workflow.entity.AssignmentStatus;

public record AssignmentDto(
        UUID id,
        UUID documentId,
        UUID workflowId,
        UUID assignedToUserId,
        UUID assignedToDeptId,
        UUID assignedBy,
        OffsetDateTime assignedAt,
        LocalDate dueDate,
        AssignmentStatus status,
        String note,
        OffsetDateTime completedAt,
        UUID completedBy,
        String resultSummary) {}
