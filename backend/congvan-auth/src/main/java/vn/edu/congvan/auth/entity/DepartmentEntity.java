package vn.edu.congvan.auth.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import vn.edu.congvan.common.entity.SoftDeletableEntity;

/** Phòng/ban trực thuộc tổ chức, có thể lồng nhau. */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(
        sql =
                "UPDATE departments SET is_deleted = true, deleted_at = NOW()"
                        + " WHERE id = ? AND is_deleted = false")
@SQLRestriction("is_deleted = false")
public class DepartmentEntity extends SoftDeletableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "head_user_id")
    private UUID headUserId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
