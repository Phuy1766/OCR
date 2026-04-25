package vn.edu.congvan.inbound.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bản ghi VB trong sổ đăng ký. UNIQUE (book_id, year, number). */
@Entity
@Table(name = "document_book_entries")
@Getter
@Setter
@NoArgsConstructor
public class DocumentBookEntryEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private long number;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_status", nullable = false, length = 20)
    private BookEntryStatus entryStatus = BookEntryStatus.OFFICIAL;

    @Column(name = "entered_at", nullable = false)
    private OffsetDateTime enteredAt;

    @Column(name = "entered_by")
    private UUID enteredBy;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_reason", length = 500)
    private String cancelledReason;
}
