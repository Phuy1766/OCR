package vn.edu.congvan.auth.security;

import java.util.Set;
import java.util.UUID;

/**
 * Principal mỏng đặt trong {@link org.springframework.security.core.Authentication}.
 * KHÔNG chứa password/hash; chỉ định danh + roles + permissions cho authorization.
 */
public record AuthPrincipal(
        UUID userId, String username, String jti, Set<String> roles, Set<String> permissions) {}
