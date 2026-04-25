package vn.edu.congvan.ocr.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.ocr.dto.AcceptOcrRequest;
import vn.edu.congvan.ocr.dto.OcrJobDto;
import vn.edu.congvan.ocr.service.OcrService;

@RestController
@RequiredArgsConstructor
public class OcrController {

    private final OcrService service;

    @GetMapping("/api/inbound-documents/{id}/ocr")
    @PreAuthorize("hasAuthority('OCR:READ')")
    public ApiResponse<OcrJobDto> getOcrJob(@PathVariable("id") UUID documentId) {
        return ApiResponse.ok(service.getJobByDocument(documentId));
    }

    @PostMapping("/api/ocr-jobs/{jobId}/accept")
    @PreAuthorize("hasAuthority('OCR:ACCEPT')")
    public ApiResponse<OcrJobDto> accept(
            @PathVariable UUID jobId,
            @Valid @RequestBody AcceptOcrRequest body,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        return ApiResponse.ok(service.accept(jobId, body, actor, clientIp(http)));
    }

    private static String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
