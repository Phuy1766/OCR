package vn.edu.congvan.signature.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "digital_signatures")
@Getter
@Setter
@NoArgsConstructor
public class DigitalSignatureEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "source_file_id", nullable = false)
    private UUID sourceFileId;

    @Column(name = "signed_file_id", nullable = false)
    private UUID signedFileId;

    @Column(name = "certificate_id", nullable = false)
    private UUID certificateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", nullable = false, length = 20)
    private SignatureType signatureType;

    @Column(name = "signer_user_id", nullable = false)
    private UUID signerUserId;

    @Column(name = "signed_at", nullable = false)
    private OffsetDateTime signedAt;

    @Column(length = 500)
    private String reason;

    @Column(length = 500)
    private String location;

    @Column(name = "contact_info", length = 500)
    private String contactInfo;
}
