package vn.edu.congvan.masterdata.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.masterdata.entity.PriorityLevelEntity;

@Repository
public interface PriorityLevelRepository extends JpaRepository<PriorityLevelEntity, UUID> {
    Optional<PriorityLevelEntity> findByCode(String code);

    @Query("SELECT p FROM PriorityLevelEntity p WHERE p.active = true ORDER BY p.level")
    List<PriorityLevelEntity> findAllActive();
}
