package vn.edu.congvan.signature.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.signature.dto.SignRequest;
import vn.edu.congvan.signature.dto.SignatureDto;
import vn.edu.congvan.signature.dto.VerificationDto;
import vn.edu.congvan.signature.service.SignatureService;

@RestController
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService service;

    @PostMapping("/api/outbound-documents/{id}/sign-personal")
    @PreAuthorize("hasAuthority('SIGN:PERSONAL')")
    public ApiResponse<SignatureDto> signPersonal(
            @PathVariable("id") UUID documentId,
            @Valid @RequestBody SignRequest body,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        return ApiResponse.ok(service.signPersonal(documentId, body, actor, clientIp(http)));
    }

    @PostMapping("/api/outbound-documents/{id}/sign-organization")
    @PreAuthorize("hasAuthority('SIGN:ORGANIZATION')")
    public ApiResponse<SignatureDto> signOrganization(
            @PathVariable("id") UUID documentId,
            @Valid @RequestBody SignRequest body,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        return ApiResponse.ok(service.signOrganization(documentId, body, actor, clientIp(http)));
    }

    @GetMapping("/api/outbound-documents/{id}/signatures")
    @PreAuthorize("hasAuthority('SIGN:VERIFY')")
    public ApiResponse<List<SignatureDto>> list(@PathVariable("id") UUID documentId) {
        return ApiResponse.ok(service.listForDocument(documentId));
    }

    @PostMapping("/api/outbound-documents/{id}/signatures/verify")
    @PreAuthorize("hasAuthority('SIGN:VERIFY')")
    public ApiResponse<List<VerificationDto>> verify(
            @PathVariable("id") UUID documentId,
            @AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.ok(service.verifyAllForDocument(documentId, actor));
    }

    private static String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
