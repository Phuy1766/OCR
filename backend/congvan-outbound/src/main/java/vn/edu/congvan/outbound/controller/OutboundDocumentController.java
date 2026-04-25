package vn.edu.congvan.outbound.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.inbound.dto.RecallRequest;
import vn.edu.congvan.inbound.service.UploadedFile;
import vn.edu.congvan.outbound.dto.ApprovalDecisionRequest;
import vn.edu.congvan.outbound.dto.CreateOutboundDraftRequest;
import vn.edu.congvan.outbound.dto.IssueRequest;
import vn.edu.congvan.outbound.dto.OutboundDocumentDto;
import vn.edu.congvan.outbound.dto.UpdateOutboundDraftRequest;
import vn.edu.congvan.outbound.service.OutboundDocumentService;

@RestController
@RequestMapping("/api/outbound-documents")
@RequiredArgsConstructor
public class OutboundDocumentController {

    private final OutboundDocumentService service;
    private final ObjectMapper json;
    private final Validator validator;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('OUTBOUND:CREATE_DRAFT')")
    public ResponseEntity<ApiResponse<OutboundDocumentDto>> createDraft(
            @RequestPart("data") String dataJson,
            @RequestPart("files") MultipartFile[] files,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http)
            throws IOException {
        CreateOutboundDraftRequest req = parseAndValidate(dataJson, CreateOutboundDraftRequest.class);
        List<UploadedFile> uploads = collectUploads(files);
        OutboundDocumentDto dto = service.createDraft(req, uploads, actor, clientIp(http));
        return ResponseEntity.status(201).body(ApiResponse.ok(dto));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('OUTBOUND:UPDATE')")
    public ApiResponse<OutboundDocumentDto> updateDraft(
            @PathVariable UUID id,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http)
            throws IOException {
        UpdateOutboundDraftRequest req =
                parseAndValidate(dataJson, UpdateOutboundDraftRequest.class);
        List<UploadedFile> uploads = files == null ? null : collectUploads(files);
        return ApiResponse.ok(service.updateDraft(id, req, uploads, actor, clientIp(http)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('OUTBOUND:SUBMIT')")
    public ApiResponse<OutboundDocumentDto> submit(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        return ApiResponse.ok(service.submit(id, actor, clientIp(http)));
    }

    @PostMapping("/{id}/approvals/dept")
    @PreAuthorize("hasAuthority('OUTBOUND:APPROVE_DEPT')")
    public ApiResponse<OutboundDocumentDto> approveDept(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalDecisionRequest body,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        return ApiResponse.ok(service.approveDept(id, body, actor, clientIp(http)));
    }

    @PostMapping("/{id}/approvals/leader")
    @PreAuthorize("hasAuthority('OUTBOUND:APPROVE_LEADER')")
    public ApiResponse<OutboundDocumentDto> approveLeader(
            @PathVariable UUID id,
            @Valid @RequestBody ApprovalDecisionRequest body,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        return ApiResponse.ok(service.approveLeader(id, body, actor, clientIp(http)));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('OUTBOUND:ISSUE')")
    public ApiResponse<OutboundDocumentDto> issue(
            @PathVariable UUID id,
            @Valid @RequestBody IssueRequest body,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        return ApiResponse.ok(service.issue(id, body, actor, clientIp(http)));
    }

    @PostMapping("/{id}/recall")
    @PreAuthorize("hasAuthority('OUTBOUND:RECALL')")
    public ApiResponse<Void> recall(
            @PathVariable UUID id,
            @Valid @RequestBody RecallRequest body,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        service.recall(id, body.reason(), actor, clientIp(http));
        return ApiResponse.ok(null);
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('OUTBOUND:READ_OWN') or hasAuthority('OUTBOUND:READ_DEPT') "
                    + "or hasAuthority('OUTBOUND:READ_ALL')")
    public ApiResponse<Page<OutboundDocumentDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.ok(service.list(page, size, actor));
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('OUTBOUND:READ_OWN') or hasAuthority('OUTBOUND:READ_DEPT') "
                    + "or hasAuthority('OUTBOUND:READ_ALL')")
    public ApiResponse<OutboundDocumentDto> get(
            @PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.ok(service.getById(id, actor));
    }

    // ---------- helpers ----------

    private <T> T parseAndValidate(String dataJson, Class<T> type) throws IOException {
        T req = json.readValue(dataJson, type);
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

    private static List<UploadedFile> collectUploads(MultipartFile[] files) throws IOException {
        List<UploadedFile> out = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f.isEmpty()) continue;
            out.add(new UploadedFile(f.getOriginalFilename(), f.getContentType(), f.getBytes()));
        }
        return out;
    }

    private static String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
