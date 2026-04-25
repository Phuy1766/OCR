package vn.edu.congvan.signature.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.signature.entity.DigitalSignatureEntity;
import vn.edu.congvan.signature.entity.SignatureType;

@Repository
public interface DigitalSignatureRepository
        extends JpaRepository<DigitalSignatureEntity, UUID> {

    List<DigitalSignatureEntity> findByDocumentIdOrderBySignedAtAsc(UUID documentId);

    Optional<DigitalSignatureEntity> findByDocumentIdAndVersionIdAndSignatureType(
            UUID documentId, UUID versionId, SignatureType signatureType);
}
