package com.example.medicalapi.api.delegate;

import com.example.medicalapi.api.generated.MedicalRessourceApiDelegate;
import com.example.medicalapi.api.generated.model.CreateMedicalRessourceRequest;
import com.example.medicalapi.api.generated.model.MedicalRessourceDto;
import com.example.medicalapi.api.generated.model.UpdateMedicalRessourceRequest;
import com.example.medicalapi.service.MedicalRessourceService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MedicalRessourceDelegateImpl implements MedicalRessourceApiDelegate {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRessourceDelegateImpl.class);
    private final MedicalRessourceService service;

    public MedicalRessourceDelegateImpl(MedicalRessourceService service) {
        this.service = service;
    }

    private void logSecurityContext(String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            logger.info("[ACCÈS - {}] Utilisateur: {}, Authentifié: {}, Autorités: {}",
                action,
                authentication.getName(),
                authentication.isAuthenticated(),
                authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        } else {
            logger.warn("[ACCÈS - {}] Aucun contexte d'authentification disponible", action);
        }
    }

    @Override
    public ResponseEntity<MedicalRessourceDto> createMedicalRessource(CreateMedicalRessourceRequest createMedicalRessourceRequest) {
        logSecurityContext("CREATE_MEDICAL_RESSOURCE");
        logger.info("Création d'une ressource médicale - Code: {}, Label: {}, Status: {}",
            createMedicalRessourceRequest.getCode(),
            createMedicalRessourceRequest.getLabel(),
            createMedicalRessourceRequest.getStatus());

        MedicalRessourceDto result = service.create(createMedicalRessourceRequest);
        logger.info("Ressource médicale créée avec succès - ID: {}", result.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Override
    public ResponseEntity<Void> deleteMedicalRessource(UUID id) {
        logSecurityContext("DELETE_MEDICAL_RESSOURCE");
        logger.info("Suppression de la ressource médicale - ID: {}", id);

        service.delete(id);
        logger.info("Ressource médicale supprimée avec succès - ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MedicalRessourceDto> getMedicalRessourceById(UUID id) {
        logSecurityContext("GET_MEDICAL_RESSOURCE_BY_ID");
        logger.info("Récupération de la ressource médicale - ID: {}", id);

        MedicalRessourceDto result = service.getById(id);
        logger.info("Ressource médicale trouvée - ID: {}, Code: {}, Label: {}",
            result.getId(), result.getCode(), result.getLabel());
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<List<MedicalRessourceDto>> listMedicalRessources() {
        logSecurityContext("LIST_MEDICAL_RESSOURCES");
        logger.info("Récupération de la liste des ressources médicales");

        List<MedicalRessourceDto> results = service.list();
        logger.info("Liste des ressources médicales récupérée - Nombre total: {}", results.size());
        return ResponseEntity.ok(results);
    }

    @Override
    public ResponseEntity<MedicalRessourceDto> patchMedicalRessource(UUID id, UpdateMedicalRessourceRequest updateMedicalRessourceRequest) {
        logSecurityContext("PATCH_MEDICAL_RESSOURCE");
        logger.info("Mise à jour partielle de la ressource médicale - ID: {}", id);
        logger.debug("Données de mise à jour: {}", updateMedicalRessourceRequest);

        MedicalRessourceDto result = service.patch(id, updateMedicalRessourceRequest);
        logger.info("Ressource médicale mise à jour avec succès (PATCH) - ID: {}", result.getId());
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<MedicalRessourceDto> replaceMedicalRessource(UUID id, UpdateMedicalRessourceRequest updateMedicalRessourceRequest) {
        logSecurityContext("REPLACE_MEDICAL_RESSOURCE");
        logger.info("Remplacement complet de la ressource médicale - ID: {}", id);
        logger.debug("Données de remplacement: {}", updateMedicalRessourceRequest);

        MedicalRessourceDto result = service.replace(id, updateMedicalRessourceRequest);
        logger.info("Ressource médicale remplacée avec succès (PUT) - ID: {}", result.getId());
        return ResponseEntity.ok(result);
    }
}
