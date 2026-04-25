package vn.edu.congvan.masterdata.dto;

import jakarta.validation.constraints.Size;

public record UpdateDocumentBookRequest(
        @Size(min = 2, max = 255) String name,
        @Size(max = 50) String prefix,
        @Size(max = 500) String description,
        Boolean active) {}
