package vn.edu.congvan.signature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.auth.service.AuditLogger;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.inbound.entity.DocumentEntity;
import vn.edu.congvan.inbound.entity.DocumentFileEntity;
import vn.edu.congvan.inbound.entity.DocumentFileRole;
import vn.edu.congvan.inbound.entity.DocumentStatus;
import vn.edu.congvan.inbound.repository.DocumentFileRepository;
import vn.edu.congvan.inbound.repository.DocumentRepository;
import vn.edu.congvan.integration.storage.MinioFileService;
import vn.edu.congvan.integration.storage.StoredObject;
import vn.edu.congvan.signature.crypto.PdfSigner;
import vn.edu.congvan.signature.dto.SignRequest;
import vn.edu.congvan.signature.dto.SignatureDto;
import vn.edu.congvan.signature.dto.VerificationDto;
import vn.edu.congvan.signature.entity.CertificateEntity;
import vn.edu.congvan.signature.entity.CertificateType;
import vn.edu.congvan.signature.entity.DigitalSignatureEntity;
import vn.edu.congvan.signature.entity.SignatureType;
import vn.edu.congvan.signature.entity.SignatureVerificationEntity;
import vn.edu.congvan.signature.repository.DigitalSignatureRepository;
import vn.edu.congvan.signature.repository.SignatureVerificationRepository;

/**
 * Ký số 2-phase cho VB đi (BR-06/12):
 *   1. signPersonal — Lãnh đạo/cấp phó ký bằng cert PERSONAL
 *   2. signOrganization — Văn thư cơ quan ký bằng cert ORGANIZATION
 *
 * <p>Sau cả 2 chữ ký, OutboundService.issue mới được phép cấp số + phát hành.
 * BR-07: ký phải đúng approved_version_id; ngăn ký lên version cũ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureService {

    private final DocumentRepository documents;
    private final DocumentFileRepository docFiles;
    private final DigitalSignatureRepository signatures;
    private final SignatureVerificationRepository verifications;
    private final CertificateService certificateService;
    private final MinioFileService minio;
    private final AuditLogger audit;
    private final ObjectMapper json;

    @Transactional
    public SignatureDto signPersonal(
            UUID documentId, SignRequest req, AuthPrincipal actor, String actorIp) {
        return doSign(documentId, req, SignatureType.PERSONAL, actor, actorIp);
    }

    @Transactional
    public SignatureDto signOrganization(
            UUID documentId, SignRequest req, AuthPrincipal actor, String actorIp) {
        return doSign(documentId, req, SignatureType.ORGANIZATION, actor, actorIp);
    }

    private SignatureDto doSign(
            UUID documentId,
            SignRequest req,
            SignatureType sigType,
            AuthPrincipal actor,
            String actorIp) {
        DocumentEntity d = documents.findById(documentId)
                .orElseThrow(() -> new BusinessException(
                        "DOCUMENT_NOT_FOUND", "Không tìm thấy công văn."));

        if (d.getApprovedVersionId() == null) {
            throw new BusinessException(
                    "SIGN_NOT_APPROVED",
                    "VB chưa được duyệt cấp đơn vị (chưa có approved_version_id) — không ký được.");
        }
        if (d.getStatus() == DocumentStatus.RECALLED
                || d.getStatus() == DocumentStatus.SENT
                || d.getStatus() == DocumentStatus.ARCHIVED) {
            throw new BusinessException(
                    "SIGN_DOC_CLOSED",
                    "Không ký được VB ở trạng thái " + d.getStatus() + ".");
        }

        // Personal sign yêu cầu chưa có chữ ký personal cho version này (BR-06: 1 lần)
        UUID approvedVerId = d.getApprovedVersionId();
        if (signatures.findByDocumentIdAndVersionIdAndSignatureType(
                        documentId, approvedVerId, sigType)
                .isPresent()) {
            throw new BusinessException(
                    "SIGN_ALREADY_DONE",
                    "Đã có chữ ký " + sigType + " cho phiên bản đã duyệt.");
        }

        // Organization sign chỉ được ký SAU khi có personal sign
        if (sigType == SignatureType.ORGANIZATION) {
            if (signatures.findByDocumentIdAndVersionIdAndSignatureType(
                            documentId, approvedVerId, SignatureType.PERSONAL)
                    .isEmpty()) {
                throw new BusinessException(
                        "SIGN_PERSONAL_REQUIRED",
                        "BR-06/12: phải ký cá nhân trước khi ký cơ quan.");
            }
        }

        // Load + validate certificate
        CertificateEntity cert = certificateService.load(req.certificateId());
        validateCertOwnership(cert, sigType, actor);

        // Lấy file source: personal sign → file FINAL_PDF/DRAFT_PDF của approved_version;
        // organization sign → file SIGNED do personal sign tạo ra (sign chồng).
        DocumentFileEntity sourceFile = pickSourceFile(documentId, approvedVerId, sigType);

        // Sign
        byte[] sourceBytes = minio.download(sourceFile.getStorageKey());
        byte[] p12Bytes = certificateService.downloadP12(cert);
        PdfSigner.LoadedKey key = PdfSigner.loadPkcs12(p12Bytes, req.pkcs12Password());

        String signerName = key.signerCert().getSubjectX500Principal().getName();
        byte[] signedBytes;
        try {
            signedBytes = PdfSigner.signPdf(
                    sourceBytes, key.privateKey(), key.chain(),
                    req.reason() != null ? req.reason() : defaultReason(sigType),
                    req.location(),
                    signerName);
        } catch (Exception e) {
            throw new BusinessException(
                    "SIGN_FAILED",
                    "Không ký được PDF: " + e.getMessage());
        }

        // Lưu signed file lên MinIO + ghi document_files (role = SIGNED)
        StoredObject stored = minio.upload(
                "outbound/signed/" + documentId,
                deriveSignedFilename(sourceFile.getFileName(), sigType),
                signedBytes,
                sourceFile.getMimeType());

        DocumentFileEntity signedFile = new DocumentFileEntity();
        signedFile.setId(UUID.randomUUID());
        signedFile.setDocumentId(documentId);
        signedFile.setVersionId(approvedVerId);
        signedFile.setFileRole(DocumentFileRole.SIGNED);
        signedFile.setFileName(stored.storageKey().substring(
                stored.storageKey().lastIndexOf('/') + 1));
        signedFile.setMimeType(sourceFile.getMimeType());
        signedFile.setSizeBytes(stored.sizeBytes());
        signedFile.setSha256(stored.sha256());
        signedFile.setStorageKey(stored.storageKey());
        signedFile.setUploadedBy(actor == null ? null : actor.userId());
        signedFile.setUploadedAt(OffsetDateTime.now());
        signedFile = docFiles.save(signedFile);

        DigitalSignatureEntity sigEntity = new DigitalSignatureEntity();
        sigEntity.setId(UUID.randomUUID());
        sigEntity.setDocumentId(documentId);
        sigEntity.setVersionId(approvedVerId);
        sigEntity.setSourceFileId(sourceFile.getId());
        sigEntity.setSignedFileId(signedFile.getId());
        sigEntity.setCertificateId(cert.getId());
        sigEntity.setSignatureType(sigType);
        sigEntity.setSignerUserId(actor == null ? null : actor.userId());
        sigEntity.setSignedAt(OffsetDateTime.now());
        sigEntity.setReason(req.reason());
        sigEntity.setLocation(req.location());
        sigEntity = signatures.save(sigEntity);

        // Update document state — sau cả 2 chữ ký → SIGNED
        boolean hasPersonal = signatures
                .findByDocumentIdAndVersionIdAndSignatureType(
                        documentId, approvedVerId, SignatureType.PERSONAL)
                .isPresent();
        boolean hasOrg = signatures
                .findByDocumentIdAndVersionIdAndSignatureType(
                        documentId, approvedVerId, SignatureType.ORGANIZATION)
                .isPresent();
        if (hasPersonal && hasOrg) {
            d.setStatus(DocumentStatus.SIGNED);
        } else {
            d.setStatus(DocumentStatus.PENDING_SIGN);
        }

        audit.log(AuditLogger.AuditRecord.builder()
                .actorId(actor == null ? null : actor.userId())
                .actorUsername(actor == null ? null : actor.username())
                .actorIp(actorIp)
                .action(sigType == SignatureType.PERSONAL ? "SIGN_PERSONAL" : "SIGN_ORGANIZATION")
                .entityType("digital_signatures")
                .entityId(sigEntity.getId().toString())
                .success(true)
                .extra(Map.of(
                        "documentId", documentId.toString(),
                        "versionId", approvedVerId.toString(),
                        "certId", cert.getId().toString(),
                        "certSerial", cert.getSerialNumber()))
                .build());

        return toDto(sigEntity);
    }

    @Transactional
    public List<VerificationDto> verifyAllForDocument(
            UUID documentId, AuthPrincipal actor) {
        var allSigs = signatures.findByDocumentIdOrderBySignedAtAsc(documentId);
        List<VerificationDto> out = new java.util.ArrayList<>();
        for (var sig : allSigs) {
            DocumentFileEntity signedFile = docFiles.findById(sig.getSignedFileId())
                    .orElseThrow(() -> new BusinessException(
                            "FILE_NOT_FOUND", "Signed file không tồn tại"));
            byte[] signedBytes = minio.download(signedFile.getStorageKey());

            PdfSigner.VerificationResult r;
            try {
                List<PdfSigner.VerificationResult> results = PdfSigner.verify(signedBytes);
                // Lấy chữ ký cuối cùng — chữ ký trên cùng (latest)
                r = results.isEmpty()
                        ? new PdfSigner.VerificationResult(false, "No signature found",
                                null, null, null, null)
                        : results.get(results.size() - 1);
            } catch (Exception e) {
                r = new PdfSigner.VerificationResult(false,
                        "Verify error: " + e.getMessage(), null, null, null, null);
            }

            SignatureVerificationEntity v = new SignatureVerificationEntity();
            v.setId(UUID.randomUUID());
            v.setSignatureId(sig.getId());
            v.setValid(r.valid());
            v.setFailureReason(r.failureReason());
            v.setVerifiedAt(OffsetDateTime.now());
            v.setVerifiedBy(actor == null ? null : actor.userId());
            try {
                v.setDetails(json.writeValueAsString(Map.of(
                        "certSerial", r.certSerial() == null ? "" : r.certSerial(),
                        "subjectDn", r.subjectDn() == null ? "" : r.subjectDn(),
                        "signerName", r.signerName() == null ? "" : r.signerName(),
                        "reason", r.reason() == null ? "" : r.reason())));
            } catch (Exception ignored) {
                // best effort
            }
            verifications.save(v);
            out.add(new VerificationDto(
                    sig.getId(), r.valid(), r.failureReason(),
                    r.signerName(), r.certSerial(), r.subjectDn(),
                    OffsetDateTime.now()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<SignatureDto> listForDocument(UUID documentId) {
        return signatures.findByDocumentIdOrderBySignedAtAsc(documentId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Check VB đã có đủ 2 chữ ký cho approved_version → cho phép issue. */
    @Transactional(readOnly = true)
    public boolean hasBothSignatures(UUID documentId, UUID versionId) {
        boolean p = signatures.findByDocumentIdAndVersionIdAndSignatureType(
                documentId, versionId, SignatureType.PERSONAL).isPresent();
        boolean o = signatures.findByDocumentIdAndVersionIdAndSignatureType(
                documentId, versionId, SignatureType.ORGANIZATION).isPresent();
        return p && o;
    }

    private void validateCertOwnership(
            CertificateEntity cert, SignatureType sigType, AuthPrincipal actor) {
        if (sigType == SignatureType.PERSONAL) {
            if (cert.getType() != CertificateType.PERSONAL) {
                throw new BusinessException(
                        "CERT_TYPE_MISMATCH",
                        "Chữ ký cá nhân phải dùng cert PERSONAL.");
            }
            if (actor != null && cert.getOwnerUserId() != null
                    && !cert.getOwnerUserId().equals(actor.userId())) {
                throw new BusinessException(
                        "CERT_OWNER_MISMATCH",
                        "Cert này không phải của bạn.");
            }
        } else {
            if (cert.getType() != CertificateType.ORGANIZATION) {
                throw new BusinessException(
                        "CERT_TYPE_MISMATCH",
                        "Chữ ký cơ quan phải dùng cert ORGANIZATION.");
            }
        }
    }

    private DocumentFileEntity pickSourceFile(
            UUID documentId, UUID approvedVerId, SignatureType sigType) {
        var allFiles = docFiles.findByDocumentIdOrderByUploadedAtAsc(documentId);
        if (sigType == SignatureType.PERSONAL) {
            // Lấy file của approved_version có role DRAFT_PDF/FINAL_PDF
            return allFiles.stream()
                    .filter(f -> approvedVerId.equals(f.getVersionId()))
                    .filter(f -> f.getFileRole() == DocumentFileRole.DRAFT_PDF
                            || f.getFileRole() == DocumentFileRole.FINAL_PDF)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "SIGN_NO_SOURCE",
                            "Không tìm thấy file PDF của phiên bản đã duyệt để ký."));
        } else {
            // ORGANIZATION sign: chồng lên file SIGNED của personal
            return allFiles.stream()
                    .filter(f -> approvedVerId.equals(f.getVersionId()))
                    .filter(f -> f.getFileRole() == DocumentFileRole.SIGNED)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "SIGN_NO_PERSONAL_FILE",
                            "Không tìm thấy file đã ký cá nhân để ký cơ quan chồng."));
        }
    }

    private static String defaultReason(SignatureType type) {
        return type == SignatureType.PERSONAL
                ? "Phê duyệt văn bản"
                : "Đóng dấu cơ quan";
    }

    private static String deriveSignedFilename(String original, SignatureType type) {
        String suffix = type == SignatureType.PERSONAL ? ".signed-personal" : ".signed-org";
        int dot = original.lastIndexOf('.');
        if (dot <= 0) return original + suffix;
        return original.substring(0, dot) + suffix + original.substring(dot);
    }

    private SignatureDto toDto(DigitalSignatureEntity s) {
        return new SignatureDto(
                s.getId(),
                s.getDocumentId(),
                s.getVersionId(),
                s.getSignedFileId(),
                s.getCertificateId(),
                s.getSignatureType(),
                s.getSignerUserId(),
                s.getSignedAt(),
                s.getReason(),
                s.getLocation());
    }
}
