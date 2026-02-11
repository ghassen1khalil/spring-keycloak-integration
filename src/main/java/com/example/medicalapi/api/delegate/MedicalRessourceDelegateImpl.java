package com.example.medicalapi.api.delegate;

import com.example.medicalapi.api.generated.MedicalRessourceApiDelegate;
import com.example.medicalapi.api.generated.model.CreateMedicalRessourceRequest;
import com.example.medicalapi.api.generated.model.MedicalRessourceDto;
import com.example.medicalapi.api.generated.model.UpdateMedicalRessourceRequest;
import com.example.medicalapi.service.MedicalRessourceService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class MedicalRessourceDelegateImpl implements MedicalRessourceApiDelegate {

    private final MedicalRessourceService service;

    public MedicalRessourceDelegateImpl(MedicalRessourceService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<MedicalRessourceDto> createMedicalRessource(CreateMedicalRessourceRequest createMedicalRessourceRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(createMedicalRessourceRequest));
    }

    @Override
    public ResponseEntity<Void> deleteMedicalRessource(UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MedicalRessourceDto> getMedicalRessourceById(UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Override
    public ResponseEntity<List<MedicalRessourceDto>> listMedicalRessources() {
        return ResponseEntity.ok(service.list());
    }

    @Override
    public ResponseEntity<MedicalRessourceDto> patchMedicalRessource(UUID id, UpdateMedicalRessourceRequest updateMedicalRessourceRequest) {
        return ResponseEntity.ok(service.patch(id, updateMedicalRessourceRequest));
    }

    @Override
    public ResponseEntity<MedicalRessourceDto> replaceMedicalRessource(UUID id, UpdateMedicalRessourceRequest updateMedicalRessourceRequest) {
        return ResponseEntity.ok(service.replace(id, updateMedicalRessourceRequest));
    }
}
