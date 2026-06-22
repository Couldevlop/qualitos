# Runbook — Chaos Engineering QualitOS

> CLAUDE.md §14.3 (Chaos Mesh) · §2.1 (SLO 99,95 %, RTO < 1 h, RPO < 5 min) ·
> §10 (Kubernetes) · §10.2 / observabilité (Prometheus, Grafana).

Ce runbook décrit comment **installer**, **lancer**, **observer** et **arrêter**
les expériences Chaos Mesh de `infra/chaos/`. L'objectif est de valider
empiriquement que la plateforme tient ses SLO sous fautes réelles (perte de pod,
latence base, pression ressources, latence I/O).

> ⚠️ **Le chaos s'exécute sur un cluster de staging / kind, jamais en production
> sans garde-fou.** Les manifests de ce dépôt ne s'exécutent pas dans la CI
> normale ; seule la **validation syntaxique** (`infra/chaos/validate.py`) y tourne.

---

## 0. Pré-requis

| Outil | Rôle | Vérifier |
|---|---|---|
| Cluster K8s ≥ 1.31 (kind, minikube, k3d, ou staging réel) | héberger l'app + Chaos Mesh | `kubectl version` |
| Helm ≥ 3 | installer Chaos Mesh et le chart QualitOS | `helm version` |
| Chaos Mesh ≥ 2.6 | injecter les fautes | `kubectl get pods -n chaos-mesh` |
| Prometheus + Grafana | observer SLO pendant la faute | `docs/runbooks/observability.md` |

La plateforme doit être déployée dans le namespace **`qualitos`** via le chart
`infra/k8s/qualitos`, avec `api-quality-engine` en `replicas: 2`
(défaut `values.yaml`), sinon l'hypothèse de résilience #01/#03 n'a pas de sens.

### Créer un cluster kind local (exemple)

```bash
kind create cluster --name qualitos-chaos
kubectl create namespace qualitos
helm upgrade --install qualitos infra/k8s/qualitos -n qualitos
```

---

## 1. Installer Chaos Mesh (Helm)

```bash
helm repo add chaos-mesh https://charts.chaos-mesh.org
helm repo update
kubectl create namespace chaos-mesh

# Runtime containerd par défaut (kind/k3d). Adapter socketPath si Docker/CRI-O.
helm upgrade --install chaos-mesh chaos-mesh/chaos-mesh \
  -n chaos-mesh \
  --set chaosDaemon.runtime=containerd \
  --set chaosDaemon.socketPath=/run/containerd/containerd.sock

kubectl get pods -n chaos-mesh   # attendre Running (controller, daemon, dashboard)
```

Dashboard (optionnel) :

```bash
kubectl port-forward -n chaos-mesh svc/chaos-dashboard 2333:2333
# → http://localhost:2333
```

---

## 2. Valider les manifests AVANT d'injecter

Validation hors cluster (YAML + structure CRD + cohérence des selectors avec
`infra/k8s`) :

```bash
python infra/chaos/validate.py
```

Vérification côté serveur (dry-run, ne crée rien — nécessite que les CRD Chaos
Mesh soient installées, étape 1) :

```bash
kubectl apply --dry-run=server -f infra/chaos/experiments/
```

---

## 3. Définir le steady-state (avant la faute)

Avant toute injection, capturer l'état nominal pour pouvoir comparer :

```bash
kubectl get pods -n qualitos -l app.kubernetes.io/name=api-quality-engine
# Attendu : 2/2 pods Running + Ready
```

Dans Grafana (dashboard « QualitOS — Vue d'ensemble services »,
`docs/runbooks/observability.md`), noter : p95 latence (< 300 ms), taux 5xx
(< 5 %), pool Hikari, `up` par service.

---

## 4. Lancer une expérience

Chaque manifest s'applique avec `kubectl apply`. Les expériences à `duration`
fixée s'auto-terminent et restaurent l'état ; `PodChaos pod-kill` est instantané.

| Expérience | Commande | Ce qu'on observe |
|---|---|---|
| #01 pod-kill engine | `kubectl apply -f infra/chaos/experiments/01-podchaos-quality-engine-kill.yaml` | un pod disparaît, le Deployment en recrée un (≤ 2 min), aucun 5xx |
| #02 latence → Postgres | `kubectl apply -f infra/chaos/experiments/02-networkchaos-engine-postgres-latency.yaml` | p95 grimpe, pool Hikari se sature, **pas** de CrashLoop |
| #03 stress CPU/mem | `kubectl apply -f infra/chaos/experiments/03-stresschaos-engine-cpu-memory.yaml` | CPU réplique ↑, pas d'OOMKill, 2e réplique tient le trafic |
| #04 latence I/O ai-service | `kubectl apply -f infra/chaos/experiments/04-iochaos-ai-service-latency.yaml` | inférence ralentie, `/health` reste OK, disjoncteur engine coupe |
| Workflow game day | `kubectl apply -f infra/chaos/workflows/resilience-gameday.yaml` | enchaîne #01 → #02 → #03 avec pauses d'observation |
| Schedule hebdo (staging) | `kubectl apply -f infra/chaos/schedules/weekly-pod-kill.yaml` | pod-kill chaque lundi 03:00 |

Suivre l'état de l'expérience :

```bash
kubectl get podchaos,networkchaos,stresschaos,iochaos,workflow,schedule -n qualitos
kubectl describe podchaos quality-engine-pod-kill -n qualitos   # events d'injection
```

---

## 5. Observer (critères de réussite)

Pendant et après chaque faute, confronter aux **critères** du catalogue
(`infra/chaos/README.md`) :

| Signal | Où | Seuil (SLO §2.1 / §20) |
|---|---|---|
| Pods Ready | `kubectl get pods -n qualitos -l app.kubernetes.io/name=api-quality-engine` | retour 2/2 < 2 min (RTO local) |
| Taux 5xx | Grafana panel 5xx / alerte `ApiErrorRateHigh` | < 5 % |
| p95 latence | Grafana panel « Latence p95 » | < 300 ms en régime nominal |
| Pool Hikari | Grafana panel « Pool JDBC Hikari » | saturation tolérée pendant #02, pas après |
| Redémarrages | `kubectl get pods … -o wide` (colonne RESTARTS) | 0 redémarrage non attendu (pas de CrashLoop/OOM) |

Une expérience est **concluante** si l'hypothèse tient (le service dégrade
gracieusement et récupère). Sinon → ouvrir une NC/CAPA résilience et corriger
(probes, timeouts JDBC, limites ressources, HPA).

---

## 6. Arrêter / rollback

Le chaos est piloté par CRD : **supprimer l'objet = arrêter et restaurer**.

```bash
# Arrêt d'une expérience précise
kubectl delete -f infra/chaos/experiments/02-networkchaos-engine-postgres-latency.yaml

# Arrêt en urgence de TOUT le chaos du namespace
kubectl delete podchaos,networkchaos,stresschaos,iochaos,workflow,schedule \
  --all -n qualitos
```

> Chaos Mesh **recover** automatiquement les fautes à la suppression de la
> ressource ou à l'expiration de `duration`. La latence réseau, le stress et la
> latence I/O sont retirés ; un pod tué est déjà remplacé par le Deployment.

Si le contrôleur Chaos Mesh est lui-même indisponible et qu'une faute persiste,
désinstaller le chart force le nettoyage :

```bash
helm uninstall chaos-mesh -n chaos-mesh
```

Détruire le cluster jetable :

```bash
kind delete cluster --name qualitos-chaos
```

---

## 7. Exécution en CI (manuelle, optionnelle)

Le workflow [`.github/workflows/chaos.yml`](../../.github/workflows/chaos.yml) :

- est déclenché **uniquement** par `workflow_dispatch` (jamais sur `push`/`pull_request`)
  → le chaos **ne bloque pas** la CI normale (CLAUDE.md §14.2) ;
- en mode `validate` (défaut) : exécute `infra/chaos/validate.py` (toujours sûr,
  sans cluster) ;
- documente, en commentaires, comment un job pourrait monter un cluster éphémère
  (kind), installer Chaos Mesh et lancer une expérience sur un environnement
  dédié — non activé par défaut pour ne pas dépendre d'un runner privilégié.

---

## Références

- Manifests : `infra/chaos/` (+ `infra/chaos/README.md`).
- ADR : `docs/adr/0042-chaos-engineering.md`.
- Observabilité : `docs/runbooks/observability.md`.
- Chart applicatif : `infra/k8s/qualitos/`.
