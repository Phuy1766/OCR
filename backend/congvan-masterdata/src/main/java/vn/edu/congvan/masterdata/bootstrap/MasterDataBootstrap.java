package vn.edu.congvan.masterdata.bootstrap;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.entity.DepartmentEntity;
import vn.edu.congvan.auth.repository.DepartmentRepository;
import vn.edu.congvan.auth.repository.OrganizationRepository;
import vn.edu.congvan.masterdata.entity.BookType;
import vn.edu.congvan.masterdata.entity.ConfidentialityScope;
import vn.edu.congvan.masterdata.entity.DocumentBookEntity;
import vn.edu.congvan.masterdata.repository.DocumentBookRepository;

/**
 * Chạy sau {@code AdminBootstrap}: tạo phòng Văn thư mặc định +
 * 2 sổ đăng ký của năm hiện tại cho tổ chức ROOT nếu chưa có.
 * Idempotent — an toàn chạy nhiều lần.
 */
@Slf4j
@Component
@Order(50) // sau AdminBootstrap (mặc định 0)
@RequiredArgsConstructor
public class MasterDataBootstrap implements ApplicationRunner {

    private static final String ROOT_ORG_CODE = "ROOT";
    private static final String DEFAULT_DEPT_CODE = "VAN_THU";
    private static final String INBOUND_BOOK_CODE_PREFIX = "SO_CV_DEN_";
    private static final String OUTBOUND_BOOK_CODE_PREFIX = "SO_CV_DI_";
    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OrganizationRepository organizations;
    private final DepartmentRepository departments;
    private final DocumentBookRepository books;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var root = organizations.findByCode(ROOT_ORG_CODE).orElse(null);
        if (root == null) {
            log.debug("Không tìm thấy organization ROOT — skip master data bootstrap.");
            return;
        }

        ensureDefaultDepartment(root.getId());
        int year = OffsetDateTime.now(ZONE_VN).getYear();
        ensureBook(root.getId(), INBOUND_BOOK_CODE_PREFIX + year,
                "Sổ công văn đến " + year, BookType.INBOUND);
        ensureBook(root.getId(), OUTBOUND_BOOK_CODE_PREFIX + year,
                "Sổ công văn đi " + year, BookType.OUTBOUND);
    }

    private void ensureDefaultDepartment(UUID organizationId) {
        boolean exists = departments.findByOrganizationId(organizationId).stream()
                .anyMatch(d -> DEFAULT_DEPT_CODE.equals(d.getCode()));
        if (exists) return;

        DepartmentEntity d = new DepartmentEntity();
        d.setId(UUID.randomUUID());
        d.setOrganizationId(organizationId);
        d.setCode(DEFAULT_DEPT_CODE);
        d.setName("Văn thư");
        d.setActive(true);
        departments.save(d);
        log.info("Created default department 'Văn thư' cho organization {}", organizationId);
    }

    private void ensureBook(UUID organizationId, String code, String name, BookType type) {
        if (books.existsByOrganizationIdAndCode(organizationId, code)) return;
        DocumentBookEntity b = new DocumentBookEntity();
        b.setId(UUID.randomUUID());
        b.setOrganizationId(organizationId);
        b.setCode(code);
        b.setName(name);
        b.setBookType(type);
        b.setConfidentialityScope(ConfidentialityScope.NORMAL);
        b.setActive(true);
        books.save(b);
        log.info("Created default book '{}' ({}) cho organization {}", name, type, organizationId);
    }
}
