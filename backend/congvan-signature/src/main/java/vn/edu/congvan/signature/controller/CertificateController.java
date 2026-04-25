package vn.edu.congvan.signature.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.signature.dto.CertificateDto;
import vn.edu.congvan.signature.dto.UploadCertificateRequest;
import vn.edu.congvan.signature.entity.CertificateType;
import vn.edu.congvan.signature.service.CertificateService;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService service;
    private final ObjectMapper json;
    private final Validator validator;

    @GetMapping
    @PreAuthorize("hasAuthority('SIGN:VERIFY') or hasAuthority('CERT:MANAGE') "
            + "or hasAuthority('SIGN:PERSONAL') or hasAuthority('SIGN:ORGANIZATION')")
    public ApiResponse<List<CertificateDto>> list(
            @RequestParam(required = false) CertificateType type,
            @RequestParam(required = false) UUID ownerUserId,
            @RequestParam(required = false) UUID ownerOrganizationId) {
        return ApiResponse.ok(service.list(type, ownerUserId, ownerOrganizationId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CERT:MANAGE')")
    public ResponseEntity<ApiResponse<CertificateDto>> upload(
            @RequestPart("data") String dataJson,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) throws IOException {
        UploadCertificateRequest req = json.readValue(dataJson, UploadCertificateRequest.class);
        var violations = validator.validate(req);
        if (!violations.isEmpty()) {
            String msg = violations.iterator().next().getMessage();
            throw new BusinessException("VALIDATION_FAILED", msg);
        }
        if (file.isEmpty()) {
            throw new BusinessException("CERT_FILE_EMPTY", "Vui lòng chọn file PKCS#12.");
        }
        CertificateDto dto = service.upload(
                req, file.getOriginalFilename(), file.getBytes(), actor, clientIp(http));
        return ResponseEntity.status(201).body(ApiResponse.ok(dto));
    }

    private static String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
