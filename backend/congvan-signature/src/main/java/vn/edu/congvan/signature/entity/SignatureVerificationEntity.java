package vn.edu.congvan.signature.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "signature_verifications")
@Getter
@Setter
@NoArgsConstructor
public class SignatureVerificationEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "signature_id", nullable = false)
    private UUID signatureId;

    @Column(name = "is_valid", nullable = false)
    private boolean valid;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(name = "verified_at", nullable = false)
    private OffsetDateTime verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;
}
