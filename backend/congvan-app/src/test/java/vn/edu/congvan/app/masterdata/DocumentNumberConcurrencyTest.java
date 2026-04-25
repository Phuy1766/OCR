package vn.edu.congvan.app.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import vn.edu.congvan.auth.entity.OrganizationEntity;
import vn.edu.congvan.auth.repository.OrganizationRepository;
import vn.edu.congvan.masterdata.dto.ReservedNumber;
import vn.edu.congvan.masterdata.entity.BookType;
import vn.edu.congvan.masterdata.entity.ConfidentialityScope;
import vn.edu.congvan.masterdata.entity.DocumentBookEntity;
import vn.edu.congvan.masterdata.repository.DocumentBookRepository;
import vn.edu.congvan.masterdata.service.DocumentNumberService;

/**
 * Kiểm tra BR-02: N thread gọi reserve() đồng thời phải nhận N số unique,
 * liên tiếp, không trùng.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DocumentNumberConcurrencyTest {

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
        registry.add("app.bootstrap.admin-password", () -> "test_admin_password_123");
    }

    @Autowired DocumentNumberService numberService;
    @Autowired DocumentBookRepository books;
    @Autowired OrganizationRepository organizations;

    @Test
    void hundredThreadsReserveUniqueConsecutiveNumbers() throws Exception {
        OrganizationEntity root = organizations.findByCode("ROOT").orElseThrow();
        DocumentBookEntity book = new DocumentBookEntity();
        book.setId(UUID.randomUUID());
        book.setOrganizationId(root.getId());
        book.setCode("TEST_CONCURRENCY_" + System.currentTimeMillis());
        book.setName("Sổ test đồng thời");
        book.setBookType(BookType.OUTBOUND);
        book.setConfidentialityScope(ConfidentialityScope.NORMAL);
        book.setActive(true);
        book = books.save(book);
        final UUID bookId = book.getId();

        final int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        try {
            List<Callable<ReservedNumber>> tasks =
                    Collections.nCopies(threads, () -> numberService.reserve(bookId));
            List<Future<ReservedNumber>> futures = pool.invokeAll(tasks);
            Set<Long> numbers = new TreeSet<>();
            for (Future<ReservedNumber> f : futures) {
                ReservedNumber r = f.get();
                assertThat(r.bookId()).isEqualTo(bookId);
                numbers.add(r.number());
            }
            assertThat(numbers).hasSize(threads);
            // Liên tiếp từ 1 đến 100 (giả sử sổ mới chưa cấp số)
            assertThat(numbers.iterator().next()).isEqualTo(1L);
            assertThat(new TreeSet<>(numbers).last()).isEqualTo((long) threads);
        } finally {
            pool.shutdownNow();
        }
    }
}
