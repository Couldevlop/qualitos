# Secteur — IT / ITSM / SaaS

[← Retour aux secteurs](README.md) · Pack : **`it-itsm`**

## Enjeux qualité du secteur

Les organisations IT et éditeurs SaaS pilotent la **qualité de service** : maîtrise des incidents
et des changements (ITIL 4), respect des SLA, résolution au premier contact, et pratiques **SRE**
(error budgets, uptime). Les post-mortems d'incident et le problem management nourrissent
l'amélioration continue.

## Normes pertinentes (Standards Hub)

- **ISO/IEC 20000-1** — Management des services IT.
- **ISO/IEC 27001** — Sécurité de l'information.

Le pack est aligné **ITIL 4** et pousse les pratiques **SRE** à côté du management classique.

## KPIs clés (apportés par le pack)

| KPI | Sens |
|---|---|
| **MTTR incidents** | Délai moyen de résolution |
| **MTBF IT** | Temps moyen entre pannes |
| **SLA respect rate** | Respect des engagements de service |
| **First Call Resolution** | Résolution au premier contact |
| **CSAT support** | Satisfaction du support |
| **Change Success Rate** / **Failed Changes** | Maîtrise des changements |
| **SRE error budget consumption** | Consommation du budget d'indisponibilité |
| **Uptime %** | Disponibilité du service |

## Modules QualitOS recommandés

- **[NC](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : post-mortems d'incident
  (`capa-incident-postmortem`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `incident-recurrent-cause`.
- **[PDCA](../modules/pdca.md)** (`/pdca`) : registre d'amélioration continue (CSI ITIL).
- **Change management** : workflow `change-management-itil`.
- **Poka-Yoke** : canary deploy, feature flag.
- **[Détection d'anomalies](../modules/anomaly.md)** (`/anomaly`) : dérive des métriques serveurs.

## Connecteurs IoT typiques

`ServiceNow`, `Jira Service Management`, `Prometheus` (métriques), `ELK` (logs). Sondes T°/HR salles
serveurs en option.

## Exemple de parcours

1. Une métrique (Prometheus) déclenche un incident ; le SLA et l'**error budget** sont suivis.
2. Après résolution, un post-mortem ouvre une [CAPA](../modules/capa.md)
   `capa-incident-postmortem`.
3. Un [Ishikawa](../modules/ishikawa.md) `incident-recurrent-cause` identifie la cause-racine.
4. Un changement (`change-management-itil`) est planifié, validé, déployé en **canary**.
5. Les KPIs **MTTR**, **Change Success Rate** et **uptime** mesurent l'amélioration.
