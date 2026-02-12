# Guide ultra-détaillé — Keycloak 26 pour sécuriser `medical-api`

Ce guide configure **Keycloak 26** de zéro pour sécuriser un microservice Spring Boot avec scopes CRUD sur `MedicalRessource`.

---

## 1) Prérequis (A)

### Outils nécessaires
- Docker 24+
- curl
- jq (recommandé)
- Une API Spring Boot qui attend un issuer Keycloak :
  - `issuer-uri: ${KEYCLOAK_URL}/realms/Medical_API`

### Bonnes pratiques
- Utiliser un environnement local dédié (pas la prod).
- Conserver les secrets dans un `.env` local non versionné.
- Vérifier systématiquement le contenu du JWT (`realm_access.roles`, éventuellement `permissions`).

### Démarrer Keycloak 26 (Docker)
```bash
docker run -d --name keycloak-26 \
  -p 8081:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.0.7 \
  start-dev
```

### Vérifications initiales
```bash
# Santé globale
curl -s http://localhost:8081/health/ready

# Métadonnées OIDC du realm master
curl -s http://localhost:8081/realms/master/.well-known/openid-configuration | jq .issuer
```

### URL et identifiants admin
- Console Admin : `http://localhost:8081/admin`
- Compte initial : `admin / admin`

---

## 2) Créer le Realm `Medical_API` (B)

### Étapes UI
1. Ouvrir `http://localhost:8081/admin`.
2. Se connecter en `admin`.
3. Menu en haut à gauche (sélecteur de realm) → **Create realm**.
4. Realm name : `Medical_API`.
5. Cliquer **Create**.

### Paramètres de base à vérifier
- Realm enabled : ON
- Login settings : laisser par défaut au début
- Tokens : garder TTL standards puis ajuster après validation

### Équivalent Admin REST API

#### 2.1 Obtenir un token admin
```bash
export KC_URL="http://localhost:8081"
export KC_ADMIN="admin"
export KC_ADMIN_PASSWORD="admin"

export ADMIN_TOKEN=$(curl -s -X POST "$KC_URL/realms/master/protocol/openid-connect/token" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=$KC_ADMIN" \
  -d "password=$KC_ADMIN_PASSWORD" | jq -r '.access_token')
```

#### 2.2 Créer le realm
```bash
curl -i -X POST "$KC_URL/admin/realms" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "realm": "Medical_API",
    "enabled": true,
    "displayName": "Medical API Realm"
  }'
```

---

## 3) Créer le client `medical-api` (C)

### Étapes UI
1. Realm `Medical_API` → **Clients** → **Create client**.
2. Client type : `OpenID Connect`.
3. Client ID : `medical-api`.
4. Next puis régler :
   - Client authentication : **ON** (confidential)
   - Authorization : **ON**
   - Standard flow : **OFF**
   - Direct access grants : **ON** (utile pour tests password grant)
   - Service accounts roles : **ON**
5. Save.

### Redirect URIs / Web Origins
Pour une API backend pure :
- Valid redirect URIs : vide ou `http://localhost:*/*` pour tests strictement locaux
- Web origins : `+` ou restreint à vos frontends

### Récupérer le secret client
- Clients → `medical-api` → onglet **Credentials** → copier `Client secret`.

### Équivalent REST

#### 3.1 Créer le client
```bash
curl -i -X POST "$KC_URL/admin/realms/Medical_API/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "clientId": "medical-api",
    "name": "medical-api",
    "enabled": true,
    "protocol": "openid-connect",
    "publicClient": false,
    "serviceAccountsEnabled": true,
    "standardFlowEnabled": false,
    "directAccessGrantsEnabled": true,
    "authorizationServicesEnabled": true,
    "redirectUris": ["http://localhost:*/*"],
    "webOrigins": ["+"]
  }'
```

#### 3.2 Récupérer l'UUID interne du client
```bash
export MEDICAL_CLIENT_UUID=$(curl -s "$KC_URL/admin/realms/Medical_API/clients?clientId=medical-api" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')
```

#### 3.3 Récupérer le secret
```bash
export MEDICAL_CLIENT_SECRET=$(curl -s "$KC_URL/admin/realms/Medical_API/clients/$MEDICAL_CLIENT_UUID/client-secret" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.value')

echo "$MEDICAL_CLIENT_SECRET"
```

---

## 4) Créer les rôles (D)

## Rôles de realm requis
- `ROLE_MEDICAL_ADMIN`
- `ROLE_MEDICAL_EDITOR`
- `ROLE_MEDICAL_READER`

### Étapes UI
1. Realm `Medical_API` → **Realm roles**.
2. **Create role** pour chaque rôle ci-dessus.

### REST API
```bash
for role in ROLE_MEDICAL_ADMIN ROLE_MEDICAL_EDITOR ROLE_MEDICAL_READER; do
  curl -i -X POST "$KC_URL/admin/realms/Medical_API/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"$role\",\"description\":\"$role for medical-api\"}"
done
```

### (Optionnel) rôles client
Si vous préférez des rôles portés par le client `medical-api` :
```bash
for role in ROLE_MEDICAL_ADMIN ROLE_MEDICAL_EDITOR ROLE_MEDICAL_READER; do
  curl -i -X POST "$KC_URL/admin/realms/Medical_API/clients/$MEDICAL_CLIENT_UUID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"$role\"}"
done
```

---

## 5) Authorization Services + Scopes/Ressources (E)

### Étapes UI
1. Clients → `medical-api` → onglet **Authorization**.
2. Vérifier que le Resource Server est actif.
3. Aller dans **Scopes** et créer :
   - `create`
   - `read`
   - `update`
   - `delete`
4. Aller dans **Resources** → Create :
   - Name : `medical-resource`
   - Type : `MedicalRessource`
   - URIs : `/api/medical-ressources/*`
   - Scopes attachés : `create`, `read`, `update`, `delete`

### REST API — Scopes
```bash
for scope in create read update delete; do
  curl -i -X POST "$KC_URL/admin/realms/Medical_API/clients/$MEDICAL_CLIENT_UUID/authz/resource-server/scope" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"name\":\"$scope\",\"displayName\":\"$scope\"}"
done
```

### REST API — Resource
```bash
curl -i -X POST "$KC_URL/admin/realms/Medical_API/clients/$MEDICAL_CLIENT_UUID/authz/resource-server/resource" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "medical-resource",
    "type": "MedicalRessource",
    "uris": ["/api/medical-ressources/*"],
    "scopes": [
      {"name":"create"},
      {"name":"read"},
      {"name":"update"},
      {"name":"delete"}
    ]
  }'
```

---

## 6) Créer les policies role-based (F)

### Matrice RBAC cible
- ADMIN : create/read/update/delete
- EDITOR : create/read/update
- READER : read

### Étapes UI
1. Clients → `medical-api` → Authorization → **Policies**.
2. Créer 3 policies de type **Role** :
   - `Admin Role Policy` → `ROLE_MEDICAL_ADMIN`
   - `Editor Role Policy` → `ROLE_MEDICAL_EDITOR`
   - `Reader Role Policy` → `ROLE_MEDICAL_READER`

### REST API
```bash
curl -i -X POST "$KC_URL/admin/realms/Medical_API/clients/$MEDICAL_CLIENT_UUID/authz/resource-server/policy/role" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Admin Role Policy",
    "type": "role",
    "logic": "POSITIVE",
    "decisionStrategy": "UNANIMOUS",
    "roles": [{"id":"ROLE_MEDICAL_ADMIN","required":true}]
  }'

curl -i -X POST "$KC_URL/admin/realms/Medical_API/clients/$MEDICAL_CLIENT_UUID/authz/resource-server/policy/role" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Editor Role Policy",
    "type": "role",
    "logic": "POSITIVE",
    "decisionStrategy": "UNANIMOUS",
    "roles": [{"id":"ROLE_MEDICAL_EDITOR","required":true}]
  }'

curl -i -X POST "$KC_URL/admin/realms/Medical_API/clients/$MEDICAL_CLIENT_UUID/authz/resource-server/policy/role" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Reader Role Policy",
    "type": "role",
    "logic": "POSITIVE",
    "decisionStrategy": "UNANIMOUS",
    "roles": [{"id":"ROLE_MEDICAL_READER","required":true}]
  }'
```

> Remarque : selon version et config, l’API policy peut attendre l’ID interne du rôle plutôt que son nom. En cas d’erreur 400, créez d’abord via UI puis faites un GET pour inspecter la structure exacte.

---

## 7) Créer les permissions scope-based (G)

### Étapes UI
1. Authorization → **Permissions** → **Create permission** → **Scope-based**.
2. Créer les permissions suivantes :
   - `Medical Create Permission` (scope `create`) → policies `Admin Role Policy`, `Editor Role Policy`
   - `Medical Read Permission` (scope `read`) → policies `Admin Role Policy`, `Editor Role Policy`, `Reader Role Policy`
   - `Medical Update Permission` (scope `update`) → policies `Admin Role Policy`, `Editor Role Policy`
   - `Medical Delete Permission` (scope `delete`) → policy `Admin Role Policy`

### REST API (payload exemples)
```bash
curl -i -X POST "$KC_URL/admin/realms/Medical_API/clients/$MEDICAL_CLIENT_UUID/authz/resource-server/permission/scope" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Medical Create Permission",
    "type":"scope",
    "resources":["medical-resource"],
    "scopes":["create"],
    "policies":["Admin Role Policy","Editor Role Policy"],
    "decisionStrategy":"UNANIMOUS"
  }'
```

Répéter pour `read`, `update`, `delete` en adaptant `scopes` et `policies`.

---

## 8) Mappers JWT — inclure les rôles et permissions (H)

### UI — rôles realm dans `realm_access.roles`
1. Clients → `medical-api` → **Client scopes**.
2. Vérifier que le scope `roles` est dans **Default client scopes**.
3. Dans **Client scopes** → `roles` → **Mappers**, vérifier mapper realm roles.
4. Paramètres clés :
   - Claim JSON type : `String`/`JSON`
   - Add to access token : ON
   - Add to ID token : ON

### Mapper permissions (optionnel)
- Créer un mapper custom (Script/Hardcoded/Policy output selon votre stratégie).
- Claim name : `permissions`
- Inclure dans Access token.

### REST API (exemple mapper hardcoded pour test)
```bash
curl -i -X POST "$KC_URL/admin/realms/Medical_API/clients/$MEDICAL_CLIENT_UUID/protocol-mappers/models" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "permissions-claim",
    "protocol": "openid-connect",
    "protocolMapper": "oidc-hardcoded-claim-mapper",
    "config": {
      "claim.name": "permissions",
      "claim.value": "[\"permission:medical:read\"]",
      "jsonType.label": "JSON",
      "access.token.claim": "true",
      "id.token.claim": "true",
      "userinfo.token.claim": "true"
    }
  }'
```

---

## 9) Créer des utilisateurs et assigner les rôles (I)

### Étapes UI
1. Realm `Medical_API` → **Users** → **Add user**.
2. Créer : `admin`, `editor`, `reader` (enabled ON).
3. Pour chaque utilisateur :
   - Onglet **Credentials** → définir password permanent.
   - Onglet **Role mapping** → assigner :
     - `admin` → `ROLE_MEDICAL_ADMIN`
     - `editor` → `ROLE_MEDICAL_EDITOR`
     - `reader` → `ROLE_MEDICAL_READER`

### REST API — création utilisateurs
```bash
for user in admin editor reader; do
  curl -i -X POST "$KC_URL/admin/realms/Medical_API/users" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$user\",\"enabled\":true}"
done
```

### REST API — définir mot de passe
```bash
set_password() {
  local username=$1
  local password=$2
  local uid
  uid=$(curl -s "$KC_URL/admin/realms/Medical_API/users?username=$username" -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')
  curl -i -X PUT "$KC_URL/admin/realms/Medical_API/users/$uid/reset-password" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"type\":\"password\",\"value\":\"$password\",\"temporary\":false}"
}

set_password admin admin123
set_password editor editor123
set_password reader reader123
```

### REST API — assigner rôles realm
```bash
assign_realm_role() {
  local username=$1
  local role_name=$2
  local uid
  uid=$(curl -s "$KC_URL/admin/realms/Medical_API/users?username=$username" -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')
  local role_json
  role_json=$(curl -s "$KC_URL/admin/realms/Medical_API/roles/$role_name" -H "Authorization: Bearer $ADMIN_TOKEN")

  curl -i -X POST "$KC_URL/admin/realms/Medical_API/users/$uid/role-mappings/realm" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "[$role_json]"
}

assign_realm_role admin ROLE_MEDICAL_ADMIN
assign_realm_role editor ROLE_MEDICAL_EDITOR
assign_realm_role reader ROLE_MEDICAL_READER
```

---

## 10) Tests — token + appel API sécurisé (J)

> Keycloak 26 utilise des URLs sans préfixe `/auth`.
> - Nouveau format (recommandé) : `/realms/...`
> - Ancien format `/auth/realms/...` : seulement si reverse-proxy configuré.

### 10.1 Obtenir un token (password grant)
```bash
export API_CLIENT_ID="medical-api"
export API_CLIENT_SECRET="$MEDICAL_CLIENT_SECRET"

export ADMIN_USER_TOKEN=$(curl -s -X POST "$KC_URL/realms/Medical_API/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$API_CLIENT_ID" \
  -d "client_secret=$API_CLIENT_SECRET" \
  -d "username=admin" \
  -d "password=admin123" | jq -r '.access_token')
```

### 10.2 Décoder le JWT
```bash
echo "$ADMIN_USER_TOKEN" | cut -d '.' -f2 | tr '_-' '/+' | base64 -d 2>/dev/null | jq
```

Exemple attendu (simplifié) :
```json
{
  "iss": "http://localhost:8081/realms/Medical_API",
  "realm_access": {
    "roles": ["ROLE_MEDICAL_ADMIN"]
  },
  "permissions": [
    "permission:medical:create",
    "permission:medical:read",
    "permission:medical:update",
    "permission:medical:delete"
  ]
}
```

### 10.3 Appeler l’API
```bash
curl -i -X GET "http://localhost:8080/api/medical-ressources" \
  -H "Authorization: Bearer $ADMIN_USER_TOKEN"
```

### 10.4 Vérifier comportement RBAC
- token `admin` : GET/POST/PUT/PATCH/DELETE attendus en succès (200/201/204)
- token `editor` : DELETE attendu en 403
- token `reader` : POST/PUT/PATCH/DELETE attendus en 403

Exemple test `reader` sur POST (doit échouer) :
```bash
export READER_TOKEN=$(curl -s -X POST "$KC_URL/realms/Medical_API/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$API_CLIENT_ID" \
  -d "client_secret=$API_CLIENT_SECRET" \
  -d "username=reader" \
  -d "password=reader123" | jq -r '.access_token')

curl -i -X POST "http://localhost:8080/api/medical-ressources" \
  -H "Authorization: Bearer $READER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"code":"MED-001","label":"Test","status":"ACTIVE"}'
```

---

## 11) Annexe — endpoints Admin REST utiles (K)

| Usage | Endpoint |
|---|---|
| Créer realm | `POST /admin/realms` |
| Créer client | `POST /admin/realms/{realm}/clients` |
| Lister clients | `GET /admin/realms/{realm}/clients?clientId=...` |
| Secret client | `GET /admin/realms/{realm}/clients/{id}/client-secret` |
| Créer rôle realm | `POST /admin/realms/{realm}/roles` |
| Créer user | `POST /admin/realms/{realm}/users` |
| Reset password | `PUT /admin/realms/{realm}/users/{id}/reset-password` |
| Mapper rôle realm user | `POST /admin/realms/{realm}/users/{id}/role-mappings/realm` |
| Créer scope authz | `POST /admin/realms/{realm}/clients/{id}/authz/resource-server/scope` |
| Créer resource authz | `POST /admin/realms/{realm}/clients/{id}/authz/resource-server/resource` |
| Créer role policy | `POST /admin/realms/{realm}/clients/{id}/authz/resource-server/policy/role` |
| Créer scope permission | `POST /admin/realms/{realm}/clients/{id}/authz/resource-server/permission/scope` |
| Créer protocol mapper | `POST /admin/realms/{realm}/clients/{id}/protocol-mappers/models` |

---

## 12) Troubleshooting 401 / 403

### 401 Unauthorized
- `issuer-uri` côté Spring ne correspond pas à `iss` du token.
- Signature/JWK inaccessible (`$KC_URL/realms/Medical_API/protocol/openid-connect/certs`).
- Token expiré (`exp`).

### 403 Forbidden
- Le token ne contient pas la permission attendue (`permission:medical:*`).
- Rôle mal assigné ou absent dans `realm_access.roles`.
- Mapping Spring Security HTTP method ↔ authority non aligné.

### Debug rapide
```bash
# Voir issuer et certs
curl -s "$KC_URL/realms/Medical_API/.well-known/openid-configuration" | jq '{issuer,jwks_uri}'

# Voir claims token
echo "$ADMIN_USER_TOKEN" | cut -d'.' -f2 | tr '_-' '/+' | base64 -d 2>/dev/null | jq
```

---

## Captures d’écran recommandées (checklist)

Si vous documentez pour un wiki interne, prendre une capture par écran :
1. Create Realm (`Medical_API`)
2. Create Client (`medical-api`) + toggles (confidential, service accounts, direct grants, authorization)
3. Authorization > Scopes (4 scopes CRUD)
4. Authorization > Resource (`medical-resource` + URI)
5. Policies (Admin/Editor/Reader)
6. Permissions (Create/Read/Update/Delete)
7. User `admin` role mapping
8. JWT décodé avec `realm_access.roles`



---

## Schéma Mermaid détaillé — interconnexions des ressources Keycloak

```mermaid
flowchart TD
    %% =========================
    %% CONTEXTE GLOBAL
    %% =========================
    A[Utilisateur
admin/editor/reader] -->|1. Login/password grant| KC[(Keycloak 26)]
    SA[Service Account
client medical-api] -->|1b. client_credentials| KC
    API[Spring Boot medical-api
Resource Server JWT] -->|2. Vérifie iss + JWK| KC

    %% =========================
    %% REALM & CLIENT
    %% =========================
    subgraph REALM[Realm: Medical_API]
      direction TB
      R1[Realm Settings
enabled=true]

      subgraph CLIENT[Client OIDC: medical-api]
        direction TB
        C1[clientId=medical-api
publicClient=false
(confidential)]
        C2[Credentials
client_secret]
        C3[Flows
standard=OFF
direct_grants=ON
service_accounts=ON]
        C4[Authorization Services=ON
(Resource Server)]
      end

      R1 --> C1
      C1 --> C2
      C1 --> C3
      C1 --> C4

      %% =========================
      %% ROLES
      %% =========================
      subgraph ROLES[Realm Roles]
        direction TB
        RA[ROLE_MEDICAL_ADMIN]
        RE[ROLE_MEDICAL_EDITOR]
        RR[ROLE_MEDICAL_READER]
      end

      %% =========================
      %% USERS
      %% =========================
      subgraph USERS[Users]
        direction TB
        U1[admin]
        U2[editor]
        U3[reader]
      end

      U1 -->|role mapping| RA
      U2 -->|role mapping| RE
      U3 -->|role mapping| RR

      %% =========================
      %% AUTHZ: SCOPES + RESOURCE
      %% =========================
      subgraph AUTHZ[Authorization Services / Resource Server]
        direction TB
        S1[Scope create]
        S2[Scope read]
        S3[Scope update]
        S4[Scope delete]

        RES[Resource medical-resource
type=MedicalRessource
uri=/api/medical-ressources/*]

        RES --> S1
        RES --> S2
        RES --> S3
        RES --> S4
      end

      C4 --> AUTHZ

      %% =========================
      %% POLICIES (ROLE-BASED)
      %% =========================
      subgraph POLICIES[Policies - type: role]
        direction TB
        P1[Admin Role Policy]
        P2[Editor Role Policy]
        P3[Reader Role Policy]
      end

      RA --> P1
      RE --> P2
      RR --> P3

      %% =========================
      %% PERMISSIONS (SCOPE-BASED)
      %% =========================
      subgraph PERMS[Permissions - type: scope]
        direction TB
        PM1[Medical Create Permission]
        PM2[Medical Read Permission]
        PM3[Medical Update Permission]
        PM4[Medical Delete Permission]
      end

      %% Scope bindings
      S1 --> PM1
      S2 --> PM2
      S3 --> PM3
      S4 --> PM4

      %% Policy bindings
      P1 --> PM1
      P2 --> PM1

      P1 --> PM2
      P2 --> PM2
      P3 --> PM2

      P1 --> PM3
      P2 --> PM3

      P1 --> PM4

      %% Resource bindings
      RES --> PM1
      RES --> PM2
      RES --> PM3
      RES --> PM4

      %% =========================
      %% TOKEN MAPPERS
      %% =========================
      subgraph MAPPERS[Protocol Mappers / Claims JWT]
        direction TB
        M1[realm roles -> realm_access.roles]
        M2[permissions -> claim permissions
(optionnel/custom)]
      end

      RA --> M1
      RE --> M1
      RR --> M1
      PM1 --> M2
      PM2 --> M2
      PM3 --> M2
      PM4 --> M2
    end

    %% =========================
    %% TOKEN & API AUTHORIZATION
    %% =========================
    KC -->|3. Access Token JWT
iss, realm_access.roles, permissions| A
    KC -->|3b. Access Token JWT| SA

    A -->|4. Bearer token| API
    SA -->|4b. Bearer token| API

    API -->|5. Spring Security mapping| CHK{Authority check}

    CHK -->|POST -> permission:medical:create| OK1[201/403]
    CHK -->|GET -> permission:medical:read| OK2[200/403]
    CHK -->|PUT/PATCH -> permission:medical:update| OK3[200/403]
    CHK -->|DELETE -> permission:medical:delete| OK4[204/403]
```

### Lecture rapide du schéma
- Le **Realm `Medical_API`** contient les rôles, utilisateurs et le client `medical-api`.
- Le client active **Authorization Services** pour définir la combinaison **Resource + Scopes + Policies + Permissions**.
- Les rôles sont injectés dans `realm_access.roles`, puis (optionnellement) les permissions dans un claim `permissions`.
- L’API Spring valide le JWT puis applique le contrôle d’accès par authority : `permission:medical:*`.

