package vn.edu.congvan.common.signature;

import java.util.UUID;

/**
 * Gate kiểm tra chữ ký số trước khi cho phép phát hành VB đi.
 * Outbound module gọi qua Optional bean — nếu không có implementation
 * (vd test profile), default permissive (no-op).
 *
 * <p>Implementation chính nằm ở module congvan-signature.
 */
public interface SignatureGate {

    /**
     * @return {@code true} nếu VB đã có đủ {@code PERSONAL + ORGANIZATION}
     *     chữ ký số cho version đã duyệt; {@code false} nếu thiếu.
     */
    boolean hasBothSignatures(UUID documentId, UUID versionId);
}
