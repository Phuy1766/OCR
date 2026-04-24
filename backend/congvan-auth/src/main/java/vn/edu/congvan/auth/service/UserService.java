package vn.edu.congvan.auth.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.dto.CreateUserRequest;
import vn.edu.congvan.auth.dto.UpdateUserRequest;
import vn.edu.congvan.auth.dto.UserDto;
import vn.edu.congvan.auth.entity.RoleEntity;
import vn.edu.congvan.auth.entity.UserEntity;
import vn.edu.congvan.auth.entity.UserRoleEntity;
import vn.edu.congvan.auth.repository.RefreshTokenRepository;
import vn.edu.congvan.auth.repository.RoleRepository;
import vn.edu.congvan.auth.repository.UserRepository;
import vn.edu.congvan.auth.repository.UserRoleRepository;
import vn.edu.congvan.auth.security.AuthPrincipal;
import vn.edu.congvan.auth.security.PasswordService;
import vn.edu.congvan.common.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final UserRoleRepository userRoles;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordService passwordService;
    private final AuditLogger audit;

    @Transactional(readOnly = true)
    public UserDto getById(UUID id) {
        UserEntity u = users.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Không tìm thấy người dùng."));
        return toDto(u);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> list(String query, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.ASC, "username"));
        return users.search(query, pageable).map(this::toDto);
    }

    @Transactional
    public UserDto create(CreateUserRequest req, AuthPrincipal actor, String actorIp) {
        if (users.existsByUsername(req.username())) {
            throw new BusinessException("USER_USERNAME_TAKEN",
                    "Tên đăng nhập đã tồn tại.");
        }
        if (users.existsByEmail(req.email())) {
            throw new BusinessException("USER_EMAIL_TAKEN", "Email đã tồn tại.");
        }

        Set<RoleEntity> assignedRoles = resolveRoles(req.roleCodes());

        UserEntity u = new UserEntity();
        u.setId(UUID.randomUUID());
        u.setUsername(req.username());
        u.setEmail(req.email());
        u.setFullName(req.fullName());
        u.setPhone(req.phone());
        u.setOrganizationId(req.organizationId());
        u.setDepartmentId(req.departmentId());
        u.setPositionTitle(req.positionTitle());
        u.setPasswordHash(passwordService.hash(req.initialPassword()));
        u.setPasswordChangedAt(OffsetDateTime.now());
        u.setMustChangePassword(true); // admin cấp pass tạm → user phải đổi ngay
        u.setActive(true);
        u.setLocked(false);
        u = users.save(u);

        assignUserRoles(u.getId(), assignedRoles, actor == null ? null : actor.userId());

        audit.logSuccess(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.username(),
                actorIp,
                "CREATE_USER",
                "users",
                u.getId().toString());

        return toDto(u);
    }

    @Transactional
    public UserDto update(UUID id, UpdateUserRequest req, AuthPrincipal actor, String actorIp) {
        UserEntity u = users.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Không tìm thấy người dùng."));

        if (req.email() != null && !req.email().equals(u.getEmail())) {
            if (users.existsByEmail(req.email())) {
                throw new BusinessException("USER_EMAIL_TAKEN", "Email đã tồn tại.");
            }
            u.setEmail(req.email());
        }
        if (req.fullName() != null) u.setFullName(req.fullName());
        if (req.phone() != null) u.setPhone(req.phone());
        if (req.organizationId() != null) u.setOrganizationId(req.organizationId());
        if (req.departmentId() != null) u.setDepartmentId(req.departmentId());
        if (req.positionTitle() != null) u.setPositionTitle(req.positionTitle());
        if (req.active() != null) u.setActive(req.active());

        if (req.roleCodes() != null) {
            Set<RoleEntity> newRoles = resolveRoles(req.roleCodes());
            userRoles.deleteAllByUserId(u.getId());
            assignUserRoles(u.getId(), newRoles, actor == null ? null : actor.userId());
        }

        audit.logSuccess(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.username(),
                actorIp,
                "UPDATE_USER",
                "users",
                u.getId().toString());
        return toDto(u);
    }

    @Transactional
    public void lock(UUID id, AuthPrincipal actor, String actorIp) {
        users.lockAccount(id, null);
        refreshTokens.revokeAllForUser(id, OffsetDateTime.now(), "ADMIN_LOCK");
        audit.logSuccess(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.username(),
                actorIp,
                "LOCK_USER",
                "users",
                id.toString());
    }

    @Transactional
    public void unlock(UUID id, AuthPrincipal actor, String actorIp) {
        users.unlockAccount(id);
        audit.logSuccess(
                actor == null ? null : actor.userId(),
                actor == null ? null : actor.username(),
                actorIp,
                "UNLOCK_USER",
                "users",
                id.toString());
    }

    private Set<RoleEntity> resolveRoles(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException("USER_ROLE_REQUIRED", "Phải chỉ định ít nhất 1 vai trò.");
        }
        return roleCodes.stream()
                .map(
                        code ->
                                roles.findByCode(code)
                                        .orElseThrow(
                                                () ->
                                                        new BusinessException(
                                                                "ROLE_NOT_FOUND",
                                                                "Không tìm thấy vai trò: " + code)))
                .collect(Collectors.toSet());
    }

    private void assignUserRoles(UUID userId, Set<RoleEntity> rs, UUID assignedBy) {
        for (RoleEntity r : rs) {
            UserRoleEntity ur = new UserRoleEntity();
            ur.setId(UUID.randomUUID());
            ur.setUserId(userId);
            ur.setRole(r);
            ur.setAssignedAt(OffsetDateTime.now());
            ur.setAssignedBy(assignedBy);
            userRoles.save(ur);
        }
    }

    private UserDto toDto(UserEntity u) {
        List<UserRoleEntity> urs = userRoles.findActiveByUserId(u.getId());
        Set<String> rolesSet = urs.stream()
                .filter(UserRoleEntity::isEffective)
                .map(ur -> ur.getRole().getCode())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        Set<String> permsSet = urs.stream()
                .filter(UserRoleEntity::isEffective)
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(p -> p.getCode())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
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
                rolesSet,
                permsSet);
    }
}
