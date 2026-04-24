package vn.edu.congvan.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Mở rộng {@link BaseEntity} với soft-delete. Các entity dùng class này
 * phải khai báo {@code @SQLDelete("UPDATE ... SET is_deleted=true, ...")}
 * và {@code @SQLRestriction("is_deleted = false")} ở class con.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    public void markDeleted(UUID actor) {
        this.deleted = true;
        this.deletedAt = OffsetDateTime.now();
        this.deletedBy = actor;
    }
}
