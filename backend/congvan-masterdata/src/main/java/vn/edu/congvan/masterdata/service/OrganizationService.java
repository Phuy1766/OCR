package vn.edu.congvan.masterdata.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.entity.OrganizationEntity;
import vn.edu.congvan.auth.repository.OrganizationRepository;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.masterdata.dto.CreateOrganizationRequest;
import vn.edu.congvan.masterdata.dto.OrganizationDto;
import vn.edu.congvan.masterdata.dto.UpdateOrganizationRequest;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository repo;

    @Transactional(readOnly = true)
    public List<OrganizationDto> listAll() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrganizationDto getById(UUID id) {
        return toDto(
                repo.findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "ORG_NOT_FOUND", "Không tìm thấy tổ chức.")));
    }

    @Transactional
    public OrganizationDto create(CreateOrganizationRequest req) {
        if (repo.findByCode(req.code()).isPresent()) {
            throw new BusinessException("ORG_CODE_TAKEN", "Mã cơ quan đã tồn tại.");
        }
        if (req.parentId() != null && !repo.existsById(req.parentId())) {
            throw new BusinessException("ORG_PARENT_NOT_FOUND", "Không tìm thấy cơ quan cấp trên.");
        }
        OrganizationEntity o = new OrganizationEntity();
        o.setId(UUID.randomUUID());
        o.setCode(req.code());
        o.setName(req.name());
        o.setFullName(req.fullName());
        o.setTaxCode(req.taxCode());
        o.setAddress(req.address());
        o.setPhone(req.phone());
        o.setEmail(req.email());
        o.setParentId(req.parentId());
        o.setActive(true);
        o = repo.save(o);
        return toDto(o);
    }

    @Transactional
    public OrganizationDto update(UUID id, UpdateOrganizationRequest req) {
        OrganizationEntity o =
                repo.findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "ORG_NOT_FOUND", "Không tìm thấy tổ chức."));
        if (req.name() != null) o.setName(req.name());
        if (req.fullName() != null) o.setFullName(req.fullName());
        if (req.taxCode() != null) o.setTaxCode(req.taxCode());
        if (req.address() != null) o.setAddress(req.address());
        if (req.phone() != null) o.setPhone(req.phone());
        if (req.email() != null) o.setEmail(req.email());
        if (req.active() != null) o.setActive(req.active());
        return toDto(o);
    }

    private OrganizationDto toDto(OrganizationEntity o) {
        return new OrganizationDto(
                o.getId(),
                o.getCode(),
                o.getName(),
                o.getFullName(),
                o.getTaxCode(),
                o.getAddress(),
                o.getPhone(),
                o.getEmail(),
                o.getParentId(),
                o.isActive());
    }
}
