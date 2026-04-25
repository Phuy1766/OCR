package vn.edu.congvan.app.inbound;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
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

/** Integration test full flow VB đến: register multipart, scope, recall, BR-03. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class InboundFlowIntegrationTest {

    static final String ADMIN_PASSWORD = "test_admin_password_inbound_123";

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
                    .withPassword("congvan_dev_password_inbound");

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
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    static String adminToken;
    static String orgId;
    static String inboundBookId;
    static String secretBookId;
    static String docTypeId;
    static String confLevelNormalId;
    static String confLevelSecretId;
    static String priorityNormalId;

    @BeforeAll
    static void setupOnce() {
        // Tránh re-init mỗi test, nhưng @BeforeAll cần static + cần MockMvc → dùng instance.
    }

    @org.junit.jupiter.api.BeforeEach
    void initData() throws Exception {
        if (adminToken != null) return;
        adminToken = loginAsAdmin();
        orgId = bootstrapOrgId();

        // Master data lookup
        JsonNode docTypes = jsonGet("/api/master/document-types", adminToken).at("/data");
        docTypeId =
                findFirstByField(docTypes, "code", "CONG_VAN").get("id").asText();

        JsonNode confs = jsonGet("/api/master/confidentiality-levels", adminToken).at("/data");
        confLevelNormalId = findFirstByField(confs, "code", "BINH_THUONG").get("id").asText();
        confLevelSecretId = findFirstByField(confs, "code", "MAT").get("id").asText();

        JsonNode priors = jsonGet("/api/master/priority-levels", adminToken).at("/data");
        priorityNormalId = findFirstByField(priors, "code", "BINH_THUONG").get("id").asText();

        // Books: dùng sổ INBOUND mặc định bootstrap, tạo thêm sổ SECRET test
        JsonNode books = jsonGet(
                "/api/master/document-books?bookType=INBOUND", adminToken).at("/data");
        inboundBookId = books.get(0).get("id").asText();

        // Tạo sổ SECRET cho test BR-03
        MvcResult r = mvcInstance().perform(
                        post("/api/master/document-books")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"organizationId\":\""
                                                + orgId
                                                + "\",\"code\":\"SO_DEN_MAT_TEST\","
                                                + "\"name\":\"Sổ CV đến mật\","
                                                + "\"bookType\":\"INBOUND\","
                                                + "\"confidentialityScope\":\"SECRET\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        secretBookId = json.readTree(r.getResponse().getContentAsString()).at("/data/id").asText();
    }

    @Test
    void adminRegistersInboundDocumentWithPdfFileAndStatusIsRegistered() throws Exception {
        String dataJson = baseInboundJson(confLevelNormalId, inboundBookId, "Hồ sơ thí điểm 1");
        MockMultipartFile data =
                new MockMultipartFile(
                        "data", "data", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes());
        MockMultipartFile file =
                new MockMultipartFile("files", "test.pdf", "application/pdf", validPdfBytes());

        MvcResult r =
                mvcInstance().perform(
                                multipart("/api/inbound-documents")
                                        .file(data)
                                        .file(file)
                                        .header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.status").value("REGISTERED"))
                        .andExpect(jsonPath("$.data.bookNumber").exists())
                        .andExpect(jsonPath("$.data.files.length()").value(1))
                        .andExpect(jsonPath("$.data.files[0].fileRole").value("ORIGINAL_SCAN"))
                        .andExpect(jsonPath("$.data.files[0].sha256").isString())
                        .andReturn();

        // Verify download trả về đúng nội dung
        JsonNode body = json.readTree(r.getResponse().getContentAsString());
        String docId = body.at("/data/id").asText();
        String fileId = body.at("/data/files/0/id").asText();

        mvcInstance().perform(
                        get("/api/inbound-documents/" + docId
                                        + "/files/" + fileId + "/download")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void rejectsRegistrationWithoutFile() throws Exception {
        String dataJson = baseInboundJson(confLevelNormalId, inboundBookId, "Không có file");
        MockMultipartFile data = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes());
        MockMultipartFile empty = new MockMultipartFile("files", "x", "application/pdf", new byte[0]);
        mvcInstance().perform(
                        multipart("/api/inbound-documents")
                                .file(data)
                                .file(empty)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INBOUND_FILE_REQUIRED"));
    }

    @Test
    void rejectsSecretDocumentInNormalBook_BR03() throws Exception {
        String dataJson = baseInboundJson(confLevelSecretId, inboundBookId, "VB Mật");
        MockMultipartFile data = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "files", "secret.pdf", "application/pdf", validPdfBytes());
        mvcInstance().perform(
                        multipart("/api/inbound-documents")
                                .file(data)
                                .file(file)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("BR_03_SECRET_BOOK_REQUIRED"));
    }

    @Test
    void acceptsSecretDocumentInSecretBook() throws Exception {
        String dataJson = baseInboundJson(confLevelSecretId, secretBookId, "VB Mật chính thức");
        MockMultipartFile data = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "files", "secret.pdf", "application/pdf", validPdfBytes());
        mvcInstance().perform(
                        multipart("/api/inbound-documents")
                                .file(data)
                                .file(file)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsFakePdfWithPngBytes() throws Exception {
        String dataJson = baseInboundJson(confLevelNormalId, inboundBookId, "Fake PDF");
        MockMultipartFile data = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes());
        // PNG header but claim PDF mime
        byte[] pngBytes = new byte[200];
        byte[] pngMagic = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(pngMagic, 0, pngBytes, 0, pngMagic.length);
        MockMultipartFile fake = new MockMultipartFile(
                "files", "evil.pdf", "application/pdf", pngBytes);
        mvcInstance().perform(
                        multipart("/api/inbound-documents")
                                .file(data)
                                .file(fake)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("FILE_MAGIC_MISMATCH"));
    }

    @Test
    void recalledDocumentCannotBeRecalledAgain() throws Exception {
        String docId = registerOk("Recall test doc");
        mvcInstance().perform(
                        post("/api/inbound-documents/" + docId + "/recall")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Cần thu hồi do sai sót\"}"))
                .andExpect(status().isOk());

        // Re-fetch → status RECALLED
        mvcInstance().perform(
                        get("/api/inbound-documents/" + docId)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECALLED"))
                .andExpect(jsonPath("$.data.recalled").value(true));

        // Recall lần 2 → 400
        mvcInstance().perform(
                        post("/api/inbound-documents/" + docId + "/recall")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Lần 2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("INBOUND_ALREADY_RECALLED"));
    }

    @Test
    void chuyenVienOnlySeesOwnDocuments() throws Exception {
        // Tạo VB do admin (xem được vì có READ_ALL)
        registerOk("VB admin");

        // Tạo chuyên viên
        String uname = "cv_" + System.currentTimeMillis();
        mvcInstance().perform(
                        post("/api/users")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\""
                                                + uname
                                                + "\",\"email\":\""
                                                + uname
                                                + "@t.local\",\"fullName\":\"Chuyên viên\","
                                                + "\"roleCodes\":[\"CHUYEN_VIEN\"],"
                                                + "\"initialPassword\":\"chuyenvien_pw_123\"}"))
                .andExpect(status().isCreated());

        MvcResult r = mvcInstance().perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\""
                                                + uname
                                                + "\",\"password\":\"chuyenvien_pw_123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String cvToken =
                json.readTree(r.getResponse().getContentAsString()).at("/data/accessToken").asText();

        // Chuyên viên list → chỉ thấy 0 (không có VB nào của mình)
        mvcInstance().perform(
                        get("/api/inbound-documents").header("Authorization", "Bearer " + cvToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ---------- helpers ----------

    @Autowired private org.springframework.web.context.WebApplicationContext webContext;

    private MockMvc mvcInstance() {
        return mvc;
    }

    private String registerOk(String subject) throws Exception {
        String dataJson = baseInboundJson(confLevelNormalId, inboundBookId, subject);
        MockMultipartFile data = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "files", "x.pdf", "application/pdf", validPdfBytes());
        MvcResult r = mvcInstance().perform(
                        multipart("/api/inbound-documents")
                                .file(data)
                                .file(file)
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private String baseInboundJson(String confId, String bookId, String subject) {
        return "{\"documentTypeId\":\""
                + docTypeId
                + "\",\"confidentialityLevelId\":\""
                + confId
                + "\",\"priorityLevelId\":\""
                + priorityNormalId
                + "\",\"subject\":\""
                + subject
                + "\",\"bookId\":\""
                + bookId
                + "\",\"organizationId\":\""
                + orgId
                + "\",\"receivedFromChannel\":\"POST\","
                + "\"externalReferenceNumber\":\"01/CV-TEST\","
                + "\"externalIssuer\":\"Cơ quan ngoài\"}";
    }

    private String loginAsAdmin() throws Exception {
        MvcResult r = mvcInstance().perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"admin\",\"password\":\""
                                                + ADMIN_PASSWORD
                                                + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString())
                .at("/data/accessToken")
                .asText();
    }

    private String bootstrapOrgId() throws Exception {
        JsonNode body = jsonGet("/api/organizations", adminToken);
        for (JsonNode o : body.at("/data")) {
            if ("ROOT".equals(o.get("code").asText())) return o.get("id").asText();
        }
        throw new IllegalStateException("ROOT organization không tồn tại");
    }

    private JsonNode jsonGet(String path, String token) throws Exception {
        MvcResult r = mvcInstance().perform(
                        get(path).header("Authorization", "Bearer " + token))
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

    private static byte[] validPdfBytes() {
        // Minimal but valid-looking PDF header + content for testing magic bytes only
        byte[] header = "%PDF-1.4\n%âãÏÓ\n".getBytes();
        byte[] body = new byte[2048];
        for (int i = 0; i < body.length; i++) body[i] = (byte) (i & 0x7F);
        byte[] r = new byte[header.length + body.length];
        System.arraycopy(header, 0, r, 0, header.length);
        System.arraycopy(body, 0, r, header.length, body.length);
        return r;
    }
}
