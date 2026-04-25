package vn.edu.congvan.masterdata.dto;

import java.util.UUID;

public record ConfidentialityLevelDto(
        UUID id,
        String code,
        String name,
        int level,
        String color,
        String description,
        int displayOrder,
        boolean requiresSecretBook) {}
