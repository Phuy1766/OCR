package vn.edu.congvan.inbound.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import vn.edu.congvan.inbound.entity.DocumentStatus;
import vn.edu.congvan.inbound.entity.ReceivedFromChannel;

/** DTO chi tiết VB đến — kèm danh sách files. */
public record InboundDocumentDto(
        UUID id,
        UUID documentTypeId,
        UUID confidentialityLevelId,
        UUID priorityLevelId,
        String subject,
        String summary,
        DocumentStatus status,
        UUID bookId,
        Integer bookYear,
        Long bookNumber,
        LocalDate receivedDate,
        ReceivedFromChannel receivedFromChannel,
        String externalReferenceNumber,
        String externalIssuer,
        LocalDate externalIssuedDate,
        UUID currentHandlerUserId,
        UUID currentHandlerDeptId,
        LocalDate dueDate,
        UUID organizationId,
        UUID departmentId,
        boolean recalled,
        OffsetDateTime recalledAt,
        String recalledReason,
        OffsetDateTime createdAt,
        UUID createdBy,
        List<DocumentFileDto> files) {}
