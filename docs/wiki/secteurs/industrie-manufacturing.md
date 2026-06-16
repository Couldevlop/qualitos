# Secteur — Industrie / Manufacturing

[← Retour aux secteurs](README.md) · Pack : **`manufacturing`**

## Enjeux qualité du secteur

L'industrie manufacturière doit tenir une **qualité produit constante en série**, maîtriser ses
procédés statistiquement (SPC, capabilité), réduire le rebut et la reprise, et relier les défauts
au **shop-floor** (machines, capteurs). C'est là que QualitOS se distingue des QMS classiques :
les données terrain (MES, capteurs) alimentent directement la qualité, et un signal SPC ou une
anomalie peut déclencher une non-conformité.

## Normes pertinentes (Standards Hub)

- **ISO 9001** — Système de management de la qualité (socle).
- **IATF 16949** — Exigences automobile (APQP, PPAP, MSA, SPC, plans de contrôle).

Opérationnalisez-les dans le [Standards Hub](../modules/standards-hub.md) : dossier de
certification, roadmap, bibliothèque documentaire, audit blanc IA.

## KPIs clés (apportés par le pack)

| KPI | Sens |
|---|---|
| **OEE** (TRS) | Disponibilité × Performance × Qualité de l'équipement |
| **First Pass Yield** | Part produite bonne du premier coup |
| **Scrap rate** / **Rework rate** | Taux de rebut / reprise (coût de non-qualité) |
| **Customer reject rate (PPM)** | Pièces rejetées client par million |
| **MTBF / MTTR** | Fiabilité et délai de remise en service des équipements |
| **LPA score** | Score d'audit en couches (Layered Process Audit) |

## Modules QualitOS recommandés

- **[SPC](../modules/spc.md)** (`/spc`) : cartes de contrôle + 8 règles de Nelson, capabilité Cp/Cpk.
- **[DMAIC + Poka-Yoke](../modules/dmaic.md)** (`/dmaic`) : projets Six Sigma ; dispositifs
  Poka-Yoke shop-floor (détrompeurs, shadow board, capteur de présence outillage).
- **[Détection d'anomalies](../modules/anomaly.md)** (`/anomaly`) : dérive multivariée des signaux.
- **[5S](../modules/fives.md)** (`/fives`) : audits terrain mobiles (`audit-lpa-shopfloor`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : templates `dérive-machine`,
  `encombrement-atelier`.
- **[NC](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : `capa-manufacturing-default`.

## Connecteurs IoT typiques

`OPC-UA` (PLC Siemens/Schneider/Rockwell), `MQTT`, `Sparkplug B`, `Modbus TCP`, `MES générique`.

## Exemple de parcours

1. Un capteur de vibration (OPC-UA) signale une dérive sur un cobot.
2. La [détection d'anomalies](../modules/anomaly.md) confirme un signal multivarié → une
   [NC](../modules/non-conformites.md) est ouverte automatiquement.
3. L'équipe construit un [Ishikawa](../modules/ishikawa.md) `dérive-machine` (branches 6M).
4. La cause-racine est traitée par une [CAPA](../modules/capa.md) ; un **Poka-Yoke** (capteur de
   présence outillage) est déployé pour rendre l'erreur impossible.
5. Le [SPC](../modules/spc.md) confirme le retour en contrôle ; le suivi alimente l'**OEE** et le
   **FPY**.
