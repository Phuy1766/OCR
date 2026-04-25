package vn.edu.congvan.signature.service;

import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.auth.service.AuditLogger;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.integration.storage.MinioFileService;
import vn.edu.congvan.integration.storage.StoredObject;
import vn.edu.congvan.signature.crypto.PdfSigner;
import vn.edu.congvan.signature.dto.CertificateDto;
import vn.edu.congvan.signature.dto.UploadCertificateRequest;
import vn.edu.congvan.signature.entity.CertificateEntity;
import vn.edu.congvan.signature.entity.CertificateType;
import vn.edu.congvan.signature.repository.CertificateRepository;

/**
 * Quản lý certificate PKCS#12. Upload file → parse X.509 → lưu metadata DB
 * + binary lên MinIO bucket signed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certs;
    private final MinioFileService minio;
    private final AuditLogger audit;

    @Transactional
    public CertificateDto upload(
            UploadCertificateRequest req,
            String filename,
            byte[] p12Bytes,
            AuthPrincipal actor,
            String actorIp) {
        // Validate ownership consistency
        if (req.type() == CertificateType.PERSONAL && req.ownerUserId() == null) {
            throw new BusinessException(
                    "CERT_OWNER_REQUIRED",
                    "Cert PERSONAL phải có owner_user_id.");
        }
        if (req.type() == CertificateType.ORGANIZATION && req.ownerOrganizationId() == null) {
            throw new BusinessException(
                    "CERT_OWNER_REQUIRED",
                    "Cert ORGANIZATION phải có owner_organization_id.");
        }

        // Load PKCS#12 + parse X.509
        PdfSigner.LoadedKey key;
        try {
            key = PdfSigner.loadPkcs12(p12Bytes, req.pkcs12Password());
        } catch (Exception e) {
            throw new BusinessException(
                    "CERT_LOAD_FAILED",
                    "Không load được PKCS#12 (password sai hoặc file hỏng): " + e.getMessage());
        }
        X509Certificate signerCert = key.signerCert();

        // Upload binary lên MinIO
        StoredObject stored = minio.upload(
                "certs/" + (req.type() == CertificateType.PERSONAL ? "personal" : "org"),
                filename == null ? "cert.p12" : filename,
                p12Bytes,
                "application/x-pkcs12");

        CertificateEntity c = new CertificateEntity();
        c.setId(UUID.randomUUID());
        c.setType(req.type());
        c.setOwnerUserId(req.ownerUserId());
        c.setOwnerOrganizationId(req.ownerOrganizationId());
        c.setAlias(req.alias());
        c.setSubjectDn(signerCert.getSubjectX500Principal().getName());
        c.setIssuerDn(signerCert.getIssuerX500Principal().getName());
        c.setSerialNumber(signerCert.getSerialNumber().toString());
        c.setValidFrom(OffsetDateTime.ofInstant(
                signerCert.getNotBefore().toInstant(), ZoneOffset.UTC));
        c.setValidTo(OffsetDateTime.ofInstant(
                signerCert.getNotAfter().toInstant(), ZoneOffset.UTC));
        c.setStorageKey(stored.storageKey());
        c.setRevoked(false);
        c.setCreatedAt(OffsetDateTime.now());
        c.setCreatedBy(actor == null ? null : actor.userId());
        c = certs.save(c);

        audit.logSuccess(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.username(),
                actorIp,
                "UPLOAD_CERTIFICATE",
                "certificates",
                c.getId().toString());
        log.info("Certificate uploaded: alias={}, type={}, valid_to={}",
                c.getAlias(), c.getType(), c.getValidTo());
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public List<CertificateDto> list(CertificateType type, UUID ownerUserId, UUID ownerOrgId) {
        return certs.findActive(type, ownerUserId, ownerOrgId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateEntity load(UUID certId) {
        CertificateEntity c = certs.findById(certId)
                .orElseThrow(() -> new BusinessException(
                        "CERT_NOT_FOUND", "Không tìm thấy certificate."));
        if (c.isRevoked()) {
            throw new BusinessException("CERT_REVOKED", "Certificate đã bị thu hồi.");
        }
        if (!c.isCurrentlyValid()) {
            throw new BusinessException(
                    "CERT_EXPIRED",
                    "Certificate đã hết hạn hoặc chưa có hiệu lực.");
        }
        return c;
    }

    /** Đọc raw PKCS#12 từ MinIO. Caller phải có quyền dùng cert này. */
    public byte[] downloadP12(CertificateEntity cert) {
        return minio.download(cert.getStorageKey());
    }

    private CertificateDto toDto(CertificateEntity c) {
        return new CertificateDto(
                c.getId(),
                c.getType(),
                c.getOwnerUserId(),
                c.getOwnerOrganizationId(),
                c.getAlias(),
                c.getSubjectDn(),
                c.getIssuerDn(),
                c.getSerialNumber(),
                c.getValidFrom(),
                c.getValidTo(),
                c.isRevoked(),
                c.isCurrentlyValid());
    }
}
