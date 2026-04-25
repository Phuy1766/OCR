package vn.edu.congvan.masterdata.dto;

import java.util.UUID;

public record PriorityLevelDto(
        UUID id,
        String code,
        String name,
        int level,
        String color,
        Integer slaHours,
        String description,
        int displayOrder,
        boolean urgent) {}
