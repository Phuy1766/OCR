package vn.edu.congvan.workflow.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.congvan.workflow.entity.NotificationType;

public record NotificationDto(
        UUID id,
        NotificationType type,
        String title,
        String body,
        String entityType,
        String entityId,
        String metadata,
        OffsetDateTime createdAt,
        OffsetDateTime readAt) {}
