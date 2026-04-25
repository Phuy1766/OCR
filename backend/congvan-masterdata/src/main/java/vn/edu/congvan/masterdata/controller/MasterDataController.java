package vn.edu.congvan.masterdata.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.masterdata.dto.ConfidentialityLevelDto;
import vn.edu.congvan.masterdata.dto.DocumentBookDto;
import vn.edu.congvan.masterdata.dto.DocumentTypeDto;
import vn.edu.congvan.masterdata.dto.PriorityLevelDto;
import vn.edu.congvan.masterdata.entity.BookType;
import vn.edu.congvan.masterdata.entity.ConfidentialityScope;
import vn.edu.congvan.masterdata.service.ConfidentialityLevelService;
import vn.edu.congvan.masterdata.service.DocumentBookService;
import vn.edu.congvan.masterdata.service.DocumentTypeService;
import vn.edu.congvan.masterdata.service.PriorityLevelService;

/** Read endpoints cho danh mục — mọi user authenticated có MASTERDATA:READ. */
@RestController
@RequestMapping("/api/master")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MASTERDATA:READ') or hasAuthority('MASTERDATA:MANAGE')")
public class MasterDataController {

    private final DocumentTypeService documentTypes;
    private final ConfidentialityLevelService confidentialityLevels;
    private final PriorityLevelService priorityLevels;
    private final DocumentBookService documentBooks;

    @GetMapping("/document-types")
    public ApiResponse<List<DocumentTypeDto>> listDocumentTypes() {
        return ApiResponse.ok(documentTypes.listActive());
    }

    @GetMapping("/confidentiality-levels")
    public ApiResponse<List<ConfidentialityLevelDto>> listConfidentialityLevels() {
        return ApiResponse.ok(confidentialityLevels.listActive());
    }

    @GetMapping("/priority-levels")
    public ApiResponse<List<PriorityLevelDto>> listPriorityLevels() {
        return ApiResponse.ok(priorityLevels.listActive());
    }

    @GetMapping("/document-books")
    public ApiResponse<List<DocumentBookDto>> listDocumentBooks(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) BookType bookType,
            @RequestParam(required = false) ConfidentialityScope scope) {
        return ApiResponse.ok(documentBooks.list(organizationId, bookType, scope));
    }
}
