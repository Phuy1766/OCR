package vn.edu.congvan.masterdata.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.masterdata.dto.CreateDepartmentRequest;
import vn.edu.congvan.masterdata.dto.DepartmentDto;
import vn.edu.congvan.masterdata.dto.UpdateDepartmentRequest;
import vn.edu.congvan.masterdata.service.DepartmentService;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService service;

    @GetMapping
    @PreAuthorize("hasAuthority('ORG:READ') or hasAuthority('ORG:MANAGE')")
    public ApiResponse<List<DepartmentDto>> listByOrganization(
            @RequestParam UUID organizationId) {
        return ApiResponse.ok(service.listByOrganization(organizationId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG:READ') or hasAuthority('ORG:MANAGE')")
    public ApiResponse<DepartmentDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORG:MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentDto>> create(
            @Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(service.create(request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG:MANAGE')")
    public ApiResponse<DepartmentDto> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateDepartmentRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }
}
