package vn.edu.congvan.masterdata.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.congvan.masterdata.dto.PriorityLevelDto;
import vn.edu.congvan.masterdata.entity.PriorityLevelEntity;
import vn.edu.congvan.masterdata.repository.PriorityLevelRepository;

@Service
@RequiredArgsConstructor
public class PriorityLevelService {

    private final PriorityLevelRepository repo;

    @Transactional(readOnly = true)
    public List<PriorityLevelDto> listActive() {
        return repo.findAllActive().stream().map(this::toDto).toList();
    }

    private PriorityLevelDto toDto(PriorityLevelEntity e) {
        return new PriorityLevelDto(
                e.getId(),
                e.getCode(),
                e.getName(),
                e.getLevel(),
                e.getColor(),
                e.getSlaHours(),
                e.getDescription(),
                e.getDisplayOrder(),
                e.isUrgent());
    }
}
