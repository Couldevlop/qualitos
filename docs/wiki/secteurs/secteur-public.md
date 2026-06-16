# Secteur — Public / Administration

[← Retour aux secteurs](README.md) · Pack : **`public`**

## Enjeux qualité du secteur

Les administrations et collectivités pilotent la **qualité du service rendu à l'usager** :
satisfaction usager, délais de réponse aux courriers et réclamations (engagements Marianne),
accessibilité numérique (RGAA), délais d'instruction des dossiers et formation continue des agents.

## Normes pertinentes (Standards Hub)

- **ISO 9001** — Socle SMQ.
- **Marianne** — Référentiel d'engagements de qualité de l'accueil et du service public (France).
- **RGAA** — Accessibilité numérique (associée à WCAG 2.2).
- **ISO 14001** — Environnement.

## KPIs clés (apportés par le pack)

| KPI | Cible | Sens |
|---|---|---|
| **Taux de satisfaction usager** | ≥ 7,5/10 | Engagement Marianne |
| **Réponse aux courriers ≤ 15j** | ≥ 90 % | Engagement Marianne |
| **RGAA Accessibility Score** | ≥ 75 % | Obligation légale services numériques |
| **Délai de traitement de dossier** | < 21 j | Qualité d'accueil et de traitement |
| **Réponse réclamations Marianne ≤ 30j** | ≥ 95 % | Engagement Marianne |
| **Complétion formation des agents** | ≥ 90 % | Formation continue obligatoire |

## Modules QualitOS recommandés

- **[Réclamations / NLP](../modules/complaints-nlp.md)** (`/complaints-nlp`) : classification et
  priorisation des réclamations usagers.
- **[NC](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : réclamations Marianne,
  RGAA, RPS (`claim-handling`, `dossier-instruction`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `Réclamation Marianne hors délai`,
  `Site web non conforme RGAA`, `Insatisfaction usager au guichet`, `Délai d'instruction dépassé`.
- **[PDCA](../modules/pdca.md)** (`/pdca`) : démarche qualité usager, revue de direction.
- **Poka-Yoke** : validation accessibilité avant publication, alerte d'échéance de dossier à J-3,
  double validation des décisions à impact élevé, chiffrement obligatoire des PJ externes.

## Connecteurs IoT typiques

`SAP ERP`, `SharePoint`, `GED Alfresco`, `SMS gateway` (notifications usagers).

## Exemple de parcours

1. Une réclamation usager arrive ; le [NLP réclamations](../modules/complaints-nlp.md) la classe et
   la priorise (sentiment, criticité).
2. Le KPI **réponse réclamations ≤ 30j** est suivi ; une alerte à J-3 (Poka-Yoke) évite le
   dépassement.
3. Un [Ishikawa](../modules/ishikawa.md) `Réclamation Marianne hors délai` cadre l'analyse (agent
   surchargé, escalade absente…).
4. Une [CAPA](../modules/capa.md) traite la cause ; le workflow `claim-handling` est ajusté.
5. Les KPIs **satisfaction usager** et **RGAA** alimentent l'audit Marianne.
