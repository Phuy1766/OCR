package vn.edu.congvan.auth.config;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.congvan.auth.security.AuthPrincipal;

/**
 * Bật JPA auditing — nạp created_by/updated_by từ SecurityContext.
 * Dùng {@link DateTimeProvider} trả {@link OffsetDateTime} để khớp kiểu
 * cột trong BaseEntity (mặc định auditing trả {@code LocalDateTime},
 * không tự convert được).
 */
@Configuration
@EnableJpaAuditing(
        auditorAwareRef = "auditorAware",
        dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return Optional.empty();
            Object principal = auth.getPrincipal();
            if (principal instanceof AuthPrincipal p) return Optional.of(p.userId());
            return Optional.empty();
        };
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of((TemporalAccessor) OffsetDateTime.now());
    }
}
