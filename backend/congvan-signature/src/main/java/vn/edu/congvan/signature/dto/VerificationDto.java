package vn.edu.congvan.signature.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VerificationDto(
        UUID signatureId,
        boolean valid,
        String failureReason,
        String signerName,
        String certSerial,
        String subjectDn,
        OffsetDateTime verifiedAt) {}
