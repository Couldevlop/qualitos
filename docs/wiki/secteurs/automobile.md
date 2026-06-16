# Secteur — Automobile

[← Retour aux secteurs](README.md) · Pack : **`automotive`**

## Enjeux qualité du secteur

Les équipementiers automobiles (Tier 1 / Tier 2) et fournisseurs OEM répondent à des **exigences
client (CSR)** strictes : First Pass Yield élevé, PPM client très bas, maîtrise APQP/PPAP,
capabilité Cpk des caractéristiques critiques, résolution de problème en 8D, et audits de processus
VDA 6.3. Les cibles diffèrent selon l'OEM (Toyota, Renault, Stellantis, BMW).

## Normes pertinentes (Standards Hub)

- **IATF 16949** — Système de management qualité automobile.
- **VDA 6.3** — Audit de processus (constructeurs allemands).
- **ISO 9001** / **ISO 14001** / **ISO 45001** — Socles.

## KPIs clés (apportés par le pack)

| KPI | Cible | Sens |
|---|---|---|
| **First Pass Yield** | ≥ 99 % | Bon du premier coup (IATF §9.1, CSR Renault/Stellantis) |
| **Scrap rate** | < 0,5 % | Rebut (coût de non-qualité) |
| **Customer Reject (PPM)** | < 50 | Toyota < 10, Renault < 50, Stellantis < 100 |
| **PPAP On-Time** | ≥ 95 % | Maîtrise APQP/PPAP §8.3 |
| **8D Cycle Time (D0→D8)** | ≤ 60 j | Délai standard OEM (IATF §10.2.3) |
| **Cpk caractéristiques critiques** | ≥ 1,67 | Sécurité (Toyota/BMW exigent ≥ 2) |
| **VDA 6.3 Process Audit Score** | ≥ 90 % | < 90 % déclasse le fournisseur (A/B/C) |

## Modules QualitOS recommandés

- **[CAPA](../modules/capa.md)** + **[NC](../modules/non-conformites.md)** : résolution **8D**
  (`8d-resolution`), réclamations OEM.
- **[DMAIC + Poka-Yoke](../modules/dmaic.md)** (`/dmaic`) : projets capabilité, détrompeurs.
- **[SPC](../modules/spc.md)** (`/spc`) : cartes de contrôle live, Cp/Cpk.
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `Défaut dimensionnel`, `PPAP refusé`,
  `Dérive Cpk < 1,33`, `Réclamation OEM majeure`.
- **Poka-Yoke** : détrompeur anti-inversion, capteur de présence outillage (Andon), tool count RFID,
  vision IA couple de serrage, verrouillage cycle si MSA expirée.

## Connecteurs IoT typiques

`OPC-UA`, `MQTT`, `Sparkplug B`, `SAP ERP`, `MES générique`.

## Exemple de parcours

1. Une réclamation OEM majeure (NCM) arrive ; le KPI **Customer Reject PPM** alerte.
2. Une [CAPA](../modules/capa.md) **8D** (`8d-resolution`) est démarrée (objectif D0→D8 ≤ 60 j).
3. Un [Ishikawa](../modules/ishikawa.md) `Réclamation OEM majeure` cadre la cause-racine ; le
   [SPC](../modules/spc.md) confirme une dérive non détectée.
4. Un **Poka-Yoke** (détrompeur ou vision IA couple) rend l'erreur impossible.
5. Les KPIs **FPY**, **Cpk critiques** et **VDA 6.3 score** suivent l'efficacité ; le dossier
   [IATF 16949](../modules/standards-hub.md) est mis à jour.
