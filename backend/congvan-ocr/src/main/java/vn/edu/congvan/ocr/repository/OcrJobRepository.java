package vn.edu.congvan.ocr.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.ocr.entity.OcrJobEntity;

@Repository
public interface OcrJobRepository extends JpaRepository<OcrJobEntity, UUID> {

    List<OcrJobEntity> findByDocumentIdOrderByEnqueuedAtDesc(UUID documentId);

    Optional<OcrJobEntity> findFirstByDocumentIdOrderByEnqueuedAtDesc(UUID documentId);
}
