package vn.edu.congvan.masterdata.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Counter cấp số theo (book_id, year). BR-02: truy cập qua SELECT FOR UPDATE
 * trong transaction để tránh race condition.
 */
@Entity
@Table(
        name = "document_book_counters",
        uniqueConstraints = @UniqueConstraint(columnNames = {"book_id", "year"}))
@Getter
@Setter
@NoArgsConstructor
public class DocumentBookCounterEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(nullable = false)
    private int year;

    @Column(name = "next_number", nullable = false)
    private long nextNumber = 1L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
