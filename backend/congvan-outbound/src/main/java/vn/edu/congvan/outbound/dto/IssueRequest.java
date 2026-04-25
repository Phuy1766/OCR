package vn.edu.congvan.outbound.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record IssueRequest(@NotNull UUID bookId, LocalDate issuedDate) {}
