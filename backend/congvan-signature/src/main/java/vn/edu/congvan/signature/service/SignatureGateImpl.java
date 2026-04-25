package vn.edu.congvan.signature.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.edu.congvan.common.signature.SignatureGate;

/**
 * Implementation của {@link SignatureGate} — delegate vào {@link SignatureService}.
 * Có thể tắt bằng {@code app.signature.gate-enabled=false} (test/dev không có cert).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.signature.gate-enabled",
        havingValue = "true", matchIfMissing = true)
public class SignatureGateImpl implements SignatureGate {

    private final SignatureService signatureService;

    @Override
    public boolean hasBothSignatures(UUID documentId, UUID versionId) {
        return signatureService.hasBothSignatures(documentId, versionId);
    }
}
