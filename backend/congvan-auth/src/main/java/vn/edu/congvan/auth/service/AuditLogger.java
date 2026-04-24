package vn.edu.congvan.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi audit_logs (append-only). Chạy trong TRANSACTION hiện tại của caller
 * (MANDATORY) — nghĩa là nếu nghiệp vụ rollback thì audit cũng rollback.
 *
 * <p>Dùng JdbcTemplate trực tiếp (KHÔNG qua JPA) để tránh tự động tham gia
 * snapshot cache của Hibernate, và để đảm bảo SQL tuân thủ chính xác schema
 * append-only (trigger từ V3 chặn UPDATE/DELETE).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogger {

    private static final String SQL =
            "INSERT INTO audit_logs ("
                    + "  event_time, actor_id, actor_username, actor_ip, action,"
                    + "  entity_type, entity_id, old_value, new_value, result,"
                    + "  error_message, request_id, user_agent, extra"
                    + ") VALUES (?,?,?,?,?, ?,?,?::jsonb,?::jsonb, ?, ?,?,?, ?::jsonb)";

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(AuditRecord rec) {
        jdbc.update(
                SQL,
                OffsetDateTime.now(),
                rec.actorId(),
                rec.actorUsername(),
                rec.actorIp(),
                rec.action(),
                rec.entityType(),
                rec.entityId(),
                toJson(rec.oldValue()),
                toJson(rec.newValue()),
                rec.success() ? "SUCCESS" : "FAILURE",
                rec.errorMessage(),
                rec.requestId(),
                rec.userAgent(),
                toJson(rec.extra()));
    }

    /** Convenience cho log thành công đơn giản. */
    public void logSuccess(
            UUID actorId,
            String actorUsername,
            String actorIp,
            String action,
            String entityType,
            String entityId) {
        log(
                AuditRecord.builder()
                        .actorId(actorId)
                        .actorUsername(actorUsername)
                        .actorIp(actorIp)
                        .action(action)
                        .entityType(entityType)
                        .entityId(entityId)
                        .success(true)
                        .build());
    }

    public void logFailure(
            UUID actorId,
            String actorUsername,
            String actorIp,
            String action,
            String entityType,
            String entityId,
            String errorMessage) {
        log(
                AuditRecord.builder()
                        .actorId(actorId)
                        .actorUsername(actorUsername)
                        .actorIp(actorIp)
                        .action(action)
                        .entityType(entityType)
                        .entityId(entityId)
                        .success(false)
                        .errorMessage(errorMessage)
                        .build());
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Không serialize được audit value: {}", e.getMessage());
            return null;
        }
    }

    @lombok.Builder
    public record AuditRecord(
            UUID actorId,
            String actorUsername,
            String actorIp,
            String action,
            String entityType,
            String entityId,
            Object oldValue,
            Object newValue,
            boolean success,
            String errorMessage,
            String requestId,
            String userAgent,
            Map<String, Object> extra) {}
}
