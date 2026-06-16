# Secteur — Agro-alimentaire

[← Retour aux secteurs](README.md) · Pack : **`agro`**

## Enjeux qualité du secteur

L'agro-alimentaire pilote la **sécurité des denrées** : maîtrise des points critiques (HACCP / CCP),
programmes prérequis (PRP), gestion des allergènes, traçabilité amont/aval rapide (rappel produit),
conformité des matières premières (certificats d'analyse). La chaîne du froid est surveillée en
continu.

## Normes pertinentes (Standards Hub)

- **ISO 22000** — Sécurité des denrées alimentaires.
- **FSSC 22000** — Schéma de certification GFSI.
- **IFS Food v8** — Standard distributeurs européens.
- **BRCGS Food v9** — Standard britannique.
- **GlobalG.A.P.** — Bonnes pratiques agricoles.
- **HACCP** — Codex Alimentarius.
- **ISO 9001** / **ISO 14001** — Socles SMQ / environnement.

## KPIs clés (apportés par le pack)

| KPI | Cible | Sens |
|---|---|---|
| **HACCP CCP Excursions / mois** | 0 | Suivi temps réel via sondes (ISO 22000 §8.7) |
| **Product Recalls / an** | 0 | Rappels produit |
| **Traceability Recall Time** | < 2 h | Traçabilité amont/aval (IFS §5.9 : 4h max) |
| **Allergen Cross-Contamination Complaints** | 0 | Réclamations allergènes (risque vital) |
| **PRP Audit Compliance** | ≥ 95 % | Programmes prérequis (ISO 22000 §8.2) |
| **Supplier COA Conformance** | ≥ 99 % | Certificats d'analyse fournisseurs |

## Modules QualitOS recommandés

- **[NC](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : excursions CCP,
  contamination, corps étrangers, gestion de rappel (`haccp-analysis`, `recall-management`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `Excursion T° chambre froide`,
  `Contamination microbiologique`, `Allergène non déclaré`, `Corps étranger`.
- **[Audits](../modules/audits.md)** (`/audits`) : `prp-audit` (audits PRP, prépa IFS/BRCGS).
- **Poka-Yoke** : détecteur de métaux + RX en fin de ligne, lavage des mains avec capteur de
  passage, étiquetage validé par caméra/OCR.
- **[SPC](../modules/spc.md)** + **[Détection d'anomalies](../modules/anomaly.md)** : surveillance
  T°, pH, profils thermiques.

## Connecteurs IoT typiques

`MQTT`, `LoRaWAN`, `SAP ERP`, `LIMS`, `MES food`. Sondes T° chaîne du froid, détecteurs, RFID lots.

## Exemple de parcours

1. Une sonde T° (MQTT) signale une excursion en chambre froide → KPI **CCP Excursion** en alerte.
2. Une [NC](../modules/non-conformites.md) est ouverte ; le lot est isolé.
3. Un [Ishikawa](../modules/ishikawa.md) `Excursion T° chambre froide` cadre l'analyse (compresseur,
   sonde, porte restée ouverte…).
4. Une [CAPA](../modules/capa.md) traite la cause ; un **Poka-Yoke** (alerte T° temps réel) est
   activé.
5. Un exercice de rappel mesure le **Traceability Recall Time** (< 2 h), preuve pour l'audit IFS.
