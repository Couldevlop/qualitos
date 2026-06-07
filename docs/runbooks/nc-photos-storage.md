# Runbook — Stockage binaire des photos de Non-Conformités

Module : `api-quality-engine` · package `com.openlab.qualitos.quality.nonconformity`
(+ sous-package `…nonconformity.storage`). Réf. CLAUDE.md §4.3.

## Principe

Les photos de NC ne sont **jamais** stockées en base : seules les métadonnées
(clé d'objet tenantisée, content-type, taille, nom d'origine assaini) le sont,
dans la table `nc_photos` (migration `V81`). Les binaires vivent dans un
stockage objet **S3-compatible** (MinIO en dev, S3/MinIO managé en prod), via le
port `ObjectStorage` et son adaptateur `S3ObjectStorage` (AWS SDK v2, path-style).

Le stockage est **désactivé par défaut**. Sans activation, l'adaptateur S3 n'est
pas instancié et les 3 endpoints photos répondent **503**
(`type=https://qualitos.io/errors/storage-disabled`).

## Activer MinIO en local

```bash
# 1. Démarrer MinIO + créer le bucket (job minio-init idempotent)
docker compose -f docker-compose.dev.yml --profile storage up -d minio minio-init

# 2. Activer le stockage côté engine (puis (re)démarrer api-quality-engine)
#    soit via .env : STORAGE_S3_ENABLED=true
#    soit en lançant l'engine en local avec la variable d'environnement.
```

- Console MinIO : http://localhost:9001 — `qualitos` / `qualitos-dev-secret`
- API S3 : http://localhost:9000 — bucket `qualitos-nc-photos`

> Les credentials MinIO ci-dessus sont du **dev local assumé** (allowlistés
> gitleaks, comme les autres creds de `docker-compose.dev.yml`).

## Variables d'environnement (engine)

| Variable                | Rôle                                  | Défaut         |
| ----------------------- | ------------------------------------- | -------------- |
| `STORAGE_S3_ENABLED`    | Active l'adaptateur S3                 | `false`        |
| `STORAGE_S3_ENDPOINT`   | Endpoint S3/MinIO                      | _(vide)_       |
| `STORAGE_S3_BUCKET`     | Bucket cible                          | _(vide)_       |
| `STORAGE_S3_REGION`     | Région SDK (MinIO l'ignore)           | `us-east-1`    |
| `STORAGE_S3_ACCESS_KEY` | Clé d'accès                           | _(vide)_       |
| `STORAGE_S3_SECRET_KEY` | Clé secrète                           | _(vide)_       |

**Aucun secret par défaut en dur** (CLAUDE.md §18.2.3). En dev, les valeurs sont
fournies par `docker-compose.dev.yml`. En **prod**, elles proviennent de
**HashiCorp Vault + External Secrets Operator** (jamais en clair).

## Sécurité (OWASP)

- **Taille max 10 Mo** : double rempart — limite multipart Spring
  (`spring.servlet.multipart.max-file-size=10MB` → 413) **et** vérification
  applicative dans le service (413).
- **Whitelist content-type** : `image/jpeg`, `image/png`, `image/webp`,
  `image/heic`. Tout autre type → 400.
- **Extension déduite du content-type validé**, jamais du nom de fichier client
  (neutralise le path traversal). Le nom d'origine est conservé à titre
  informatif uniquement, après assainissement.
- **Clé tenantisée** : `tenants/{tenantId}/nc/{ncId}/{uuid}.{ext}`. Le `tenantId`
  vient toujours du JWT (jamais du body).
- **Lecture par URL pré-signée** uniquement, **TTL 15 min** — aucun objet n'est
  exposé en accès public.
- NC `CLOSED`/`CANCELLED` → 409 (pas d'ajout de preuve sur une NC figée).

## Endpoints

- `POST   /api/v1/nc/{id}/photos` (multipart, champ `file`) → 201
- `GET    /api/v1/nc/{id}/photos` → 200, liste avec `url` pré-signée
- `DELETE /api/v1/nc/{id}/photos/{photoId}` → 204 (supprime objet + ligne, idempotent côté storage)

## Prod

S3 (AWS) ou MinIO en cluster, bucket par environnement, chiffrement au repos
(SSE-S3/KMS), credentials via Vault+ESO, politique de cycle de vie (lifecycle)
selon la rétention NC. Le path-style reste activé pour MinIO ; sur S3 AWS pur il
est toléré.
