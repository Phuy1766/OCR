package vn.edu.congvan.app.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import vn.edu.congvan.ocr.client.OcrClient;
import vn.edu.congvan.ocr.client.OcrServiceResponse;

/**
 * Integration test OCR flow: register VB → auto-trigger OCR job → mock OCR
 * client trả response → COMPLETED → văn thư accept → metadata áp vào document.
 *
 * <p>OcrClient được mock để không cần FastAPI thật chạy trong CI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OcrFlowIntegrationTest {

    static final String ADMIN_PASSWORD = "test_admin_password_ocr";

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
                    .withPassword("congvan_dev_ocr");

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
        registry.add("app.ocr.auto-trigger", () -> "true");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @MockBean OcrClient ocrClient;

    @Test
    void registerInboundTriggersOcrAutomatically() throws Exception {
        // Mock OCR client trả về 4 fields đã trích xuất
        OcrServiceResponse mockResponse = new OcrServiceResponse(
                "BỘ NỘI VỤ\nSố: 99/QĐ-BNV\nHà Nội, ngày 01 tháng 9 năm 2025\nV/v phê duyệt kế hoạch năm",
                new BigDecimal("0.920"),
                1234,
                1,
                "PaddleOCR-vi",
                List.of(
                        new OcrServiceResponse.ExtractedField(
                                "external_reference_number", "99/QĐ-BNV",
                                new BigDecimal("0.85"), null, 1),
                        new OcrServiceResponse.ExtractedField(
                                "external_issuer", "BỘ NỘI VỤ",
                                new BigDecimal("0.6"), null, 1),
                        new OcrServiceResponse.ExtractedField(
                                "external_issued_date", "2025-09-01",
                                new BigDecimal("0.9"), null, 1),
                        new OcrServiceResponse.ExtractedField(
                                "subject", "phê duyệt kế hoạch năm",
                                new BigDecimal("0.75"), null, 1)));
        org.mockito.Mockito.when(ocrClient.process(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(byte[].class)))
                .thenReturn(mockResponse);

        Ctx ctx = setupContext();
        String docId = registerInbound(ctx);

        // Đợi async job processing complete (poll OCR status)
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    MvcResult r = mvc.perform(get("/api/inbound-documents/" + docId + "/ocr")
                                    .header("Authorization", "Bearer " + ctx.adminToken))
                            .andReturn();
                    if (r.getResponse().getStatus() != 200) return false;
                    JsonNode body = json.readTree(r.getResponse().getContentAsString());
                    JsonNode data = body.at("/data");
                    if (data.isMissingNode() || data.isNull()) return false;
                    return "COMPLETED".equals(data.get("status").asText());
                });

        // Verify OCR result + extracted fields có sẵn
        MvcResult ocrR = mvc.perform(get("/api/inbound-documents/" + docId + "/ocr")
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.result.fields.length()").value(4))
                .andReturn();
        JsonNode body = json.readTree(ocrR.getResponse().getContentAsString());
        String jobId = body.at("/data/jobId").asText();

        // Văn thư accept (override subject) → áp metadata vào document
        mvc.perform(post("/api/ocr-jobs/" + jobId + "/accept")
                        .header("Authorization", "Bearer " + ctx.adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Trích yếu chỉnh tay\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result.accepted").value(true));

        // Document phải có:
        // - subject = "Trích yếu chỉnh tay" (user override)
        // - external_reference_number = "99/QĐ-BNV" (từ OCR)
        // - external_issuer = "BỘ NỘI VỤ" (từ OCR)
        // - external_issued_date = "2025-09-01" (từ OCR)
        mvc.perform(get("/api/inbound-documents/" + docId)
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject").value("Trích yếu chỉnh tay"))
                .andExpect(jsonPath("$.data.externalReferenceNumber").value("99/QĐ-BNV"))
                .andExpect(jsonPath("$.data.externalIssuer").value("BỘ NỘI VỤ"))
                .andExpect(jsonPath("$.data.externalIssuedDate").value("2025-09-01"))
                .andExpect(jsonPath("$.data.status").value("REGISTERED"));
    }

    @Test
    void ocrServiceFailureMarksJobFailed() throws Exception {
        org.mockito.Mockito.when(ocrClient.process(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(byte[].class)))
                .thenThrow(new vn.edu.congvan.common.exception.BusinessException(
                        "OCR_SERVICE_ERROR", "FastAPI down"));

        Ctx ctx = setupContext();
        String docId = registerInbound(ctx);

        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    MvcResult r = mvc.perform(get("/api/inbound-documents/" + docId + "/ocr")
                                    .header("Authorization", "Bearer " + ctx.adminToken))
                            .andReturn();
                    JsonNode body = json.readTree(r.getResponse().getContentAsString());
                    JsonNode data = body.at("/data");
                    if (data.isMissingNode() || data.isNull()) return false;
                    return "SERVICE_UNAVAILABLE".equals(data.get("status").asText());
                });

        // Verify error message stored
        MvcResult r = mvc.perform(get("/api/inbound-documents/" + docId + "/ocr")
                        .header("Authorization", "Bearer " + ctx.adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(r.getResponse().getContentAsString());
        assertThat(body.at("/data/errorMessage").asText()).contains("FastAPI down");
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

    private String registerInbound(Ctx ctx) throws Exception {
        String dataJson = "{\"documentTypeId\":\"" + ctx.docTypeId
                + "\",\"confidentialityLevelId\":\"" + ctx.confLevelId
                + "\",\"priorityLevelId\":\"" + ctx.priorityId
                + "\",\"subject\":\"Test OCR pipeline\","
                + "\"bookId\":\"" + ctx.inboundBookId + "\","
                + "\"organizationId\":\"" + ctx.orgId + "\"}";
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

    @SuppressWarnings("unused")
    private static UUID unused() { return null; }
}
