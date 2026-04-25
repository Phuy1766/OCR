package vn.edu.congvan.masterdata.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.entity.DepartmentEntity;
import vn.edu.congvan.auth.repository.DepartmentRepository;
import vn.edu.congvan.auth.repository.OrganizationRepository;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.masterdata.dto.CreateDepartmentRequest;
import vn.edu.congvan.masterdata.dto.DepartmentDto;
import vn.edu.congvan.masterdata.dto.UpdateDepartmentRequest;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository repo;
    private final OrganizationRepository organizations;

    @Transactional(readOnly = true)
    public List<DepartmentDto> listByOrganization(UUID organizationId) {
        return repo.findByOrganizationId(organizationId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentDto getById(UUID id) {
        return toDto(
                repo.findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "DEPT_NOT_FOUND",
                                                "Không tìm thấy phòng/ban.")));
    }

    @Transactional
    public DepartmentDto create(CreateDepartmentRequest req) {
        if (!organizations.existsById(req.organizationId())) {
            throw new BusinessException("ORG_NOT_FOUND", "Không tìm thấy tổ chức.");
        }
        if (req.parentId() != null) {
            DepartmentEntity parent =
                    repo.findById(req.parentId())
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    "DEPT_PARENT_NOT_FOUND",
                                                    "Không tìm thấy phòng/ban cấp trên."));
            if (!parent.getOrganizationId().equals(req.organizationId())) {
                throw new BusinessException(
                        "DEPT_PARENT_WRONG_ORG",
                        "Phòng/ban cấp trên phải cùng tổ chức.");
            }
        }
        DepartmentEntity d = new DepartmentEntity();
        d.setId(UUID.randomUUID());
        d.setOrganizationId(req.organizationId());
        d.setCode(req.code());
        d.setName(req.name());
        d.setParentId(req.parentId());
        d.setHeadUserId(req.headUserId());
        d.setActive(true);
        d = repo.save(d);
        return toDto(d);
    }

    @Transactional
    public DepartmentDto update(UUID id, UpdateDepartmentRequest req) {
        DepartmentEntity d =
                repo.findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "DEPT_NOT_FOUND",
                                                "Không tìm thấy phòng/ban."));
        if (req.name() != null) d.setName(req.name());
        if (req.active() != null) d.setActive(req.active());
        if (req.headUserId() != null) d.setHeadUserId(req.headUserId());
        if (req.parentId() != null) {
            if (req.parentId().equals(d.getId())) {
                throw new BusinessException("DEPT_SELF_PARENT", "Không thể chọn chính nó làm cấp trên.");
            }
            // Kiểm tra cycle đơn giản: leo parent chain, nếu gặp d.getId() → cycle.
            UUID cursor = req.parentId();
            int hops = 0;
            while (cursor != null && hops++ < 20) {
                if (cursor.equals(d.getId())) {
                    throw new BusinessException(
                            "DEPT_CYCLE", "Thay đổi này tạo vòng lặp phòng/ban.");
                }
                cursor = repo.findById(cursor).map(DepartmentEntity::getParentId).orElse(null);
            }
            d.setParentId(req.parentId());
        }
        return toDto(d);
    }

    private DepartmentDto toDto(DepartmentEntity d) {
        return new DepartmentDto(
                d.getId(),
                d.getOrganizationId(),
                d.getCode(),
                d.getName(),
                d.getParentId(),
                d.getHeadUserId(),
                d.isActive());
    }
}
