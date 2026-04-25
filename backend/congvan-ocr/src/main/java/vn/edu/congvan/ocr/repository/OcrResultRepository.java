package vn.edu.congvan.ocr.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.ocr.entity.OcrResultEntity;

@Repository
public interface OcrResultRepository extends JpaRepository<OcrResultEntity, UUID> {

    Optional<OcrResultEntity> findByJobId(UUID jobId);
}
