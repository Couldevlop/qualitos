# Configuration CI — QualitOS

## Variables et secrets GitHub à configurer

Aller dans **Settings → Secrets and variables → Actions** sur le repo.

### Repository variables

| Variable             | Valeur attendue | Effet                                                                                                          |
| -------------------- | --------------- | -------------------------------------------------------------------------------------------------------------- |
| `OWASP_DC_ENABLED`   | `true`          | Active le job **SCA (OWASP Dependency-Check)** sur les push `main`/`develop`. Désactivé tant que non défini.   |

### Repository secrets

| Secret           | Comment l'obtenir                                                                                          | Utilisé par              |
| ---------------- | ---------------------------------------------------------------------------------------------------------- | ------------------------ |
| `NVD_API_KEY`    | https://nvd.nist.gov/developers/request-an-api-key (gratuit, 5 min, requis depuis 2024 pour DC fiable)     | `sca-owasp` (CI)         |

> Sans `NVD_API_KEY` la base de vulnérabilités NVD met **45+ minutes** à se télécharger
> et atteint rapidement le quota anonyme (5 req/30s). Le job échoue ou se fait throttle.

## Activation pas à pas

1. Demander une clé NVD : https://nvd.nist.gov/developers/request-an-api-key
2. La copier dans `Settings → Secrets → New repository secret` → nom `NVD_API_KEY`.
3. Créer la variable `OWASP_DC_ENABLED = true` dans `Settings → Variables`.
4. Pousser un commit sur `develop` ou `main` → le job `sca-owasp` apparaît dans Actions.

## Branch protection (rappel)

Pour `main` :
- Require pull request reviews : **2 approvers**
- Require status checks : `Build, test & coverage`, `Frontend build & tests`, `SAST (Semgrep)`, `SCA (OWASP Dependency-Check)` (une fois activé)
- Require signed commits, Include administrators

Pour `develop` :
- Require pull request reviews : **1 approver**
- Require status checks : `Build, test & coverage`, `Frontend build & tests`
