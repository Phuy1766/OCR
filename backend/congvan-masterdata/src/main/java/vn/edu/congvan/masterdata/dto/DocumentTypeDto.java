package vn.edu.congvan.masterdata.dto;

import java.util.UUID;

public record DocumentTypeDto(
        UUID id,
        String code,
        String abbreviation,
        String name,
        String description,
        int displayOrder,
        boolean system,
        boolean active) {}
