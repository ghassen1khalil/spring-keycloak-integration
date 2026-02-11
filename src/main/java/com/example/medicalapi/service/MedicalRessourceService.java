package com.example.medicalapi.service;

import com.example.medicalapi.api.generated.model.CreateMedicalRessourceRequest;
import com.example.medicalapi.api.generated.model.MedicalRessourceDto;
import com.example.medicalapi.api.generated.model.UpdateMedicalRessourceRequest;
import com.example.medicalapi.domain.entity.MedicalRessourceEntity;
import com.example.medicalapi.domain.mapper.MedicalRessourceMapper;
import com.example.medicalapi.domain.repository.MedicalRessourceRepository;
import com.example.medicalapi.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalRessourceService {

    private final MedicalRessourceRepository repository;
    private final MedicalRessourceMapper mapper;

    public MedicalRessourceService(MedicalRessourceRepository repository, MedicalRessourceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public MedicalRessourceDto create(CreateMedicalRessourceRequest request) {
        MedicalRessourceEntity entity = mapper.toEntity(request);
        MedicalRessourceEntity saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MedicalRessourceDto> list() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MedicalRessourceDto getById(UUID id) {
        return mapper.toDto(findOrThrow(id));
    }

    @Transactional
    public MedicalRessourceDto replace(UUID id, UpdateMedicalRessourceRequest request) {
        MedicalRessourceEntity entity = findOrThrow(id);
        if (request.getCode() == null || request.getLabel() == null || request.getStatus() == null) {
            throw new IllegalArgumentException("code, label and status are required for full update");
        }
        entity.setCode(request.getCode());
        entity.setLabel(request.getLabel());
        entity.setDescription(request.getDescription());
        entity.setStatus(com.example.medicalapi.domain.entity.MedicalStatus.valueOf(request.getStatus().getValue()));
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public MedicalRessourceDto patch(UUID id, UpdateMedicalRessourceRequest request) {
        MedicalRessourceEntity entity = findOrThrow(id);
        mapper.applyPatch(request, entity);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        MedicalRessourceEntity entity = findOrThrow(id);
        repository.delete(entity);
    }

    private MedicalRessourceEntity findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical resource not found: " + id));
    }
}
