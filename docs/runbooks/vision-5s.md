# Runbook — ai-vision-5s (Vision 5S)

Service de vision par ordinateur pour le scoring **5S** (CLAUDE.md §3.2 / §1.4 / §12).
FastAPI + Clean Architecture (domaine / application / infrastructure / présentation).
Backend d'inférence **ONNX** (modèle YOLOv8 exporté) avec **fallback transparent**
vers un stub déterministe quand aucun modèle n'est fourni — le contrat REST est
identique dans les deux modes.

- Code : `apps/ai-vision-5s/`
- Port interne du conteneur : **8090** (Dockerfile `EXPOSE 8090`, uvicorn `--port 8090`)
- Port publié sur l'hôte (dev compose) : **8086** (mappé `8086:8090` pour éviter le
  conflit avec `blockchain-service` qui occupe 8090 sous le profil `fabric`)

---

## 1. Démarrage

### Via docker-compose (profil `vision`, OFF par défaut)

```bash
docker compose -f docker-compose.dev.yml --profile vision up -d --build ai-vision-5s
# Health :
curl http://localhost:8086/health        # -> {"status":"UP"}
# OpenAPI / Swagger : http://localhost:8086/v1/docs
```

Arrêt : `docker compose -f docker-compose.dev.yml --profile vision down`

### Exécution locale (sans Docker, dev rapide)

Python 3.12 (`C:\Python312` sur le poste de dev). Pas de venv requis ;
installer les deps dans l'env courant comme pour `ai-service`.

```powershell
$env:TMP='D:\tmp'; $env:TEMP='D:\tmp'
Set-Location D:\OPENLAB\qualitOs\apps\ai-vision-5s
C:\Python312\python.exe -m pip install -r requirements-dev.txt   # 1re fois
# (optionnel) inférence réelle : C:\Python312\python.exe -m pip install ".[onnx]"

$env:AUTH_BYPASS='true'; $env:APP_PROFILE='dev'
C:\Python312\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8086
```

### Tests

Le `pyproject.toml` impose un gate de couverture (`--cov-fail-under=85`). Pour lancer
les tests sans le gate (pattern `ai-service`) :

```powershell
$env:TMP='D:\tmp'; $env:TEMP='D:\tmp'
Set-Location D:\OPENLAB\qualitOs\apps\ai-vision-5s
C:\Python312\python.exe -m pytest -p no:cacheprovider -o addopts=""
```

Référence : **43 tests** verts (domaine, application, image-safety, onnx-backend, routes, auth).

---

## 2. Mode stub vs ONNX

La sélection du backend est faite au démarrage par `app/infrastructure/backend_factory.py`
et **journalisée** (`backend.factory selected=stub|onnx ...`) :

| Condition | Backend retenu |
| --- | --- |
| `VISION5S_ONNX_MODEL_PATH` vide / non défini | `DeterministicStubBackend` (stub) |
| `VISION5S_ONNX_MODEL_PATH` pointe sur un fichier absent | stub (warning `model-missing`) |
| `onnxruntime`/`numpy` non installés ou modèle illisible | stub (warning `onnx-unavailable`) |
| modèle ONNX présent + libs installées | `OnnxInferenceBackend` (inférence réelle) |

Le fallback est **transparent** : aucune rupture d'API, le service boote toujours.

### Activer l'inférence ONNX réelle

1. Exporter le modèle YOLOv8 fine-tuné 5S au format ONNX.
   Contrat modèle (V1, cf. `app/infrastructure/onnx_backend.py`) :
   - entrée : tenseur RGB unique, **NCHW float32**, normalisé 0..1, redimensionné à
     `VISION5S_ONNX_INPUT_SIZE` (défaut **224**) ;
   - sortie : vecteur **1×5** de scores par pilier (Seiri, Seiton, Seiso, Seiketsu,
     Shitsuke) dans 0..1, clampés puis mis à l'échelle 0..100.
2. Déposer le `.onnx` accessible au conteneur (volume monté) ou au process local.
3. Installer le runtime : `pip install ".[onnx]"` (onnxruntime + numpy).
4. Renseigner `VISION5S_ONNX_MODEL_PATH` (+ `VISION5S_ONNX_INPUT_SIZE` si ≠ 224).

En compose, ajouter un volume + l'env :

```yaml
ai-vision-5s:
  environment:
    VISION5S_ONNX_MODEL_PATH: /models/vision5s-yolov8.onnx
  volumes:
    - ./infra/models/vision5s-yolov8.onnx:/models/vision5s-yolov8.onnx:ro
```

> Aucun modèle entraîné n'est versionné dans le repo. La voie ONNX réelle est
> couverte par les tests en mockant `onnxruntime.InferenceSession`.

---

## 3. Auth

L'auth est gérée dans `app/infrastructure/auth.py` :

- **Prod** : en-tête `Authorization: Bearer <JWT>`, validé (RS256) contre
  `KEYCLOAK_JWKS_URI` (iss/aud/exp/nbf + signature). Le `tenant_id` provient de la
  claim JWT, **jamais** du body (invariant CLAUDE.md §18.2). `aud` attendu :
  `api-ai-vision-5s`.
- **Dev** : `AUTH_BYPASS=true` court-circuite la validation et synthétise un
  `TenantContext` (tenant `00000000-0000-0000-0000-000000000001`, rôle
  `QUALITY_MANAGER`). **Aucun en-tête d'auth n'est requis** dans ce mode.

> Note : ce service n'utilise PAS l'en-tête `X-Dev-Claims` de `ai-service`. Son
> bypass dev se pilote uniquement via `AUTH_BYPASS`.

Garde-fou : `assert_production_safe()` (appelé au boot) **refuse de démarrer** si
`AUTH_BYPASS=true` et `APP_PROFILE=prod`. Le Dockerfile fixe `APP_PROFILE=prod`,
`AUTH_BYPASS=false`.

### 3.1 Tenant en mode Bearer — appels de service (ADR 0021)

Un jeton **client_credentials** (service, ex. api-quality-engine) ne porte pas de
tenant par requête. Résolution du tenant côté service (`_resolve_tenant_id`) :

1. claim `tenant_id` du token (token utilisateur) — **gagne toujours** ;
2. sinon, l'en-tête **`X-Tenant-Id`** est accepté **uniquement** si l'`azp` du token
   (client id Keycloak) figure dans `TRUSTED_SERVICE_AZP` (liste CSV, **vide par
   défaut** = en-tête jamais accepté) ;
3. sinon → `401` (fail-closed).

L'en-tête reste protégé par la signature du Bearer : sans jeton valide d'un client
de confiance, impossible d'usurper un tenant.

---

## 3bis. Auth prod — appel engine → ai-vision-5s (Bearer service token)

Côté `api-quality-engine`, la passerelle `VisionGatewayClient` a trois modes
(`qualitos.vision.auth` / env `VISION_AUTH`) :

| Mode | Comportement | Usage |
| --- | --- | --- |
| `dev-claims` (défaut) | en-tête `X-Dev-Claims` (ignoré par ai-vision-5s en `AUTH_BYPASS`) | dev local uniquement |
| `bearer` | `Authorization: Bearer <token client_credentials>` + `X-Tenant-Id: <tenant du JWT utilisateur>` | **OBLIGATOIRE en prod** |
| `none` | aucun en-tête | réseau de confiance/mTLS mesh, déconseillé |

Le jeton est obtenu par `ServiceTokenProvider` (POST client_credentials au token
endpoint Keycloak, cache jusqu'à `expires_in - marge`, thread-safe, timeouts courts,
jamais de secret/jeton dans les logs). **Échec d'obtention du jeton → 503
`vision-unavailable` (fail-closed, aucun repli dev-claims).**

### Client Keycloak à provisionner (prod)

1. Créer un client **confidentiel** `api-quality-engine-vision` dans le realm
   `qualitos` : *Client authentication* ON, *Service accounts roles* ON
   (client_credentials), aucun flow navigateur.
2. Ajouter un **Audience mapper** (hardcoded) qui injecte `api-ai-vision-5s` dans
   `aud` du token de service (sinon ai-vision-5s rejette en 401).
3. Récupérer le secret (onglet *Credentials*) et le provisionner via Vault/ESO —
   jamais en clair (§18.2-3).

### Variables d'environnement

Côté **api-quality-engine** :

```bash
VISION_ENABLED=true
VISION_BASE_URL=http://ai-vision-5s:8090
VISION_AUTH=bearer
VISION_TOKEN_URI=https://keycloak.example.com/realms/qualitos/protocol/openid-connect/token
VISION_CLIENT_ID=api-quality-engine-vision
VISION_CLIENT_SECRET=<via Vault/ESO>
# Optionnels :
VISION_SCOPE=                       # si un scope déclenche l'audience mapper
VISION_TOKEN_REFRESH_MARGIN_S=30    # marge avant expiration pour rafraîchir
VISION_TOKEN_CONNECT_TIMEOUT_MS=3000
VISION_TOKEN_READ_TIMEOUT_MS=5000
```

Côté **ai-vision-5s** :

```bash
APP_PROFILE=prod
AUTH_BYPASS=false
KEYCLOAK_JWKS_URI=https://keycloak.example.com/realms/qualitos/protocol/openid-connect/certs
KEYCLOAK_ISSUER=https://keycloak.example.com/realms/qualitos
KEYCLOAK_AUDIENCE=api-ai-vision-5s
# Autorise le client de service de l'engine à porter le tenant via X-Tenant-Id :
TRUSTED_SERVICE_AZP=api-quality-engine-vision
```

### Vérification rapide

```bash
TOKEN=$(curl -s -d 'grant_type=client_credentials' \
  -d 'client_id=api-quality-engine-vision' -d "client_secret=$SECRET" \
  https://keycloak.example.com/realms/qualitos/protocol/openid-connect/token | jq -r .access_token)
curl -s -H "Authorization: Bearer $TOKEN" -H "X-Tenant-Id: <uuid-tenant>" \
  -F image=@photo.jpg https://ai-vision-5s.internal/v1/vision/5s/score
```

---

## 4. Contrat des endpoints

Préfixe `/v1/vision/5s`. Les deux endpoints prennent un upload `multipart/form-data`
champ **`image`** (JPEG / PNG / WebP, ≤ 10 Mo ; EXIF stripé, MIME sniffé libmagic).
Rate limit : 60 req/min/IP (slowapi).

### `POST /v1/vision/5s/analyze` → `200`

Analyse complète : score par pilier + findings (non-conformités).

```json
{
  "image_sha256": "07f99228d267ead38c775927f87e9a7c7f63725f20b29e3d184f6efce4a75370",
  "width": 64,
  "height": 64,
  "score": {
    "seiri": 37, "seiton": 66, "seiso": 34,
    "seiketsu": 70, "shitsuke": 98, "overall": 61
  },
  "findings": [
    {
      "pillar": "seiri",
      "description": "Unused items detected in workspace",
      "severity": "critical",
      "confidence": 0.63,
      "bbox": null
    },
    {
      "pillar": "seiso",
      "description": "Cleanliness below standard",
      "severity": "critical",
      "confidence": 0.66,
      "bbox": null
    }
  ]
}
```

(Exemple réel — sortie du **stub** pour une petite image PNG unie.)

Règles findings : un finding est émis par pilier dont le score < 60 ;
`severity = critical` si score < 45, sinon `warning`. `confidence = round(1 - score/100, 2)`.
`bbox` est `[x, y, w, h]` ou `null` (le stub ne produit pas de bbox).

### `POST /v1/vision/5s/score` → `200`

Scoring léger, sans findings :

```json
{ "seiri": 37, "seiton": 66, "seiso": 34, "seiketsu": 70, "shitsuke": 98, "overall": 61 }
```

`overall` = moyenne entière des 5 piliers (poids égaux, surchargeable par industry pack).

### Erreurs (RFC 7807 `application/problem+json`)

- `400` image vide / > 10 Mo / MIME non supporté / non décodable (image-safety).
- `400` validation de requête (form-data manquant).
- `401` (prod uniquement) Bearer absent / token invalide / tenant irrésolvable
  (claim `tenant_id` absente et `X-Tenant-Id` absent ou `azp` non listé dans
  `TRUSTED_SERVICE_AZP`).
- `429` dépassement du rate limit.

---

## 5. Intégration engine

L'`api-quality-engine` (Java) consomme `/analyze` lors d'un audit 5S terrain :
l'image remontée alimente le score 5S et génère des findings (→ NC / plan d'action).
Le contrat ci-dessus (score + findings) est stable entre les modes stub et ONNX.
L'auth interne de cet appel est décrite en §3bis (dev-claims en dev, **bearer en
prod**, ADR 0021).
