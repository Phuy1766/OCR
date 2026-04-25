package vn.edu.congvan.inbound.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.congvan.inbound.entity.DocumentFileRole;

public record DocumentFileDto(
        UUID id,
        UUID documentId,
        DocumentFileRole fileRole,
        String fileName,
        String mimeType,
        long sizeBytes,
        String sha256,
        OffsetDateTime uploadedAt) {}
