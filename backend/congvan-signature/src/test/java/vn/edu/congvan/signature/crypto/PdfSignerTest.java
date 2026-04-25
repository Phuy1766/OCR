package vn.edu.congvan.signature.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

class PdfSignerTest {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    void signAndVerifyRoundTrip() throws Exception {
        // 1. Tạo self-signed cert + private key
        KeyPair kp = generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp, "CN=Test Signer, O=ConGvan Test, C=VN");

        // 2. Tạo PDF mẫu
        byte[] pdfBytes = createSamplePdf();

        // 3. Sign
        byte[] signed = PdfSigner.signPdf(
                pdfBytes,
                kp.getPrivate(),
                new java.security.cert.Certificate[]{cert},
                "Phê duyệt văn bản",
                "Hà Nội",
                "CN=Test Signer");

        assertThat(signed).isNotEmpty();
        assertThat(signed.length).isGreaterThan(pdfBytes.length); // có signature appended

        // 4. Verify
        var results = PdfSigner.verify(signed);
        assertThat(results).hasSize(1);
        var r = results.get(0);
        assertThat(r.valid()).isTrue();
        assertThat(r.subjectDn()).contains("Test Signer");
        assertThat(r.reason()).isEqualTo("Phê duyệt văn bản");
    }

    @Test
    void verifyDetectsTampering() throws Exception {
        KeyPair kp = generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp, "CN=Tamper Test, O=Test, C=VN");
        byte[] pdfBytes = createSamplePdf();
        byte[] signed = PdfSigner.signPdf(
                pdfBytes, kp.getPrivate(),
                new java.security.cert.Certificate[]{cert},
                "Test", "Test", "CN=Tamper Test");

        // Sửa nội dung (flip bytes ở giữa file, ngoài vùng signature)
        byte[] tampered = signed.clone();
        // Flip 1 byte ở khoảng đầu, sau header PDF (chỗ stream content)
        int target = 200;
        if (target < tampered.length) {
            tampered[target] = (byte) (tampered[target] ^ 0xFF);
        }

        var results = PdfSigner.verify(tampered);
        assertThat(results).hasSize(1);
        // Hoặc invalid hoặc throw — chấp nhận cả 2
        assertThat(results.get(0).valid()).isFalse();
    }

    @Test
    void loadPkcs12WithCorrectPassword() throws Exception {
        KeyPair kp = generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp, "CN=PKCS12 Test, O=Test, C=VN");
        String password = "test-password-123";

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("test", kp.getPrivate(), password.toCharArray(),
                new java.security.cert.Certificate[]{cert});

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ks.store(out, password.toCharArray());

        var loaded = PdfSigner.loadPkcs12(out.toByteArray(), password);
        assertThat(loaded.privateKey()).isNotNull();
        assertThat(loaded.signerCert().getSubjectX500Principal().getName())
                .contains("PKCS12 Test");
    }

    @Test
    void loadPkcs12WithWrongPasswordThrows() throws Exception {
        KeyPair kp = generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp, "CN=Wrong, O=Test, C=VN");

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("x", kp.getPrivate(), "correct".toCharArray(),
                new java.security.cert.Certificate[]{cert});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ks.store(out, "correct".toCharArray());

        assertThat(catchException(() -> PdfSigner.loadPkcs12(out.toByteArray(), "wrong")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- helpers ----------

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048, new SecureRandom());
        return kpg.generateKeyPair();
    }

    private static X509Certificate generateSelfSignedCert(KeyPair kp, String dn) throws Exception {
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 60_000);
        Date notAfter = new Date(now + 365L * 24 * 3600 * 1000);
        BigInteger serial = BigInteger.valueOf(now);
        X500Name name = new X500Name(dn);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, serial, notBefore, notAfter, name, kp.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(kp.getPrivate());
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(builder.build(signer));
    }

    private static byte[] createSamplePdf() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                cs.newLineAtOffset(72, 720);
                cs.showText("Mau cong van Phase 7 OCR + Sign");
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static Throwable catchException(ThrowingRunnable r) {
        try {
            r.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
