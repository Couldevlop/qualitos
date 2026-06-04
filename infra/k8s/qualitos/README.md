# Chart Helm QualitOS

Déploie le frontend Angular, les APIs Spring Boot (`api-core`,
`api-quality-engine`, `api-iot-hub`), la passerelle IA FastAPI (`ai-service`)
et, en option, `blockchain-service` (Hyperledger Fabric, ADR 0012).

## Prérequis (hors chart)

| Dépendance | Recommandation |
|---|---|
| PostgreSQL 17 (+ bases `qualitos_core`, `qualitos_quality`, `qualitos_iot`) | Service managé (RDS/Cloud SQL) ou chart Bitnami |
| Keycloak 25 (realm `qualitos`, import `infra/keycloak/realm-export.json`) | Chart Bitnami / opérateur Keycloak |
| Kafka (si `QUALITOS_KAFKA_ENABLED=true`) | Strimzi / managé |
| Qdrant + Ollama (chemins IA) | charts officiels |
| External Secrets Operator + Vault | obligatoire — aucun secret en clair (§18.2.3) |
| ingress-nginx + cert-manager | TLS de l'ingress |

## Secrets attendus (créés par ESO depuis Vault)

- `qualitos-api-core`, `qualitos-api-quality-engine`, `qualitos-api-iot-hub` :
  `DB_USER` / `DB_PASSWORD` (ou `IOT_DB_USERNAME`/`IOT_DB_PASSWORD`).
- `qualitos-ai-service` : `ANTHROPIC_API_KEY`, `MISTRAL_API_KEY`, `NLQ_READONLY_DSN`.

## Installation

```bash
helm upgrade --install qualitos infra/k8s/qualitos \
  --namespace qualitos --create-namespace \
  --set global.imageTag=v0.1.0 \
  --set ingress.host=qualitos.mondomaine.com
```

Vérification : `kubectl get pods -n qualitos` puis
`https://<host>/actuator/health` derrière l'ingress (`/api` →
api-quality-engine).

## Observabilité

- Annotations `prometheus.io/*` posées sur chaque pod Spring Boot
  (`/actuator/prometheus`).
- Avec Prometheus Operator : `--set prometheus.serviceMonitor.enabled=true`.
- Profil Spring `prod` actif par défaut → logs JSON une-ligne (Loki/OpenSearch).

## Sécurité

- `runAsNonRoot`, rootfs read-only (+ `/tmp` emptyDir pour la JVM),
  `capabilities: drop ALL`, seccomp `RuntimeDefault`.
- NetworkPolicies default-deny ingress + intra-namespace + ingress-controller.
- `automountServiceAccountToken: false` partout.

## GitOps

Chart pensé pour ArgoCD (§14.2) : pointer une Application sur
`infra/k8s/qualitos` avec un values d'environnement par cluster.
