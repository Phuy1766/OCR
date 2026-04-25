package vn.edu.congvan.masterdata.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(min = 2, max = 255) String name,
        @Size(max = 500) String fullName,
        @Size(max = 20) String taxCode,
        @Size(max = 500) String address,
        @Size(max = 30) String phone,
        @Email @Size(max = 255) String email,
        Boolean active) {}
