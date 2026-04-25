package vn.edu.congvan.inbound.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.inbound.entity.DocumentFileEntity;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFileEntity, UUID> {

    List<DocumentFileEntity> findByDocumentIdOrderByUploadedAtAsc(UUID documentId);
}
