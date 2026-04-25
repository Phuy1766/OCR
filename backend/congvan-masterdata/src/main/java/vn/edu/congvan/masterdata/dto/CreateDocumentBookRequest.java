package vn.edu.congvan.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import vn.edu.congvan.masterdata.entity.BookType;
import vn.edu.congvan.masterdata.entity.ConfidentialityScope;

public record CreateDocumentBookRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(min = 2, max = 50) String code,
        @NotBlank @Size(min = 2, max = 255) String name,
        @NotNull BookType bookType,
        @NotNull ConfidentialityScope confidentialityScope,
        @Size(max = 50) String prefix,
        @Size(max = 500) String description) {}
