package vn.edu.congvan.integration.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.common.exception.BusinessException;

/**
 * Ghi event vào {@code outbox_messages} TRONG cùng transaction nghiệp vụ
 * (Propagation.MANDATORY) — đảm bảo nếu nghiệp vụ rollback thì event cũng
 * rollback (atomic with business write).
 *
 * <p>Worker {@link OutboxPublisher} sẽ poll bảng và publish lên RabbitMQ
 * trong transaction riêng.
 */
@Service
@RequiredArgsConstructor
public class OutboxRecorder {

    private static final String SQL =
            "INSERT INTO outbox_messages ("
                    + "  aggregate_type, aggregate_id, event_type, routing_key,"
                    + "  payload, headers, status, attempt_count, created_at, available_at"
                    + ") VALUES (?,?,?,?, ?::jsonb, ?::jsonb, 'PENDING', 0, NOW(), NOW())";

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(OutboxEvent event) {
        jdbc.update(
                SQL,
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.routingKey(),
                toJson(event.payload()),
                toJson(event.headers()));
    }

    /** Convenience builder for caller. */
    public OutboxEvent.Builder event(String aggregateType, String aggregateId, String eventType) {
        return OutboxEvent.builder(aggregateType, aggregateId, eventType);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(
                    "OUTBOX_SERIALIZE_FAIL",
                    "Không serialize được payload outbox: " + e.getMessage());
        }
    }

    /** Helper toMap cho payload đơn giản. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> map(Object... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("kv phải có số phần tử chẵn");
        }
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        // Add timestamp
        map.put("occurredAt", OffsetDateTime.now().toString());
        return map;
    }
}
