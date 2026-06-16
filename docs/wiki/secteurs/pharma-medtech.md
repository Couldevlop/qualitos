# Secteur — Pharmaceutique / Dispositifs médicaux

[← Retour aux secteurs](README.md) · Pack : **`pharma`**

## Enjeux qualité du secteur

Les industries pharmaceutiques, biotech et fabricants de dispositifs médicaux évoluent dans un
environnement **GxP** fortement régulé : libération de lots sans déviation, gestion des résultats
hors spécification (OOS), validation des systèmes informatisés (CSV / GAMP 5), audit trails
électroniques conformes 21 CFR Part 11, vigilance et surveillance après commercialisation (PMS).

## Normes pertinentes (Standards Hub)

- **ISO 13485** — Dispositifs médicaux.
- **FDA 21 CFR Part 11** — Signatures et enregistrements électroniques.
- **FDA 21 CFR Part 820** — Quality System Regulation (US).
- **GAMP 5** — Validation des systèmes informatisés.
- **MDR** — Règlement européen dispositifs médicaux.
- **ISO 14971** — Gestion du risque dispositif médical.
- **ISO 9001** — Socle SMQ.

## KPIs clés (apportés par le pack)

| KPI | Cible | Sens |
|---|---|---|
| **Batch Right First Time** | ≥ 95 % | Lots libérés sans déviation (ICH Q10) |
| **Deviation Rate / batch** | < 0,5 | Déviations par lot produit |
| **OOS Rate** | < 1 % | Résultats hors spécification confirmés |
| **21 CFR Part 11 CAPA aging > 30j** | 0 | Évite une observation Form 483 FDA |
| **Batch Release Cycle Time** | < 48 h | Délai de libération par le QP |
| **Vigilance incidents / trimestre** | suivi | PMS ISO 13485 §8.2.1 / MDR |
| **CSV Validation Coverage (GAMP 5)** | 100 % | Systèmes GxP validés (IQ/OQ/PQ) |

## Modules QualitOS recommandés

- **[CAPA](../modules/capa.md)** + **[NC](../modules/non-conformites.md)** : gestion des déviations
  (`deviation-handling`), suivi Part 11.
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `Déviation batch cGMP`, `Résultat OOS`,
  `Audit trail incomplet`, `Échec stérilisation`.
- **[Audits](../modules/audits.md)** (`/audits`) : préparation audit FDA.
- **Poka-Yoke** : 2FA obligatoire e-signature Part 11, verrouillage cycle si étalonnage capteur
  expiré, audit trail append-only, scan UDI avant libération.
- **[SPC](../modules/spc.md)** + **[Détection d'anomalies](../modules/anomaly.md)** : surveillance
  procédés (bioréacteur, HPLC).

## Connecteurs IoT typiques

`HL7 FHIR`, `LIMS`, `SAP ERP`, `OPC-UA`, `MES pharma` (bioréacteurs, chromatographes, monitoring
environnement salle propre).

## Exemple de parcours

1. Une sonde pH/O₂/T° (OPC-UA) détecte une excursion CCP sur un bioréacteur en cours de batch.
2. Une [NC](../modules/non-conformites.md) GxP est ouverte, le batch est mis en investigation.
3. Un [Ishikawa](../modules/ishikawa.md) `Déviation batch cGMP` (branches 6M) cadre l'analyse.
4. Une [CAPA](../modules/capa.md) traite la cause ; le dossier d'audit FDA est pré-rempli, signé
   (e-signature 2FA Part 11) et ancré.
5. Les KPIs **Deviation Rate** et **Batch Right First Time** suivent l'effet ; la
   [couverture CSV](../modules/standards-hub.md) reste à 100 %.
