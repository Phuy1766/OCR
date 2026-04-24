package vn.edu.congvan.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình JWT: TTL + đường dẫn RSA keypair. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        int accessTokenTtlMinutes,
        int refreshTokenTtlDays,
        String privateKeyPath,
        String publicKeyPath,
        String issuer) {

    public JwtProperties {
        if (accessTokenTtlMinutes <= 0) accessTokenTtlMinutes = 30;
        if (refreshTokenTtlDays <= 0) refreshTokenTtlDays = 7;
        if (issuer == null || issuer.isBlank()) issuer = "congvan-system";
    }
}
