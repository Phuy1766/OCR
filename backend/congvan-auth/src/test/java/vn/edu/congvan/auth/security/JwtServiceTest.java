package vn.edu.congvan.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import com.nimbusds.jwt.JWTClaimsSet;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.congvan.common.exception.BusinessException;

class JwtServiceTest {

    private JwtService service;
    private JwtKeyProvider keyProvider;
    private JwtProperties props;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        var kp = kpg.generateKeyPair();
        keyProvider = Mockito.mock(JwtKeyProvider.class);
        Mockito.when(keyProvider.privateKey()).thenReturn((RSAPrivateKey) kp.getPrivate());
        Mockito.when(keyProvider.publicKey()).thenReturn((RSAPublicKey) kp.getPublic());
        props = new JwtProperties(30, 7, "x", "y", "test-issuer");
        service = new JwtService(keyProvider, props);
    }

    @Test
    void issueAndParseAccessToken() {
        UUID userId = UUID.randomUUID();
        var issued =
                service.issueAccessToken(
                        userId, "alice", Set.of("ADMIN"), Set.of("USER:MANAGE", "USER:VIEW_SELF"));
        assertThat(issued.token()).isNotBlank();
        JWTClaimsSet claims = service.parseAndVerify(issued.token());
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getClaim(JwtService.CLAIM_USERNAME)).isEqualTo("alice");
        assertThat(claims.getClaim(JwtService.CLAIM_SCOPE)).isEqualTo("access");
        assertThat(claims.getClaim(JwtService.CLAIM_ROLES))
                .asInstanceOf(list(String.class))
                .containsExactlyInAnyOrder("ADMIN");
        assertThat(claims.getClaim(JwtService.CLAIM_PERMISSIONS))
                .asInstanceOf(list(String.class))
                .containsExactlyInAnyOrder("USER:MANAGE", "USER:VIEW_SELF");
    }

    @Test
    void issueAndParseRefreshToken() {
        UUID userId = UUID.randomUUID();
        var issued = service.issueRefreshToken(userId);
        JWTClaimsSet claims = service.parseAndVerify(issued.token());
        assertThat(claims.getClaim(JwtService.CLAIM_SCOPE)).isEqualTo("refresh");
        assertThat(claims.getJWTID()).isEqualTo(issued.jti());
    }

    @Test
    void parseRejectsTamperedToken() {
        var issued = service.issueAccessToken(UUID.randomUUID(), "x", Set.of(), Set.of());
        String tampered = issued.token().substring(0, issued.token().length() - 4) + "XXXX";
        assertThatThrownBy(() -> service.parseAndVerify(tampered))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void parseRejectsWrongIssuer() {
        JwtService other =
                new JwtService(keyProvider, new JwtProperties(30, 7, "x", "y", "different-issuer"));
        var issued = other.issueAccessToken(UUID.randomUUID(), "x", Set.of(), Set.of());
        assertThatThrownBy(() -> service.parseAndVerify(issued.token()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Issuer");
    }
}
