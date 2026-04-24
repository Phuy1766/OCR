package vn.edu.congvan.auth.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.congvan.common.exception.BusinessException;

/**
 * Phát hành & parse JWT RS256.
 *
 * <p>Access token (scope=access): chứa {@code sub}=userId, {@code username},
 * {@code roles}, {@code perms}. Refresh token (scope=refresh): chỉ chứa
 * {@code sub} và {@code jti} để khớp với bảng refresh_tokens.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMISSIONS = "perms";
    public static final String CLAIM_SCOPE = "scope";
    public static final String SCOPE_ACCESS = "access";
    public static final String SCOPE_REFRESH = "refresh";

    private final JwtKeyProvider keyProvider;
    private final JwtProperties props;

    public IssuedToken issueAccessToken(
            UUID userId, String username, Set<String> roles, Set<String> permissions) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.accessTokenTtlMinutes(), ChronoUnit.MINUTES);
        String jti = UUID.randomUUID().toString();
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .issuer(props.issuer())
                        .subject(userId.toString())
                        .jwtID(jti)
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(exp))
                        .claim(CLAIM_USERNAME, username)
                        .claim(CLAIM_ROLES, List.copyOf(roles))
                        .claim(CLAIM_PERMISSIONS, List.copyOf(permissions))
                        .claim(CLAIM_SCOPE, SCOPE_ACCESS)
                        .build();
        return new IssuedToken(sign(claims), jti, exp);
    }

    public IssuedToken issueRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.refreshTokenTtlDays(), ChronoUnit.DAYS);
        String jti = UUID.randomUUID().toString();
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .issuer(props.issuer())
                        .subject(userId.toString())
                        .jwtID(jti)
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(exp))
                        .claim(CLAIM_SCOPE, SCOPE_REFRESH)
                        .build();
        return new IssuedToken(sign(claims), jti, exp);
    }

    /** Parse + verify signature + expiry; KHÔNG kiểm tra blacklist (làm ở filter). */
    public JWTClaimsSet parseAndVerify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new RSASSAVerifier(keyProvider.publicKey()))) {
                throw new BusinessException("AUTH_JWT_INVALID_SIGNATURE", "Chữ ký JWT không hợp lệ.");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.before(new Date())) {
                throw new BusinessException("AUTH_JWT_EXPIRED", "Token đã hết hạn.");
            }
            if (!props.issuer().equals(claims.getIssuer())) {
                throw new BusinessException(
                        "AUTH_JWT_BAD_ISSUER", "Issuer không đúng: " + claims.getIssuer());
            }
            return claims;
        } catch (ParseException e) {
            throw new BusinessException("AUTH_JWT_MALFORMED", "JWT không đúng định dạng.");
        } catch (JOSEException e) {
            throw new BusinessException(
                    "AUTH_JWT_VERIFY_FAIL", "Không xác minh được JWT: " + e.getMessage());
        }
    }

    private String sign(JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(keyProvider.privateKey()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new BusinessException("AUTH_JWT_SIGN_FAIL", "Không ký được JWT: " + e.getMessage());
        }
    }

    /** Kết quả phát hành token — chứa chuỗi serialized + jti + expiry (để lưu DB). */
    public record IssuedToken(String token, String jti, Instant expiresAt) {}
}
