package vn.edu.congvan.workflow.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.workflow.dto.AssignRequest;
import vn.edu.congvan.workflow.dto.AssignmentDto;
import vn.edu.congvan.workflow.dto.CompleteAssignmentRequest;
import vn.edu.congvan.workflow.entity.AssignmentStatus;
import vn.edu.congvan.workflow.service.AssignmentService;

@RestController
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService service;

    @PostMapping("/api/inbound-documents/{id}/assign")
    @PreAuthorize("hasAuthority('WORKFLOW:ASSIGN')")
    public ResponseEntity<ApiResponse<AssignmentDto>> assign(
            @PathVariable("id") UUID documentId,
            @Valid @RequestBody AssignRequest body,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok(service.assign(documentId, body, actor, clientIp(http))));
    }

    @PostMapping("/api/assignments/{assignmentId}/reassign")
    @PreAuthorize("hasAuthority('WORKFLOW:REASSIGN')")
    public ApiResponse<AssignmentDto> reassign(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody AssignRequest body,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        return ApiResponse.ok(service.reassign(assignmentId, body, actor, clientIp(http)));
    }

    @PostMapping("/api/assignments/{assignmentId}/complete")
    @PreAuthorize("hasAuthority('WORKFLOW:HANDLE')")
    public ApiResponse<AssignmentDto> complete(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody CompleteAssignmentRequest body,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        return ApiResponse.ok(service.complete(assignmentId, body, actor, clientIp(http)));
    }

    @GetMapping("/api/assignments/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<AssignmentDto>> myAssignments(
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.ok(service.myAssignments(actor.userId(), status, page, size));
    }

    @GetMapping("/api/inbound-documents/{id}/assignments")
    @PreAuthorize("hasAuthority('WORKFLOW:READ')")
    public ApiResponse<List<AssignmentDto>> historyOfDocument(
            @PathVariable("id") UUID documentId) {
        return ApiResponse.ok(service.historyOfDocument(documentId));
    }

    private static String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
