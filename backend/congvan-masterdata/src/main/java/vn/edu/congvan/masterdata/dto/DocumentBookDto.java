package vn.edu.congvan.masterdata.dto;

import java.util.UUID;
import vn.edu.congvan.masterdata.entity.BookType;
import vn.edu.congvan.masterdata.entity.ConfidentialityScope;

public record DocumentBookDto(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        BookType bookType,
        ConfidentialityScope confidentialityScope,
        String prefix,
        String description,
        boolean active) {}
