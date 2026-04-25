package vn.edu.congvan.masterdata.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateDepartmentRequest(
        @Size(min = 2, max = 255) String name,
        UUID parentId,
        UUID headUserId,
        Boolean active) {}
