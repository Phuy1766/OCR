package vn.edu.congvan.workflow.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.workflow.entity.WorkflowInstanceEntity;

@Repository
public interface WorkflowInstanceRepository
        extends JpaRepository<WorkflowInstanceEntity, UUID> {

    Optional<WorkflowInstanceEntity> findByDocumentId(UUID documentId);
}
