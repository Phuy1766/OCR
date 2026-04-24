package vn.edu.congvan.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordServiceTest {

    private final PasswordService service = new PasswordService();

    @Test
    void hashProducesArgon2idEncodedString() {
        String encoded = service.hash("super_secret_pw_123");
        assertThat(encoded).startsWith("$argon2id$v=19$m=65536,t=3,p=1$");
    }

    @Test
    void hashAndVerifyRoundTrip() {
        String encoded = service.hash("correct-horse-battery-staple");
        assertThat(service.verify(encoded, "correct-horse-battery-staple")).isTrue();
        assertThat(service.verify(encoded, "wrong-password")).isFalse();
    }

    @Test
    void twoHashesOfSamePasswordDiffer() {
        String a = service.hash("same-password");
        String b = service.hash("same-password");
        assertThat(a).isNotEqualTo(b); // random salt
    }

    @Test
    void verifyReturnsFalseForMalformedEncoded() {
        assertThat(service.verify("not-a-real-argon2-hash", "anything")).isFalse();
    }
}
