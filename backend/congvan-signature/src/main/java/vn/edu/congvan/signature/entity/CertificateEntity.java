package vn.edu.congvan.signature.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
public class CertificateEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificateType type;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "owner_organization_id")
    private UUID ownerOrganizationId;

    @Column(nullable = false, length = 255)
    private String alias;

    @Column(name = "subject_dn", nullable = false, length = 1000)
    private String subjectDn;

    @Column(name = "issuer_dn", length = 1000)
    private String issuerDn;

    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private OffsetDateTime validTo;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "is_revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Transient
    public boolean isCurrentlyValid() {
        if (revoked) return false;
        OffsetDateTime now = OffsetDateTime.now();
        return !now.isBefore(validFrom) && now.isBefore(validTo);
    }
}
