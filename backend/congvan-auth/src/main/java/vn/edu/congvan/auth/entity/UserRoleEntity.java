package vn.edu.congvan.auth.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Gán role cho user, có thể scope theo department.
 * {@code department_id = null} nghĩa là áp cho toàn cơ quan.
 */
@Entity
@Table(name = "user_roles")
@Getter
@Setter
@NoArgsConstructor
public class UserRoleEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    /** Gán còn hiệu lực? (chưa hết hạn nếu có expires_at.) */
    @Transient
    public boolean isEffective() {
        return expiresAt == null || expiresAt.isAfter(OffsetDateTime.now());
    }
}
