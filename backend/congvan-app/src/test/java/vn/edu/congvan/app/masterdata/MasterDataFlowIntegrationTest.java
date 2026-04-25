package vn.edu.congvan.app.masterdata;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Master data CRUD + permission check qua MockMvc. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class MasterDataFlowIntegrationTest {

    static final String ADMIN_PASSWORD = "test_admin_password_master_data";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("congvan_test")
                    .withUsername("congvan")
                    .withPassword("congvan");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("app.bootstrap.admin-password", () -> ADMIN_PASSWORD);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void listSeedMasterDataAsAuthenticatedUser() throws Exception {
        String token = loginAsAdmin();

        mvc.perform(get("/api/master/document-types").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(29)) // Phụ lục III
                .andExpect(jsonPath("$.data[0].abbreviation").value("NQ"));

        mvc.perform(
                        get("/api/master/confidentiality-levels")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].code").value("BINH_THUONG"))
                .andExpect(jsonPath("$.data[3].requiresSecretBook").value(true));

        mvc.perform(
                        get("/api/master/priority-levels")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[3].code").value("HOA_TOC"))
                .andExpect(jsonPath("$.data[3].slaHours").value(6));
    }

    @Test
    void adminCreatesOrganizationDepartmentAndDocumentBook() throws Exception {
        String token = loginAsAdmin();

        // 1) Tạo organization con
        MvcResult orgR =
                mvc.perform(
                                post("/api/organizations")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"code\":\"PHONG_A\",\"name\":\"Phòng A\","
                                                        + "\"fullName\":\"Phòng Tổ chức cán bộ\"}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        String orgId =
                json.readTree(orgR.getResponse().getContentAsString()).at("/data/id").asText();

        // 2) Tạo department trong organization đó
        mvc.perform(
                        post("/api/departments")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"organizationId\":\""
                                                + orgId
                                                + "\",\"code\":\"TCCB\",\"name\":\"Tổ chức cán bộ\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.organizationId").value(orgId));

        // 3) Tạo sổ CV đi 2026
        mvc.perform(
                        post("/api/master/document-books")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"organizationId\":\""
                                                + orgId
                                                + "\",\"code\":\"SO_DI_TEST\",\"name\":\"Sổ test\","
                                                + "\"bookType\":\"OUTBOUND\",\"confidentialityScope\":\"NORMAL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookType").value("OUTBOUND"))
                .andExpect(jsonPath("$.data.confidentialityScope").value("NORMAL"));

        // 4) List books filter theo organization
        mvc.perform(
                        get("/api/master/document-books")
                                .param("organizationId", orgId)
                                .param("bookType", "OUTBOUND")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("SO_DI_TEST"));
    }

    @Test
    void nonAdminCannotCreateDocumentBook() throws Exception {
        String adminAccess = loginAsAdmin();
        String uname = "nondamin_" + System.currentTimeMillis();

        // Tạo user role CHUYEN_VIEN (không có MASTERDATA:MANAGE)
        mvc.perform(
                        post("/api/users")
                                .header("Authorization", "Bearer " + adminAccess)
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

        MvcResult r =
                mvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\""
                                                        + uname
                                                        + "\",\"password\":\"chuyenvien_pw_123\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        String userAccess =
                json.readTree(r.getResponse().getContentAsString())
                        .at("/data/accessToken")
                        .asText();

        // List master data vẫn được (MASTERDATA:READ)
        mvc.perform(
                        get("/api/master/document-types")
                                .header("Authorization", "Bearer " + userAccess))
                .andExpect(status().isOk());

        // Nhưng POST book → 403 (body hợp lệ validation để kiểm tra đúng lớp @PreAuthorize)
        mvc.perform(
                        post("/api/master/document-books")
                                .header("Authorization", "Bearer " + userAccess)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"organizationId\":\"00000000-0000-0000-0000-000000000001\","
                                                + "\"code\":\"DENIED_BOOK\","
                                                + "\"name\":\"Sổ bị từ chối\","
                                                + "\"bookType\":\"OUTBOUND\","
                                                + "\"confidentialityScope\":\"NORMAL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void departmentParentMustBelongToSameOrganization() throws Exception {
        String token = loginAsAdmin();

        // Tạo 2 org
        String org1 = createOrg(token, "O1", "Tổ chức 1");
        String org2 = createOrg(token, "O2", "Tổ chức 2");

        // Dept trong org1
        String deptId1 = createDept(token, org1, "DEPT1", "Phòng 1", null);

        // Cố tạo dept trong org2 với parent = deptId1 (khác org) → 400
        mvc.perform(
                        post("/api/departments")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"organizationId\":\""
                                                + org2
                                                + "\",\"code\":\"DEPT2\",\"name\":\"Phòng 2\","
                                                + "\"parentId\":\""
                                                + deptId1
                                                + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value("DEPT_PARENT_WRONG_ORG"));
    }

    // ---------- helpers ----------

    private String loginAsAdmin() throws Exception {
        MvcResult r =
                mvc.perform(
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

    private String createOrg(String token, String code, String name) throws Exception {
        MvcResult r =
                mvc.perform(
                                post("/api/organizations")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"code\":\""
                                                        + code
                                                        + "\",\"name\":\""
                                                        + name
                                                        + "\"}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).at("/data/id").asText();
    }

    private String createDept(String token, String orgId, String code, String name, String parentId)
            throws Exception {
        String parentJson = parentId == null ? "" : ",\"parentId\":\"" + parentId + "\"";
        MvcResult r =
                mvc.perform(
                                post("/api/departments")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"organizationId\":\""
                                                        + orgId
                                                        + "\",\"code\":\""
                                                        + code
                                                        + "\",\"name\":\""
                                                        + name
                                                        + "\""
                                                        + parentJson
                                                        + "}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        JsonNode body = json.readTree(r.getResponse().getContentAsString());
        return body.at("/data/id").asText();
    }
}
