package vn.edu.congvan.masterdata.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.masterdata.dto.ConfidentialityLevelDto;
import vn.edu.congvan.masterdata.entity.ConfidentialityLevelEntity;
import vn.edu.congvan.masterdata.repository.ConfidentialityLevelRepository;

@Service
@RequiredArgsConstructor
public class ConfidentialityLevelService {

    private final ConfidentialityLevelRepository repo;

    @Transactional(readOnly = true)
    public List<ConfidentialityLevelDto> listActive() {
        return repo.findAllActive().stream().map(this::toDto).toList();
    }

    private ConfidentialityLevelDto toDto(ConfidentialityLevelEntity e) {
        return new ConfidentialityLevelDto(
                e.getId(),
                e.getCode(),
                e.getName(),
                e.getLevel(),
                e.getColor(),
                e.getDescription(),
                e.getDisplayOrder(),
                e.requiresSecretBook());
    }
}
