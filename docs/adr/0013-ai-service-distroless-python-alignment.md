# ADR 0013 — ai-service : image distroless alignée sur Python 3.11 (sans venv)

- **Statut** : Accepté
- **Date** : 2026-05-26
- **Owners** : @Couldevlop

## Contexte

L'image Docker de `apps/ai-service` (passerelle IA FastAPI) ne démarrait pas :

```
exec: "/opt/venv/bin/python": stat /opt/venv/bin/python: no such file or directory
```

Cause racine — **incohérence de version Python** entre les deux étages du build :

- `pyproject.toml` exigeait `requires-python = ">=3.13"` et le builder utilisait
  `python:3.13-slim`, qui crée un virtualenv pointant vers un interpréteur **3.13**.
- L'étage runtime est `gcr.io/distroless/python3-debian12`, qui embarque
  **Python 3.11**.

Un `venv` ne contient pas l'interpréteur : `…/bin/python` est un lien (ou un binaire
copié) vers le Python de base. Copié tel quel dans le distroless, ce lien pointe vers
un interpréteur 3.13 **absent** du runtime → le conteneur ne peut pas démarrer. C'est
le piège classique « venv + distroless + versions Python divergentes ».

Le venv est de toute façon superflu dans une image distroless mono-application : il
n'y a rien à isoler, et il introduit ce point de fragilité.

## Décision

Aligner le build de l'ai-service sur l'interpréteur du runtime distroless et
**supprimer le venv** :

1. **Builder** = `python:3.11-slim-bookworm` — même version mineure (3.11) et même
   base (Debian *bookworm*) que `distroless/python3-debian12`, garantissant des
   wheels compilées **ABI-compatibles** (`pydantic-core`, `psycopg`, `spacy`…).
2. **Dépendances** installées avec `pip install --target=/deps` (pas de venv). Le
   code applicatif est copié dans `/app` et exécuté via `PYTHONPATH=/app:/deps` par
   **l'interpréteur natif du distroless** (entrypoint Python par défaut de l'image).
3. `requires-python` relâché à `">=3.11"` dans `pyproject.toml` (cohérent avec le
   runtime ; aucune syntaxe spécifique à 3.12/3.13 n'est utilisée dans le code).
4. **Distroless conservé** (CLAUDE.md §10.2) : surface d'attaque minimale, exécution
   en `nonroot`.

5. **Presidio exclu de l'image** : `presidio-analyzer/anonymizer` ne sont **pas**
   installés. Presidio charge le modèle spaCy `en_core_web_lg` et, absent, déclenche
   un **auto-download via la CLI spaCy** qui lève un `SystemExit` — non rattrapé par
   le `except Exception` de `PresidioPiiFilter.__init__` → crash au démarrage. En
   revanche, **package absent** ⇒ `ImportError` (rattrapé) ⇒ bascule propre sur
   `HeuristicPiiFilter`. On retient donc l'absence de Presidio dans l'image de dev/démo
   (filtre PII heuristique), conforme à l'intention « optional in dev » du `pyproject`.
   Healthcheck via `GET /healthz`.

## Justification

- **Aligner builder ↔ runtime sur 3.11** est la racine du correctif : tout le reste
  (venv, `--copies`) ne fait que contourner le symptôme.
- **Sans venv + `--target`** : l'image utilise l'interpréteur garanti présent du
  distroless ; plus aucun lien d'interpréteur à casser.
- **Alternatives écartées** :
  - *Runtime `python:3.11-slim` (non distroless)* → ferait démarrer le conteneur mais
    **viole CLAUDE.md §10.2** (distroless obligatoire) et augmente la surface d'attaque.
  - *Base distroless Python 3.13* → inexistante (`python3-debian12` = 3.11).
  - *`python -m venv --copies`* → copie un binaire 3.13 dont les libs partagées
    (`libpython3.13`) sont absentes du distroless 3.11 : toujours cassé.

## Conséquences

- ✅ L'image démarre sur le distroless ; `GET /healthz` répond.
- ✅ Reste distroless + nonroot, conforme §10.2.
- ⚠ La liste des dépendances runtime est **dupliquée** dans le Dockerfile (au lieu de
  `pip install .`) pour éviter l'ambiguïté du *flat-layout* setuptools (plusieurs
  packages top-level). Dette à résorber : générer un `requirements.txt` figé depuis
  `pyproject.toml` (`pip-compile`/`uv`) et l'installer via `--target`.
- ⚠ Tout passage à une syntaxe Python 3.12+/3.13+ imposera de remonter conjointement
  le builder ET la base distroless (qui devra exister pour cette version).
- ℹ Les dépendances lourdes ML (`torch`, `transformers`…) restent dans l'extra
  optionnel `[ml]`, **non installé** dans l'image (embeddings/LLM délégués à Ollama).
- ⚠ Détection PII en mode **heuristique** (Presidio exclu) dans l'image de dev/démo.
  Suivi prod : réintégrer Presidio en **embarquant le modèle spaCy au build** (où pip
  existe), p. ex. `python -m spacy download en_core_web_lg` puis copie dans `/deps`,
  pour supprimer toute tentative de téléchargement au runtime distroless.

## Tests d'invariant

- Build + run : `docker-compose -f docker-compose.dev.yml up -d --build ai-service`
  puis `GET http://localhost:8085/healthz` → 200.
- Le Dockerfile builder et la base runtime doivent référencer la **même** version
  mineure de Python (grep `3.11` cohérent dans `apps/ai-service/Dockerfile`).
- `requires-python` dans `pyproject.toml` ≥ version de la base distroless.

## Références

- CLAUDE.md §10.2 (images distroless signées, surface minimale), §12.2 (architecture IA).
- ADR [0011](./0011-pq-crypto-agility-signing.md) (crypto), [0012](./0012-blockchain-anchoring-fabric.md) (ancrage).
- `apps/ai-service/Dockerfile`, `apps/ai-service/pyproject.toml`.
