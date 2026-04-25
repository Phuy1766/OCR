package vn.edu.congvan.masterdata.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 4 mức khẩn: BINH_THUONG/KHAN/THUONG_KHAN/HOA_TOC. */
@Entity
@Table(name = "priority_levels")
@Getter
@Setter
@NoArgsConstructor
public class PriorityLevelEntity {

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

    /** SLA giờ xử lý đề xuất (null = không giới hạn). */
    @Column(name = "sla_hours")
    private Integer slaHours;

    @Column(length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** BR-04: cần ưu tiên (notification, OCR) khi khẩn/thượng khẩn/hỏa tốc. */
    @Transient
    public boolean isUrgent() {
        return level >= 1;
    }
}
