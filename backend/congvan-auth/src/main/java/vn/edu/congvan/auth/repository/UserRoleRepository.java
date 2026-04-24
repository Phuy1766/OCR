package vn.edu.congvan.auth.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.auth.entity.UserRoleEntity;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {

    @Query(
            "SELECT ur FROM UserRoleEntity ur "
                    + "JOIN FETCH ur.role r "
                    + "LEFT JOIN FETCH r.permissions "
                    + "WHERE ur.userId = :userId "
                    + "  AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)")
    List<UserRoleEntity> findActiveByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserRoleEntity ur WHERE ur.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
