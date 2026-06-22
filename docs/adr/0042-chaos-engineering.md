# ADR 0042 — Chaos Engineering (Chaos Mesh) pour la résilience

- **Statut** : Accepté
- **Date** : 2026-06-22
- **Owners** : @Couldevlop
- **Phase** : §14.3 (Qualité logicielle & DevSecOps — Chaos engineering)

## Contexte

CLAUDE.md §14.3 liste explicitement le **chaos engineering (Litmus, Chaos Mesh)**
comme pratique de qualité, mais ce reliquat était absent du dépôt. Les SLO §2.1
(disponibilité 99,95 %, RTO < 1 h, RPO < 5 min) et §20 (p95 API < 300 ms) ne
peuvent être tenus avec confiance sans **vérifier empiriquement** que la
plateforme dégrade gracieusement et récupère sous fautes réelles : perte de pod,
latence/perte réseau vers la base, pression CPU/mémoire, latence I/O.

Le déploiement cible est Kubernetes (§10), via le chart Helm `infra/k8s/qualitos`
(namespace `qualitos`, `api-quality-engine` en `replicas: 2`). L'observabilité
(Prometheus/Grafana, §10.2) fournit déjà les signaux de steady-state (p95, 5xx,
pool Hikari, `up`).

## Décision

Adopter **Chaos Mesh** (et non Litmus) comme outil de chaos, pour sa modélisation
**100 % déclarative par CRD** (`apiVersion: chaos-mesh.org/v1alpha1`), cohérente
avec l'approche GitOps du projet, et son intégration native Kubernetes/Prometheus.

Les expériences vivent sous **`infra/chaos/`** (à côté de `infra/k8s`), organisées
en `experiments/` (fautes unitaires), `workflows/` (enchaînements via le CRD
`Workflow`), `schedules/` (CRD `Schedule`, récurrence staging). Chaque manifest
porte, en commentaires d'en-tête, une **hypothèse de résilience** explicite
(steady-state), son **scope** (selector namespace + label), sa **durée** et son
**critère de réussite** rattaché à un SLO.

Expériences livrées :

| # | Kind | Cible | Faute | Hypothèse → critère |
|---|------|-------|-------|---------------------|
| 01 | `PodChaos` | `api-quality-engine` | `pod-kill` (1 réplique) | replicas=2 absorbe → 2/2 Ready < 2 min, 5xx < 5 % |
| 02 | `NetworkChaos` | engine → Postgres | delay 300 ms + loss 10 % | pool Hikari/timeouts absorbent → pas de CrashLoop, retour < 1 min |
| 03 | `StressChaos` | `api-quality-engine` | CPU 2 workers + 256 Mo | pas d'OOMKill, 2e réplique tient |
| 04 | `IOChaos` (opt.) | `ai-service` | latence I/O 100 ms | `/health` OK, disjoncteur engine coupe les appels lents |

Le **Workflow** `resilience-gameday` enchaîne 01 → 02 → 03 avec des pauses
d'observation (`Suspend`). Un **Schedule** optionnel rejoue le pod-kill chaque
semaine en staging.

**Cohérence des selectors** : tous les selectors ciblent le namespace `qualitos`
et le label `app.kubernetes.io/name=<service>` — exactement le `matchLabels`
produit par `infra/k8s/qualitos/templates/deployment.yaml` à partir de
`services.<name>` dans `values.yaml`. La cible Postgres
(`postgres.qualitos.svc.cluster.local`) provient de `externalServices.postgresHost`
/ `services.api-quality-engine.env.DB_URL`.

**Garde-fous CI (§14.2)** : le workflow GitHub `chaos.yml` est déclenché
**uniquement** par `workflow_dispatch` (jamais sur `push`/`pull_request`), donc le
chaos **ne bloque pas** la CI normale. Son mode par défaut exécute la seule
**validation syntaxique** (`infra/chaos/validate.py`) — sans cluster.

## Conséquences

- **Positives** : SLO/RTO vérifiables ; régressions de résilience détectables
  (Schedule) ; manifests versionnés et revus comme du code ; validation hors
  cluster intégrable en CI sans privilège.
- **Limites** : l'**exécution réelle requiert un cluster + Chaos Mesh installé**
  (staging/kind) ; ce dépôt ne fournit que les manifests + la validation
  syntaxique. L'exécution sur un cluster éphémère en CI est documentée mais non
  activée (nécessite un runner privilégié / nested K8s).
- **Sécurité** : aucun secret dans les manifests ; le chaos est réservé au
  staging ; suppression du CRD = rollback immédiat (recover automatique).

## Alternatives écartées

- **Litmus** : également cité §14.3, mais Chaos Mesh offre une couverture CRD plus
  homogène (Pod/Network/Stress/IO/Workflow/Schedule) et une meilleure intégration
  Prometheus pour corréler aux SLO existants. Litmus reste une option future.
- **Tests de résilience applicatifs uniquement** (Resilience4j) : déjà présents
  (disjoncteurs IA) mais ne couvrent pas les fautes d'infrastructure (pod, réseau,
  ressources) — complémentaires, pas substituables.

## Validation

`python infra/chaos/validate.py` → 6 manifests valides (apiVersion, kinds réels,
champs obligatoires, namespace `qualitos`, selectors cohérents avec `infra/k8s`).
Vérification serveur possible via `kubectl apply --dry-run=server` une fois les
CRD Chaos Mesh installées. Runbook : `docs/runbooks/chaos-engineering.md`.

> Note : entrée à ajouter manuellement dans `docs/adr/README.md` (non modifié ici).
