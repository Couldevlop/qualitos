# Secteur — Énergie / Utilities

[← Retour aux secteurs](README.md) · Pack : **`energy`**

## Enjeux qualité du secteur

Les acteurs de l'énergie et des utilities (électricité, eau, gaz, renouvelables) pilotent la
**performance énergétique** (EnPI vs ligne de base), la **gestion d'actifs critiques** (disponibilité,
MTBF, maintenance prédictive) et la **cybersécurité OT** réglementée (NIS 2, segmentation IT/OT).

## Normes pertinentes (Standards Hub)

- **ISO 50001** — Management de l'énergie.
- **ISO 55001** — Gestion d'actifs.
- **NIS 2** — Cybersécurité des entités essentielles.
- **CIS Controls** — Contrôles de sécurité critiques.
- **ISO 9001** / **ISO 45001** — Socles.

## KPIs clés (apportés par le pack)

| KPI | Cible | Sens |
|---|---|---|
| **EnPI** | tendance ↓ | Performance énergétique vs activité (ISO 50001 §6.4) |
| **Écart à la ligne de base (EnB)** | ≤ 0 | Dérive de consommation vs baseline ajustée |
| **Disponibilité des actifs critiques** | ≥ 99 % | ISO 55001 |
| **MTBF équipements critiques** | ≥ 8760 h | Fiabilité |
| **Incidents critiques infrastructure** | 0 | Coupure, fuite, cyber |
| **NIS 2 — Notification sous 24h** | 100 % | Alerte précoce (NIS 2 Art. 23) |

## Modules QualitOS recommandés

- **[NC](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : incidents, fuites,
  cybersécurité (`nis2-incident`, `maintenance-predictive`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `Dérive EnPI`, `Indisponibilité actif`,
  `Incident cyber SCADA (NIS 2)`, `Coupure non planifiée`, `Fuite réseau`.
- **[PDCA](../modules/pdca.md)** (`/pdca`) : revue énergétique (`revue-energetique`).
- **[Détection d'anomalies](../modules/anomaly.md)** (`/anomaly`) : dérive capteurs, maintenance
  prédictive (RUL).
- **Poka-Yoke** : LOTO (consignation), seuil d'alarme dérive EnPI, segmentation IT/OT (diode de
  données), arrêt automatique sur dépassement de seuil critique.

## Connecteurs IoT typiques

`SCADA`, `IoT`, `MQTT`, `OPC-UA`, `CMMS Maximo` (GMAO). Compteurs, smart grid.

## Exemple de parcours

1. Un capteur de vibration (SCADA/OPC-UA) annonce une dérive ; la
   [détection d'anomalies](../modules/anomaly.md) estime une **RUL** courte.
2. Une [NC](../modules/non-conformites.md) déclenche une maintenance préventive
   (`maintenance-predictive`) avant la panne.
3. En parallèle, un incident cyber OT ouvre une [CAPA](../modules/capa.md) `nis2-incident` ; la
   notification **sous 24h** est suivie.
4. Un [Ishikawa](../modules/ishikawa.md) `Incident cyber SCADA` cadre l'analyse ; un **Poka-Yoke**
   (diode de données IT/OT) renforce la segmentation.
5. Les KPIs **disponibilité actifs**, **MTBF** et **EnPI** pilotent performance et fiabilité.
