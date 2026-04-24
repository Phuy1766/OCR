package vn.edu.congvan.auth.service;

import com.nimbusds.jwt.JWTClaimsSet;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.dto.LoginRequest;
import vn.edu.congvan.auth.dto.TokenPairResponse;
import vn.edu.congvan.auth.dto.UserDto;
import vn.edu.congvan.auth.entity.LoginAttemptEntity;
import vn.edu.congvan.auth.entity.RefreshTokenEntity;
import vn.edu.congvan.auth.entity.UserEntity;
import vn.edu.congvan.auth.entity.UserRoleEntity;
import vn.edu.congvan.auth.exception.AuthException;
import vn.edu.congvan.auth.repository.LoginAttemptRepository;
import vn.edu.congvan.auth.repository.RefreshTokenRepository;
import vn.edu.congvan.auth.repository.UserRepository;
import vn.edu.congvan.auth.repository.UserRoleRepository;
import vn.edu.congvan.auth.security.JwtService;
import vn.edu.congvan.auth.security.JwtService.IssuedToken;
import vn.edu.congvan.auth.security.PasswordService;
import vn.edu.congvan.auth.security.TokenBlacklistService;

/**
 * Login / refresh / logout.
 *
 * <p>BR-12: sau 5 lần sai liên tiếp → khóa tài khoản 30 phút (soft lock) +
 * set {@code is_locked=true} nếu vượt 10 lần → cần admin mở khóa (hard lock).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int SOFT_LOCK_THRESHOLD = 5;
    private static final int HARD_LOCK_THRESHOLD = 10;
    private static final Duration SOFT_LOCK_DURATION = Duration.ofMinutes(30);

    private final UserRepository users;
    private final UserRoleRepository userRoles;
    private final RefreshTokenRepository refreshTokens;
    private final LoginAttemptRepository loginAttempts;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;
    private final AuditLogger audit;

    // Không rollback khi AuthException — cần giữ lại increment failedLoginCount
    // và bản ghi login_attempts để BR-12 lockout phát huy tác dụng.
    @Transactional(noRollbackFor = AuthException.class)
    public TokenPairResponse login(LoginRequest request, String ip, String userAgent) {
        UserEntity user = users.findByUsername(request.username()).orElse(null);
        if (user == null) {
            recordAttempt(null, request.username(), ip, userAgent, false, "USER_NOT_FOUND");
            audit.logFailure(null, request.username(), ip,
                    "LOGIN", "users", null, "Username không tồn tại.");
            throw new AuthException(AuthException.INVALID_CREDENTIALS,
                    "Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        // Soft lock đang hiệu lực?
        if (user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            recordAttempt(user.getId(), user.getUsername(), ip, userAgent, false, "SOFT_LOCKED");
            throw new AuthException(AuthException.ACCOUNT_LOCKED,
                    "Tài khoản đang tạm khóa. Thử lại sau " + SOFT_LOCK_DURATION.toMinutes()
                            + " phút hoặc liên hệ quản trị viên.");
        }

        if (user.isLocked()) {
            recordAttempt(user.getId(), user.getUsername(), ip, userAgent, false, "HARD_LOCKED");
            throw new AuthException(AuthException.ACCOUNT_LOCKED,
                    "Tài khoản đã bị khóa. Liên hệ quản trị viên để mở khóa.");
        }

        if (!user.isActive() || user.isDeleted()) {
            recordAttempt(user.getId(), user.getUsername(), ip, userAgent, false, "INACTIVE");
            throw new AuthException(AuthException.ACCOUNT_INACTIVE,
                    "Tài khoản đã bị vô hiệu hóa.");
        }

        if (!passwordService.verify(user.getPasswordHash(), request.password())) {
            handleFailedLogin(user, ip, userAgent);
            throw new AuthException(AuthException.INVALID_CREDENTIALS,
                    "Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        return handleSuccessfulLogin(user, ip, userAgent);
    }

    private void handleFailedLogin(UserEntity user, String ip, String userAgent) {
        int attempts = user.getFailedLoginCount() + 1;
        users.incrementFailedLoginCount(user.getId());
        recordAttempt(user.getId(), user.getUsername(), ip, userAgent, false, "INVALID_PASSWORD");

        if (attempts >= HARD_LOCK_THRESHOLD) {
            users.lockAccount(user.getId(), null); // hard lock — null locked_until, cần admin unlock
            audit.logFailure(user.getId(), user.getUsername(), ip,
                    "LOGIN_FAIL_HARDLOCK", "users", user.getId().toString(),
                    "Khóa cứng sau " + attempts + " lần sai.");
        } else if (attempts >= SOFT_LOCK_THRESHOLD) {
            users.lockAccount(user.getId(),
                    OffsetDateTime.now().plus(SOFT_LOCK_DURATION));
            audit.logFailure(user.getId(), user.getUsername(), ip,
                    "LOGIN_FAIL_SOFTLOCK", "users", user.getId().toString(),
                    "Khóa mềm " + SOFT_LOCK_DURATION.toMinutes() + " phút.");
        } else {
            audit.logFailure(user.getId(), user.getUsername(), ip,
                    "LOGIN_FAIL", "users", user.getId().toString(),
                    "Sai mật khẩu (" + attempts + "/" + SOFT_LOCK_THRESHOLD + ").");
        }
    }

    private TokenPairResponse handleSuccessfulLogin(UserEntity user, String ip, String userAgent) {
        users.markLoginSuccess(user.getId(), OffsetDateTime.now(), ip);
        recordAttempt(user.getId(), user.getUsername(), ip, userAgent, true, null);

        RolesAndPermissions rp = loadRolesAndPermissions(user.getId());
        UserDto userDto = toUserDto(user, rp);

        IssuedToken access =
                jwtService.issueAccessToken(
                        user.getId(), user.getUsername(), rp.roles(), rp.permissions());
        IssuedToken refresh = jwtService.issueRefreshToken(user.getId());
        persistRefreshToken(refresh, user.getId(), ip, userAgent, null);

        audit.logSuccess(user.getId(), user.getUsername(), ip,
                "LOGIN_SUCCESS", "users", user.getId().toString());

        return new TokenPairResponse(
                access.token(),
                access.expiresAt(),
                refresh.token(),
                refresh.expiresAt(),
                userDto);
    }

    @Transactional
    public TokenPairResponse refresh(String refreshToken, String ip, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthException.REFRESH_TOKEN_INVALID,
                    "Thiếu refresh token.");
        }
        JWTClaimsSet claims = jwtService.parseAndVerify(refreshToken);
        if (!JwtService.SCOPE_REFRESH.equals(claims.getClaim(JwtService.CLAIM_SCOPE))) {
            throw new AuthException(AuthException.REFRESH_TOKEN_INVALID,
                    "Token không phải refresh token.");
        }
        String tokenHash = sha256(refreshToken);
        RefreshTokenEntity existing = refreshTokens.findByTokenHash(tokenHash).orElse(null);
        if (existing == null) {
            // Token hợp lệ chữ ký nhưng không có trong DB → nghi ngờ reuse/compromise.
            UUID userId = UUID.fromString(claims.getSubject());
            refreshTokens.revokeAllForUser(userId, OffsetDateTime.now(), "COMPROMISED");
            audit.logFailure(userId, null, ip,
                    "REFRESH_UNKNOWN", "refresh_tokens", null,
                    "Refresh token hợp lệ chữ ký nhưng không có trong DB — revoke all.");
            throw new AuthException(AuthException.REFRESH_TOKEN_REUSED,
                    "Refresh token không hợp lệ. Các phiên đăng nhập đã bị thu hồi vì an toàn.");
        }
        if (!existing.isActive()) {
            refreshTokens.revokeAllForUser(
                    existing.getUserId(), OffsetDateTime.now(), "COMPROMISED");
            audit.logFailure(existing.getUserId(), null, ip,
                    "REFRESH_REUSED", "refresh_tokens", existing.getId().toString(),
                    "Refresh token đã bị thu hồi/hết hạn — revoke all.");
            throw new AuthException(AuthException.REFRESH_TOKEN_REUSED,
                    "Refresh token đã hết hạn hoặc bị thu hồi. Vui lòng đăng nhập lại.");
        }

        // Rotate: revoke old, issue new.
        existing.setRevokedAt(OffsetDateTime.now());
        existing.setRevokedReason("ROTATED");
        refreshTokens.save(existing);

        UserEntity user =
                users.findById(existing.getUserId())
                        .filter(UserEntity::isLoginAllowed)
                        .orElseThrow(
                                () ->
                                        new AuthException(
                                                AuthException.ACCOUNT_INACTIVE,
                                                "Tài khoản không còn hoạt động."));

        RolesAndPermissions rp = loadRolesAndPermissions(user.getId());
        IssuedToken access =
                jwtService.issueAccessToken(
                        user.getId(), user.getUsername(), rp.roles(), rp.permissions());
        IssuedToken newRefresh = jwtService.issueRefreshToken(user.getId());
        persistRefreshToken(newRefresh, user.getId(), ip, userAgent, existing.getId());

        audit.logSuccess(user.getId(), user.getUsername(), ip,
                "REFRESH_TOKEN", "refresh_tokens", newRefresh.jti());

        return new TokenPairResponse(
                access.token(),
                access.expiresAt(),
                newRefresh.token(),
                newRefresh.expiresAt(),
                toUserDto(user, rp));
    }

    @Transactional
    public void logout(String accessToken, String refreshToken, UUID userId, String ip) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                JWTClaimsSet claims = jwtService.parseAndVerify(accessToken);
                Instant exp = claims.getExpirationTime().toInstant();
                blacklist.blacklist(claims.getJWTID(), exp);
            } catch (Exception e) {
                // Token hỏng → bỏ qua, vẫn tiếp tục revoke refresh.
                log.debug("Logout: access token không hợp lệ, bỏ qua blacklist ({})",
                        e.getMessage());
            }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            String hash = sha256(refreshToken);
            refreshTokens
                    .findByTokenHash(hash)
                    .ifPresent(
                            rt -> {
                                rt.setRevokedAt(OffsetDateTime.now());
                                rt.setRevokedReason("LOGOUT");
                                refreshTokens.save(rt);
                            });
        }
        if (userId != null) {
            audit.logSuccess(userId, null, ip, "LOGOUT", "users", userId.toString());
        }
    }

    private void persistRefreshToken(
            IssuedToken issued, UUID userId, String ip, String userAgent, UUID parentId) {
        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setId(UUID.randomUUID());
        rt.setUserId(userId);
        rt.setTokenHash(sha256(issued.token()));
        rt.setParentId(parentId);
        rt.setUserAgent(truncate(userAgent, 500));
        rt.setIpAddress(ip);
        rt.setIssuedAt(OffsetDateTime.now());
        rt.setExpiresAt(OffsetDateTime.ofInstant(issued.expiresAt(), OffsetDateTime.now().getOffset()));
        refreshTokens.save(rt);
    }

    private void recordAttempt(
            UUID userId,
            String username,
            String ip,
            String userAgent,
            boolean success,
            String failureReason) {
        LoginAttemptEntity a = new LoginAttemptEntity();
        a.setUserId(userId);
        a.setUsername(username);
        a.setIpAddress(ip);
        a.setUserAgent(truncate(userAgent, 500));
        a.setSuccess(success);
        a.setFailureReason(failureReason);
        a.setAttemptTime(OffsetDateTime.now());
        loginAttempts.save(a);
    }

    private RolesAndPermissions loadRolesAndPermissions(UUID userId) {
        var urs = userRoles.findActiveByUserId(userId);
        Set<String> roles = urs.stream()
                .filter(UserRoleEntity::isEffective)
                .map(ur -> ur.getRole().getCode())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        Set<String> perms = urs.stream()
                .filter(UserRoleEntity::isEffective)
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(p -> p.getCode())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return new RolesAndPermissions(roles, perms);
    }

    private UserDto toUserDto(UserEntity u, RolesAndPermissions rp) {
        return new UserDto(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                u.getOrganizationId(),
                u.getDepartmentId(),
                u.getPositionTitle(),
                u.isActive(),
                u.isLocked(),
                u.isMustChangePassword(),
                u.getLastLoginAt(),
                rp.roles(),
                rp.permissions());
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    public static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }

    /** Record internal giữ roles + permissions load cùng nhau. */
    public record RolesAndPermissions(Set<String> roles, Set<String> permissions) {}

    // Tránh unused warning khi chưa dùng Map<?,?> trực tiếp.
    @SuppressWarnings("unused")
    private static Map<String, Object> placeholder() {
        return Map.of();
    }
}
