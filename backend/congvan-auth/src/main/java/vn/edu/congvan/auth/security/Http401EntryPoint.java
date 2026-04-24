package vn.edu.congvan.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import vn.edu.congvan.common.dto.ApiResponse;

/** Trả 401 JSON theo ApiResponse format. */
@Component
@RequiredArgsConstructor
public class Http401EntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper;

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<?> body =
                new ApiResponse<>(
                        false,
                        null,
                        List.of(
                                new ApiResponse.ApiError(
                                        "AUTH_UNAUTHORIZED",
                                        null,
                                        "Yêu cầu xác thực. Vui lòng đăng nhập.")),
                        null,
                        OffsetDateTime.now());
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
