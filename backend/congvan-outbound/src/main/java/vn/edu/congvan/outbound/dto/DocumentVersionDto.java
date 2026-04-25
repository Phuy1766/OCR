package vn.edu.congvan.outbound.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.congvan.outbound.entity.VersionStatus;

public record DocumentVersionDto(
        UUID id,
        UUID documentId,
        int versionNumber,
        UUID parentVersionId,
        VersionStatus versionStatus,
        String hashSha256,
        String contentSnapshot,
        OffsetDateTime createdAt,
        UUID createdBy) {}
