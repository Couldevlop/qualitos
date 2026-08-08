# Runbook — Stockage objet des pièces jointes

Deux usages, **une seule infrastructure** : les photos de Non-Conformité
(CLAUDE.md §4.3, package `…quality.nonconformity`) et les preuves des dossiers
CAPA (§4.2, ISO 9001 §10.2, package `…quality.capa`, ADR 0050).

## Principe

Les binaires ne sont **jamais** en base : seules les métadonnées (clé d'objet
tenantisée, content-type, taille, nom d'origine assaini) le sont, dans
`nc_photos` (migration `V81`) et `capa_evidences` (`V102`). Les fichiers vivent
dans un stockage objet **S3-compatible** (MinIO), via le port `ObjectStorage` et
son adaptateur `S3ObjectStorage` (AWS SDK v2, path-style).

Le stockage est **désactivé par défaut**. Sans activation, l'adaptateur n'est pas
instancié et tous les points d'entrée répondent **503**
(`type=https://qualitos.io/errors/storage-disabled`). Les deux écrans l'annoncent
alors explicitement au lieu d'afficher une erreur brute.

### Un seul bucket, deux préfixes

```
qualitos-evidence/
  tenants/{tenantId}/nc/{ncId}/{uuid}.{ext}       ← photos de NC
  tenants/{tenantId}/capa/{capaId}/{uuid}.{ext}   ← preuves CAPA
```

Deux buckets imposeraient deux jeux d'identifiants et deux routes publiques pour
la même politique d'accès.

### Deux endpoints, et ce n'est pas une redondance

| Variable                     | Qui l'utilise  | Pourquoi                                                          |
| ---------------------------- | -------------- | ----------------------------------------------------------------- |
| `STORAGE_S3_ENDPOINT`        | le **serveur** | dépôts et suppressions, par le réseau interne                     |
| `STORAGE_S3_PUBLIC_ENDPOINT` | le **client**  | hôte pour lequel les URL de lecture sont **signées**              |

La signature couvre l'hôte : une URL présignée sur `minio.…svc.cluster.local`
serait parfaitement valide et pourtant inouvrable depuis un poste, sans qu'aucune
erreur ne remonte côté serveur. On ne peut pas non plus réécrire l'URL après
coup. Laissée vide, la variable retombe sur l'endpoint interne — ce qui ne
convient qu'à un déploiement où les deux se confondent.

## Activer en local

```bash
# 1. Démarrer MinIO + créer le bucket (job minio-init idempotent)
docker compose -f docker-compose.dev.yml --profile storage up -d minio minio-init

# 2. Activer le stockage côté engine, puis (re)démarrer api-quality-engine
#    via .env : STORAGE_S3_ENABLED=true
```

- Console MinIO : http://localhost:9001 — `qualitos` / `qualitos-dev-secret`
- API S3 : http://localhost:9000 — bucket `qualitos-evidence`

> Credentials de **dev local assumé** (allowlistés gitleaks, comme les autres
> creds de `docker-compose.dev.yml`).

## Activer sur un cluster

Rien à faire à la main : `infra/k8s/deploy.sh <env> <version>` s'en charge.

1. `infra/k8s/deps/50-minio.yaml` — PVC 5 Gi, Service (port 9000 seulement),
   Deployment, et un Job `minio-init` qui crée le bucket. Le Job est supprimé
   avant d'être réappliqué : un Job est immuable, et sans cela la deuxième
   exécution échouerait sur un champ non modifiable.
2. Secret `qualitos-minio` — identifiants **générés une seule fois** dans le
   cluster, jamais régénérés (MinIO conserve les siens dans son volume).
3. Secret `qualitos-api-quality-engine-storage` — les mêmes clés, remises à
   l'engine. Séparé des mots de passe de base : faire tourner une clé de
   stockage ne doit pas obliger à toucher aux identifiants PostgreSQL.
4. Valeurs Helm (`values-preprod.yaml`, `values-prod.yaml`) —
   `STORAGE_S3_ENABLED/ENDPOINT/PUBLIC_ENDPOINT/BUCKET` et
   `objectStorage.enabled: true`, qui publie la route de lecture.

### La route publique de lecture

`https://<hôte>/qualitos-evidence/…` → Service `minio`, sur un **Ingress
séparé** (`templates/ingress-object-storage.yaml`). Le nom du bucket est le
préfixe de chemin (accès S3 path-style) ; **aucune réécriture** n'est appliquée,
car la signature couvre le chemin autant que l'hôte.

Ce que cette route n'ouvre pas :

- Le bucket est **privé** (`mc anonymous set none`) : sans signature valide,
  MinIO refuse. L'autorisation, c'est la signature — valable 15 minutes.
- **Lecture seule au niveau du proxy** (`limit_except GET HEAD OPTIONS`) : même
  un bucket rendu public par erreur resterait non modifiable de l'extérieur.
- La console d'administration (port 9001) n'est **pas** dans le Service, donc
  pas publiée. La racine non plus : la liste des buckets est hors du préfixe.
- Le contenu est servi **inerte** : `Content-Security-Policy: sandbox`,
  `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`,
  `Cache-Control: private, no-store`. C'est le seul endroit de la plateforme où
  du contenu déposé par des utilisateurs est servi sur l'origine de
  l'application ; il ne doit rien pouvoir exécuter, et l'URL présignée — qui
  **est** le jeton d'accès — ne doit fuir ni par le Referer ni par un cache
  partagé.

### Taille des corps au proxy

`nginx.ingress.kubernetes.io/proxy-body-size: 12m` sur l'ingress applicatif. Le
défaut du contrôleur est **1 Mo** : sans ce relèvement, toute pièce au-delà
serait refusée **avant** d'atteindre l'application, avec un 413 que rien ne
rattacherait à sa cause. 12 Mo et non 10 : l'enveloppe multipart s'ajoute au
fichier. La borne qui fait foi reste celle de l'application.

## Variables d'environnement (engine)

| Variable                     | Rôle                                         | Défaut      |
| ---------------------------- | -------------------------------------------- | ----------- |
| `STORAGE_S3_ENABLED`         | Active l'adaptateur S3                        | `false`     |
| `STORAGE_S3_ENDPOINT`        | Endpoint joignable par le serveur             | _(vide)_    |
| `STORAGE_S3_PUBLIC_ENDPOINT` | Endpoint joignable par le navigateur          | _(vide)_    |
| `STORAGE_S3_BUCKET`          | Bucket cible                                  | _(vide)_    |
| `STORAGE_S3_REGION`          | Région SDK (MinIO l'ignore)                   | `us-east-1` |
| `STORAGE_S3_ACCESS_KEY`      | Clé d'accès                                   | _(vide)_    |
| `STORAGE_S3_SECRET_KEY`      | Clé secrète                                   | _(vide)_    |

**Aucun secret par défaut en dur** (CLAUDE.md §18.2.3).

## Sécurité (OWASP)

- **Taille** : double rempart — limite multipart Spring (10 Mo → 413) **et**
  vérification applicative (413). Côté CAPA s'ajoutent deux bornes de dossier :
  10 pièces et 25 Mo cumulés (409, jamais de purge silencieuse).
- **Liste blanche de content-types** : images (`jpeg`, `png`, `webp`, `heic`)
  pour les NC ; plus `application/pdf`, `docx` et `xlsx` pour les preuves CAPA.
  Tout autre type → 400.
- **Signature binaire vérifiée** contre le type déclaré (`%PDF-`, `PK\x03\x04`
  pour les formats OOXML, signatures d'image). Le content-type client est
  falsifiable ; un exécutable renommé est refusé.
- **Extension déduite du type validé**, jamais du nom de fichier client (neutralise
  la traversée de chemin). Le nom d'origine n'est conservé qu'à titre informatif,
  après assainissement (`[^A-Za-z0-9._-]` → `_`).
- **Clé tenantisée** ; le `tenantId` vient **toujours** du JWT (§18.2 #2).
- **Lecture par URL présignée uniquement**, TTL 15 min — aucun objet public.
- **Verrou d'état** : NC `CLOSED`/`CANCELLED` et CAPA `CLOSED`/`REJECTED` → 409.
  Une pièce ne s'ajoute plus à un dossier figé.
- **Suppression** : réservée Manager Qualité et au-dessus par la règle générique
  `DELETE /api/v1/**` du `SecurityConfig`. Un utilisateur dépose, il ne retire pas.
- **Journal chaîné** (preuves CAPA) : `capa.evidence.uploaded` et
  `capa.evidence.removed` sont inscrits au journal du tenant, avec l'auteur, le nom
  d'origine et le poids — jamais la clé d'objet.

## Points d'entrée

| Photos de NC                          | Preuves CAPA                                     |
| ------------------------------------- | ------------------------------------------------ |
| `POST /api/v1/nc/{id}/photos` → 201    | `POST /api/v1/capa/cases/{id}/evidences` → 201     |
| `GET /api/v1/nc/{id}/photos` → 200     | `GET /api/v1/capa/cases/{id}/evidences` → 200      |
| `DELETE /api/v1/nc/{id}/photos/{pid}` → 204 | `DELETE /api/v1/capa/cases/{id}/evidences/{eid}` → 204 |

## À vérifier au premier usage réel sur un cluster

Deux points ne peuvent pas être éprouvés hors cluster, et se manifesteraient par
un refus **en amont de l'application** — le diagnostic le plus trompeur :

1. **Dépôt** : ModSecurity inspecte le corps des requêtes. Si un dépôt revient en
   403 sans trace applicative, la règle CRS en cause se lit dans les journaux du
   contrôleur ; l'exclusion se pose sur l'ingress applicatif, ciblée par identifiant.
2. **Lecture** : une URL présignée porte sa signature en paramètres. Si une
   lecture revient en 403 sans atteindre MinIO, l'exclusion se pose sur
   `qualitos-object-storage`, et là seulement.

## Production

Bucket par environnement, chiffrement au repos (SSE-S3/KMS sur S3 managé),
politique de cycle de vie selon la rétention, credentials via Vault + ESO. Le
path-style reste activé pour MinIO ; sur S3 AWS pur il est toléré.
