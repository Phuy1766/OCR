package vn.edu.congvan.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point của hệ thống quản lý công văn.
 *
 * <p>Quét toàn bộ package {@code vn.edu.congvan} để pick up các module:
 * common, auth, masterdata, inbound, outbound, workflow, ocr, signature,
 * search, audit, integration.
 */
@SpringBootApplication(scanBasePackages = "vn.edu.congvan")
@EntityScan("vn.edu.congvan")
@EnableJpaRepositories("vn.edu.congvan")
@EnableAsync
public class CongvanApplication {

    public static void main(String[] args) {
        SpringApplication.run(CongvanApplication.class, args);
    }
}
