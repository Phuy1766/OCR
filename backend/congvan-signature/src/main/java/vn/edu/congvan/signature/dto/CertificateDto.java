package vn.edu.congvan.signature.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.congvan.signature.entity.CertificateType;

public record CertificateDto(
        UUID id,
        CertificateType type,
        UUID ownerUserId,
        UUID ownerOrganizationId,
        String alias,
        String subjectDn,
        String issuerDn,
        String serialNumber,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        boolean revoked,
        boolean currentlyValid) {}
