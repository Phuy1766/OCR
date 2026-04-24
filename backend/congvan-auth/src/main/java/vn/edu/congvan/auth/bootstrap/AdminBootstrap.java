package vn.edu.congvan.auth.bootstrap;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.entity.UserEntity;
import vn.edu.congvan.auth.repository.UserRepository;
import vn.edu.congvan.auth.security.PasswordService;

/**
 * Khi ứng dụng khởi động: nếu admin user còn dùng placeholder password
 * (từ V6 migration) thì cập nhật hash thật từ env {@code APP_BOOTSTRAP_ADMIN_PASSWORD}.
 *
 * <p>{@code must_change_password=TRUE} được giữ để admin phải đổi ngay lần đầu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String PLACEHOLDER_HASH_PREFIX = "$argon2id$v=19$m=65536,t=3,p=1$PLACEHOLDER";

    private final UserRepository users;
    private final PasswordService passwordService;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.debug("APP_BOOTSTRAP_ADMIN_PASSWORD không set → skip admin bootstrap.");
            return;
        }
        if (adminPassword.length() < 10) {
            log.warn("APP_BOOTSTRAP_ADMIN_PASSWORD quá ngắn (<10 ký tự) → skip để tránh insecure bootstrap.");
            return;
        }

        UserEntity admin = users.findByUsername(ADMIN_USERNAME).orElse(null);
        if (admin == null) {
            log.warn("Không tìm thấy admin user — migration V6 chưa chạy?");
            return;
        }
        if (admin.getPasswordHash() == null
                || !admin.getPasswordHash().startsWith(PLACEHOLDER_HASH_PREFIX)) {
            // Admin đã được setup password thật → không override.
            return;
        }
        admin.setPasswordHash(passwordService.hash(adminPassword));
        admin.setPasswordChangedAt(OffsetDateTime.now());
        users.save(admin);
        log.info("Admin password được khởi tạo từ APP_BOOTSTRAP_ADMIN_PASSWORD.");
    }
}
