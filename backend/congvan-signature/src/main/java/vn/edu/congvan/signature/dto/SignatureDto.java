package vn.edu.congvan.signature.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.congvan.signature.entity.SignatureType;

public record SignatureDto(
        UUID id,
        UUID documentId,
        UUID versionId,
        UUID signedFileId,
        UUID certificateId,
        SignatureType signatureType,
        UUID signerUserId,
        OffsetDateTime signedAt,
        String reason,
        String location) {}
