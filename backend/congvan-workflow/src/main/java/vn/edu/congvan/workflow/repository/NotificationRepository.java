package vn.edu.congvan.workflow.repository;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.congvan.workflow.entity.NotificationEntity;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query(
            "SELECT n FROM NotificationEntity n "
                    + "WHERE n.recipientUserId = :userId "
                    + "  AND (:unreadOnly = false OR n.readAt IS NULL) "
                    + "ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findForUser(
            @Param("userId") UUID userId,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable);

    @Query(
            "SELECT COUNT(n) FROM NotificationEntity n "
                    + "WHERE n.recipientUserId = :userId AND n.readAt IS NULL")
    long countUnread(@Param("userId") UUID userId);

    @Modifying
    @Query(
            "UPDATE NotificationEntity n SET n.readAt = :now "
                    + "WHERE n.recipientUserId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}
