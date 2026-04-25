package vn.edu.congvan.signature.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.signature.entity.CertificateEntity;
import vn.edu.congvan.signature.entity.CertificateType;

@Repository
public interface CertificateRepository extends JpaRepository<CertificateEntity, UUID> {

    @Query(
            "SELECT c FROM CertificateEntity c "
                    + "WHERE c.revoked = false "
                    + "  AND (:type IS NULL OR c.type = :type) "
                    + "  AND (:userId IS NULL OR c.ownerUserId = :userId) "
                    + "  AND (:orgId IS NULL OR c.ownerOrganizationId = :orgId) "
                    + "ORDER BY c.alias")
    List<CertificateEntity> findActive(
            @Param("type") CertificateType type,
            @Param("userId") UUID userId,
            @Param("orgId") UUID orgId);
}
