package vn.edu.congvan.workflow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.workflow.entity.AssignmentEntity;
import vn.edu.congvan.workflow.entity.AssignmentStatus;

@Repository
public interface AssignmentRepository extends JpaRepository<AssignmentEntity, UUID> {

    @Query(
            "SELECT a FROM AssignmentEntity a "
                    + "WHERE a.documentId = :documentId AND a.status = :status")
    Optional<AssignmentEntity> findActive(
            @Param("documentId") UUID documentId,
            @Param("status") AssignmentStatus status);

    @Query(
            "SELECT a FROM AssignmentEntity a "
                    + "WHERE a.assignedToUserId = :userId "
                    + "  AND (:status IS NULL OR a.status = :status) "
                    + "ORDER BY a.assignedAt DESC")
    Page<AssignmentEntity> findByUser(
            @Param("userId") UUID userId,
            @Param("status") AssignmentStatus status,
            Pageable pageable);

    List<AssignmentEntity> findByDocumentIdOrderByAssignedAtAsc(UUID documentId);
}
