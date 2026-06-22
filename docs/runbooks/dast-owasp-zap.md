# Runbook — DAST OWASP ZAP (local + CI)

> Scan de sécurité dynamique (DAST) de `api-quality-engine` avec OWASP ZAP.
> Décision d'architecture : **ADR 0039**. Job CI : **`dast-zap`** dans
> `.github/workflows/ci.yml`. Règles & faux positifs : **`.zap/rules.tsv`**.

## Ce que fait le scan

1. **Baseline** (`zaproxy/action-baseline`, passif + spider léger) sur la racine
   HTTP de l'engine — en-têtes de sécurité, fuite d'info, exposition actuator.
2. **API scan** (`zaproxy/action-api-scan`, actif ciblé) sur la spec OpenAPI
   `/v3/api-docs` — teste chaque opération déclarée.

**Seuil d'échec** : le job échoue sur toute alerte de risque **HIGH** non
ignorée (`-l FAIL`). Les **MEDIUM/LOW** sont rapportés en **WARN** (visibles, non
bloquants). Aligné sur la DoD §20 (« 0 critique, 0 haute »).

La stack auditée est **minimale** (`docker-compose.dast.yml`) : Postgres +
engine, **sans Keycloak**. La validation JWT étant paresseuse, l'engine boote et
répond `UP` sans IdP ; les routes protégées renvoient `401`. Le scan est donc
**non authentifié** (dette suivie dans l'ADR 0039 : scan authentifié ultérieur).

---

## Lancer en local

> Prérequis : Docker + docker-compose (v1 « tiret » sur le poste de dev).
> La stack DAST utilise des ports **décalés** (`8092`, `55432`) : elle cohabite
> avec `docker-compose.dev.yml` sans conflit.

### 1. Démarrer la stack éphémère et attendre la santé

```powershell
# Depuis la racine du repo. TEMP sur D: (C: saturé).
$env:TEMP="D:\tmp"; $env:TMP="D:\tmp"
docker-compose -f docker-compose.dast.yml up -d --build

# Attendre que l'engine soit UP (le 1er boot inclut Flyway + démarrage Spring) :
do {
  Start-Sleep 5
  $h = (Invoke-RestMethod http://localhost:8092/actuator/health -ErrorAction SilentlyContinue)
} until ($h.status -eq 'UP')
"Engine UP. OpenAPI :"; (Invoke-WebRequest http://localhost:8092/v3/api-docs).StatusCode
```

### 2. Lancer le baseline ZAP (mêmes règles que la CI)

```powershell
# -v monte le repo en /zap/wrk (ZAP y lit rules.tsv et y écrit les rapports).
# host networking n'existe pas sous Docker Desktop Windows → on cible
# host.docker.internal au lieu de localhost.
docker run --rm -v "${PWD}:/zap/wrk:rw" zaproxy/zap-stable zap-baseline.py `
  -t http://host.docker.internal:8092 `
  -c .zap/rules.tsv `
  -l FAIL `
  -r zap-baseline-report.html
```

Sous Linux/macOS (et en CI) on peut utiliser `--network host` et cibler
`http://localhost:8092` directement.

### 3. Lancer l'API scan sur l'OpenAPI

```powershell
docker run --rm -v "${PWD}:/zap/wrk:rw" zaproxy/zap-stable zap-api-scan.py `
  -t http://host.docker.internal:8092/v3/api-docs `
  -f openapi `
  -c .zap/rules.tsv `
  -l FAIL `
  -r zap-api-scan-report.html
```

### 4. Démonter

```powershell
docker-compose -f docker-compose.dast.yml down -v
```

Les rapports `zap-baseline-report.html` / `zap-api-scan-report.html` sont à la
racine du repo (les ouvrir dans un navigateur). **Ne pas les committer** (déjà
couverts par le `.gitignore`, cf. ci-dessous).

---

## En CI

Le job **`dast-zap`** (`.github/workflows/ci.yml`) :

- s'exécute sur push `main`/`develop` et sur les PR **internes** (pas les forks) ;
- est **100 % autonome** (aucun secret : images publiques ZAP, pas de création
  d'issue GitHub — `allow_issue_writing: false`) ;
- publie les artefacts `zap-baseline-report` et `zap-api-scan-report` (HTML + MD
  + JSON), plus `dast-stack-log` (logs de la stack si échec).

Récupérer un rapport : onglet **Actions** → run → section **Artifacts**.

---

## Traiter un finding

1. **Reproduire en local** (étapes ci-dessus) pour isoler l'alerte. Le rapport
   HTML donne : le `pluginId`, l'URL, le risque (High/Medium/Low), la requête/
   réponse et une remédiation.
2. **Vrai positif** → **corriger l'application** (ajouter l'en-tête manquant,
   restreindre l'endpoint, masquer la bannière…). Ne JAMAIS faire taire un vrai
   positif via `rules.tsv`.
3. **Faux positif avéré** → ajouter une ligne dans **`.zap/rules.tsv`** :
   ```
   <pluginId>\tIGNORE\t# <date> <justification>
   ```
   (séparateurs = **TABULATIONS**). Justification + date obligatoires (revue à
   chaque montée de version de ZAP). Exemple existant : `10096` (timestamps
   actuator), `10027` (commentaires OpenAPI).
4. **Médium à promouvoir bloquant** : passer la ligne en `FAIL` dans `rules.tsv`
   une fois le correctif disponible (ex. durcissement CSP/COOP — voir ADR 0039).

### Identifier le pluginId

Le rapport HTML l'affiche en tête de chaque alerte. Liste de référence :
<https://www.zaproxy.org/docs/alerts/>.

---

## Pièges connus

- **`rules.tsv` muet** : le fichier doit être séparé par des **TABULATIONS**, pas
  des espaces — sinon ZAP ignore la ligne silencieusement.
- **Windows / Docker Desktop** : pas de `--network host` ; cibler
  `host.docker.internal:8092`. En CI Linux, `localhost` fonctionne (l'action ZAP
  utilise le réseau hôte).
- **Engine `DOWN` au scan** : le 1er boot prend ~30-60 s (Flyway). Toujours
  attendre `/actuator/health = UP` avant de lancer ZAP, sinon le baseline ne voit
  qu'une page d'erreur.
- **Port occupé** : `8092`/`55432` choisis pour ne PAS toucher la stack dev
  (`8082`, `5434`, `4200`). Ne pas réaligner sur les ports dev.
