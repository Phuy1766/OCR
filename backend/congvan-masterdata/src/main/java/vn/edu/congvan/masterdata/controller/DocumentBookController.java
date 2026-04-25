package vn.edu.congvan.masterdata.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.masterdata.dto.CreateDocumentBookRequest;
import vn.edu.congvan.masterdata.dto.DocumentBookDto;
import vn.edu.congvan.masterdata.dto.UpdateDocumentBookRequest;
import vn.edu.congvan.masterdata.service.DocumentBookService;

/** Admin CRUD cho sổ đăng ký. Chỉ MASTERDATA:MANAGE (ADMIN + VAN_THU_CQ). */
@RestController
@RequestMapping("/api/master/document-books")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA:MANAGE')")
public class DocumentBookController {

    private final DocumentBookService service;

    @GetMapping("/{id}")
    public ApiResponse<DocumentBookDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DocumentBookDto>> create(
            @Valid @RequestBody CreateDocumentBookRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(service.create(request)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<DocumentBookDto> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateDocumentBookRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }
}
