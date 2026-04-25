package vn.edu.congvan.workflow.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.common.dto.ApiResponse;
import vn.edu.congvan.workflow.dto.NotificationDto;
import vn.edu.congvan.workflow.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ApiResponse<Page<NotificationDto>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.ok(service.list(actor.userId(), unreadOnly, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCount> unreadCount(@AuthenticationPrincipal AuthPrincipal actor) {
        return ApiResponse.ok(new UnreadCount(service.countUnread(actor.userId())));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(
            @PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal actor) {
        service.markRead(actor.userId(), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/mark-all-read")
    public ApiResponse<MarkAllReadResponse> markAllRead(
            @AuthenticationPrincipal AuthPrincipal actor) {
        int updated = service.markAllRead(actor.userId());
        return ApiResponse.ok(new MarkAllReadResponse(updated));
    }

    public record UnreadCount(long count) {}

    public record MarkAllReadResponse(int updated) {}
}
