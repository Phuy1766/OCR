package vn.edu.congvan.masterdata.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.masterdata.dto.CreateOrganizationRequest;
import vn.edu.congvan.masterdata.dto.OrganizationDto;
import vn.edu.congvan.masterdata.dto.UpdateOrganizationRequest;
import vn.edu.congvan.masterdata.service.OrganizationService;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService service;

    @GetMapping
    @PreAuthorize("hasAuthority('ORG:READ') or hasAuthority('ORG:MANAGE')")
    public ApiResponse<List<OrganizationDto>> list() {
        return ApiResponse.ok(service.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG:READ') or hasAuthority('ORG:MANAGE')")
    public ApiResponse<OrganizationDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORG:MANAGE')")
    public ResponseEntity<ApiResponse<OrganizationDto>> create(
            @Valid @RequestBody CreateOrganizationRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(service.create(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG:MANAGE')")
    public ApiResponse<OrganizationDto> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateOrganizationRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }
}
