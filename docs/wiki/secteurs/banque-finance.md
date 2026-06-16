# Secteur — Banque / Finance

[← Retour aux secteurs](README.md) · Pack : **`banking`**

## Enjeux qualité du secteur

Les établissements financiers (banque, assurance, gestion d'actifs, fintech) pilotent un **risque
opérationnel** réglementé : résilience opérationnelle numérique (DORA), maîtrise des pertes
opérationnelles (Bâle), dispositif LCB-FT, contrôles permanents et auto-évaluation des risques
(RCSA), traitement des réclamations sous délai réglementaire.

## Normes pertinentes (Standards Hub)

- **DORA** — Résilience opérationnelle numérique (UE 2022/2554).
- **Bâle III** — Exigences prudentielles bancaires.
- **Solvabilité II** — Assurance.
- **MiFID II** — Marchés financiers.
- **LCB-FT** — Anti-blanchiment.
- **ISO 27001** — Sécurité de l'information.
- **ISO 22301** — Continuité d'activité.
- **ISO 9001** — Socle SMQ.

## KPIs clés (apportés par le pack)

| KPI | Cible | Sens |
|---|---|---|
| **DORA Incident Notification SLA (≤4h)** | 100 % | Notification incident TIC majeur (DORA Art. 19) |
| **Operational Losses (Basel L1-L7)** | < budget | Pertes ORM Bâle III |
| **RCSA KRI Breaches / mois** | < 5 | Dépassements des Key Risk Indicators |
| **AML/LCB-FT False Positive Rate** | < 90 % | Tuning des scénarios anti-blanchiment |
| **TLPT Programme Coverage** | 100 % | Tests d'intrusion menés ≤ 3 ans (DORA Art. 26) |
| **Third Party Contracts DORA-Ready** | 100 % | Clauses contractuelles tiers TIC (DORA Art. 30) |
| **Délai réponse réclamation** | ≥ 95 % | Délai ACPR/AMF |

## Modules QualitOS recommandés

- **[NC](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : incidents TIC,
  remédiation (`dora-incident-notification`, `aml-alert-handling`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `Incident DORA non notifié 4h`,
  `Faux positifs AML > 95%`, `Cyberattaque ransomware`, `Erreur de réconciliation`.
- **[PDCA](../modules/pdca.md)** (`/pdca`) : plans de remédiation et contrôles permanents.
- **Poka-Yoke** : double validation 4-yeux > seuil, 2FA opérations sensibles, geo-velocity check,
  notification DORA automatisée.
- **[Clustering de NC](../modules/nc-clusters.md)** (`/nc-clusters`) : détection de patterns
  d'incidents récurrents.

## Connecteurs IoT typiques

`SAP ERP`, `ServiceNow ITSM`, `SWIFT MQ`, `OPC-UA` (datacenter / supervision).

## Exemple de parcours

1. Un incident TIC majeur est classé : la procédure `dora-incident-notification` ouvre
   automatiquement une case régulateur (ESA) — le SLA **≤ 4h** est suivi.
2. Un [Ishikawa](../modules/ishikawa.md) `Incident DORA non notifié 4h` cadre l'analyse.
3. Une [CAPA](../modules/capa.md) traite la cause ; un **Poka-Yoke** (notification DORA
   automatisée) verrouille le processus.
4. Le [clustering de NC](../modules/nc-clusters.md) vérifie l'absence de pattern systémique.
5. Les KPIs **DORA SLA** et **RCSA KRI Breaches** alimentent le tableau de bord risque.
