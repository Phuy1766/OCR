package vn.edu.congvan.outbound.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.congvan.outbound.entity.ApprovalDecision;
import vn.edu.congvan.outbound.entity.ApprovalLevel;

public record ApprovalDto(
        UUID id,
        UUID documentId,
        UUID versionId,
        ApprovalLevel approvalLevel,
        ApprovalDecision decision,
        String comment,
        UUID decidedBy,
        OffsetDateTime decidedAt) {}
