package vn.edu.congvan.auth.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Factory.Argon2Types;
import org.springframework.stereotype.Service;

/**
 * Băm + xác minh mật khẩu bằng Argon2id (KHÔNG dùng BCrypt).
 *
 * <p>Tham số: m=65536 KiB (64MB), t=3 iterations, p=1 parallelism — cân bằng
 * an toàn và hiệu năng trên server commodity theo OWASP 2024.
 */
@Service
public class PasswordService {

    private static final int MEMORY_KIB = 65_536; // 64 MB
    private static final int ITERATIONS = 3;
    private static final int PARALLELISM = 1;

    private final Argon2 argon2 = Argon2Factory.create(Argon2Types.ARGON2id);

    /**
     * Băm mật khẩu. Mảng {@code char[]} được wipe sau khi băm xong.
     *
     * @param rawPassword mật khẩu clear-text
     * @return chuỗi encoded (có salt + params + hash)
     */
    public String hash(char[] rawPassword) {
        try {
            return argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, rawPassword);
        } finally {
            argon2.wipeArray(rawPassword);
        }
    }

    /** Convenience: nhận String; tạo {@code char[]} nội bộ rồi wipe. */
    public String hash(String rawPassword) {
        return hash(rawPassword.toCharArray());
    }

    /** Xác minh mật khẩu. Không ném exception nếu hash sai định dạng — trả false. */
    public boolean verify(String encoded, char[] rawPassword) {
        try {
            return argon2.verify(encoded, rawPassword);
        } catch (Exception e) {
            return false;
        } finally {
            argon2.wipeArray(rawPassword);
        }
    }

    public boolean verify(String encoded, String rawPassword) {
        return verify(encoded, rawPassword.toCharArray());
    }
}
