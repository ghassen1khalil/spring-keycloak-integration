package com.example.medicalapi.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.addAll(extractRoleAuthorities(jwt));
        authorities.addAll(extractPermissionAuthorities(jwt));
        return new UsernamePasswordAuthenticationToken(jwt.getSubject(), "n/a", authorities);
    }

    private Collection<? extends GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmMap)) {
            return List.of();
        }
        Object roles = realmMap.get("roles");
        if (!(roles instanceof Collection<?> roleCollection)) {
            return List.of();
        }

        List<SimpleGrantedAuthority> result = new ArrayList<>();
        for (Object roleObject : roleCollection) {
            String role = String.valueOf(roleObject).trim();
            if (role.isBlank()) {
                continue;
            }
            String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            result.add(new SimpleGrantedAuthority(normalizedRole));
            addPermissionsForRole(result, normalizedRole);
        }
        return result;
    }

    private Collection<? extends GrantedAuthority> extractPermissionAuthorities(Jwt jwt) {
        Object permissions = jwt.getClaims().get("permissions");
        if (!(permissions instanceof Collection<?> permissionCollection)) {
            return List.of();
        }

        List<SimpleGrantedAuthority> result = new ArrayList<>();
        for (Object permissionObject : permissionCollection) {
            String permission = String.valueOf(permissionObject).trim();
            if (!permission.isBlank()) {
                result.add(new SimpleGrantedAuthority(permission));
            }
        }
        return result;
    }

    private void addPermissionsForRole(List<SimpleGrantedAuthority> result, String role) {
        switch (role) {
            case "ROLE_MEDICAL_ADMIN" -> {
                result.add(new SimpleGrantedAuthority("permission:medical:create"));
                result.add(new SimpleGrantedAuthority("permission:medical:read"));
                result.add(new SimpleGrantedAuthority("permission:medical:update"));
                result.add(new SimpleGrantedAuthority("permission:medical:delete"));
            }
            case "ROLE_MEDICAL_EDITOR" -> {
                result.add(new SimpleGrantedAuthority("permission:medical:create"));
                result.add(new SimpleGrantedAuthority("permission:medical:read"));
                result.add(new SimpleGrantedAuthority("permission:medical:update"));
            }
            case "ROLE_MEDICAL_READER" -> result.add(new SimpleGrantedAuthority("permission:medical:read"));
            default -> {
            }
        }
    }
}
