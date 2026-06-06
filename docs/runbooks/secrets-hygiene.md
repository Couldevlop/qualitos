# Hygiène des secrets — règles & outillage

> CLAUDE.md §18.2.3 : **aucun secret en clair** dans le repo. Défense en
> profondeur : prévention (gitignore/.env.example) → détection (gitleaks
> hook + CI) → distribution propre (Actions Secrets / Vault+ESO / SOPS) →
> réaction (rotation).

## Où mettre quoi

| Contexte | Mécanisme |
|---|---|
| Poste dev | `.env` local (jamais committé — `*.env` est dans `.gitignore`). Modèle : `.env.example` (sans valeurs). |
| CI GitHub Actions | **Actions Secrets** (chiffrés libsodium, masqués dans les logs). Ex. `NVD_API_KEY` — cf. `ci-sca-owasp-activation.md`. |
| Runtime K8s | **Vault + External Secrets Operator** — aucun secret dans les Helm values. |
| Fichier versionné qui doit contenir des valeurs sensibles | **SOPS** (age/KMS) : committé chiffré, diffs lisibles. |

## Détection — gitleaks

- **Hook pre-commit** (`.githooks/pre-commit`) : scanne le staged, bloque le
  commit. Activation une fois par clone :
  ```bash
  git config core.hooksPath .githooks
  ```
  Sans gitleaks installé, le hook prévient sans bloquer — la CI reste le filet.
- **Job CI `secrets-scan`** : gitleaks v8.21.2 (binaire pinné) sur
  l'**historique complet** à chaque push/PR (~10 s). Bloquant.
- **Config** : `.gitleaks.toml` — règles par défaut + allowlist motivée/datée
  (identifiants dev des compose, `.env.example`). Tout ajout d'allowlist se
  justifie en revue.
- **Audit initial** : historique complet (261 commits) scanné le 2026-06-06 —
  aucune fuite.

## Si un secret fuit malgré tout

1. **Révoquer / régénérer immédiatement** — un secret qui a touché un commit
   (ou un canal tiers) est compromis, même « supprimé » ensuite.
2. Purger l'historique (`git filter-repo --replace-text`) — hygiène, pas sécurité.
3. Re-scanner : `gitleaks git --log-opts="--all" -v`.
4. Tracer l'incident (NC interne) et vérifier les accès sur la période.
