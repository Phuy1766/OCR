package vn.edu.congvan.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.congvan.auth.dto.LoginRequest;
import vn.edu.congvan.auth.dto.RefreshRequest;
import vn.edu.congvan.auth.dto.TokenPairResponse;
import vn.edu.congvan.auth.dto.UserDto;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.auth.service.AuthService;
import vn.edu.congvan.auth.service.UserService;
import vn.edu.congvan.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "congvan_refresh";

    private final AuthService authService;
    private final UserService userService;

    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Strict}")
    private String cookieSameSite;

    @PostMapping("/login")
    public ApiResponse<TokenPairResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest http,
            HttpServletResponse response) {
        TokenPairResponse tokens =
                authService.login(request, clientIp(http), userAgent(http));
        writeRefreshCookie(response, tokens.refreshToken(), tokens.refreshTokenExpiresAt());
        return ApiResponse.ok(tokens);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenPairResponse> refresh(
            @RequestBody(required = false) RefreshRequest body,
            HttpServletRequest http,
            HttpServletResponse response) {
        String token = extractRefresh(http, body);
        TokenPairResponse tokens =
                authService.refresh(token, clientIp(http), userAgent(http));
        writeRefreshCookie(response, tokens.refreshToken(), tokens.refreshTokenExpiresAt());
        return ApiResponse.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest http,
            HttpServletResponse response,
            @AuthenticationPrincipal AuthPrincipal principal) {
        String access = extractBearer(http);
        String refresh = readCookie(http, REFRESH_COOKIE_NAME).orElse(null);
        authService.logout(
                access,
                refresh,
                principal == null ? null : principal.userId(),
                clientIp(http));
        clearRefreshCookie(response);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('USER:VIEW_SELF')")
    public ApiResponse<UserDto> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(userService.getById(principal.userId()));
    }

    // ---------- helpers ----------

    private String extractRefresh(HttpServletRequest http, RefreshRequest body) {
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return body.refreshToken();
        }
        return readCookie(http, REFRESH_COOKIE_NAME).orElse(null);
    }

    private static Optional<String> readCookie(HttpServletRequest http, String name) {
        if (http.getCookies() == null) return Optional.empty();
        for (Cookie c : http.getCookies()) {
            if (name.equals(c.getName())) return Optional.ofNullable(c.getValue());
        }
        return Optional.empty();
    }

    private static String extractBearer(HttpServletRequest http) {
        String h = http.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) return null;
        return h.substring(7).trim();
    }

    private static String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return http.getRemoteAddr();
    }

    private static String userAgent(HttpServletRequest http) {
        return http.getHeader("User-Agent");
    }

    private void writeRefreshCookie(HttpServletResponse response, String token, Instant expiresAt) {
        long maxAge = Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
        StringBuilder sb = new StringBuilder();
        sb.append(REFRESH_COOKIE_NAME).append('=').append(token);
        sb.append("; Path=/api/auth");
        sb.append("; HttpOnly");
        sb.append("; Max-Age=").append(maxAge);
        sb.append("; SameSite=").append(cookieSameSite);
        if (cookieSecure) sb.append("; Secure");
        response.addHeader("Set-Cookie", sb.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append(REFRESH_COOKIE_NAME).append('=');
        sb.append("; Path=/api/auth");
        sb.append("; HttpOnly");
        sb.append("; Max-Age=0");
        sb.append("; SameSite=").append(cookieSameSite);
        if (cookieSecure) sb.append("; Secure");
        response.addHeader("Set-Cookie", sb.toString());
    }
}
