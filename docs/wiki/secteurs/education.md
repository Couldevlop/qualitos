# Secteur — Éducation / EdTech

[← Retour aux secteurs](README.md) · Pack : **`education`**

## Enjeux qualité du secteur

Les organismes de formation, établissements d'enseignement et acteurs EdTech pilotent la **qualité
pédagogique** : satisfaction et réussite des apprenants, taux de complétion et d'abandon des
parcours, employabilité, qualification des formateurs, et conformité **Qualiopi** (Référentiel
National Qualité) ou **ISO 21001**.

## Normes pertinentes (Standards Hub)

- **ISO 21001** — Organismes éducatifs.
- **Qualiopi** — Certification qualité des organismes de formation (France, RNQ).
- **ISO 9001** — Socle SMQ.

## KPIs clés (apportés par le pack)

| KPI | Cible | Sens (référence) |
|---|---|---|
| **Satisfaction des apprenants** | ≥ 8/10 | ISO 21001 §9.1.2 / Qualiopi ind. 11 |
| **Taux de complétion** | ≥ 85 % | Apprenants achevant le parcours |
| **Taux d'abandon** | < 10 % | Qualiopi ind. 9 |
| **Taux d'employabilité / insertion** | ≥ 70 % | Qualiopi ind. 3 (insertion à 6 mois) |
| **Réclamations apprenants hors délai** | 0 | Qualiopi ind. 31 |
| **Taux de réussite aux certifications** | ≥ 80 % | Qualiopi ind. 5 |
| **Formateurs qualifiés à jour** | 100 % | Qualiopi ind. 21 |

## Modules QualitOS recommandés

- **[NC](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : réclamations,
  non-conformités Qualiopi (`traitement-reclamation`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `Taux d'abandon élevé`, `Faible
  satisfaction apprenant`, `Faible réussite certification`, `Non-conformité audit Qualiopi`.
- **[PDCA](../modules/pdca.md)** (`/pdca`) : amélioration des parcours, revue qualité.
- **[Audits](../modules/audits.md)** (`/audits`) : préparation audit Qualiopi.
- **Poka-Yoke** : vérification automatique des prérequis à l'inscription, alerte de décrochage,
  verrouillage du certificat tant que les modules ne sont pas complétés, contrôle qualité SCORM.

## Connecteurs IoT typiques

`LMS SCORM`, `xAPI` (traçage d'apprentissage), `SharePoint`, `SMS gateway`.

## Exemple de parcours

1. Un apprenant devient inactif au-delà du seuil → alerte de décrochage (Poka-Yoke, via xAPI) au
   tuteur.
2. Le KPI **taux d'abandon** alerte ; un [Ishikawa](../modules/ishikawa.md) `Taux d'abandon élevé`
   cadre l'analyse (rythme inadapté, support obsolète, décrochage non détecté tôt…).
3. Une [CAPA](../modules/capa.md) ajuste le suivi individualisé.
4. Une réclamation apprenant est traitée dans les délais (`traitement-reclamation`) → KPI Qualiopi
   ind. 31.
5. Les KPIs **satisfaction**, **complétion** et **réussite certification** alimentent le dossier
   [Qualiopi](../modules/standards-hub.md).
