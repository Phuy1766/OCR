package vn.edu.congvan.integration.outbox;

import java.util.Map;

/**
 * 1 event ghi vào outbox.
 *
 * @param aggregateType  vd "documents", "approvals"
 * @param aggregateId    vd document UUID
 * @param eventType      vd "InboundDocumentRegistered"
 * @param routingKey     RabbitMQ routing key (vd "document.inbound.registered")
 * @param payload        JSON-serializable map
 * @param headers        optional headers (cũng JSONB)
 */
public record OutboxEvent(
        String aggregateType,
        String aggregateId,
        String eventType,
        String routingKey,
        Object payload,
        Map<String, Object> headers) {

    public static Builder builder(String aggregateType, String aggregateId, String eventType) {
        return new Builder(aggregateType, aggregateId, eventType);
    }

    public static final class Builder {
        private final String aggregateType;
        private final String aggregateId;
        private final String eventType;
        private String routingKey;
        private Object payload;
        private Map<String, Object> headers;

        private Builder(String aggregateType, String aggregateId, String eventType) {
            this.aggregateType = aggregateType;
            this.aggregateId = aggregateId;
            this.eventType = eventType;
        }

        public Builder routingKey(String routingKey) {
            this.routingKey = routingKey;
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public Builder headers(Map<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        public OutboxEvent build() {
            if (routingKey == null) {
                throw new IllegalStateException("routingKey is required");
            }
            return new OutboxEvent(
                    aggregateType, aggregateId, eventType, routingKey, payload, headers);
        }
    }
}
