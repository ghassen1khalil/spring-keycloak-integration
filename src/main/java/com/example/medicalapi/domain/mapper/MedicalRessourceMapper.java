package com.example.medicalapi.domain.mapper;

import com.example.medicalapi.api.generated.model.CreateMedicalRessourceRequest;
import com.example.medicalapi.api.generated.model.MedicalRessourceDto;
import com.example.medicalapi.api.generated.model.MedicalStatus;
import com.example.medicalapi.api.generated.model.UpdateMedicalRessourceRequest;
import com.example.medicalapi.domain.entity.MedicalRessourceEntity;
import org.springframework.stereotype.Component;

@Component
public class MedicalRessourceMapper {

    public MedicalRessourceEntity toEntity(CreateMedicalRessourceRequest request) {
        MedicalRessourceEntity entity = new MedicalRessourceEntity();
        entity.setCode(request.getCode());
        entity.setLabel(request.getLabel());
        entity.setDescription(request.getDescription());
        entity.setStatus(com.example.medicalapi.domain.entity.MedicalStatus.valueOf(request.getStatus().getValue()));
        return entity;
    }

    public void applyPatch(UpdateMedicalRessourceRequest request, MedicalRessourceEntity entity) {
        if (request.getCode() != null) {
            entity.setCode(request.getCode());
        }
        if (request.getLabel() != null) {
            entity.setLabel(request.getLabel());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            entity.setStatus(com.example.medicalapi.domain.entity.MedicalStatus.valueOf(request.getStatus().getValue()));
        }
    }

    public MedicalRessourceDto toDto(MedicalRessourceEntity entity) {
        MedicalRessourceDto dto = new MedicalRessourceDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setLabel(entity.getLabel());
        dto.setDescription(entity.getDescription());
        dto.setStatus(MedicalStatus.fromValue(entity.getStatus().name()));
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
