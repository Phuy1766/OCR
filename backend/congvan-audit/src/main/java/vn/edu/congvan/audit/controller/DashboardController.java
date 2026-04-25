package vn.edu.congvan.audit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.congvan.audit.dto.DashboardStatsDto;
import vn.edu.congvan.audit.service.DashboardService;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DashboardStatsDto> stats(@AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.ok(service.getStats(actor));
    }
}
