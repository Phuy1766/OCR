package vn.edu.congvan.auth.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limit trên endpoint nhạy cảm (chỉ {@code /api/auth/login} Phase 1).
 * 10 requests/phút/IP — đủ để người dùng gõ sai vài lần, không làm chậm UI.
 * Khi vượt giới hạn trả 429 để phòng brute-force (defense-in-depth với BR-12
 * lockout ở tầng business).
 *
 * <p>Có thể tắt bằng {@code app.security.rate-limit.enabled=false} (dùng trong
 * integration test để không bị bucket share giữa các test method).
 */
@Component
@Order(1)
@ConditionalOnProperty(value = "app.security.rate-limit.enabled", havingValue = "true",
        matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 10;
    private static final Duration REFILL = Duration.ofMinutes(1);
    private static final String TARGET_PATH = "/api/auth/login";

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        if (!TARGET_PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        Bucket bucket = buckets.computeIfAbsent(clientKey(request), k -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429); // HTTP 429 Too Many Requests (không có hằng trong Jakarta).
            response.setHeader("Retry-After", String.valueOf(REFILL.toSeconds()));
        }
    }

    private static String clientKey(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private static Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder().capacity(CAPACITY).refillGreedy(CAPACITY, REFILL).build();
        return Bucket.builder().addLimit(limit).build();
    }
}
