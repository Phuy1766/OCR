package vn.edu.congvan.app.workflow;

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
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Full workflow flow: TP gán cho chuyên viên → CV complete + outbox events
 * + notifications được tạo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AssignmentFlowIntegrationTest {

    static final String ADMIN_PASSWORD = "test_admin_password_workflow";

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
                    .withPassword("congvan_dev_workflow");

    @Container
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine");

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
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("app.outbox.poll-interval-ms", () -> "300");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void truongPhongAssignsThenChuyenVienCompletes() throws Exception {
        Ctx ctx = setupContext();

        // 1) Văn thư cơ quan tạo VB đến (admin có quyền)
        String docId = registerInbound(ctx);

        // 2) TP assign cho CV
        MvcResult assignR = mvc.perform(post("/api/inbound-documents/" + docId + "/assign")
                        .header("Authorization", "Bearer " + ctx.truongPhongToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToUserId\":\"" + ctx.chuyenVienId
                                + "\",\"dueDate\":\"2026-12-31\","
                                + "\"note\":\"Vui lòng xử lý trước hạn\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.assignedToUserId").value(ctx.chuyenVienId))
                .andReturn();
        String assignmentId = json.readTree(assignR.getResponse().getContentAsString())
                .at("/data/id").asText();

        // 3) CV thấy notification
        mvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        mvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("ASSIGNMENT"))
                .andExpect(jsonPath("$.data.content[0].entityId").value(docId));

        // 4) CV thấy assignment trong /assignments/me
        mvc.perform(get("/api/assignments/me")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].documentId").value(docId));

        // 5) CV complete
        mvc.perform(post("/api/assignments/" + assignmentId + "/complete")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultSummary\":\"Đã trả lời công văn theo đúng quy trình\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 6) Document → COMPLETED
        mvc.perform(get("/api/inbound-documents/" + docId)
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 7) Outbox có ít nhất 2 events: registered + assigned + completed
        Integer outboxCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_messages WHERE aggregate_id = ?",
                Integer.class, docId);
        assertThat(outboxCount).isGreaterThanOrEqualTo(3);
    }

    @Test
    void chuyenVienCannotAssign() throws Exception {
        Ctx ctx = setupContext();
        String docId = registerInbound(ctx);
        mvc.perform(post("/api/inbound-documents/" + docId + "/assign")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToUserId\":\"" + ctx.chuyenVienId + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAssigneeCanCompleteAssignment() throws Exception {
        Ctx ctx = setupContext();
        String docId = registerInbound(ctx);
        MvcResult r = mvc.perform(post("/api/inbound-documents/" + docId + "/assign")
                        .header("Authorization", "Bearer " + ctx.truongPhongToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToUserId\":\"" + ctx.chuyenVienId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String assignmentId = json.readTree(r.getResponse().getContentAsString())
                .at("/data/id").asText();

        // TP cố complete assignment của CV → 400 (HANDLE permission có nhưng không phải assignee)
        // Tuy nhiên TP không có WORKFLOW:HANDLE → 403 trước.
        mvc.perform(post("/api/assignments/" + assignmentId + "/complete")
                        .header("Authorization", "Bearer " + ctx.truongPhongToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultSummary\":\"Người khác complete\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotAssignTwiceWithoutReassign() throws Exception {
        Ctx ctx = setupContext();
        String docId = registerInbound(ctx);
        mvc.perform(post("/api/inbound-documents/" + docId + "/assign")
                        .header("Authorization", "Bearer " + ctx.truongPhongToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToUserId\":\"" + ctx.chuyenVienId + "\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/inbound-documents/" + docId + "/assign")
                        .header("Authorization", "Bearer " + ctx.truongPhongToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToUserId\":\"" + ctx.chuyenVienId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("WORKFLOW_ALREADY_ASSIGNED"));
    }

    @Test
    void markAllNotificationsRead() throws Exception {
        Ctx ctx = setupContext();
        String docId = registerInbound(ctx);
        mvc.perform(post("/api/inbound-documents/" + docId + "/assign")
                        .header("Authorization", "Bearer " + ctx.truongPhongToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToUserId\":\"" + ctx.chuyenVienId + "\"}"))
                .andExpect(status().isCreated());

        // CV: unread = 1 → mark all read → unread = 0
        mvc.perform(post("/api/notifications/mark-all-read")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(1));
        mvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + ctx.chuyenVienToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));
    }

    // ---------- helpers ----------

    private record Ctx(
            String adminToken,
            String truongPhongToken,
            String chuyenVienToken,
            String chuyenVienId,
            String orgId,
            String docTypeId,
            String confLevelId,
            String priorityId,
            String inboundBookId) {}

    private Ctx setupContext() throws Exception {
        String adminToken = loginAs("admin", ADMIN_PASSWORD);
        JsonNode docTypes = jsonGet("/api/master/document-types", adminToken).at("/data");
        String docTypeId = findFirstByField(docTypes, "code", "CONG_VAN").get("id").asText();
        JsonNode confs = jsonGet("/api/master/confidentiality-levels", adminToken).at("/data");
        String confId = findFirstByField(confs, "code", "BINH_THUONG").get("id").asText();
        JsonNode priors = jsonGet("/api/master/priority-levels", adminToken).at("/data");
        String priorityId = findFirstByField(priors, "code", "BINH_THUONG").get("id").asText();
        JsonNode orgs = jsonGet("/api/organizations", adminToken).at("/data");
        String orgId = findFirstByField(orgs, "code", "ROOT").get("id").asText();
        JsonNode books = jsonGet("/api/master/document-books?bookType=INBOUND", adminToken).at("/data");
        String inboundBookId = books.get(0).get("id").asText();

        String suffix = String.valueOf(System.currentTimeMillis());
        UserAuth tp = createUser(adminToken, "tp" + suffix, "TRUONG_PHONG");
        UserAuth cv = createUser(adminToken, "cv" + suffix, "CHUYEN_VIEN");

        return new Ctx(adminToken, tp.token, cv.token, cv.userId,
                orgId, docTypeId, confId, priorityId, inboundBookId);
    }

    private String registerInbound(Ctx ctx) throws Exception {
        String dataJson = "{\"documentTypeId\":\"" + ctx.docTypeId
                + "\",\"confidentialityLevelId\":\"" + ctx.confLevelId
                + "\",\"priorityLevelId\":\"" + ctx.priorityId
                + "\",\"subject\":\"Công văn test workflow\","
                + "\"bookId\":\"" + ctx.inboundBookId + "\","
                + "\"organizationId\":\"" + ctx.orgId + "\","
                + "\"receivedFromChannel\":\"POST\"}";
        MockMultipartFile data = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "files", "x.pdf", "application/pdf", validPdfBytes());
        MvcResult r = mvc.perform(multipart("/api/inbound-documents")
                        .file(data).file(file)
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).at("/data/id").asText();
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
        String userId = json.readTree(r.getResponse().getContentAsString()).at("/data/id").asText();
        return new UserAuth(userId, loginAs(username, "user_pw_test_123"));
    }

    private String loginAs(String username, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\","
                                + "\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).at("/data/accessToken").asText();
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
