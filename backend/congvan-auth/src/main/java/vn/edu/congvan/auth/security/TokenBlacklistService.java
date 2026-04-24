package vn.edu.congvan.auth.security;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Blacklist access-token qua Redis. Key = jti của token; TTL = thời gian
 * còn lại của token. Khi check, chỉ cần {@code EXISTS} trên key.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "auth:blacklist:access:";

    private final StringRedisTemplate redis;

    public void blacklist(String jti, Instant expiresAt) {
        long ttlSeconds = Math.max(1, Duration.between(Instant.now(), expiresAt).getSeconds());
        redis.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String jti) {
        Boolean exists = redis.hasKey(KEY_PREFIX + jti);
        return exists != null && exists;
    }
}
