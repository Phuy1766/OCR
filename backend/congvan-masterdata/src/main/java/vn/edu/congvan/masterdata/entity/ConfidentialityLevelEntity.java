package vn.edu.congvan.masterdata.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 4 mức độ mật (Luật BVBMNN 2018): BINH_THUONG/MAT/TOI_MAT/TUYET_MAT. */
@Entity
@Table(name = "confidentiality_levels")
@Getter
@Setter
@NoArgsConstructor
public class ConfidentialityLevelEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int level;

    @Column(length = 20)
    private String color;

    @Column(length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Có yêu cầu dùng sổ mật riêng không (BR-03). */
    @Transient
    public boolean requiresSecretBook() {
        return level >= 1;
    }
}
