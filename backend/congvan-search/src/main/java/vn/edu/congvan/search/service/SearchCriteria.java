package vn.edu.congvan.search.service;

import java.time.LocalDate;
import java.util.UUID;

public record SearchCriteria(
        String query,
        String direction,
        String status,
        UUID organizationId,
        UUID bookId,
        UUID confidentialityLevelId,
        UUID priorityLevelId,
        LocalDate fromDate,
        LocalDate toDate) {}
