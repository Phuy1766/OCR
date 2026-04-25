package vn.edu.congvan.signature.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import vn.edu.congvan.signature.entity.CertificateType;

public record UploadCertificateRequest(
        @NotNull CertificateType type,
        @NotBlank @Size(max = 255) String alias,
        UUID ownerUserId,
        UUID ownerOrganizationId,
        @NotBlank String pkcs12Password) {}
