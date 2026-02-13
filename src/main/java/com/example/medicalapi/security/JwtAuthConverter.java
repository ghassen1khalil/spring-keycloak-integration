package com.example.medicalapi.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthConverter.class);

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        logger.info("=== DÉBUT DU TRAITEMENT JWT ===");
        logger.info("Subject (utilisateur): {}", jwt.getSubject());
        logger.info("Claims disponibles: {}", jwt.getClaims().keySet());

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        Collection<? extends GrantedAuthority> roleAuthorities = extractRoleAuthorities(jwt);
        authorities.addAll(roleAuthorities);
        logger.info("Autorités (rôles) extraites: {}", roleAuthorities.size());

        Collection<? extends GrantedAuthority> permissionAuthorities = extractPermissionAuthorities(jwt);
        authorities.addAll(permissionAuthorities);
        logger.info("Autorités (permissions) extraites: {}", permissionAuthorities.size());

        logger.info("Nombre total d'autorités: {}", authorities.size());
        logger.info("Autorités finales: {}", authorities.stream().map(GrantedAuthority::getAuthority).toList());
        logger.info("=== FIN DU TRAITEMENT JWT ===");

        return new UsernamePasswordAuthenticationToken(jwt.getSubject(), "n/a", authorities);
    }

    private Collection<? extends GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
        logger.debug("--- Extraction des rôles (realm_access) ---");
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmMap)) {
            logger.warn("Aucun claim 'realm_access' trouvé ou format invalide");
            return List.of();
        }
        logger.debug("Claim 'realm_access' trouvé: {}", realmMap.keySet());

        Object roles = realmMap.get("roles");
        if (!(roles instanceof Collection<?> roleCollection)) {
            logger.warn("Aucun rôle trouvé dans realm_access ou format invalide");
            return List.of();
        }

        logger.info("Rôles bruts trouvés: {}", roleCollection);
        List<SimpleGrantedAuthority> result = new ArrayList<>();

        for (Object roleObject : roleCollection) {
            String role = String.valueOf(roleObject).trim();
            if (role.isBlank()) {
                logger.debug("Rôle vide ignoré");
                continue;
            }
            String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            logger.info("Rôle normalisé: {} -> {}", role, normalizedRole);
            result.add(new SimpleGrantedAuthority(normalizedRole));
            addPermissionsForRole(result, normalizedRole);
        }
        logger.info("Total des autorités basées sur les rôles: {}", result.size());
        return result;
    }

    private Collection<? extends GrantedAuthority> extractPermissionAuthorities(Jwt jwt) {
        logger.debug("--- Extraction des permissions (claims directs) ---");
        Object permissions = jwt.getClaims().get("permissions");
        if (!(permissions instanceof Collection<?> permissionCollection)) {
            logger.warn("Aucun claim 'permissions' trouvé ou format invalide");
            return List.of();
        }

        logger.info("Permissions brutes trouvées: {}", permissionCollection);
        List<SimpleGrantedAuthority> result = new ArrayList<>();
        for (Object permissionObject : permissionCollection) {
            String permission = String.valueOf(permissionObject).trim();
            if (!permission.isBlank()) {
                logger.info("Permission ajoutée: {}", permission);
                result.add(new SimpleGrantedAuthority(permission));
            }
        }
        logger.info("Total des autorités basées sur les permissions: {}", result.size());
        return result;
    }

    private void addPermissionsForRole(List<SimpleGrantedAuthority> result, String role) {
        logger.debug("--- Attribution des permissions pour le rôle: {} ---", role);
        switch (role) {
            case "ROLE_MEDICAL_ADMIN" -> {
                logger.debug("Rôle MEDICAL_ADMIN détecté - ajout de toutes les permissions");
                result.add(new SimpleGrantedAuthority("permission:medical:create"));
                result.add(new SimpleGrantedAuthority("permission:medical:read"));
                result.add(new SimpleGrantedAuthority("permission:medical:update"));
                result.add(new SimpleGrantedAuthority("permission:medical:delete"));
                logger.debug("4 permissions ajoutées pour ROLE_MEDICAL_ADMIN");
            }
            case "ROLE_MEDICAL_EDITOR" -> {
                logger.debug("Rôle MEDICAL_EDITOR détecté - ajout des permissions create/read/update");
                result.add(new SimpleGrantedAuthority("permission:medical:create"));
                result.add(new SimpleGrantedAuthority("permission:medical:read"));
                result.add(new SimpleGrantedAuthority("permission:medical:update"));
                logger.debug("3 permissions ajoutées pour ROLE_MEDICAL_EDITOR");
            }
            case "ROLE_MEDICAL_READER" -> {
                logger.debug("Rôle MEDICAL_READER détecté - ajout permission read seulement");
                result.add(new SimpleGrantedAuthority("permission:medical:read"));
                logger.debug("1 permission ajoutée pour ROLE_MEDICAL_READER");
            }
            default -> logger.debug("Rôle non reconnu ou pas de permissions associées: {}", role);
        }
    }
}
