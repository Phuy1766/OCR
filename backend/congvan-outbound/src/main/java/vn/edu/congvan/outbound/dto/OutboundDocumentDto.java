package vn.edu.congvan.outbound.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import vn.edu.congvan.inbound.dto.DocumentFileDto;
import vn.edu.congvan.inbound.entity.DocumentStatus;

public record OutboundDocumentDto(
        UUID id,
        UUID documentTypeId,
        UUID confidentialityLevelId,
        UUID priorityLevelId,
        String subject,
        String summary,
        DocumentStatus status,
        UUID approvedVersionId,
        UUID bookId,
        Integer bookYear,
        Long bookNumber,
        LocalDate issuedDate,
        UUID organizationId,
        UUID departmentId,
        LocalDate dueDate,
        boolean recalled,
        OffsetDateTime createdAt,
        UUID createdBy,
        List<DocumentFileDto> latestFiles,
        List<DocumentVersionDto> versions,
        List<ApprovalDto> approvals) {}
