package vn.edu.congvan.app.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Full outbound flow: draft → submit → approve dept → approve leader → issue. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OutboundFlowIntegrationTest {

    static final String ADMIN_PASSWORD = "test_admin_password_outbound_123";

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
                    .withPassword("congvan_dev_password_outbound");

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

    @Test
    void fullHappyPathDraftToIssue() throws Exception {
        // Setup: tạo các user role + lookup data
        Ctx ctx = setupContext();

        // 1) CHUYEN_VIEN tạo dự thảo
        String docId = createDraftAs(ctx.chuyenVienToken, ctx);
        // 2) CHUYEN_VIEN sửa dự thảo → version 2
        updateDraftAs(ctx.chuyenVienToken, docId, "Trích yếu phiên bản 2");
        // Verify có 2 version, version 1 SUPERSEDED, version 2 DRAFT
        JsonNode body = getDoc(docId, ctx.chuyenVienToken);
        assertThat(body.at("/data/versions").size()).isEqualTo(2);
        assertThat(body.at("/data/versions/0/versionStatus").asText()).isEqualTo("SUPERSEDED");
        assertThat(body.at("/data/versions/1/versionStatus").asText()).isEqualTo("DRAFT");

        // 3) Submit → PENDING_DEPT_APPROVAL
        mvc.perform(post("/api/outbound-documents/" + docId + "/submit")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_DEPT_APPROVAL"));

        // 4) TRUONG_PHONG approve dept
        mvc.perform(post("/api/outbound-documents/" + docId + "/approvals/dept")
                        .header("Authorization", "Bearer " + ctx.truongPhongToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\",\"comment\":\"OK cấp phòng\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_LEADER_APPROVAL"));

        // 5) LANH_DAO approve leader → APPROVED + chốt approved_version_id + hash
        MvcResult leaderResult = mvc.perform(
                        post("/api/outbound-documents/" + docId + "/approvals/leader")
                                .header("Authorization", "Bearer " + ctx.lanhDaoToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"decision\":\"APPROVED\",\"comment\":\"Đồng ý phát hành\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedVersionId").exists())
                .andReturn();
        JsonNode leaderBody = json.readTree(leaderResult.getResponse().getContentAsString());
        String approvedVersionId = leaderBody.at("/data/approvedVersionId").asText();
        // Version cuối cùng phải có status APPROVED + hash
        JsonNode approvedV = findVersion(leaderBody, approvedVersionId);
        assertThat(approvedV.get("versionStatus").asText()).isEqualTo("APPROVED");
        assertThat(approvedV.get("hashSha256").asText()).hasSize(64);

        // 6) VAN_THU_CQ issue → cấp số ra sổ outbound
        mvc.perform(post("/api/outbound-documents/" + docId + "/issue")
                        .header("Authorization", "Bearer " + ctx.vanThuToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":\"" + ctx.outboundBookId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.bookNumber").exists())
                .andExpect(jsonPath("$.data.issuedDate").exists());
    }

    @Test
    void cannotEditAfterApproved_BR07() throws Exception {
        Ctx ctx = setupContext();
        String docId = createDraftAs(ctx.chuyenVienToken, ctx);

        // Submit + approve cấp phòng + approve cấp đơn vị
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
                .andExpect(status().isOk());

        // Cố sửa sau APPROVED → 400 OUTBOUND_NOT_DRAFT
        MockMultipartFile data = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE,
                "{\"subject\":\"Sửa sau khi duyệt\"}".getBytes());
        mvc.perform(multipart("/api/outbound-documents/" + docId)
                        .file(data)
                        .with(req -> { req.setMethod("PATCH"); return req; })
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("OUTBOUND_NOT_DRAFT"));
    }

    @Test
    void rejectionReturnsToDraft() throws Exception {
        Ctx ctx = setupContext();
        String docId = createDraftAs(ctx.chuyenVienToken, ctx);
        mvc.perform(post("/api/outbound-documents/" + docId + "/submit")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken))
                .andExpect(status().isOk());
        // TRUONG_PHONG reject
        mvc.perform(post("/api/outbound-documents/" + docId + "/approvals/dept")
                        .header("Authorization", "Bearer " + ctx.truongPhongToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECTED\",\"comment\":\"Cần sửa nội dung\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void chuyenVienCannotApproveDept() throws Exception {
        Ctx ctx = setupContext();
        String docId = createDraftAs(ctx.chuyenVienToken, ctx);
        mvc.perform(post("/api/outbound-documents/" + docId + "/submit")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken))
                .andExpect(status().isOk());
        // CHUYEN_VIEN không có OUTBOUND:APPROVE_DEPT
        mvc.perform(post("/api/outbound-documents/" + docId + "/approvals/dept")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotIssueBeforeApproved() throws Exception {
        Ctx ctx = setupContext();
        String docId = createDraftAs(ctx.chuyenVienToken, ctx);
        // Vẫn ở DRAFT — issue → 400
        mvc.perform(post("/api/outbound-documents/" + docId + "/issue")
                        .header("Authorization", "Bearer " + ctx.vanThuToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":\"" + ctx.outboundBookId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("OUTBOUND_NOT_APPROVED"));
    }

    // ---------- helpers ----------

    private record Ctx(
            String adminToken,
            String chuyenVienToken,
            String truongPhongToken,
            String lanhDaoToken,
            String vanThuToken,
            String orgId,
            String docTypeId,
            String confLevelId,
            String priorityId,
            String outboundBookId) {}

    private Ctx setupContext() throws Exception {
        String adminToken = loginAs("admin", ADMIN_PASSWORD);

        // Lookup master data
        JsonNode docTypes = jsonGet("/api/master/document-types", adminToken).at("/data");
        String docTypeId = findFirstByField(docTypes, "code", "QUYET_DINH").get("id").asText();
        JsonNode confs = jsonGet("/api/master/confidentiality-levels", adminToken).at("/data");
        String confId = findFirstByField(confs, "code", "BINH_THUONG").get("id").asText();
        JsonNode priors = jsonGet("/api/master/priority-levels", adminToken).at("/data");
        String priorityId = findFirstByField(priors, "code", "BINH_THUONG").get("id").asText();

        // Lấy organization ROOT + sổ outbound bootstrap
        JsonNode orgs = jsonGet("/api/organizations", adminToken).at("/data");
        String orgId = findFirstByField(orgs, "code", "ROOT").get("id").asText();
        JsonNode books = jsonGet("/api/master/document-books?bookType=OUTBOUND", adminToken).at("/data");
        String outboundBookId = books.get(0).get("id").asText();

        // Tạo 4 user mỗi role và login
        String suffix = String.valueOf(System.currentTimeMillis());
        String chuyenVienToken =
                createUserAndLogin(adminToken, "cv" + suffix, "CHUYEN_VIEN");
        String truongPhongToken =
                createUserAndLogin(adminToken, "tp" + suffix, "TRUONG_PHONG");
        String lanhDaoToken =
                createUserAndLogin(adminToken, "ld" + suffix, "LANH_DAO");
        String vanThuToken =
                createUserAndLogin(adminToken, "vt" + suffix, "VAN_THU_CQ");

        return new Ctx(
                adminToken, chuyenVienToken, truongPhongToken, lanhDaoToken,
                vanThuToken, orgId, docTypeId, confId, priorityId, outboundBookId);
    }

    private String createDraftAs(String token, Ctx ctx) throws Exception {
        String dataJson = "{\"documentTypeId\":\"" + ctx.docTypeId
                + "\",\"confidentialityLevelId\":\"" + ctx.confLevelId
                + "\",\"priorityLevelId\":\"" + ctx.priorityId
                + "\",\"subject\":\"Quyết định phê duyệt kế hoạch\","
                + "\"organizationId\":\"" + ctx.orgId + "\"}";
        MockMultipartFile data = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "files", "draft.pdf", "application/pdf", validPdfBytes());
        MvcResult r = mvc.perform(multipart("/api/outbound-documents")
                        .file(data).file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private void updateDraftAs(String token, String docId, String newSubject) throws Exception {
        String dataJson = "{\"subject\":\"" + newSubject + "\"}";
        MockMultipartFile data = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes());
        mvc.perform(multipart("/api/outbound-documents/" + docId)
                        .file(data)
                        .with(req -> { req.setMethod("PATCH"); return req; })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private JsonNode getDoc(String docId, String token) throws Exception {
        return jsonGet("/api/outbound-documents/" + docId, token);
    }

    private JsonNode findVersion(JsonNode docBody, String versionId) {
        for (JsonNode v : docBody.at("/data/versions")) {
            if (versionId.equals(v.get("id").asText())) return v;
        }
        throw new IllegalStateException("Version " + versionId + " không có trong response");
    }

    private String createUserAndLogin(String adminToken, String username, String roleCode)
            throws Exception {
        mvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username
                                + "\",\"email\":\"" + username + "@t.local\","
                                + "\"fullName\":\"" + username + "\","
                                + "\"roleCodes\":[\"" + roleCode + "\"],"
                                + "\"initialPassword\":\"user_pw_test_123\"}"))
                .andExpect(status().isCreated());
        return loginAs(username, "user_pw_test_123");
    }

    private String loginAs(String username, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\","
                                + "\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString())
                .at("/data/accessToken")
                .asText();
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

    private static byte[] validPdfBytes() {
        byte[] header = "%PDF-1.4\n%âãÏÓ\n".getBytes();
        byte[] body = new byte[2048];
        for (int i = 0; i < body.length; i++) body[i] = (byte) (i & 0x7F);
        byte[] r = new byte[header.length + body.length];
        System.arraycopy(header, 0, r, 0, header.length);
        System.arraycopy(body, 0, r, header.length, body.length);
        return r;
    }
}
