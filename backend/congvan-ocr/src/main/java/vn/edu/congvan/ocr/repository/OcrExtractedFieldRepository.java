package vn.edu.congvan.ocr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.ocr.entity.OcrExtractedFieldEntity;

@Repository
public interface OcrExtractedFieldRepository
        extends JpaRepository<OcrExtractedFieldEntity, UUID> {

    List<OcrExtractedFieldEntity> findByResultIdOrderByFieldNameAsc(UUID resultId);
}
