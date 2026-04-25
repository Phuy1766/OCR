-- =========================================================================
-- V15 — Workflow & Notification.
-- Căn cứ: architecture §5.5, BR-04 (ưu tiên VB khẩn), BR-10 (audit).
-- Phase 5: workflow đơn giản 1-step (TP gán cho chuyên viên → complete).
-- =========================================================================

-- ================= workflow_instances =================
CREATE TABLE workflow_instances (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID         NOT NULL UNIQUE REFERENCES documents(id) ON DELETE CASCADE,
    template_code   VARCHAR(50)  NOT NULL,
        -- Phase 5: STANDARD_INBOUND. Phase 6+ có thể có template khác.
    state           VARCHAR(30)  NOT NULL,
        -- INITIAL → ASSIGNED → IN_PROGRESS → COMPLETED → CLOSED, hoặc CANCELLED
    started_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ  NULL,

    CONSTRAINT chk_workflow_state CHECK (state IN
        ('INITIAL','ASSIGNED','IN_PROGRESS','COMPLETED','CLOSED','CANCELLED'))
);
CREATE INDEX idx_workflow_doc ON workflow_instances(document_id);
COMMENT ON TABLE workflow_instances IS
    'Trạng thái workflow hiện tại của VB. Phase 5: 1-1 với documents.';

-- ================= workflow_steps =================
-- Append-only log của mọi action trong workflow.
CREATE TABLE workflow_steps (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id       UUID         NOT NULL REFERENCES workflow_instances(id) ON DELETE CASCADE,
    step_type         VARCHAR(30)  NOT NULL,
        -- ASSIGN, REASSIGN, COMPLETE, REOPEN, COMMENT, FORWARD
    actor_id          UUID         NULL REFERENCES users(id),
    target_user_id    UUID         NULL REFERENCES users(id),
    target_dept_id    UUID         NULL REFERENCES departments(id),
    note              VARCHAR(2000) NULL,
    occurred_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_workflow_step_type CHECK (step_type IN
        ('ASSIGN','REASSIGN','COMPLETE','REOPEN','COMMENT','FORWARD'))
);
CREATE INDEX idx_workflow_steps_wf ON workflow_steps(workflow_id, occurred_at DESC);

-- Trigger chặn UPDATE/DELETE
CREATE OR REPLACE FUNCTION workflow_steps_no_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'workflow_steps là append-only — không cho phép %', TG_OP
        USING ERRCODE = 'check_violation';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER trg_workflow_steps_immutable
    BEFORE UPDATE OR DELETE ON workflow_steps
    FOR EACH ROW EXECUTE FUNCTION workflow_steps_no_modification();

-- ================= assignments =================
CREATE TABLE assignments (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id       UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    workflow_id       UUID         NOT NULL REFERENCES workflow_instances(id) ON DELETE CASCADE,
    assigned_to_user_id UUID       NOT NULL REFERENCES users(id),
    assigned_to_dept_id UUID       NULL REFERENCES departments(id),
    assigned_by       UUID         NOT NULL REFERENCES users(id),
    assigned_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    due_date          DATE         NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
        -- ACTIVE, COMPLETED, CANCELLED, REASSIGNED
    note              VARCHAR(2000) NULL,
    completed_at      TIMESTAMPTZ  NULL,
    completed_by      UUID         NULL REFERENCES users(id),
    result_summary    TEXT         NULL,

    CONSTRAINT chk_assignment_status CHECK (status IN
        ('ACTIVE','COMPLETED','CANCELLED','REASSIGNED'))
);
CREATE INDEX idx_assignments_user_active
    ON assignments(assigned_to_user_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_assignments_doc ON assignments(document_id);
CREATE INDEX idx_assignments_due ON assignments(due_date) WHERE status = 'ACTIVE' AND due_date IS NOT NULL;

-- ================= notifications =================
CREATE TABLE notifications (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(50)  NOT NULL,
        -- ASSIGNMENT, APPROVAL_REQUEST, DEADLINE_WARNING, STATUS_CHANGE,
        -- DOCUMENT_RECALLED, INFO
    title           VARCHAR(255) NOT NULL,
    body            TEXT         NULL,
    entity_type     VARCHAR(50)  NULL,
    entity_id       VARCHAR(100) NULL,
    metadata        JSONB        NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    read_at         TIMESTAMPTZ  NULL
);
CREATE INDEX idx_notifications_recipient_unread
    ON notifications(recipient_user_id, created_at DESC) WHERE read_at IS NULL;
CREATE INDEX idx_notifications_recipient
    ON notifications(recipient_user_id, created_at DESC);

COMMENT ON TABLE notifications IS
    'In-app notification. Phase 5: chỉ DB; email/push hoãn Phase 9.';

-- ================= WORKFLOW permissions =================
INSERT INTO permissions (code, name, resource, action, description) VALUES
    ('WORKFLOW:ASSIGN',       'Phân công xử lý VB',
     'WORKFLOW', 'ASSIGN',  'TRUONG_PHONG/VAN_THU phân công chuyên viên xử lý.'),
    ('WORKFLOW:REASSIGN',     'Phân công lại',
     'WORKFLOW', 'REASSIGN','Đổi người xử lý khi cần.'),
    ('WORKFLOW:HANDLE',       'Xử lý VB được giao',
     'WORKFLOW', 'HANDLE',  'Chuyên viên hoàn thành nhiệm vụ được giao.'),
    ('WORKFLOW:READ',         'Xem workflow VB',
     'WORKFLOW', 'READ',    'Xem lịch sử workflow + assignments.');

-- Mapping role:
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code LIKE 'WORKFLOW:%'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('TRUONG_PHONG','VAN_THU_CQ','VAN_THU_PB')
  AND p.code IN ('WORKFLOW:ASSIGN','WORKFLOW:REASSIGN','WORKFLOW:READ')
ON CONFLICT DO NOTHING;

-- LANH_DAO/CAP_PHO chỉ xem
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('LANH_DAO','CAP_PHO')
  AND p.code = 'WORKFLOW:READ'
ON CONFLICT DO NOTHING;

-- CHUYEN_VIEN: HANDLE + READ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CHUYEN_VIEN'
  AND p.code IN ('WORKFLOW:HANDLE','WORKFLOW:READ')
ON CONFLICT DO NOTHING;

-- LUU_TRU: read only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'LUU_TRU' AND p.code = 'WORKFLOW:READ'
ON CONFLICT DO NOTHING;
