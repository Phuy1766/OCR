package vn.edu.congvan.signature.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.signature.entity.SignatureVerificationEntity;

@Repository
public interface SignatureVerificationRepository
        extends JpaRepository<SignatureVerificationEntity, UUID> {

    List<SignatureVerificationEntity> findBySignatureIdOrderByVerifiedAtDesc(UUID signatureId);
}
