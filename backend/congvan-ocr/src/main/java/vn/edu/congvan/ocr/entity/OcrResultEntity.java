package vn.edu.congvan.ocr.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ocr_results")
@Getter
@Setter
@NoArgsConstructor
public class OcrResultEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "job_id", nullable = false, unique = true)
    private UUID jobId;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "confidence_avg", precision = 4, scale = 3)
    private BigDecimal confidenceAvg;

    @Column(name = "processing_ms")
    private Integer processingMs;

    @Column(name = "engine_version", length = 50)
    private String engineVersion;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "is_accepted", nullable = false)
    private boolean accepted;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "accepted_by")
    private UUID acceptedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
