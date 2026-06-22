# Chaos Engineering — QualitOS

> CLAUDE.md §14.3 (« Chaos engineering : Litmus, Chaos Mesh ») · §2.1 (SLO
> 99,95 %, RTO < 1 h, RPO < 5 min) · §10 (architecture Kubernetes).

Expériences **Chaos Mesh** (`apiVersion: chaos-mesh.org/v1alpha1`) qui vérifient
la résilience de la plateforme face à des fautes réelles : perte de pod, latence
réseau vers la base, pression CPU/mémoire, latence I/O.

Le **runbook complet** (installation, exécution, observation, rollback) est dans
[`docs/runbooks/chaos-engineering.md`](../../docs/runbooks/chaos-engineering.md).

## Arborescence

```
infra/chaos/
├── experiments/                              # expériences unitaires
│   ├── 01-podchaos-quality-engine-kill.yaml      # PodChaos — kill réplique engine
│   ├── 02-networkchaos-engine-postgres-latency.yaml  # NetworkChaos — latence/perte → Postgres
│   ├── 03-stresschaos-engine-cpu-memory.yaml     # StressChaos — CPU + mémoire
│   └── 04-iochaos-ai-service-latency.yaml        # IOChaos (optionnel) — latence I/O ai-service
├── workflows/
│   └── resilience-gameday.yaml               # Workflow — enchaîne les 3 fautes principales
├── schedules/
│   └── weekly-pod-kill.yaml                  # Schedule (optionnel, staging) — pod-kill hebdo
└── validate.py                               # validation YAML + structure CRD (hors cluster)
```

## Catalogue des expériences

| # | Kind | Cible (label) | Hypothèse de résilience | Critère de réussite |
|---|------|---------------|--------------------------|----------------------|
| 01 | `PodChaos` (pod-kill) | `api-quality-engine` | replicas=2 → la perte d'une réplique ne rompt pas le service | 2/2 Ready < 2 min, 5xx < 5 %, p95 < 300 ms |
| 02 | `NetworkChaos` (delay+loss) | `api-quality-engine` → Postgres | pool Hikari + timeouts absorbent 300 ms / 10 % perte | pas de CrashLoop, retour nominal < 1 min |
| 03 | `StressChaos` (cpu+mem) | `api-quality-engine` | limites 2 CPU / 1536Mi → pas d'OOM, 2e réplique tient | pas d'OOMKill, 5xx < 5 %, retour < 1 min |
| 04 | `IOChaos` (latency) | `ai-service` | latence I/O ralentit l'inférence sans rendre `/health` KO | `/health` OK, appels coupés par disjoncteur engine |

Les hypothèses, scopes et critères détaillés figurent en en-tête de chaque
manifest (commentaires) et dans le runbook.

## Cohérence avec `infra/k8s`

Les `selector` ciblent le namespace **`qualitos`** et le label
**`app.kubernetes.io/name=<service>`**, qui est exactement le `matchLabels` posé
par le chart Helm (`infra/k8s/qualitos/templates/deployment.yaml`, ligne
`app.kubernetes.io/name: {{ $name }}`) à partir de `services.<name>`
(`infra/k8s/qualitos/values.yaml`). L'hôte Postgres ciblé
(`postgres.qualitos.svc.cluster.local`) provient de
`values.yaml` → `externalServices.postgresHost` / `services.api-quality-engine.env.DB_URL`.

## Validation locale (sans cluster)

```bash
python infra/chaos/validate.py
```

Vérifie : YAML parsable, `apiVersion` Chaos Mesh, kinds réels, champs
obligatoires par kind, namespace `qualitos`, et que chaque label
`app.kubernetes.io/name` correspond à un service réellement déployé.

> **L'exécution réelle requiert un cluster Kubernetes avec Chaos Mesh installé.**
> Aucune expérience ne s'exécute lors de la CI normale (cf. workflow
> `chaos.yml`, déclenché manuellement uniquement).
