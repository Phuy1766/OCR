package vn.edu.congvan.masterdata.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.auth.repository.OrganizationRepository;
import vn.edu.congvan.common.exception.BusinessException;
import vn.edu.congvan.masterdata.dto.CreateDocumentBookRequest;
import vn.edu.congvan.masterdata.dto.DocumentBookDto;
import vn.edu.congvan.masterdata.dto.UpdateDocumentBookRequest;
import vn.edu.congvan.masterdata.entity.BookType;
import vn.edu.congvan.masterdata.entity.ConfidentialityScope;
import vn.edu.congvan.masterdata.entity.DocumentBookEntity;
import vn.edu.congvan.masterdata.repository.DocumentBookRepository;

@Service
@RequiredArgsConstructor
public class DocumentBookService {

    private final DocumentBookRepository books;
    private final OrganizationRepository organizations;

    @Transactional(readOnly = true)
    public List<DocumentBookDto> list(UUID organizationId, BookType bookType, ConfidentialityScope scope) {
        return books.findActive(organizationId, bookType, scope).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DocumentBookDto getById(UUID id) {
        return toDto(
                books.findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "DOCUMENT_BOOK_NOT_FOUND",
                                                "Không tìm thấy sổ đăng ký.")));
    }

    @Transactional
    public DocumentBookDto create(CreateDocumentBookRequest req) {
        if (!organizations.existsById(req.organizationId())) {
            throw new BusinessException(
                    "ORG_NOT_FOUND", "Không tìm thấy tổ chức.");
        }
        if (books.existsByOrganizationIdAndCode(req.organizationId(), req.code())) {
            throw new BusinessException(
                    "DOCUMENT_BOOK_CODE_TAKEN",
                    "Mã sổ đã tồn tại trong tổ chức.");
        }
        DocumentBookEntity b = new DocumentBookEntity();
        b.setId(UUID.randomUUID());
        b.setOrganizationId(req.organizationId());
        b.setCode(req.code());
        b.setName(req.name());
        b.setBookType(req.bookType());
        b.setConfidentialityScope(req.confidentialityScope());
        b.setPrefix(req.prefix());
        b.setDescription(req.description());
        b.setActive(true);
        b = books.save(b);
        return toDto(b);
    }

    @Transactional
    public DocumentBookDto update(UUID id, UpdateDocumentBookRequest req) {
        DocumentBookEntity b =
                books.findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "DOCUMENT_BOOK_NOT_FOUND",
                                                "Không tìm thấy sổ đăng ký."));
        if (req.name() != null) b.setName(req.name());
        if (req.prefix() != null) b.setPrefix(req.prefix());
        if (req.description() != null) b.setDescription(req.description());
        if (req.active() != null) b.setActive(req.active());
        return toDto(b);
    }

    private DocumentBookDto toDto(DocumentBookEntity b) {
        return new DocumentBookDto(
                b.getId(),
                b.getOrganizationId(),
                b.getCode(),
                b.getName(),
                b.getBookType(),
                b.getConfidentialityScope(),
                b.getPrefix(),
                b.getDescription(),
                b.isActive());
    }
}
