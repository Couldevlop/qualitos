# Secteur — Santé / Hôpital / CHU

[← Retour aux secteurs](README.md) · Pack : **`healthcare-hospital`**

## Enjeux qualité du secteur

Les établissements de santé (CHU, hôpitaux, cliniques) pilotent la **sécurité du patient**, la
gestion des **événements indésirables graves**, la traçabilité des dispositifs et des médicaments,
et la préparation à la **certification HAS**. La chaîne du froid des médicaments/vaccins, la
qualité des laboratoires et la dynamique d'amélioration continue sont au cœur du dispositif.

## Normes pertinentes (Standards Hub)

- **ISO 13485** — Dispositifs médicaux.
- **ISO 15189** — Laboratoires de biologie médicale.
- **HAS Certification V2024** — Établissements de santé (France).
- **Joint Commission / JCI** — Certification hospitalière internationale.

## KPIs clés (apportés par le pack)

| KPI | Sens |
|---|---|
| **Taux d'infections nosocomiales** | Infections associées aux soins |
| **Taux de réadmission 30j** | Qualité de prise en charge / sortie |
| **Durée moyenne de séjour** | Efficience du parcours patient |
| **Délai d'attente urgences** | Fluidité des urgences |
| **Taux d'annulation chirurgie** | Maîtrise de la programmation |
| **Satisfaction patient (HCAHPS)** | Vécu et expérience patient |
| **Événements indésirables graves (EIG)** | Sécurité patient |
| **Taux de chute patient** / **erreurs médicamenteuses** | Risques cliniques |
| **Taux d'occupation lits** | Capacité (cible ~85 %) |

## Modules QualitOS recommandés

- **[Non-conformités](../modules/non-conformites.md)** + **[CAPA](../modules/capa.md)** : déclaration
  d'EIG (`incident-indesirable-grave`, `capa-soins-default`).
- **[Ishikawa](../modules/ishikawa.md)** (`/ishikawa`) : `infection-nosocomiale`,
  `erreur-medicamenteuse`.
- **[Audits](../modules/audits.md)** (`/audits`) : `audit-bloc-operatoire`.
- **[PDCA](../modules/pdca.md)** (`/pdca`) : cycles d'amélioration continue.
- **Poka-Yoke** : double-check médicament, bracelet d'identification patient.
- **[Détection d'anomalies](../modules/anomaly.md)** (`/anomaly`) : surveillance des dérives
  (T° frigos, indicateurs cliniques).

## Connecteurs IoT typiques

`HL7 FHIR`, `HL7 v2` (EHR/HIS), `DICOM` (imagerie), `LIS ASTM` (automates labo), sondes T° frigos
pharma.

## Exemple de parcours

1. Une sonde T° (HL7) d'un frigo médicaments dépasse le seuil.
2. Une [NC](../modules/non-conformites.md) est ouverte, le lot est mis en quarantaine, le
   pharmacien est alerté ; une **preuve** est ancrée (traçabilité ANSM/ARS).
3. Un [Ishikawa](../modules/ishikawa.md) `erreur-medicamenteuse` ou un EIG structure l'analyse.
4. Une [CAPA](../modules/capa.md) `capa-soins-default` traite la cause ; un **Poka-Yoke**
   (double-check) est mis en place.
5. Les indicateurs (**EIG**, **erreurs médicamenteuses**) alimentent le dossier de certification
   [HAS V2024](../modules/standards-hub.md).
