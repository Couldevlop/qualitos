# Activer le job SCA (OWASP Dependency-Check) en CI

> Le job `sca-owasp` de `ci.yml` est conditionné à `vars.OWASP_DC_ENABLED == 'true'`
> et requiert le secret `NVD_API_KEY`. Tant que les deux ne sont pas posés, il est
> **skipped** (comportement attendu — ce n'est pas une anomalie).

## Pourquoi une clé API

Depuis 2024, le NIST rate-limite drastiquement les téléchargements anonymes de la
base NVD : sans clé, le téléchargement initial (~2 h de données CVE) échoue ou
prend des heures. Avec clé : ~10 min, et le cache de l'action est réutilisé.

## Procédure (une fois, par le propriétaire du repo)

1. **Demander la clé** : https://nvd.nist.gov/developers/request-an-api-key
   (formulaire e-mail — la clé arrive par mail, lien d'activation à cliquer
   sous 7 jours).
2. **Poser le secret** : Settings → Secrets and variables → Actions →
   *New repository secret* → nom `NVD_API_KEY`, valeur = la clé.
3. **Activer le job** : onglet *Variables* → *New repository variable* →
   nom `OWASP_DC_ENABLED`, valeur `true`.
4. Pousser (ou relancer) un commit sur `develop`/`main` : le job `SCA (OWASP
   Dependency-Check)` doit passer de *skipped* à *success* (premier run long :
   téléchargement NVD ; suivants : cache).

Alternative : fournir la clé à l'assistant (session Claude Code) qui peut poser
le secret (chiffrement libsodium via l'API) et la variable en une commande.

## Comportement du job

- `--failOnCVSS 8` : échec si CVE ≥ 8.0 (haute/critique) — règle CLAUDE.md §18.2.1.
- Suppressions motivées dans `.dependency-check-suppressions.xml` (time-boxées).
- Rapport HTML en artefact (`owasp-dc-report`), rétention 30 j.
- Exécuté uniquement sur push `develop`/`main` (lent), pas sur les PR.

## État au 2026-06-06

- Secret `NVD_API_KEY` : ❌ absent
- Variable `OWASP_DC_ENABLED` : ❌ absente
- Job : *skipped* sur tous les runs (vérifié via l'API Actions)
