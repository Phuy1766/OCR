package vn.edu.congvan.search.controller;

import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.search.dto.SearchResponse;
import vn.edu.congvan.search.service.SearchCriteria;
import vn.edu.congvan.search.service.SearchService;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService service;

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('SEARCH:READ')")
    public ApiResponse<SearchResponse> searchDocuments(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID bookId,
            @RequestParam(required = false) UUID confidentialityLevelId,
            @RequestParam(required = false) UUID priorityLevelId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal actor) {
        SearchCriteria criteria = new SearchCriteria(
                q, direction, status, organizationId, bookId,
                confidentialityLevelId, priorityLevelId, fromDate, toDate);
        return ApiResponse.ok(service.search(criteria, actor, page, size));
    }
}
