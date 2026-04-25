package vn.edu.congvan.masterdata.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.masterdata.entity.DocumentTypeEntity;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentTypeEntity, UUID> {
    Optional<DocumentTypeEntity> findByCode(String code);

    @Query("SELECT t FROM DocumentTypeEntity t WHERE t.active = true ORDER BY t.displayOrder")
    java.util.List<DocumentTypeEntity> findAllActive();
}
