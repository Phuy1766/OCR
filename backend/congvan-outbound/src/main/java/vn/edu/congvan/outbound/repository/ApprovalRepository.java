package vn.edu.congvan.outbound.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.outbound.entity.ApprovalEntity;

@Repository
public interface ApprovalRepository extends JpaRepository<ApprovalEntity, UUID> {

    List<ApprovalEntity> findByDocumentIdOrderByDecidedAtAsc(UUID documentId);
}
