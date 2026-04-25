package vn.edu.congvan.masterdata.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.masterdata.dto.DocumentTypeDto;
import vn.edu.congvan.masterdata.entity.DocumentTypeEntity;
import vn.edu.congvan.masterdata.repository.DocumentTypeRepository;

/** Read-only service — 29 loại VB là seed system, không cho CRUD qua API. */
@Service
@RequiredArgsConstructor
public class DocumentTypeService {

    private final DocumentTypeRepository repo;

    @Transactional(readOnly = true)
    public List<DocumentTypeDto> listActive() {
        return repo.findAllActive().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DocumentTypeDto getByCode(String code) {
        DocumentTypeEntity e =
                repo.findByCode(code)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "DOCUMENT_TYPE_NOT_FOUND",
                                                "Không tìm thấy loại VB: " + code));
        return toDto(e);
    }

    private DocumentTypeDto toDto(DocumentTypeEntity e) {
        return new DocumentTypeDto(
                e.getId(),
                e.getCode(),
                e.getAbbreviation(),
                e.getName(),
                e.getDescription(),
                e.getDisplayOrder(),
                e.isSystem(),
                e.isActive());
    }
}
