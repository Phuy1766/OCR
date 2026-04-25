package vn.edu.congvan.outbound.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.outbound.entity.DocumentVersionEntity;
import vn.edu.congvan.outbound.entity.VersionStatus;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersionEntity, UUID> {

    List<DocumentVersionEntity> findByDocumentIdOrderByVersionNumberAsc(UUID documentId);

    @Query(
            "SELECT v FROM DocumentVersionEntity v "
                    + "WHERE v.documentId = :documentId "
                    + "ORDER BY v.versionNumber DESC LIMIT 1")
    Optional<DocumentVersionEntity> findLatest(@Param("documentId") UUID documentId);

    @Query(
            "SELECT MAX(v.versionNumber) FROM DocumentVersionEntity v "
                    + "WHERE v.documentId = :documentId")
    Integer maxVersionNumber(@Param("documentId") UUID documentId);

    @Modifying
    @Query(
            "UPDATE DocumentVersionEntity v SET v.versionStatus = :status "
                    + "WHERE v.documentId = :documentId AND v.versionStatus = :fromStatus")
    int markStatus(
            @Param("documentId") UUID documentId,
            @Param("fromStatus") VersionStatus fromStatus,
            @Param("status") VersionStatus toStatus);
}
