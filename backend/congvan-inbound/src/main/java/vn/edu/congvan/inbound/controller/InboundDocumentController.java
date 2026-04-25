package vn.edu.congvan.inbound.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.inbound.dto.CreateInboundRequest;
import vn.edu.congvan.inbound.dto.InboundDocumentDto;
import vn.edu.congvan.inbound.dto.RecallRequest;
import vn.edu.congvan.inbound.entity.DocumentFileEntity;
import vn.edu.congvan.inbound.entity.DocumentStatus;
import vn.edu.congvan.inbound.repository.DocumentFileRepository;
import vn.edu.congvan.inbound.service.InboundDocumentService;
import vn.edu.congvan.inbound.service.InboundDocumentService.DocumentSearchCriteria;
import vn.edu.congvan.inbound.service.PermissionScope;
import vn.edu.congvan.inbound.service.UploadedFile;
import vn.edu.congvan.integration.storage.MinioFileService;

/**
 * Controller cho công văn đến.
 *
 * <p>{@code POST /} dùng multipart: phần JSON metadata bind vào
 * {@code @RequestPart("data") String} (parse thủ công + validate) + phần files
 * trong {@code @RequestPart("files") MultipartFile[]}.
 */
@RestController
@RequestMapping("/api/inbound-documents")
@RequiredArgsConstructor
public class InboundDocumentController {

    private final InboundDocumentService service;
    private final DocumentFileRepository docFiles;
    private final MinioFileService minio;
    private final ObjectMapper json;
    private final Validator validator;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INBOUND:CREATE')")
    public ResponseEntity<ApiResponse<InboundDocumentDto>> register(
            @RequestPart("data") String dataJson,
            @RequestPart("files") MultipartFile[] files,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http)
            throws IOException {
        CreateInboundRequest req = parseAndValidate(dataJson);
        if (files == null || files.length == 0) {
            throw new BusinessException(
                    "INBOUND_FILE_REQUIRED",
                    "VB đến phải có ít nhất 1 file đính kèm.");
        }
        List<UploadedFile> uploads = new ArrayList<>(files.length);
        for (MultipartFile f : files) {
            if (f.isEmpty()) continue;
            uploads.add(new UploadedFile(f.getOriginalFilename(), f.getContentType(), f.getBytes()));
        }
        InboundDocumentDto dto = service.register(req, uploads, actor, clientIp(http));
        return ResponseEntity.status(201).body(ApiResponse.ok(dto));
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('INBOUND:READ_OWN') or hasAuthority('INBOUND:READ_DEPT') "
                    + "or hasAuthority('INBOUND:READ_ALL')")
    public ApiResponse<Page<InboundDocumentDto>> list(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID bookId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal actor) {
        if (!PermissionScope.canRead(actor)) {
            throw new BusinessException("AUTH_FORBIDDEN", "Không có quyền xem.");
        }
        DocumentSearchCriteria criteria =
                new DocumentSearchCriteria(status, organizationId, departmentId, bookId,
                        fromDate, toDate, q);
        return ApiResponse.ok(service.list(criteria, actor, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('INBOUND:READ_OWN') or hasAuthority('INBOUND:READ_DEPT') "
                    + "or hasAuthority('INBOUND:READ_ALL')")
    public ApiResponse<InboundDocumentDto> get(
            @PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.ok(service.getById(id, actor));
    }

    @GetMapping("/{id}/files/{fileId}/download")
    @PreAuthorize(
            "hasAuthority('INBOUND:READ_OWN') or hasAuthority('INBOUND:READ_DEPT') "
                    + "or hasAuthority('INBOUND:READ_ALL')")
    public ResponseEntity<byte[]> download(
            @PathVariable UUID id,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal AuthPrincipal actor) {
        // Re-check scope qua getById trước khi cho download
        service.getById(id, actor);
        DocumentFileEntity file =
                docFiles.findById(fileId)
                        .filter(f -> f.getDocumentId().equals(id))
                        .orElseThrow(
                                () -> new BusinessException(
                                        "FILE_NOT_FOUND", "File không tồn tại."));
        byte[] content = minio.download(file.getStorageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getFileName() + "\"")
                .body(content);
    }

    @PostMapping("/{id}/recall")
    @PreAuthorize("hasAuthority('INBOUND:RECALL')")
    public ApiResponse<Void> recall(
            @PathVariable UUID id,
            @Valid @RequestBody RecallRequest body,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        service.recall(id, body.reason(), actor, clientIp(http));
        return ApiResponse.ok(null);
    }

    private CreateInboundRequest parseAndValidate(String dataJson) throws IOException {
        CreateInboundRequest req = json.readValue(dataJson, CreateInboundRequest.class);
        var violations = validator.validate(req);
        if (!violations.isEmpty()) {
            String msg =
                    violations.stream()
                            .findFirst()
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .orElse("Dữ liệu không hợp lệ");
            throw new BusinessException("VALIDATION_FAILED", msg);
        }
        return req;
    }

    private static String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
