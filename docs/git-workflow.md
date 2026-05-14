# Git workflow — QualitOS

> Conforme à CLAUDE.md §14 (DevSecOps) et §18.2 (règles non négociables).

## Modèle de branches

QualitOS suit un **GitFlow allégé** :

```
main      ──●─────────────────●─────────────●──  (production, taggué vX.Y.Z)
             \               /             /
develop      ─●─●─●─●─●─●─●─●─●─●─●─●─●─●─●──    (intégration continue)
                \   \           \   \
feature/*        ●─●─●           ●─●─●           (travail en cours)
```

### Branches permanentes

| Branche   | Rôle                            | Protection                                              |
| --------- | ------------------------------- | ------------------------------------------------------- |
| `main`    | Production, code release-ready  | PR only · 2 reviewers · CI verte · signed commits       |
| `develop` | Intégration des features        | PR only · 1 reviewer minimum · CI verte                 |

### Branches temporaires

| Préfixe         | Origine | Cible    | Exemple                              |
| --------------- | ------- | -------- | ------------------------------------ |
| `feature/*`     | develop | develop  | `feature/standards-hub`              |
| `fix/*`         | develop | develop  | `fix/pdca-status-transition`         |
| `hotfix/*`      | main    | main + develop | `hotfix/security-cve-2026-xxxx`|
| `release/*`     | develop | main + develop | `release/0.2.0`                |
| `chore/*`       | develop | develop  | `chore/bump-spring-boot-3.6`         |
| `docs/*`        | develop | develop  | `docs/architecture-c4-niveau-3`      |

## Convention de nommage des commits

[Conventional Commits](https://www.conventionalcommits.org/) — la CI peut s'appuyer dessus pour générer le changelog (release-please).

```
<type>(<scope>): <sujet à l'impératif, ≤ 72 caractères>

<corps détaillé, optionnel, expliquant le POURQUOI>

<footer : refs / breaking change / co-author>
```

Types autorisés : `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `security`, `perf`, `ci`, `build`.

Scopes courants : `pdca`, `ishikawa`, `fives`, `capa`, `docs`, `audit`, `standards-hub`, `core`, `tenant`, `security`, `infra`, `ci`.

Exemples :

```
feat(ishikawa): support 7M and 8M diagram modes
fix(capa): block reject on resolved CAPA cases
chore(deps): bump spring-boot to 3.5.1
ci: add OWASP Dependency-Check on push to main/develop
```

## Cycle de vie d'une feature

```bash
# 1. Synchroniser develop
git checkout develop
git pull --ff-only origin develop

# 2. Créer la feature branch
git checkout -b feature/standards-hub

# 3. Coder + commiter en commits atomiques
git add ...
git commit -m "feat(standards-hub): bootstrap entity model"
git commit -m "feat(standards-hub): clause requirement evidence scoring"
git commit -m "test(standards-hub): cover all service branches"

# 4. Pousser et ouvrir une PR
git push -u origin feature/standards-hub
# → ouvrir PR feature/standards-hub → develop sur GitHub

# 5. Une fois la PR mergée (squash recommandé pour les petites features,
#    merge --no-ff pour les grosses pour préserver l'historique de branche)
git checkout develop
git pull --ff-only origin develop
git branch -d feature/standards-hub
```

## Cycle de release

```bash
# 1. Préparer la release depuis develop
git checkout develop && git pull --ff-only
git checkout -b release/0.2.0

# 2. Bump version, mettre à jour CHANGELOG, ADR
mvn versions:set -DnewVersion=0.2.0
git commit -am "chore(release): bump version to 0.2.0"

# 3. Merger dans main (PR avec ≥ 2 reviewers)
# 4. Tagger une fois mergé
git checkout main && git pull --ff-only
git tag -s -a v0.2.0 -m "Release 0.2.0"
git push origin v0.2.0   # → déclenche release.yml

# 5. Re-merger main vers develop pour aligner
git checkout develop
git merge --ff-only main
git push origin develop
```

## Hotfix

```bash
# Bug critique sur main → ne PAS attendre develop
git checkout main && git pull --ff-only
git checkout -b hotfix/cve-2026-xxxx

# ... fix + tests + commit ...

# PR vers main + PR vers develop (deux PR séparées) ou cherry-pick après merge main
git push -u origin hotfix/cve-2026-xxxx
```

## Règles non négociables

- ❌ **Jamais de force-push** sur `main` ou `develop`.
- ❌ **Jamais de merge direct** sans PR.
- ❌ **Jamais de `--no-verify`** (skip pre-commit hooks) sans accord explicite.
- ✅ **Tous les commits doivent être signés GPG** (`commit.gpgsign = true`).
- ✅ **CI verte** obligatoire avant merge — voir `.github/workflows/ci.yml`.
- ✅ **2 reviewers** pour `main`, 1 reviewer pour `develop` — voir `.github/CODEOWNERS`.
- ✅ **Couverture JaCoCo** ≥ 85 % lignes / 75 % branches (cf. `pom.xml`).
- ✅ **Aucune CVE Critique/Haute** dans les dépendances (cf. OWASP DC dans CI).

## Branch protection à configurer sur GitHub

Settings → Branches → Add rule, pour `main` puis pour `develop` :

- Require a pull request before merging
- Require approvals : 2 pour `main`, 1 pour `develop`
- Dismiss stale pull request approvals when new commits are pushed
- Require review from Code Owners
- Require status checks to pass before merging : `Build, test & coverage`, `SAST (Semgrep)`, `SCA (OWASP Dependency-Check)` (sur main)
- Require branches to be up to date before merging
- Require signed commits
- Include administrators
- Do not allow bypassing the above settings

## Aliases git utiles

```bash
git config --global alias.lg "log --graph --oneline --decorate --all"
git config --global alias.uncommit "reset --soft HEAD^"
git config --global alias.amend "commit --amend --no-edit"
```
