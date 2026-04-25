package vn.edu.congvan.masterdata.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.masterdata.entity.DocumentBookCounterEntity;

@Repository
public interface DocumentBookCounterRepository
        extends JpaRepository<DocumentBookCounterEntity, UUID> {

    Optional<DocumentBookCounterEntity> findByBookIdAndYear(UUID bookId, int year);

    /**
     * BR-02: Cấp số phải dùng SELECT ... FOR UPDATE để chặn race condition.
     * Transaction ngoài cùng phải đang ACTIVE.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT c FROM DocumentBookCounterEntity c "
                    + "WHERE c.bookId = :bookId AND c.year = :year")
    Optional<DocumentBookCounterEntity> findForUpdate(
            @Param("bookId") UUID bookId, @Param("year") int year);

    /**
     * Đảm bảo counter tồn tại cho {@code (book_id, year)}. Idempotent — nếu đã
     * có thì DO NOTHING. Dùng native INSERT ... ON CONFLICT để tránh race ở
     * bước tạo counter đầu năm (nhiều thread cùng thấy "chưa có" rồi cùng INSERT).
     */
    @Modifying
    @Query(
            value =
                    "INSERT INTO document_book_counters "
                            + "  (id, book_id, year, next_number, created_at, updated_at) "
                            + "VALUES (gen_random_uuid(), :bookId, :year, 1, NOW(), NOW()) "
                            + "ON CONFLICT (book_id, year) DO NOTHING",
            nativeQuery = true)
    int ensureExists(@Param("bookId") UUID bookId, @Param("year") int year);
}
