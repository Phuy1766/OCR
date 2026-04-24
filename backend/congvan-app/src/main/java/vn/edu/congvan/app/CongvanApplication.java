package vn.edu.congvan.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point của hệ thống quản lý công văn.
 *
 * <p>Quét toàn bộ package {@code vn.edu.congvan} để pick up các module:
 * common, auth, masterdata, inbound, outbound, workflow, ocr, signature,
 * search, audit, integration.
 */
@SpringBootApplication(scanBasePackages = "vn.edu.congvan")
public class CongvanApplication {

    public static void main(String[] args) {
        SpringApplication.run(CongvanApplication.class, args);
    }
}
