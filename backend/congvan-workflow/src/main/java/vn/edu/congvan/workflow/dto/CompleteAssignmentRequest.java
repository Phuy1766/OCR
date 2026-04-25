package vn.edu.congvan.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteAssignmentRequest(
        @NotBlank @Size(min = 5, max = 5000) String resultSummary) {}
