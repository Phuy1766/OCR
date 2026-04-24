package vn.edu.congvan.auth.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import vn.edu.congvan.common.entity.SoftDeletableEntity;

/** Tổ chức / cơ quan cấp cao nhất. */
@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(
        sql =
                "UPDATE organizations SET is_deleted = true, deleted_at = NOW()"
                        + " WHERE id = ? AND is_deleted = false")
@SQLRestriction("is_deleted = false")
public class OrganizationEntity extends SoftDeletableEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "full_name", length = 500)
    private String fullName;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(length = 500)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
