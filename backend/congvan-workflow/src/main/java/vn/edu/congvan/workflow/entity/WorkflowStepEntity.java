package vn.edu.congvan.workflow.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Append-only — trigger DB chặn UPDATE/DELETE. */
@Entity
@Table(name = "workflow_steps")
@Getter
@Setter
@NoArgsConstructor
public class WorkflowStepEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "workflow_id", nullable = false, updatable = false)
    private UUID workflowId;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 30, updatable = false)
    private WorkflowStepType stepType;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "target_user_id", updatable = false)
    private UUID targetUserId;

    @Column(name = "target_dept_id", updatable = false)
    private UUID targetDeptId;

    @Column(length = 2000, updatable = false)
    private String note;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;
}
