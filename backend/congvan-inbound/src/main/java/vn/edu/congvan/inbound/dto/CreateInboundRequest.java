package vn.edu.congvan.inbound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import vn.edu.congvan.inbound.entity.ReceivedFromChannel;

/**
 * Request tạo VB đến — phần JSON metadata trong multipart.
 * Files đi kèm trong phần khác của multipart.
 */
public record CreateInboundRequest(
        @NotNull UUID documentTypeId,
        @NotNull UUID confidentialityLevelId,
        @NotNull UUID priorityLevelId,
        @NotBlank @Size(min = 5, max = 1000) String subject,
        @Size(max = 5000) String summary,
        @NotNull UUID bookId,
        @NotNull UUID organizationId,
        UUID departmentId,
        LocalDate receivedDate,
        ReceivedFromChannel receivedFromChannel,
        @Size(max = 100) String externalReferenceNumber,
        @Size(max = 500) String externalIssuer,
        LocalDate externalIssuedDate,
        LocalDate dueDate) {}
