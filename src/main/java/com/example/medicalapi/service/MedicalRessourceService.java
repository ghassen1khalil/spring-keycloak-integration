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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalRessourceService {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRessourceService.class);
    private final MedicalRessourceRepository repository;
    private final MedicalRessourceMapper mapper;

    public MedicalRessourceService(MedicalRessourceRepository repository, MedicalRessourceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public MedicalRessourceDto create(CreateMedicalRessourceRequest request) {
        logger.info("[SERVICE] Création d'une ressource médicale");
        logger.debug("[SERVICE] Paramètres - Code: {}, Label: {}, Status: {}",
            request.getCode(), request.getLabel(), request.getStatus());

        MedicalRessourceEntity entity = mapper.toEntity(request);
        logger.debug("[SERVICE] Entité mappée - Prêt pour la sauvegarde");

        MedicalRessourceEntity saved = repository.save(entity);
        logger.info("[SERVICE] Ressource médicale sauvegardée avec succès - ID: {}", saved.getId());

        MedicalRessourceDto dto = mapper.toDto(saved);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<MedicalRessourceDto> list() {
        logger.info("[SERVICE] Récupération de la liste des ressources médicales");

        List<MedicalRessourceEntity> entities = repository.findAll();
        logger.info("[SERVICE] Nombre de ressources trouvées: {}", entities.size());

        List<MedicalRessourceDto> dtos = entities.stream().map(mapper::toDto).toList();
        logger.debug("[SERVICE] Ressources mappées en DTOs");

        return dtos;
    }

    @Transactional(readOnly = true)
    public MedicalRessourceDto getById(UUID id) {
        logger.info("[SERVICE] Récupération de la ressource médicale - ID: {}", id);

        MedicalRessourceEntity entity = findOrThrow(id);
        logger.debug("[SERVICE] Ressource trouvée - Code: {}, Label: {}", entity.getCode(), entity.getLabel());

        MedicalRessourceDto dto = mapper.toDto(entity);
        return dto;
    }

    @Transactional
    public MedicalRessourceDto replace(UUID id, UpdateMedicalRessourceRequest request) {
        logger.info("[SERVICE] Remplacement complet de la ressource médicale - ID: {}", id);
        logger.debug("[SERVICE] Nouveaux paramètres - Code: {}, Label: {}, Status: {}",
            request.getCode(), request.getLabel(), request.getStatus());

        MedicalRessourceEntity entity = findOrThrow(id);
        logger.debug("[SERVICE] Ressource actuelle trouvée - Code actuel: {}", entity.getCode());

        if (request.getCode() == null || request.getLabel() == null || request.getStatus() == null) {
            logger.error("[SERVICE] Remplacement échoué - Champs requis manquants (code, label, status)");
            throw new IllegalArgumentException("code, label and status are required for full update");
        }

        entity.setCode(request.getCode());
        entity.setLabel(request.getLabel());
        entity.setDescription(request.getDescription());
        entity.setStatus(com.example.medicalapi.domain.entity.MedicalStatus.valueOf(request.getStatus().getValue()));
        logger.debug("[SERVICE] Ressource mise à jour en mémoire");

        MedicalRessourceEntity saved = repository.save(entity);
        logger.info("[SERVICE] Ressource remplacée avec succès - ID: {}", saved.getId());

        MedicalRessourceDto dto = mapper.toDto(saved);
        return dto;
    }

    @Transactional
    public MedicalRessourceDto patch(UUID id, UpdateMedicalRessourceRequest request) {
        logger.info("[SERVICE] Mise à jour partielle de la ressource médicale - ID: {}", id);
        logger.debug("[SERVICE] Données de patch: Code={}, Label={}, Status={}, Description={}",
            request.getCode(), request.getLabel(), request.getStatus(), request.getDescription());

        MedicalRessourceEntity entity = findOrThrow(id);
        logger.debug("[SERVICE] Ressource trouvée - Code actuel: {}", entity.getCode());

        mapper.applyPatch(request, entity);
        logger.debug("[SERVICE] Patch appliqué à la ressource");

        MedicalRessourceEntity saved = repository.save(entity);
        logger.info("[SERVICE] Ressource patchée avec succès - ID: {}", saved.getId());

        MedicalRessourceDto dto = mapper.toDto(saved);
        return dto;
    }

    @Transactional
    public void delete(UUID id) {
        logger.info("[SERVICE] Suppression de la ressource médicale - ID: {}", id);

        MedicalRessourceEntity entity = findOrThrow(id);
        logger.debug("[SERVICE] Ressource trouvée - Code: {}, Label: {}", entity.getCode(), entity.getLabel());

        repository.delete(entity);
        logger.info("[SERVICE] Ressource supprimée avec succès - ID: {}", id);
    }

    private MedicalRessourceEntity findOrThrow(UUID id) {
        logger.debug("[SERVICE] Recherche de la ressource médicale - ID: {}", id);

        var optional = repository.findById(id);
        optional.ifPresent(entity ->
            logger.debug("[SERVICE] Ressource trouvée - Code: {}, Status: {}",
                entity.getCode(), entity.getStatus())
        );

        return optional.orElseThrow(() -> {
            logger.error("[SERVICE] Ressource non trouvée - ID: {}", id);
            return new ResourceNotFoundException("Medical resource not found: " + id);
        });
    }
}
