package vn.edu.congvan.inbound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecallRequest(@NotBlank @Size(min = 5, max = 1000) String reason) {}
