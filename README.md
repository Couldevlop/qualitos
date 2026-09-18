# QualitOS — Quality Operating System

> **One platform. Five methods. Every industry. Zero compromise.**

QualitOS agrège les méthodes fondamentales de la qualité totale (PDCA, 5S, Cercle de Qualité,
DMAIC + Poka-Yoke, Ishikawa) dans un référentiel unique, augmenté par l'IA et certifié par
blockchain. Voir [`CLAUDE.md`](./CLAUDE.md) pour le projet complet.

> **Vous reprenez le projet ?** Commencez par
> [`docs/PASSATION.md`](./docs/PASSATION.md). Son **§0** range tout ce qu'il faut
> savoir sous les six capacités qui font qu'on tient un projet seul : **lancer**,
> **comprendre**, **modifier sans casser**, **livrer**, **exploiter**, **réparer**.
> Les procédures d'exploitation sont indexées dans
> [`docs/runbooks/README.md`](./docs/runbooks/README.md).

## Démarrage rapide (Docker)

```bash
# Démarre tout : 15 services (Postgres, Keycloak, MinIO, Redpanda, Qdrant,
# Ollama, Prometheus, Grafana, les quatre services applicatifs et le front).
docker compose -f docker-compose.dev.yml up --build
```

> Le tableau ci-dessous ne liste que les points d'entrée utiles au quotidien.
> **Postgres écoute sur 5434 et non 5432** : un PostgreSQL natif occupe souvent
> déjà le port standard, et le conflit se manifeste par un démarrage qui semble
> réussir mais parle à la mauvaise base.

Accès :

| Service                | URL                                  | Credentials       |
| ---------------------- | ------------------------------------ | ----------------- |
| Frontend Angular       | http://localhost:4200                | superadmin / superadmin |
| api-core               | http://localhost:8181/swagger-ui.html | JWT requis (port 8181 car httpd local sur 8081) |
| api-quality-engine     | http://localhost:8082/swagger-ui.html | JWT requis        |
| Keycloak               | http://localhost:8080                | admin / admin     |
| Postgres               | localhost:**5434**                   | qualitos / qualitos |

Le realm `qualitos` est pré-seedé avec **trois comptes** et les rôles plateforme :

| Compte       | Mot de passe | Rôle(s)                   | Usage                                          |
| ------------ | ------------ | ------------------------- | ---------------------------------------------- |
| `superadmin` | `superadmin` | `super_admin`             | **Compte principal** — admin plateforme global |
| `admin`      | `admin`      | `admin_tenant`            | Admin d'un tenant                              |
| `demo`       | `demo`       | `quality_manager`, `user` | Démos fonctionnelles                           |

Rôles disponibles dans le realm : `super_admin`, `admin_tenant`, `quality_director`,
`quality_manager`, `auditor`, `user`, `external_auditor`.

> Console admin Keycloak (master realm) : `admin` / `admin` sur http://localhost:8080

## Structure du monorepo

```
qualitos/
├── apps/
│   ├── api-core/              # Spring Boot — tenant, user, auth (Java 21)
│   ├── api-quality-engine/    # Spring Boot — 8 modules métier qualité
│   └── web/                   # Angular 18 + Material 3
├── infra/
│   ├── postgres/              # Scripts init multi-db
│   └── keycloak/              # Realm export
├── docs/                      # ADR, git workflow
├── .github/
│   ├── workflows/             # CI (build, tests, SAST, SCA) + release
│   └── CI_SETUP.md            # Variables/secrets à configurer
├── CLAUDE.md                  # Spécification projet (source de vérité)
├── docker-compose.dev.yml
└── pom.xml                    # Maven parent
```

## Backend (api-core + api-quality-engine)

8 modules opérationnels, **551 tests Java**, JaCoCo gate ≥ 85 % lignes / 75 % branches :

| Module             | Endpoint racine                  | Réf CLAUDE.md |
| ------------------ | -------------------------------- | ------------- |
| Tenant / User      | `/api/v1/tenants`, `/users`      | §10.3, §11    |
| PDCA               | `/api/v1/pdca/cycles`            | §3.1          |
| Ishikawa           | `/api/v1/ishikawa/diagrams`      | §3.5          |
| 5S                 | `/api/v1/fives/audits`           | §3.2          |
| CAPA               | `/api/v1/capa/cases`             | §4.2          |
| Document Control   | `/api/v1/documents`              | §4.1          |
| Audit Management   | `/api/v1/audits/plans`           | §4.4          |
| Standards Hub      | `/api/v1/standards`              | §8            |
| Cercle de Qualité  | `/api/v1/circles`                | §3.3          |

```bash
# Build + tests Java
mvn verify
```

## Frontend (apps/web)

Angular 18, Material 3, NgModules, HTML/SCSS séparés (jamais inline). Voir
[`apps/web/README.md`](apps/web/README.md).

```bash
cd apps/web
nvm use         # Node 20.18 (.nvmrc)
npm install
npm start       # http://localhost:4200
```

Mode démo par défaut (données mockées) — bascule sur le vrai backend via
`src/environments/environment.ts` (`useMockApi = false`).

## Git workflow & CI

- **Branches** : `main` (release-ready) ← `develop` (intégration) ← `feature/*`
- **PR template + CODEOWNERS + branch protection** : voir [`docs/git-workflow.md`](docs/git-workflow.md)
- **CI** : [`.github/workflows/ci.yml`](.github/workflows/ci.yml) — build + tests + JaCoCo gate
  + Semgrep SAST + OWASP DC (gaté par `vars.OWASP_DC_ENABLED`).
- **Setup** : [`.github/CI_SETUP.md`](.github/CI_SETUP.md) (variables, secrets, branch
  protection rules à configurer dans Settings).

## Roadmap

Phase actuelle : **fin P1 (MVP démontrable)**. Modules métier livrés ✅, frontend en cours
d'extension (PDCA UI ✅, autres modules en suivants).

Phases CLAUDE.md §17 :
- P0 socle ✅
- P1 MVP — PDCA + Ishikawa + 5S + Document Control + CAPA + Audit + Standards Hub ✅, IA reco + blockchain à venir
- P2 DMAIC + Poka-Yoke + Cercle ✅, transverses : Supplier, Change, Calibration, EHS
- P3 Industry Packs + IoT + Vision CV
- P4 Multi-secteur (Banque, Pharma, Agro, Aéro, Auto, Public)
- P5 Excellence opérationnelle (federated learning, NLQ, marketplace)

## Licence

À définir.
