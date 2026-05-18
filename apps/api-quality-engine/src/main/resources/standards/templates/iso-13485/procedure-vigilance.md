# Procédure — Vigilance post-marché et signalement aux autorités — ISO 13485 §8.2.1 / §8.2.3

> Tenant : `{{tenant.name}}` — Version 1.0 — `{{date.today}}`.

## 1. Objet

Détecter, analyser, classer, traiter et signaler aux autorités compétentes (ANSM, FDA, BfArM, MHRA, autres) tout incident impliquant un dispositif médical commercialisé.

## 2. Définitions

- **Incident grave** : décès, dégradation grave de santé, menace grave santé publique.
- **Incident** : tout dysfonctionnement, défaillance ou inadéquation de l'étiquetage / notice ayant pu / pouvant entraîner un incident grave.
- **FSCA** (Field Safety Corrective Action) : action corrective de sécurité (rappel, modification, information sécurité).
- **FSN** (Field Safety Notice) : notification de sécurité.

## 3. Sources d'information

- Réclamations clients / patients / professionnels santé.
- Surveillance des bases d'incidents (MAUDE FDA, IRIS, EUDAMED).
- Revue littérature scientifique.
- Données du SAV.
- Retours fournisseurs.

## 4. Workflow

1. Saisie incident dans QualitOS module Complaints.
2. Analyse criticité (équipe vigilance — 24h).
3. Classification : non-incident / incident / incident grave.
4. Si grave : notification autorité dans les délais (UE 15j / 10j / 2j selon gravité ; FDA 30j).
5. Investigation root cause (Ishikawa + 5 pourquoi).
6. CAPA si nécessaire (lien automatique).
7. FSCA / FSN si pertinent.
8. Rapport périodique (PSUR pour MDR, MDR Annual Report pour FDA).

## 5. Délais réglementaires (UE — MDR / IVDR)

| Type incident | Délai notification |
|---------------|--------------------|
| Décès ou dégradation grave imprévue | ≤ 2 jours |
| Incident grave | ≤ 10 jours |
| Menace publique grave | ≤ 2 jours |
| Tendance significative non grave | rapport trimestriel |

## 6. Délais FDA (US — 21 CFR Part 803)

| Type | Délai |
|------|-------|
| Death / serious injury | 30 calendar days |
| Event requiring corrective action to prevent serious health risk | 5 working days |

## 7. Indicateurs

| KPI | Cible |
|-----|-------|
| Délai d'analyse incident grave | ≤ 48h |
| Taux respect délai notification autorité | 100 % |
| Délai clôture CAPA vigilance | ≤ 90j |

## 8. Validation & blockchain

Toute notification autorité est signée 21 CFR 11 + ancrée blockchain pour preuve opposable.
