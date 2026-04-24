package vn.edu.congvan.auth.security;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.congvan.common.exception.BusinessException;

/**
 * Filter đọc Authorization Bearer → verify JWT → check blacklist → set Authentication.
 *
 * <p>Ưu tiên access-token scope; token hết hạn hay blacklist → để tiếp filter
 * chain, endpoint không public sẽ 401 qua {@link Http401EntryPoint}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        String token = extractBearer(request);
        if (token != null) {
            try {
                JWTClaimsSet claims = jwtService.parseAndVerify(token);
                String scope = (String) claims.getClaim(JwtService.CLAIM_SCOPE);
                if (!JwtService.SCOPE_ACCESS.equals(scope)) {
                    // Refresh token không được dùng để gọi API → bỏ qua, để 401.
                    chain.doFilter(request, response);
                    return;
                }
                String jti = claims.getJWTID();
                if (blacklist.isBlacklisted(jti)) {
                    chain.doFilter(request, response);
                    return;
                }

                AuthPrincipal principal = buildPrincipal(claims);
                Set<SimpleGrantedAuthority> authorities =
                        buildAuthorities(principal.roles(), principal.permissions());
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (BusinessException e) {
                log.debug("JWT rejected: {} {}", e.getCode(), e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                log.debug("JWT parse error: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private static String extractBearer(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7).trim();
    }

    private static AuthPrincipal buildPrincipal(JWTClaimsSet claims) {
        UUID userId = UUID.fromString(claims.getSubject());
        String username = (String) claims.getClaim(JwtService.CLAIM_USERNAME);
        Set<String> roles = toStringSet(claims.getClaim(JwtService.CLAIM_ROLES));
        Set<String> perms = toStringSet(claims.getClaim(JwtService.CLAIM_PERMISSIONS));
        return new AuthPrincipal(userId, username, claims.getJWTID(), roles, perms);
    }

    private static Set<String> toStringSet(Object claim) {
        if (claim instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    private static Set<SimpleGrantedAuthority> buildAuthorities(
            Set<String> roles, Set<String> permissions) {
        Set<SimpleGrantedAuthority> out = new HashSet<>(roles.size() + permissions.size());
        for (String r : roles) out.add(new SimpleGrantedAuthority("ROLE_" + r));
        for (String p : permissions) out.add(new SimpleGrantedAuthority(p));
        return out;
    }
}
