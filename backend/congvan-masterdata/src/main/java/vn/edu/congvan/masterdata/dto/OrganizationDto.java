package vn.edu.congvan.masterdata.dto;

import java.util.UUID;

public record OrganizationDto(
        UUID id,
        String code,
        String name,
        String fullName,
        String taxCode,
        String address,
        String phone,
        String email,
        UUID parentId,
        boolean active) {}
