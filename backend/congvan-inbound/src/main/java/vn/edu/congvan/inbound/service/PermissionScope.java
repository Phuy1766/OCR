package vn.edu.congvan.inbound.service;

import java.util.Set;
import vn.edu.congvan.auth.security.AuthPrincipal;

/** Tiện ích kiểm tra quyền scope (READ_OWN / READ_DEPT / READ_ALL). */
public final class PermissionScope {

    public static final String READ_OWN = "INBOUND:READ_OWN";
    public static final String READ_DEPT = "INBOUND:READ_DEPT";
    public static final String READ_ALL = "INBOUND:READ_ALL";

    private PermissionScope() {}

    public static boolean canReadAll(AuthPrincipal p) {
        return has(p, READ_ALL);
    }

    public static boolean canReadDept(AuthPrincipal p) {
        return has(p, READ_DEPT);
    }

    public static boolean canReadOwn(AuthPrincipal p) {
        return has(p, READ_OWN);
    }

    public static boolean canRead(AuthPrincipal p) {
        return canReadAll(p) || canReadDept(p) || canReadOwn(p);
    }

    private static boolean has(AuthPrincipal p, String code) {
        Set<String> perms = p == null ? Set.of() : p.permissions();
        return perms != null && perms.contains(code);
    }
}
