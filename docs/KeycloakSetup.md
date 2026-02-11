# Keycloak Setup — Medical API

## 1) Création du Realm `Medical_API`
1. Ouvrir Keycloak Admin Console (`http://localhost:8081/admin`).
2. Se connecter avec `admin/admin`.
3. Cliquer sur **Create realm**.
4. Nommer le realm : `Medical_API`.

## 2) Création du Client `medical-api`
1. Aller dans **Clients** > **Create client**.
2. Client type: **OpenID Connect**.
3. Client ID: `medical-api`.
4. Activer:
   - **Client authentication** (confidential)
   - **Service accounts roles**
5. Enregistrer.

## 3) Création des Realm Roles
Dans **Realm roles**, créer:
- `ROLE_MEDICAL_ADMIN`
- `ROLE_MEDICAL_EDITOR`
- `ROLE_MEDICAL_READER`

## 4) Activation Authorization Services
1. Ouvrir client `medical-api`.
2. Onglet **Authorization**.
3. Activer **Authorization Enabled**.

## 5) Création des Resources
Dans Authorization > Resources :
- Name: `medical-resource`
- URI: `/api/medical-ressources/*`

## 6) Création des Scopes
Dans Authorization > Scopes, créer :
- `create`
- `read`
- `update`
- `delete`

## 7) Création des Policies (Role-based)
Dans Authorization > Policies, créer 3 policies de type **Role** :
1. `policy-admin`
   - Role: `ROLE_MEDICAL_ADMIN`
2. `policy-editor`
   - Role: `ROLE_MEDICAL_EDITOR`
3. `policy-reader`
   - Role: `ROLE_MEDICAL_READER`

## 8) Création des Permissions (Scope-based)
Dans Authorization > Permissions > **Create scope-based permission**:
1. `permission-medical-create`
   - Resource: `medical-resource`
   - Scope: `create`
   - Policies: `policy-admin`, `policy-editor`
2. `permission-medical-read`
   - Scope: `read`
   - Policies: `policy-admin`, `policy-editor`, `policy-reader`
3. `permission-medical-update`
   - Scope: `update`
   - Policies: `policy-admin`, `policy-editor`
4. `permission-medical-delete`
   - Scope: `delete`
   - Policies: `policy-admin`

## 9) Mapper les roles/permissions vers JWT
### Realm roles dans token
1. Client scopes > `roles` (default) doit être attaché.
2. Vérifier que claim `realm_access.roles` est présent dans access token.

### Permissions custom
Ajouter un protocol mapper (client `medical-api`) :
- Mapper type: **User Realm Role** pour les roles
- Mapper additionnel (ou script mapper selon politique): claim `permissions`
  - Exemple de valeurs :
    - `permission:medical:create`
    - `permission:medical:read`
    - `permission:medical:update`
    - `permission:medical:delete`

## 10) Tests curl EXACTS

### 10.1 Token admin (client credentials)
```bash
curl -s -X POST "http://localhost:8081/realms/Medical_API/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=medical-api" \
  -d "client_secret=<CLIENT_SECRET>"
```

### 10.2 Extraire access_token
```bash
export ACCESS_TOKEN=$(curl -s -X POST "http://localhost:8081/realms/Medical_API/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=medical-api" \
  -d "client_secret=<CLIENT_SECRET>" | jq -r '.access_token')
```

### 10.3 Appel POST sécurisé
```bash
curl -i -X POST "http://localhost:8080/api/medical-ressources" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "MED-001",
    "label": "Stethoscope",
    "description": "Diagnostic resource",
    "status": "ACTIVE"
  }'
```

### 10.4 Appel GET sécurisé
```bash
curl -i -X GET "http://localhost:8080/api/medical-ressources" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```
