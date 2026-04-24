-- =========================================================================
-- V3 — Hạ tầng cross-cutting:
--   audit_logs    : append-only, ghi mọi thao tác write (BR-10).
--   outbox_messages: append-only, publish async event sang RabbitMQ
--                    (Outbox Pattern — không gọi MQ trong transaction nghiệp vụ).
-- Hai bảng này được tạo ngay Phase 0 để các module Phase 1+ dùng được.
-- =========================================================================

-- ================= audit_logs =================
CREATE TABLE audit_logs (
    id                BIGSERIAL PRIMARY KEY,
    event_time        TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    actor_id          UUID             NULL,             -- user thực hiện (NULL nếu system)
    actor_username    VARCHAR(100)     NULL,
    actor_ip          VARCHAR(45)      NULL,             -- hỗ trợ IPv6
    action            VARCHAR(100)     NOT NULL,         -- CREATE_DOCUMENT, APPROVE, SIGN, ...
    entity_type       VARCHAR(100)     NOT NULL,         -- documents, users, approvals, ...
    entity_id         VARCHAR(100)     NULL,             -- UUID hoặc BIGINT string
    old_value         JSONB            NULL,             -- snapshot trước (khi UPDATE/DELETE)
    new_value         JSONB            NULL,             -- snapshot sau (khi CREATE/UPDATE)
    result            VARCHAR(20)      NOT NULL DEFAULT 'SUCCESS',  -- SUCCESS/FAILURE
    error_message     TEXT             NULL,
    request_id        VARCHAR(64)      NULL,             -- trace id
    user_agent        VARCHAR(500)     NULL,
    extra             JSONB            NULL              -- metadata tự do
);

CREATE INDEX idx_audit_logs_event_time   ON audit_logs (event_time DESC);
CREATE INDEX idx_audit_logs_actor        ON audit_logs (actor_id) WHERE actor_id IS NOT NULL;
CREATE INDEX idx_audit_logs_entity       ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_action       ON audit_logs (action);

COMMENT ON TABLE audit_logs IS
    'Append-only audit log (BR-10 NĐ 30/2020). Không được phép UPDATE/DELETE ở tầng ứng dụng.';

-- Chặn UPDATE/DELETE từ application (chỉ cho superuser vận hành lưu trữ làm thủ công).
CREATE OR REPLACE FUNCTION audit_logs_reject_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs là bảng append-only — không cho phép %', TG_OP
        USING ERRCODE = 'check_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_logs_no_update
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit_logs_reject_modification();

-- ================= outbox_messages =================
CREATE TABLE outbox_messages (
    id                BIGSERIAL PRIMARY KEY,
    aggregate_type    VARCHAR(100)  NOT NULL,            -- DOCUMENT, APPROVAL, OCR_JOB, ...
    aggregate_id      VARCHAR(100)  NOT NULL,
    event_type        VARCHAR(100)  NOT NULL,            -- DocumentCreated, ApprovalCompleted, ...
    routing_key       VARCHAR(200)  NOT NULL,            -- RabbitMQ routing key
    payload           JSONB         NOT NULL,
    headers           JSONB         NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
        -- PENDING / PUBLISHING / PUBLISHED / FAILED
    attempt_count     INT           NOT NULL DEFAULT 0,
    last_error        TEXT          NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    available_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),  -- retry backoff
    published_at      TIMESTAMPTZ   NULL,
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING','PUBLISHING','PUBLISHED','FAILED'))
);

CREATE INDEX idx_outbox_status_available
    ON outbox_messages (status, available_at)
    WHERE status IN ('PENDING', 'FAILED');

CREATE INDEX idx_outbox_aggregate ON outbox_messages (aggregate_type, aggregate_id);

COMMENT ON TABLE outbox_messages IS
    'Outbox Pattern: lưu event trong transaction nghiệp vụ, worker publish sang RabbitMQ sau.';
