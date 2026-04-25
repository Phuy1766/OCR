package vn.edu.congvan.outbound.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.congvan.outbound.entity.ApprovalDecision;

public record ApprovalDecisionRequest(
        @NotNull ApprovalDecision decision, @Size(max = 2000) String comment) {}
