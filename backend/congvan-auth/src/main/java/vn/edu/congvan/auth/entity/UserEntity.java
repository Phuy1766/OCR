package vn.edu.congvan.auth.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import vn.edu.congvan.common.entity.SoftDeletableEntity;

/** Tài khoản người dùng. Mật khẩu luôn lưu dưới dạng Argon2id hash. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(
        sql =
                "UPDATE users SET is_deleted = true, deleted_at = NOW()"
                        + " WHERE id = ? AND is_deleted = false")
@SQLRestriction("is_deleted = false")
public class UserEntity extends SoftDeletableEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(length = 30)
    private String phone;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "position_title", length = 255)
    private String positionTitle;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_locked", nullable = false)
    private boolean locked = false;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "password_changed_at", nullable = false)
    private OffsetDateTime passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    /** Xem có thể đăng nhập hay không (active + chưa soft-delete + chưa lock). */
    @Transient
    public boolean isLoginAllowed() {
        return !isDeleted() && active && !locked;
    }
}
