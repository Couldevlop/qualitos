# ADR 0039 — DAST OWASP ZAP intégré à la CI

- **Statut** : Accepté
- **Date** : 2026-06-22
- **Owners** : @Couldevlop

## Contexte

Le pipeline CI/CD cible (CLAUDE.md §14.2) prévoit explicitement une étape
**`DAST (OWASP ZAP)`** entre les tests d'intégration et la signature Cosign, et
la Definition of Done globale (§20) exige « SAST + SCA + **DAST** + scan image :
0 critique, 0 haute ». Or `ci.yml` couvrait déjà le SAST (Semgrep), la SCA
(OWASP Dependency-Check), le scan d'image (Trivy) et le scan de secrets
(gitleaks), mais **pas le DAST** : c'était le dernier maillon manquant du
reliquat §11/§14.

Le DAST (Dynamic Application Security Testing) audite l'application **en cours
d'exécution** (HTTP réel) là où le SAST analyse le code statique. Il attrape des
classes de défauts invisibles au statique : en-têtes de sécurité absents
(CSP, HSTS, X-Content-Type-Options), fuite d'information dans les réponses,
endpoints actuator/openapi trop exposés, comportements de cache, méthodes HTTP
permissives, divulgation de bannières serveur.

Contraintes propres au projet :

- **api-quality-engine** est un *resource server* OAuth2 : la quasi-totalité des
  routes exige un JWT. Un scan **non authentifié** n'explore donc pas la logique
  métier — mais il **doit** auditer la surface publique (actuator exposé,
  `/v3/api-docs`, `/swagger-ui.html`) et vérifier que les routes protégées
  répondent bien `401` et non `200`/`500`.
- La CI ne dispose **d'aucun secret** garanti (forks, premières PR) : le job doit
  être **100 % autonome**, sans clé API ni credential externe.
- Le build de l'image engine est lourd (Maven multi-module) : on réutilise le
  Dockerfile distroless existant via un **compose CI dédié minimal**.

## Décision

1. **Job CI `dast-zap`** ajouté à `.github/workflows/ci.yml`. Il :
   - démarre une stack **minimale éphémère** via `docker-compose.dast.yml`
     (Postgres + api-quality-engine, **sans Keycloak**) ;
   - attend `/actuator/health = UP` (boucle `curl` depuis le runner, max 5 min) ;
   - vérifie que `/v3/api-docs` répond `200` (la cible du scan API) ;
   - lance **deux scans ZAP** via les actions officielles
     `zaproxy/action-baseline` (passif, racine HTTP) et `zaproxy/action-api-scan`
     (actif, ciblé sur l'OpenAPI) ;
   - publie les rapports (HTML/MD/JSON) en artefacts et démonte la stack.

2. **Stack DAST sans Keycloak.** La validation JWT (`jwk-set-uri`) de Spring
   Security est **paresseuse** : le JWKS n'est récupéré qu'à la première
   validation d'un token. L'engine boote et répond `UP` sans IdP joignable. Un
   scan non authentifié reçoit `401` sur les routes protégées — exactement le
   comportement à auditer. On évite ainsi de démarrer (et seeder le realm)
   Keycloak juste pour un baseline, ce qui divise par deux le temps de boot et
   la surface de panne CI.

   > Trade-off assumé : le DAST ne couvre pas les chemins **post-authentification**.
   > Un scan authentifié (token de service injecté) est un chantier ultérieur
   > (cf. « Conséquences »). Le baseline non authentifié est la pratique standard
   > et déjà à forte valeur (en-têtes, exposition, fuite d'info).

3. **Ports décalés** dans `docker-compose.dast.yml` (`8092→8082`, `55432→5432`,
   réseau `qualitos-dast`, base en `tmpfs`) pour que la stack DAST tourne **en
   parallèle** de `docker-compose.dev.yml` sur un poste développeur sans collision
   de ports ni de volumes.

4. **Règles & faux positifs** dans `.zap/rules.tsv` (TSV `pluginId / action`).
   - **Seuil d'échec** : `-l FAIL` côté CLI → le job échoue sur toute alerte de
     risque **HIGH** non ignorée ; les **MEDIUM/LOW** sont rapportés en **WARN**
     (visibles dans le rapport, non bloquants). Aligné sur §20 (0 haute/critique).
   - Chaque `IGNORE` est **motivé et daté** (faux positifs assumés : timestamps
     actuator, commentaires de la spec OpenAPI, sémantique de cache API).
   - Un faux positif se traite **dans ce fichier**, jamais en désactivant le scan.

5. **Aucun secret requis.** Images publiques (`zaproxy/*`,
   `ghcr.io/zaproxy/action-*`), `allow_issue_writing: false` (pas de création
   d'issue GitHub → pas de permission `issues:write`, pas de bruit), `permissions:
   contents: read` au niveau du job. Le job tourne sur les push `main`/`develop`
   et sur les PR **internes** (pas sur les forks, cohérent avec `image-scan`).

## Conséquences

### Positives
- Reliquat §14.2 / §20 soldé : DAST présent, réellement exécutable, bloquant sur
  HIGH.
- Autonome (aucun secret), reproductible en local à l'identique
  (`docs/runbooks/dast-owasp-zap.md`).
- Non invasif : nouveau job isolé, aucune modification des jobs existants, stack
  éphémère démontée en fin de run.

### Négatives / dette
- **Scan non authentifié** : pas de couverture des chemins métier
  post-login. Dette suivie : injecter un token de service (client_credentials
  Keycloak) dans une config ZAP `replacer` pour un scan authentifié — nécessite
  de démarrer Keycloak dans la stack DAST.
- Coût CI : ~build engine + ~5-8 min de scan. Acceptable (job parallèle des
  autres), `timeout-minutes: 35`.
- Le durcissement des en-têtes de sécurité de l'API (CSP, COOP/COEP) reste en
  **WARN** : à promouvoir en `FAIL` une fois le hardening tranché (threat model
  api-quality-engine).

## Alternatives écartées

- **ZAP full active scan** : trop long et bruyant pour la CI sur chaque push ;
  le baseline + API scan ciblé offre le meilleur ratio signal/temps.
- **Scanner l'image via une stack complète `docker-compose.dev.yml`** : démarre
  6+ services (Keycloak, Qdrant, Ollama…) inutiles au DAST → lent et fragile.
- **DAST sur un environnement de staging déployé** : pas encore disponible ;
  l'approche éphémère in-CI est self-contained et ne dépend d'aucune infra.
