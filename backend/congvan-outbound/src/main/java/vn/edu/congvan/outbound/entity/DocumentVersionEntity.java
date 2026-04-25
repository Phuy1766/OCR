package vn.edu.congvan.outbound.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Phiên bản nội dung VB — IMMUTABLE sau khi tạo. Chỉ {@code versionStatus}
 * và {@code hashSha256} (1 lần) được phép update qua trigger DB.
 */
@Entity
@Table(name = "document_versions")
@Getter
@Setter
@NoArgsConstructor
public class DocumentVersionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "version_number", nullable = false, updatable = false)
    private int versionNumber;

    @Column(name = "parent_version_id", updatable = false)
    private UUID parentVersionId;

    /** Snapshot JSON — chứa metadata + danh sách file_id đính kèm tại thời điểm version. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_snapshot", nullable = false, columnDefinition = "jsonb", updatable = false)
    private String contentSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "version_status", nullable = false, length = 20)
    private VersionStatus versionStatus = VersionStatus.DRAFT;

    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;
}
