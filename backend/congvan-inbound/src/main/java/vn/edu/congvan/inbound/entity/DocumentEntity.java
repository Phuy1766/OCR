package vn.edu.congvan.inbound.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import vn.edu.congvan.common.entity.SoftDeletableEntity;

/**
 * Công văn (đến + đi). Soft delete BR-09. State trong cột {@code status}
 * (string-encoded enum {@link DocumentStatus}).
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(
        sql =
                "UPDATE documents SET is_deleted = true, deleted_at = NOW()"
                        + " WHERE id = ? AND is_deleted = false")
@SQLRestriction("is_deleted = false")
public class DocumentEntity extends SoftDeletableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DocumentDirection direction;

    @Column(name = "document_type_id", nullable = false)
    private UUID documentTypeId;

    @Column(name = "confidentiality_level_id", nullable = false)
    private UUID confidentialityLevelId;

    @Column(name = "priority_level_id", nullable = false)
    private UUID priorityLevelId;

    @Column(nullable = false, length = 1000)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentStatus status;

    @Column(name = "book_id")
    private UUID bookId;

    @Column(name = "book_year")
    private Integer bookYear;

    @Column(name = "book_number")
    private Long bookNumber;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "received_from_channel", length = 30)
    private ReceivedFromChannel receivedFromChannel;

    @Column(name = "external_reference_number", length = 100)
    private String externalReferenceNumber;

    @Column(name = "external_issuer", length = 500)
    private String externalIssuer;

    @Column(name = "external_issued_date")
    private LocalDate externalIssuedDate;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "current_handler_user_id")
    private UUID currentHandlerUserId;

    @Column(name = "current_handler_dept_id")
    private UUID currentHandlerDeptId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "department_id")
    private UUID departmentId;

    /** Version đã duyệt cuối — chốt khi LANH_DAO approve (BR-07). Phase 4. */
    @Column(name = "approved_version_id")
    private UUID approvedVersionId;

    @Column(name = "is_recalled", nullable = false)
    private boolean recalled = false;

    @Column(name = "recalled_at")
    private OffsetDateTime recalledAt;

    @Column(name = "recalled_by")
    private UUID recalledBy;

    @Column(name = "recalled_reason", length = 1000)
    private String recalledReason;
}
