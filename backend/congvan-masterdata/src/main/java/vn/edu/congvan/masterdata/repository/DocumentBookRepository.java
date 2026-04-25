package vn.edu.congvan.masterdata.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.masterdata.entity.BookType;
import vn.edu.congvan.masterdata.entity.ConfidentialityScope;
import vn.edu.congvan.masterdata.entity.DocumentBookEntity;

@Repository
public interface DocumentBookRepository extends JpaRepository<DocumentBookEntity, UUID> {

    Optional<DocumentBookEntity> findByOrganizationIdAndCode(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);

    @Query(
            "SELECT b FROM DocumentBookEntity b "
                    + "WHERE (:organizationId IS NULL OR b.organizationId = :organizationId) "
                    + "  AND (:bookType IS NULL OR b.bookType = :bookType) "
                    + "  AND (:scope IS NULL OR b.confidentialityScope = :scope) "
                    + "  AND b.active = true "
                    + "ORDER BY b.createdAt DESC")
    List<DocumentBookEntity> findActive(
            @Param("organizationId") UUID organizationId,
            @Param("bookType") BookType bookType,
            @Param("scope") ConfidentialityScope scope);
}
