package vn.edu.congvan.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.congvan.auth.dto.CreateUserRequest;
import vn.edu.congvan.auth.dto.UpdateUserRequest;
import vn.edu.congvan.auth.dto.UserDto;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.auth.service.UserService;
import vn.edu.congvan.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER:READ') or hasAuthority('USER:MANAGE')")
    public ApiResponse<Page<UserDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userService.list(q, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:READ') or hasAuthority('USER:MANAGE')")
    public ApiResponse<UserDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER:MANAGE')")
    public ResponseEntity<ApiResponse<UserDto>> create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        UserDto created = userService.create(request, actor, clientIp(http));
        return ResponseEntity.status(201).body(ApiResponse.ok(created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:MANAGE')")
    public ApiResponse<UserDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        return ApiResponse.ok(userService.update(id, request, actor, clientIp(http)));
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('USER:MANAGE')")
    public ApiResponse<Void> lock(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        userService.lock(id, actor, clientIp(http));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('USER:MANAGE')")
    public ApiResponse<Void> unlock(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal actor,
            HttpServletRequest http) {
        userService.unlock(id, actor, clientIp(http));
        return ApiResponse.ok(null);
    }

    private static String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
