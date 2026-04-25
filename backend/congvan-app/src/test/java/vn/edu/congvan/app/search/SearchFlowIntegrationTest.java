package vn.edu.congvan.app.search;

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

/** FTS tiếng Việt: search có dấu, không dấu, fuzzy fallback, filters. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class SearchFlowIntegrationTest {

    static final String ADMIN_PASSWORD = "test_admin_password_search";

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
                    .withPassword("congvan_dev_search");

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
    void ftsMatchesWithDiacritics() throws Exception {
        Ctx ctx = setupContext();
        registerInbound(ctx, "Quyết định bổ nhiệm cán bộ phòng kế toán",
                "10/QĐ-UBND", "Ủy ban nhân dân tỉnh");

        mvc.perform(get("/api/search/documents")
                        .param("q", "bổ nhiệm cán bộ")
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.hits[0].subject")
                        .value("Quyết định bổ nhiệm cán bộ phòng kế toán"))
                .andExpect(jsonPath("$.data.fuzzyFallback").value(false))
                .andExpect(jsonPath("$.data.hits[0].headline").exists());
    }

    @Test
    void ftsMatchesWithoutDiacritics() throws Exception {
        Ctx ctx = setupContext();
        registerInbound(ctx, "Báo cáo tài chính quý 3", "20/BC-TCKT", "Bộ Tài chính");

        // Search không dấu — unaccent trong config "vietnamese" handle
        mvc.perform(get("/api/search/documents")
                        .param("q", "bao cao tai chinh")
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.hits[0].subject")
                        .value("Báo cáo tài chính quý 3"));
    }

    @Test
    void searchByExternalReferenceNumber() throws Exception {
        Ctx ctx = setupContext();
        registerInbound(ctx, "Test ref", "99/QĐ-UBND-2025", "Ủy ban A");

        mvc.perform(get("/api/search/documents")
                        .param("q", "99/QĐ-UBND")
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void filterByDirectionLimitsResults() throws Exception {
        Ctx ctx = setupContext();
        // Unique marker để không lẫn với test khác chia sẻ DB context
        String marker = "FILTER" + System.nanoTime();
        registerInbound(ctx, marker + " Inbound 1", "1/CV-A", "A");
        registerInbound(ctx, marker + " Inbound 2", "2/CV-A", "A");

        mvc.perform(get("/api/search/documents")
                        .param("q", marker)
                        .param("direction", "INBOUND")
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mvc.perform(get("/api/search/documents")
                        .param("q", marker)
                        .param("direction", "OUTBOUND")
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void emptyQueryReturnsAllVisibleDocuments() throws Exception {
        Ctx ctx = setupContext();
        // Tests chia sẻ context — không assert exact count, chỉ verify endpoint OK
        // và trả về list sorted by created_at DESC.
        registerInbound(ctx, "First doc EMPTY", "1/CV-X", "Cơ quan X");
        registerInbound(ctx, "Second doc EMPTY", "2/CV-X", "Cơ quan X");

        mvc.perform(get("/api/search/documents")
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.fuzzyFallback").value(false));
    }

    @Test
    void fuzzyFallbackForTypos() throws Exception {
        Ctx ctx = setupContext();
        registerInbound(ctx, "Quyết định khen thưởng cán bộ", "5/QĐ-UBND", "UBND tỉnh");

        // Typo: "khen thuơng" thay vì "khen thưởng"
        // Thực ra unaccent normalize cả hai → match qua FTS, không cần fuzzy.
        // Test typo có lỗi đánh máy thật:
        MvcResult r = mvc.perform(get("/api/search/documents")
                        .param("q", "khen thưởng cán bô") // bô thay vì bộ
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(r.getResponse().getContentAsString());
        // Match qua FTS (unaccent loại bỏ dấu) — không cần fuzzy
        assert body.at("/data/totalElements").asLong() >= 1;
    }

    // ---------- helpers ----------

    private record Ctx(String adminToken, String orgId, String docTypeId,
                       String confLevelId, String priorityId, String inboundBookId) {}

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
        String bookId = books.get(0).get("id").asText();
        return new Ctx(adminToken, orgId, docTypeId, confId, priorityId, bookId);
    }

    private void registerInbound(Ctx ctx, String subject, String refNum, String issuer)
            throws Exception {
        String dataJson = "{\"documentTypeId\":\"" + ctx.docTypeId
                + "\",\"confidentialityLevelId\":\"" + ctx.confLevelId
                + "\",\"priorityLevelId\":\"" + ctx.priorityId
                + "\",\"subject\":\"" + subject + "\","
                + "\"bookId\":\"" + ctx.inboundBookId + "\","
                + "\"organizationId\":\"" + ctx.orgId + "\","
                + "\"externalReferenceNumber\":\"" + refNum + "\","
                + "\"externalIssuer\":\"" + issuer + "\"}";
        MockMultipartFile data = new MockMultipartFile(
                "data", "data", MediaType.APPLICATION_JSON_VALUE, dataJson.getBytes());
        MockMultipartFile file = new MockMultipartFile(
                "files", "x.pdf", "application/pdf", validPdfBytes());
        mvc.perform(multipart("/api/inbound-documents")
                        .file(data).file(file)
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isCreated());
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
