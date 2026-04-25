package vn.edu.congvan.masterdata.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.masterdata.entity.ConfidentialityLevelEntity;

@Repository
public interface ConfidentialityLevelRepository
        extends JpaRepository<ConfidentialityLevelEntity, UUID> {
    Optional<ConfidentialityLevelEntity> findByCode(String code);

    @Query(
            "SELECT c FROM ConfidentialityLevelEntity c WHERE c.active = true "
                    + "ORDER BY c.level")
    List<ConfidentialityLevelEntity> findAllActive();
}
