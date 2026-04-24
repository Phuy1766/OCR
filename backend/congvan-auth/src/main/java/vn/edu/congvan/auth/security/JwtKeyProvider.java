package vn.edu.congvan.auth.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import vn.edu.congvan.common.exception.BusinessException;

/** Load RSA keypair (PEM PKCS8 + X509) từ classpath hoặc filesystem. */
@Slf4j
@Component
public class JwtKeyProvider {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public JwtKeyProvider(JwtProperties props, ResourceLoader resourceLoader) {
        this.privateKey = loadPrivateKey(props.privateKeyPath(), resourceLoader);
        this.publicKey = loadPublicKey(props.publicKeyPath(), resourceLoader);
        log.info("JWT RS256 keys loaded: private={}, public={}",
                props.privateKeyPath(), props.publicKeyPath());
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    private RSAPrivateKey loadPrivateKey(String path, ResourceLoader loader) {
        try {
            byte[] der = readDer(loader.getResource(path), "PRIVATE KEY");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new BusinessException(
                    "AUTH_JWT_KEY_LOAD_FAIL",
                    "Không load được JWT private key từ " + path + ": " + e.getMessage());
        }
    }

    private RSAPublicKey loadPublicKey(String path, ResourceLoader loader) {
        try {
            byte[] der = readDer(loader.getResource(path), "PUBLIC KEY");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new BusinessException(
                    "AUTH_JWT_KEY_LOAD_FAIL",
                    "Không load được JWT public key từ " + path + ": " + e.getMessage());
        }
    }

    private static byte[] readDer(Resource resource, String label) throws IOException {
        if (!resource.exists()) {
            throw new IllegalArgumentException("Resource không tồn tại: " + resource);
        }
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String body =
                pem.replace("-----BEGIN " + label + "-----", "")
                        .replace("-----END " + label + "-----", "")
                        .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(body);
    }
}
