package vn.edu.congvan.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.workflow.dto.NotificationDto;
import vn.edu.congvan.workflow.entity.NotificationEntity;
import vn.edu.congvan.workflow.entity.NotificationType;
import vn.edu.congvan.workflow.repository.NotificationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final ObjectMapper json;

    @Transactional
    public void notify(
            UUID recipientUserId,
            NotificationType type,
            String title,
            String body,
            String entityType,
            String entityId,
            Map<String, Object> metadata) {
        if (recipientUserId == null) {
            log.debug("notify() bỏ qua: recipient null");
            return;
        }
        NotificationEntity n = new NotificationEntity();
        n.setId(UUID.randomUUID());
        n.setRecipientUserId(recipientUserId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setEntityType(entityType);
        n.setEntityId(entityId);
        n.setCreatedAt(OffsetDateTime.now());
        if (metadata != null && !metadata.isEmpty()) {
            try {
                n.setMetadata(json.writeValueAsString(metadata));
            } catch (Exception e) {
                log.warn("Không serialize được notification metadata: {}", e.getMessage());
            }
        }
        repo.save(n);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> list(UUID userId, boolean unreadOnly, int page, int size) {
        return repo.findForUser(
                        userId,
                        unreadOnly,
                        PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100)))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return repo.countUnread(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        NotificationEntity n =
                repo.findById(notificationId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "NOTIFICATION_NOT_FOUND",
                                                "Không tìm thấy thông báo."));
        if (!n.getRecipientUserId().equals(userId)) {
            throw new BusinessException(
                    "AUTH_FORBIDDEN", "Không thể đánh dấu thông báo của người khác.");
        }
        if (n.getReadAt() == null) {
            n.setReadAt(OffsetDateTime.now());
        }
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repo.markAllRead(userId, OffsetDateTime.now());
    }

    private NotificationDto toDto(NotificationEntity n) {
        return new NotificationDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getEntityType(),
                n.getEntityId(),
                n.getMetadata(),
                n.getCreatedAt(),
                n.getReadAt());
    }
}
