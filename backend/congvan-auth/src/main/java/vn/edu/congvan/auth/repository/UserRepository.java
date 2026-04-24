package vn.edu.congvan.auth.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.auth.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query(
            "SELECT u FROM UserEntity u "
                    + "WHERE (:query IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) "
                    + "     OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) "
                    + "     OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<UserEntity> search(@Param("query") String query, Pageable pageable);

    @Modifying
    @Query(
            "UPDATE UserEntity u "
                    + "SET u.failedLoginCount = u.failedLoginCount + 1 "
                    + "WHERE u.id = :id")
    int incrementFailedLoginCount(@Param("id") UUID id);

    @Modifying
    @Query(
            "UPDATE UserEntity u "
                    + "SET u.failedLoginCount = 0, u.lastLoginAt = :at, u.lastLoginIp = :ip "
                    + "WHERE u.id = :id")
    int markLoginSuccess(
            @Param("id") UUID id,
            @Param("at") OffsetDateTime at,
            @Param("ip") String ip);

    @Modifying
    @Query(
            "UPDATE UserEntity u "
                    + "SET u.locked = true, u.lockedUntil = :until "
                    + "WHERE u.id = :id")
    int lockAccount(@Param("id") UUID id, @Param("until") OffsetDateTime until);

    @Modifying
    @Query(
            "UPDATE UserEntity u "
                    + "SET u.locked = false, u.lockedUntil = null, u.failedLoginCount = 0 "
                    + "WHERE u.id = :id")
    int unlockAccount(@Param("id") UUID id);
}
