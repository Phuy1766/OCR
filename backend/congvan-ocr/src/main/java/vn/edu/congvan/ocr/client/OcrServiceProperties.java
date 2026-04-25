package vn.edu.congvan.ocr.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ocr")
public record OcrServiceProperties(
        String serviceUrl, String internalApiKey, int timeoutSeconds, boolean autoTrigger) {

    public OcrServiceProperties {
        if (serviceUrl == null || serviceUrl.isBlank()) {
            serviceUrl = "http://ocr-service:5000";
        }
        if (timeoutSeconds <= 0) timeoutSeconds = 60;
    }
}
