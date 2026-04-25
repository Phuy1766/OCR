package vn.edu.congvan.outbound.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Quyết định duyệt — append-only mỗi lần approve/reject. */
@Entity
@Table(name = "approvals")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false, length = 30)
    private ApprovalLevel approvalLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalDecision decision;

    @Column(length = 2000)
    private String comment;

    @Column(name = "decided_by", nullable = false)
    private UUID decidedBy;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt;
}
