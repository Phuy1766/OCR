package vn.edu.congvan.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import vn.edu.congvan.common.dto.ApiResponse;

/** Trả 403 JSON theo ApiResponse format. */
@Component
@RequiredArgsConstructor
public class Http403AccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper mapper;

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<?> body =
                new ApiResponse<>(
                        false,
                        null,
                        List.of(
                                new ApiResponse.ApiError(
                                        "AUTH_FORBIDDEN",
                                        null,
                                        "Không đủ quyền thực hiện thao tác này.")),
                        null,
                        OffsetDateTime.now());
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
