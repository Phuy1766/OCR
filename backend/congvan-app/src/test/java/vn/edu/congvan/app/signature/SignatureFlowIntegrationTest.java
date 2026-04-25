package vn.edu.congvan.app.signature;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.UUID;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Full signature flow: approve → signPersonal → signOrganization → issue. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class SignatureFlowIntegrationTest {

    static final String ADMIN_PASSWORD = "test_admin_password_signature";
    static final String P12_PASSWORD = "p12-test-pw";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("congvan_test")
                    .withUsername("congvan")
                    .withPassword("congvan");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @Container
    static final MinIOContainer MINIO =
            new MinIOContainer("minio/minio:RELEASE.2024-07-10T18-41-49Z")
                    .withUserName("congvan_admin")
                    .withPassword("congvan_dev_signature");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("app.bootstrap.admin-password", () -> ADMIN_PASSWORD);
        registry.add("app.storage.minio.endpoint", MINIO::getS3URL);
        registry.add("app.storage.minio.access-key", MINIO::getUserName);
        registry.add("app.storage.minio.secret-key", MINIO::getPassword);
        // Bật signature gate cho Phase 7 test
        registry.add("app.signature.gate-enabled", () -> "true");
    }

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void issueIsBlockedWithoutSignaturesThenAllowedAfterBoth() throws Exception {
        Ctx ctx = setupContext();
        // 1. Tạo + duyệt VB đi
        String docId = createApprovedDocument(ctx);

        // 2. Issue ngay khi chưa ký → 400 BR_06_SIGNATURES_REQUIRED
        mvc.perform(post("/api/outbound-documents/" + docId + "/issue")
                        .header("Authorization", "Bearer " + ctx.vanThuToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":\"" + ctx.outboundBookId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("BR_06_SIGNATURES_REQUIRED"));

        // 3. Upload cert PERSONAL cho lãnh đạo
        String personalCertId = uploadCertificate(ctx.adminToken,
                "PERSONAL", "Lãnh đạo Test", ctx.lanhDaoId, null);

        // 4. Lãnh đạo ký personal
        mvc.perform(post("/api/outbound-documents/" + docId + "/sign-personal")
                        .header("Authorization", "Bearer " + ctx.lanhDaoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certificateId\":\"" + personalCertId + "\","
                                + "\"pkcs12Password\":\"" + P12_PASSWORD + "\","
                                + "\"reason\":\"Phê duyệt phát hành\","
                                + "\"location\":\"Hà Nội\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signatureType").value("PERSONAL"));

        // 5. Issue thử — vẫn thiếu cơ quan → 400
        mvc.perform(post("/api/outbound-documents/" + docId + "/issue")
                        .header("Authorization", "Bearer " + ctx.vanThuToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":\"" + ctx.outboundBookId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("BR_06_SIGNATURES_REQUIRED"));

        // 6. Văn thư ký cơ quan (cần upload cert ORGANIZATION)
        String orgCertId = uploadCertificate(ctx.adminToken,
                "ORGANIZATION", "Cơ quan Test", null, ctx.orgId);
        mvc.perform(post("/api/outbound-documents/" + docId + "/sign-organization")
                        .header("Authorization", "Bearer " + ctx.vanThuToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certificateId\":\"" + orgCertId + "\","
                                + "\"pkcs12Password\":\"" + P12_PASSWORD + "\","
                                + "\"reason\":\"Đóng dấu cơ quan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.signatureType").value("ORGANIZATION"));

        // 7. Issue OK
        mvc.perform(post("/api/outbound-documents/" + docId + "/issue")
                        .header("Authorization", "Bearer " + ctx.vanThuToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":\"" + ctx.outboundBookId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.bookNumber").exists());

        // 8. List signatures
        mvc.perform(get("/api/outbound-documents/" + docId + "/signatures")
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 9. Verify signatures (chấp nhận self-signed cert tạm)
        mvc.perform(post("/api/outbound-documents/" + docId + "/signatures/verify")
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void cannotSignOrganizationBeforePersonal() throws Exception {
        Ctx ctx = setupContext();
        String docId = createApprovedDocument(ctx);
        String orgCertId = uploadCertificate(ctx.adminToken,
                "ORGANIZATION", "Cơ quan TestB", null, ctx.orgId);

        mvc.perform(post("/api/outbound-documents/" + docId + "/sign-organization")
                        .header("Authorization", "Bearer " + ctx.vanThuToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certificateId\":\"" + orgCertId + "\","
                                + "\"pkcs12Password\":\"" + P12_PASSWORD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("SIGN_PERSONAL_REQUIRED"));
    }

    // ---------- helpers ----------

    private record Ctx(
            String adminToken,
            String chuyenVienToken,
            String truongPhongToken,
            String lanhDaoToken,
            String vanThuToken,
            String lanhDaoId,
            String orgId,
            String docTypeId,
            String confLevelId,
            String priorityId,
            String outboundBookId) {}

    private Ctx setupContext() throws Exception {
        String adminToken = loginAs("admin", ADMIN_PASSWORD);
        JsonNode docTypes = jsonGet("/api/master/document-types", adminToken).at("/data");
        String docTypeId = findFirstByField(docTypes, "code", "QUYET_DINH").get("id").asText();
        JsonNode confs = jsonGet("/api/master/confidentiality-levels", adminToken).at("/data");
        String confId = findFirstByField(confs, "code", "BINH_THUONG").get("id").asText();
        JsonNode priors = jsonGet("/api/master/priority-levels", adminToken).at("/data");
        String priorityId = findFirstByField(priors, "code", "BINH_THUONG").get("id").asText();
        JsonNode orgs = jsonGet("/api/organizations", adminToken).at("/data");
        String orgId = findFirstByField(orgs, "code", "ROOT").get("id").asText();
        JsonNode books = jsonGet("/api/master/document-books?bookType=OUTBOUND", adminToken).at("/data");
        String outboundBookId = books.get(0).get("id").asText();

        String suffix = String.valueOf(System.currentTimeMillis());
        UserAuth cv = createUser(adminToken, "cv" + suffix, "CHUYEN_VIEN");
        UserAuth tp = createUser(adminToken, "tp" + suffix, "TRUONG_PHONG");
        UserAuth ld = createUser(adminToken, "ld" + suffix, "LANH_DAO");
        UserAuth vt = createUser(adminToken, "vt" + suffix, "VAN_THU_CQ");
        return new Ctx(adminToken, cv.token, tp.token, ld.token, vt.token,
                ld.userId, orgId, docTypeId, confId, priorityId, outboundBookId);
    }

    private String createApprovedDocument(Ctx ctx) throws Exception {
        // Chuyên viên tạo dự thảo với PDF mẫu
        String dataJson = "{\"documentTypeId\":\"" + ctx.docTypeId
                + "\",\"confidentialityLevelId\":\"" + ctx.confLevelId
                + "\",\"priorityLevelId\":\"" + ctx.priorityId
                + "\",\"subject\":\"Quyết định test ký số\","
                + "\"organizationId\":\"" + ctx.orgId + "\"}";
        MockMultipartFile data = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "files", "draft.pdf", "application/pdf", samplePdfBytes());
        MvcResult r = mvc.perform(multipart("/api/outbound-documents")
                        .file(data).file(file)
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken))
                .andExpect(status().isCreated())
                .andReturn();
        String docId = json.readTree(r.getResponse().getContentAsString())
                .at("/data/id").asText();

        mvc.perform(post("/api/outbound-documents/" + docId + "/submit")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken))
                .andExpect(status().isOk());
        mvc.perform(post("/api/outbound-documents/" + docId + "/approvals/dept")
                        .header("Authorization", "Bearer " + ctx.truongPhongToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/outbound-documents/" + docId + "/approvals/leader")
                        .header("Authorization", "Bearer " + ctx.lanhDaoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        return docId;
    }

    private String uploadCertificate(
            String adminToken, String type, String alias, String userId, String orgId)
            throws Exception {
        byte[] p12 = generatePkcs12(alias);
        String body = "{\"type\":\"" + type + "\",\"alias\":\"" + alias
                + "\",\"pkcs12Password\":\"" + P12_PASSWORD + "\""
                + (userId == null ? "" : ",\"ownerUserId\":\"" + userId + "\"")
                + (orgId == null ? "" : ",\"ownerOrganizationId\":\"" + orgId + "\"")
                + "}";
        MockMultipartFile dataPart = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE, body.getBytes());
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "test.p12", "application/x-pkcs12", p12);
        MvcResult r = mvc.perform(multipart("/api/certificates")
                        .file(dataPart).file(filePart)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString())
                .at("/data/id").asText();
    }

    private record UserAuth(String userId, String token) {}

    private UserAuth createUser(String adminToken, String username, String role) throws Exception {
        MvcResult r = mvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username
                                + "\",\"email\":\"" + username + "@t.local\","
                                + "\"fullName\":\"" + username + "\","
                                + "\"roleCodes\":[\"" + role + "\"],"
                                + "\"initialPassword\":\"user_pw_test_123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = json.readTree(r.getResponse().getContentAsString())
                .at("/data/id").asText();
        return new UserAuth(userId, loginAs(username, "user_pw_test_123"));
    }

    private String loginAs(String username, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\","
                                + "\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
    }

    private JsonNode jsonGet(String path, String token) throws Exception {
        MvcResult r = mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private static JsonNode findFirstByField(JsonNode array, String field, String value) {
        for (JsonNode n : array) {
            if (value.equals(n.get(field).asText())) return n;
        }
        throw new IllegalStateException("Không tìm thấy " + field + "=" + value);
    }

    private static byte[] samplePdfBytes() throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument doc =
                new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(page);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                    new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(
                        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 14);
                cs.newLineAtOffset(72, 720);
                cs.showText("Quyet dinh test ky so");
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] generatePkcs12(String cn) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048, new SecureRandom());
        KeyPair kp = kpg.generateKeyPair();

        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 60_000);
        Date notAfter = new Date(now + 365L * 24 * 3600 * 1000);
        X500Name name = new X500Name("CN=" + cn + ", O=Congvan Test, C=VN");

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(kp.getPrivate());
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(new JcaX509v3CertificateBuilder(
                        name, BigInteger.valueOf(now), notBefore, notAfter, name, kp.getPublic())
                        .build(signer));

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("test", kp.getPrivate(), P12_PASSWORD.toCharArray(),
                new java.security.cert.Certificate[]{cert});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ks.store(out, P12_PASSWORD.toCharArray());
        return out.toByteArray();
    }

    @SuppressWarnings("unused")
    private static UUID unused() { return null; }
}
