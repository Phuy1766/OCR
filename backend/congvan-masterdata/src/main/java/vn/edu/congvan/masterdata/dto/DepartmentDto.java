package vn.edu.congvan.masterdata.dto;

import java.util.UUID;

public record DepartmentDto(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        UUID parentId,
        UUID headUserId,
        boolean active) {}
