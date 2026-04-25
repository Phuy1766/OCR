package vn.edu.congvan.integration.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Worker poll {@code outbox_messages} → publish RabbitMQ → mark PUBLISHED.
 * Mỗi tick xử lý batch ≤ {@code app.outbox.batch-size}. Failure tăng
 * {@code attempt_count}, đặt {@code available_at = NOW + 2^attempt} phút,
 * sau {@code max-attempts} → status FAILED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final String SELECT_BATCH =
            "SELECT id, aggregate_type, aggregate_id, event_type, routing_key, "
                    + "       payload, headers, attempt_count "
                    + "FROM outbox_messages "
                    + "WHERE status IN ('PENDING','FAILED') "
                    + "  AND available_at <= NOW() "
                    + "  AND attempt_count < ? "
                    + "ORDER BY created_at "
                    + "LIMIT ? "
                    + "FOR UPDATE SKIP LOCKED";

    private static final String MARK_PUBLISHED =
            "UPDATE outbox_messages "
                    + "SET status='PUBLISHED', published_at=NOW(), last_error=NULL "
                    + "WHERE id=?";

    private static final String MARK_FAILED =
            "UPDATE outbox_messages "
                    + "SET status=CASE WHEN ?>=? THEN 'FAILED' ELSE 'PENDING' END, "
                    + "    attempt_count=?, "
                    + "    available_at=NOW() + (INTERVAL '1 minute' * (POW(2, ?))), "
                    + "    last_error=? "
                    + "WHERE id=?";

    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;
    private final ObjectMapper mapper;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishBatch() {
        int batchSize = 50;
        int maxAttempts = 5;
        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList(SELECT_BATCH, maxAttempts, batchSize);
        } catch (EmptyResultDataAccessException e) {
            return;
        }
        if (rows.isEmpty()) return;
        log.debug("Outbox publishing batch of {}", rows.size());
        for (Map<String, Object> row : rows) {
            publishOne(row, maxAttempts);
        }
    }

    private void publishOne(Map<String, Object> row, int maxAttempts) {
        Long id = ((Number) row.get("id")).longValue();
        String routingKey = (String) row.get("routing_key");
        String eventType = (String) row.get("event_type");
        String payloadJson = String.valueOf(row.get("payload"));
        int attemptCount = ((Number) row.get("attempt_count")).intValue();

        try {
            Object payload = mapper.readTree(payloadJson);
            MessageProperties props = new MessageProperties();
            props.setHeader("eventType", eventType);
            props.setHeader("aggregateType", row.get("aggregate_type"));
            props.setHeader("aggregateId", row.get("aggregate_id"));
            props.setHeader("eventId", UUID.randomUUID().toString());
            props.setMessageId(String.valueOf(id));
            props.setTimestamp(java.util.Date.from(OffsetDateTime.now().toInstant()));
            props.setContentType("application/json");

            rabbit.convertAndSend(
                    OutboxConfig.EXCHANGE_EVENTS,
                    routingKey,
                    Map.of("type", eventType, "data", payload),
                    m -> {
                        m.getMessageProperties().getHeaders().putAll(props.getHeaders());
                        return m;
                    });
            jdbc.update(MARK_PUBLISHED, id);
        } catch (Exception e) {
            int newAttempt = attemptCount + 1;
            int backoffExp = Math.min(newAttempt, 10); // cap 2^10 = 1024 phút
            log.warn("Outbox publish failed for id={} (attempt {}): {}",
                    id, newAttempt, e.getMessage());
            jdbc.update(
                    MARK_FAILED,
                    newAttempt,
                    maxAttempts,
                    newAttempt,
                    backoffExp,
                    truncate(e.getMessage(), 500),
                    id);
        }
    }

    @SuppressWarnings("unused")
    private static Duration backoff(int attempt) {
        return Duration.ofMinutes((long) Math.pow(2, attempt));
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    @SuppressWarnings("unused")
    private static Map<String, Object> emptyHeaders() {
        return new HashMap<>();
    }
}
