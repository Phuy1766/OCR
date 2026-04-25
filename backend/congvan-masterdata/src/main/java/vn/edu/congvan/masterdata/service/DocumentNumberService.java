package vn.edu.congvan.masterdata.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.masterdata.dto.ReservedNumber;
import vn.edu.congvan.masterdata.entity.DocumentBookCounterEntity;
import vn.edu.congvan.masterdata.entity.DocumentBookEntity;
import vn.edu.congvan.masterdata.repository.DocumentBookCounterRepository;
import vn.edu.congvan.masterdata.repository.DocumentBookRepository;

/**
 * Cấp số công văn vào sổ. BR-01 (reset 01/01) + BR-02 (FOR UPDATE).
 *
 * <p>Luôn chạy trong transaction (REQUIRED). Caller cần mở transaction để
 * pessimistic lock kéo dài đến khi ghi xong bản ghi document/book_entry liên quan.
 *
 * <p>Thuật toán (race-free):
 * <ol>
 *   <li>Xác định năm hiện tại (timezone Asia/Ho_Chi_Minh — BR-01).
 *   <li>{@code INSERT ... ON CONFLICT DO NOTHING} cho counter (idempotent,
 *       chỉ chèn nếu chưa có — tránh race giữa nhiều tx cùng tạo counter đầu năm).
 *   <li>{@code SELECT ... FOR UPDATE} (BR-02) — chờ lock nếu tx khác đang giữ.
 *   <li>Đọc {@code next_number}, cấp cho caller, {@code next_number++}.
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentNumberService {

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DocumentBookRepository books;
    private final DocumentBookCounterRepository counters;

    @Transactional(propagation = Propagation.REQUIRED)
    public ReservedNumber reserve(UUID bookId) {
        DocumentBookEntity book =
                books.findById(bookId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "DOCUMENT_BOOK_NOT_FOUND",
                                                "Không tìm thấy sổ đăng ký."));
        if (!book.isActive()) {
            throw new BusinessException(
                    "DOCUMENT_BOOK_INACTIVE",
                    "Sổ đăng ký đã bị khóa. Không thể cấp số mới.");
        }

        int year = OffsetDateTime.now(ZONE_VN).getYear();

        // Bước 1: đảm bảo counter tồn tại (idempotent, ON CONFLICT DO NOTHING).
        counters.ensureExists(bookId, year);

        // Bước 2: lấy lock pessimistic. Tại thời điểm này counter chắc chắn đã tồn tại.
        DocumentBookCounterEntity counter =
                counters.findForUpdate(bookId, year)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Counter biến mất sau ensureExists — không khả thi."));

        long assigned = counter.getNextNumber();
        counter.setNextNumber(assigned + 1);
        counter.setUpdatedAt(OffsetDateTime.now());
        counters.save(counter);
        log.debug("Reserved number {} for book={} year={}", assigned, bookId, year);
        return new ReservedNumber(bookId, year, assigned);
    }
}
