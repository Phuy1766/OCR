package vn.edu.congvan.ocr.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import vn.edu.congvan.common.exception.BusinessException;

/** Client gọi FastAPI OCR service qua RestClient (Spring 6). */
@Slf4j
@Service
public class OcrClient {

    private final RestClient client;
    private final OcrServiceProperties props;

    public OcrClient(OcrServiceProperties props, ObjectMapper json) {
        this.props = props;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        rf.setReadTimeout((int) Duration.ofSeconds(props.timeoutSeconds()).toMillis());
        this.client = RestClient.builder()
                .baseUrl(props.serviceUrl())
                .requestFactory(rf)
                .messageConverters(c -> {
                    // Giữ default + đảm bảo ObjectMapper share với app
                })
                .build();
    }

    /** Gọi POST /ocr/process với multipart file. */
    public OcrServiceResponse process(String filename, String mimeType, byte[] content) {
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        try {
            return client.post()
                    .uri("/ocr/process")
                    .header("X-Internal-API-Key", nullToEmpty(props.internalApiKey()))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                    .body(body)
                    .retrieve()
                    .body(OcrServiceResponse.class);
        } catch (Exception e) {
            log.warn("OCR service call failed: {}", e.getMessage());
            throw new BusinessException(
                    "OCR_SERVICE_ERROR",
                    "Không gọi được OCR service: " + e.getMessage());
        }
    }

    /**
     * Endpoint test/debug — gọi /ocr/process-text với raw_text → chỉ field
     * extractor chạy. Dùng cho integration test backend không cần PaddleOCR.
     */
    public OcrServiceResponse processTextOnly(String rawText) {
        try {
            return client.post()
                    .uri("/ocr/process-text")
                    .header("X-Internal-API-Key", nullToEmpty(props.internalApiKey()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of("raw_text", rawText))
                    .retrieve()
                    .body(OcrServiceResponse.class);
        } catch (Exception e) {
            throw new BusinessException(
                    "OCR_SERVICE_ERROR",
                    "Không gọi được OCR service (text-only): " + e.getMessage());
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Force load IOException import (avoid unused import warnings). */
    @SuppressWarnings("unused")
    private static IOException unused() {
        return null;
    }
}

@Configuration
@EnableConfigurationProperties(OcrServiceProperties.class)
class OcrClientConfig {}
