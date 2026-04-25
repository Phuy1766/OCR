package vn.edu.congvan.signature.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSSignerDigestMismatchException;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

/**
 * Ký PDF detached CMS (PAdES-B) qua PDFBox 3 + Bouncy Castle.
 *
 * <p>Pattern:
 * <ol>
 *   <li>{@link #signPdf} đọc PDF gốc, mở {@code PDDocument}, gán {@link PDSignature}
 *       với reason/location, save incremental → bytes.
 *   <li>PDFBox callback {@code SignatureInterface.sign(InputStream)} truyền digest
 *       của byte range → {@link CmsSigner} dùng BC tạo CMS detached.
 *   <li>Trả PDF bytes mới có chữ ký nhúng.
 * </ol>
 *
 * <p>{@link #verify} duyệt mọi {@link PDSignature} trong PDF, lấy CMS bytes
 * + signed bytes (excluded gap), verify cryptographic integrity.
 */
@Slf4j
public class PdfSigner {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Ký 1 chữ ký mới vào PDF.
     *
     * @param pdfBytes      nội dung PDF gốc (đã có thể có chữ ký trước — sẽ giữ)
     * @param privateKey    private key signer
     * @param certChain     X509 cert chain của signer (thường gồm 1 cert)
     * @param reason        "Phê duyệt văn bản" / "Đóng dấu cơ quan"
     * @param location      "Hà Nội"
     * @param signerName    Common Name (sẽ ghi vào {@code /Name})
     */
    public static byte[] signPdf(
            byte[] pdfBytes,
            PrivateKey privateKey,
            Certificate[] certChain,
            String reason,
            String location,
            String signerName)
            throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDSignature sig = new PDSignature();
            sig.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            sig.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            sig.setName(signerName);
            sig.setLocation(location == null ? "" : location);
            sig.setReason(reason == null ? "" : reason);
            sig.setSignDate(Calendar.getInstance());

            CmsSigner cmsSigner = new CmsSigner(privateKey, certChain);
            doc.addSignature(sig, cmsSigner);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.saveIncremental(out);
            return out.toByteArray();
        }
    }

    /** Verify mọi chữ ký trong PDF. Trả {@link VerificationResult} per signature. */
    public static List<VerificationResult> verify(byte[] signedPdfBytes) throws IOException {
        List<VerificationResult> results = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(signedPdfBytes)) {
            for (PDSignature sig : doc.getSignatureDictionaries()) {
                results.add(verifyOne(sig, signedPdfBytes));
            }
        }
        return results;
    }

    private static VerificationResult verifyOne(PDSignature sig, byte[] pdfBytes)
            throws IOException {
        byte[] signedContent = sig.getSignedContent(pdfBytes);
        byte[] signatureContent = sig.getContents(pdfBytes);
        String subFilter = sig.getSubFilter();
        if (subFilter == null) {
            return new VerificationResult(false, "Missing subFilter", sig.getName(),
                    sig.getReason(), null, null);
        }
        try {
            CMSSignedData cms = new CMSSignedData(
                    new CMSProcessableByteArray(signedContent), signatureContent);
            SignerInformationStore signers = cms.getSignerInfos();
            JcaX509CertificateConverter converter = new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);

            for (SignerInformation signer : signers.getSigners()) {
                @SuppressWarnings("unchecked")
                java.util.Collection<X509CertificateHolder> matches =
                        (java.util.Collection<X509CertificateHolder>)
                                cms.getCertificates().getMatches(signer.getSID());
                if (matches.isEmpty()) {
                    return new VerificationResult(false, "No matching certificate",
                            sig.getName(), sig.getReason(), null, null);
                }
                X509Certificate cert = converter.getCertificate(matches.iterator().next());
                boolean ok = signer.verify(
                        new JcaSimpleSignerInfoVerifierBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                .build(cert));
                if (!ok) {
                    return new VerificationResult(false, "Signature verify failed",
                            sig.getName(), sig.getReason(),
                            cert.getSerialNumber().toString(),
                            cert.getSubjectX500Principal().getName());
                }
                return new VerificationResult(true, null, sig.getName(), sig.getReason(),
                        cert.getSerialNumber().toString(),
                        cert.getSubjectX500Principal().getName());
            }
            return new VerificationResult(false, "No signer info found",
                    sig.getName(), sig.getReason(), null, null);
        } catch (CMSSignerDigestMismatchException e) {
            return new VerificationResult(false, "Digest mismatch — file đã bị sửa sau khi ký",
                    sig.getName(), sig.getReason(), null, null);
        } catch (CMSException | java.security.cert.CertificateException
                | org.bouncycastle.operator.OperatorCreationException e) {
            return new VerificationResult(false,
                    "CMS verify error: " + e.getMessage(),
                    sig.getName(), sig.getReason(), null, null);
        }
    }

    /** Load PKCS#12 file → first PrivateKey + cert chain. */
    public static LoadedKey loadPkcs12(byte[] p12Bytes, String password) {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream in = new ByteArrayInputStream(p12Bytes)) {
                ks.load(in, password == null ? new char[0] : password.toCharArray());
            }
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    PrivateKey key = (PrivateKey) ks.getKey(
                            alias, password == null ? new char[0] : password.toCharArray());
                    Certificate[] chain = ks.getCertificateChain(alias);
                    if (chain == null || chain.length == 0) {
                        throw new IllegalStateException("Cert chain rỗng cho alias " + alias);
                    }
                    return new LoadedKey(key, chain, (X509Certificate) chain[0]);
                }
            }
            throw new IllegalStateException("Không tìm thấy key entry trong PKCS#12");
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Không load được PKCS#12 (password sai hoặc file hỏng): " + e.getMessage(), e);
        }
    }

    public record LoadedKey(PrivateKey privateKey, Certificate[] chain, X509Certificate signerCert) {}

    public record VerificationResult(
            boolean valid,
            String failureReason,
            String signerName,
            String reason,
            String certSerial,
            String subjectDn) {}

    /** Implementation cho callback PDFBox.signature → CMS detached qua BC. */
    private static final class CmsSigner implements SignatureInterface {
        private final PrivateKey privateKey;
        private final Certificate[] certChain;

        CmsSigner(PrivateKey privateKey, Certificate[] certChain) {
            this.privateKey = privateKey;
            this.certChain = certChain;
        }

        @Override
        public byte[] sign(InputStream content) throws IOException {
            try {
                CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
                ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(privateKey);
                gen.addSignerInfoGenerator(
                        new JcaSignerInfoGeneratorBuilder(
                                new JcaDigestCalculatorProviderBuilder()
                                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                        .build())
                                .build(signer, (X509Certificate) certChain[0]));
                gen.addCertificates(new JcaCertStore(java.util.Arrays.asList(certChain)));
                CMSTypedData cmsContent = new CMSProcessableByteArray(content.readAllBytes());
                CMSSignedData signedData = gen.generate(cmsContent, false);
                return signedData.getEncoded();
            } catch (Exception e) {
                throw new IOException("Không tạo được CMS signature: " + e.getMessage(), e);
            }
        }
    }

    /** Force load COSName để tránh unused import. */
    @SuppressWarnings("unused")
    private static COSName unused() {
        return COSName.SIG;
    }
}
