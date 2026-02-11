package com.example.medicalapi.domain.repository;

import com.example.medicalapi.domain.entity.MedicalRessourceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRessourceRepository extends JpaRepository<MedicalRessourceEntity, UUID> {
}
