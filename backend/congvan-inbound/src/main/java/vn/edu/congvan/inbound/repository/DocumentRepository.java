package vn.edu.congvan.inbound.repository;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.inbound.entity.DocumentDirection;
import vn.edu.congvan.inbound.entity.DocumentEntity;
import vn.edu.congvan.inbound.entity.DocumentStatus;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    /**
     * Search có scope quyền:
     * <ul>
     *   <li>{@code scopeOwn=true} — chỉ trả VB do user tạo hoặc đang xử lý
     *   <li>{@code scopeDeptId != null} — trả VB thuộc phòng ban ID đó (hoặc handler thuộc phòng đó)
     *   <li>cả hai null — toàn organization (không filter scope)
     * </ul>
     *
     * <p>Kết hợp filter direction, status, sổ, ngày, full-text trên subject/external_ref.
     */
    @Query(
            "SELECT d FROM DocumentEntity d "
                    + "WHERE (:direction IS NULL OR d.direction = :direction) "
                    + "  AND (:status IS NULL OR d.status = :status) "
                    + "  AND (:organizationId IS NULL OR d.organizationId = :organizationId) "
                    + "  AND (:bookId IS NULL OR d.bookId = :bookId) "
                    + "  AND (:fromDate IS NULL OR d.receivedDate >= :fromDate) "
                    + "  AND (:toDate IS NULL OR d.receivedDate <= :toDate) "
                    + "  AND (:scopeOwnUserId IS NULL "
                    + "       OR d.currentHandlerUserId = :scopeOwnUserId "
                    + "       OR d.createdBy = :scopeOwnUserId) "
                    + "  AND (:scopeDeptId IS NULL "
                    + "       OR d.departmentId = :scopeDeptId "
                    + "       OR d.currentHandlerDeptId = :scopeDeptId) "
                    + "  AND (:query IS NULL "
                    + "       OR LOWER(d.subject) LIKE LOWER(CONCAT('%', cast(:query as string), '%')) "
                    + "       OR LOWER(d.externalReferenceNumber) LIKE LOWER(CONCAT('%', cast(:query as string), '%'))) "
                    + "ORDER BY d.createdAt DESC")
    Page<DocumentEntity> search(
            @Param("direction") DocumentDirection direction,
            @Param("status") DocumentStatus status,
            @Param("organizationId") UUID organizationId,
            @Param("bookId") UUID bookId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("scopeOwnUserId") UUID scopeOwnUserId,
            @Param("scopeDeptId") UUID scopeDeptId,
            @Param("query") String query,
            Pageable pageable);
}
