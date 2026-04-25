package vn.edu.congvan.masterdata.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import vn.edu.congvan.common.entity.SoftDeletableEntity;

/** Sổ đăng ký công văn. BR-03: VB mật phải dùng sổ SECRET riêng. */
@Entity
@Table(name = "document_books")
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(
        sql =
                "UPDATE document_books SET is_deleted = true, deleted_at = NOW()"
                        + " WHERE id = ? AND is_deleted = false")
@SQLRestriction("is_deleted = false")
public class DocumentBookEntity extends SoftDeletableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_type", nullable = false, length = 20)
    private BookType bookType;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidentiality_scope", nullable = false, length = 20)
    private ConfidentialityScope confidentialityScope = ConfidentialityScope.NORMAL;

    @Column(length = 50)
    private String prefix;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
