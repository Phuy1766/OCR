package vn.edu.congvan.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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

/**
 * Integration test full flow auth: login thành công, /auth/me, sai password,
 * lockout sau 5 lần, refresh rotation, logout.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthFlowIntegrationTest {

    static final String ADMIN_PASSWORD = "admin_strong_test_password_123";

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
    void loginSuccessReturnsTokensAndRefreshCookie() throws Exception {
        MvcResult result =
                mvc.perform(
                                post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\"admin\",\"password\":\""
                                                        + ADMIN_PASSWORD
                                                        + "\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.accessToken").exists())
                        .andExpect(jsonPath("$.data.refreshToken").exists())
                        .andExpect(jsonPath("$.data.user.username").value("admin"))
                        .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"))
                        .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie("congvan_refresh");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        String access = body.at("/data/accessToken").asText();

        // /auth/me với access token
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void fiveFailedLoginsLockAccountTemporarily() throws Exception {
        // Dùng user khác để không ảnh hưởng các test song song cùng admin.
        // Tạo user qua admin token.
        String adminAccess = loginAsAdmin();
        String username = "lockme_" + System.currentTimeMillis();
        mvc.perform(
                        post("/api/users")
                                .header("Authorization", "Bearer " + adminAccess)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\""
                                                + username
                                                + "\",\"email\":\""
                                                + username
                                                + "@test.local\",\"fullName\":\"Lock Test\","
                                                + "\"roleCodes\":[\"CHUYEN_VIEN\"],"
                                                + "\"initialPassword\":\"user_initial_pw_123\"}"))
                .andExpect(status().isCreated());

        // 5 lần sai mật khẩu → lockout soft.
        for (int i = 0; i < 5; i++) {
            mvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"username\":\""
                                                    + username
                                                    + "\",\"password\":\"wrong_pw\"}"))
                    .andExpect(status().isUnauthorized());
        }
        // Lần thứ 6 — dù đúng mật khẩu vẫn phải bị 403 do soft lock.
        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\""
                                                + username
                                                + "\",\"password\":\"user_initial_pw_123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0].code").value("AUTH_ACCOUNT_LOCKED"));
    }

    @Test
    void refreshRotationRevokesPreviousToken() throws Exception {
        MvcResult login = login("admin", ADMIN_PASSWORD).andReturn();
        String firstRefresh =
                json.readTree(login.getResponse().getContentAsString())
                        .at("/data/refreshToken")
                        .asText();

        // Refresh lần 1 — OK, returns new pair.
        MvcResult r1 =
                mvc.perform(
                                post("/api/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"refreshToken\":\""
                                                        + firstRefresh
                                                        + "\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        String secondRefresh =
                json.readTree(r1.getResponse().getContentAsString())
                        .at("/data/refreshToken")
                        .asText();
        assertThat(secondRefresh).isNotEqualTo(firstRefresh);

        // Dùng lại first refresh → reuse detection → revoke all + 401.
        mvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errors[0].code").value("AUTH_REFRESH_REUSED"));
    }

    @Test
    void logoutBlacklistsAccessToken() throws Exception {
        MvcResult login = login("admin", ADMIN_PASSWORD).andReturn();
        String access =
                json.readTree(login.getResponse().getContentAsString())
                        .at("/data/accessToken")
                        .asText();

        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());

        // Dùng lại access token sau logout → 401.
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequestsToProtectedEndpointReturn401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotCreateUser() throws Exception {
        String adminAccess = loginAsAdmin();
        String username = "plainuser_" + System.currentTimeMillis();
        mvc.perform(
                        post("/api/users")
                                .header("Authorization", "Bearer " + adminAccess)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\""
                                                + username
                                                + "\",\"email\":\""
                                                + username
                                                + "@test.local\",\"fullName\":\"Plain\","
                                                + "\"roleCodes\":[\"CHUYEN_VIEN\"],"
                                                + "\"initialPassword\":\"plain_pw_1234\"}"))
                .andExpect(status().isCreated());

        // Login bằng user chuyên viên → không có USER:MANAGE.
        MvcResult login = login(username, "plain_pw_1234").andReturn();
        String access =
                json.readTree(login.getResponse().getContentAsString())
                        .at("/data/accessToken")
                        .asText();
        mvc.perform(
                        post("/api/users")
                                .header("Authorization", "Bearer " + access)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"denied_user\","
                                                + "\"email\":\"denied@test.local\","
                                                + "\"fullName\":\"Denied User\","
                                                + "\"roleCodes\":[\"CHUYEN_VIEN\"],"
                                                + "\"initialPassword\":\"any_valid_pw_123\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.ResultActions login(
            String username, String password) throws Exception {
        return mvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"username\":\""
                                        + username
                                        + "\",\"password\":\""
                                        + password
                                        + "\"}"));
    }

    private String loginAsAdmin() throws Exception {
        MvcResult r = login("admin", ADMIN_PASSWORD).andReturn();
        return json.readTree(r.getResponse().getContentAsString())
                .at("/data/accessToken")
                .asText();
    }
}
