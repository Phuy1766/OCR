package vn.edu.congvan.inbound.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.inbound.entity.DocumentBookEntryEntity;

@Repository
public interface DocumentBookEntryRepository
        extends JpaRepository<DocumentBookEntryEntity, UUID> {

    Optional<DocumentBookEntryEntity> findByDocumentId(UUID documentId);
}
