package vn.edu.congvan.auth.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.auth.entity.RefreshTokenEntity;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query(
            "UPDATE RefreshTokenEntity t "
                    + "SET t.revokedAt = :at, t.revokedReason = :reason "
                    + "WHERE t.userId = :userId AND t.revokedAt IS NULL")
    int revokeAllForUser(
            @Param("userId") UUID userId,
            @Param("at") OffsetDateTime at,
            @Param("reason") String reason);
}
