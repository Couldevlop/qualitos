# Plan HACCP — ISO 22000:2018 §8.5 (Codex Alimentarius)

> Tenant : `{{tenant.name}}` — Famille produit : `{{product.family}}` — `{{date.today}}`.

## 1. Équipe HACCP

| Rôle | Nom | Compétence |
|------|-----|------------|
| Animateur HACCP | `{{haccp.lead}}` | Certification HACCP |
| Microbiologiste | `{{haccp.micro}}` | — |
| Production | `{{haccp.prod}}` | — |
| Maintenance | `{{haccp.maint}}` | — |
| Qualité | `{{haccp.qa}}` | — |

## 2. Description du produit

- Composition, additifs, allergènes.
- Caractéristiques physico-chimiques (pH, aw, T°, conservateurs).
- Conditions de stockage / transport.
- DLC / DDM.
- Usage prévu (population cible, mode de préparation).

## 3. Diagramme de flux

(à joindre — module Workflow Designer QualitOS BPMN 2.0)

## 4. Vérification sur site du diagramme

Validé par l'équipe HACCP, `{{date.flowchart_validation}}`.

## 5. Analyse des dangers

| Étape | Danger biologique | Danger chimique | Danger physique | Allergène | Sévérité (1-5) | Probabilité (1-5) | Score | Mesure de maîtrise | Type (CCP/PRPo/PRP) |
|-------|-------------------|-----------------|-----------------|-----------|----------------|-------------------|-------|---------------------|----------------------|
| Réception MP | Salmonella, E. coli | Mycotoxines, métaux | Corps étrangers | Multiple | 4 | 2 | 8 | Contrôle réception + COA | PRPo |
| Stockage froid | Listeria | — | — | — | 5 | 2 | 10 | T° ≤ 4 °C + monitoring IoT | CCP-1 |
| Pasteurisation | Pathogènes thermosensibles | — | — | — | 5 | 1 | 5 | 72 °C / 15s | CCP-2 |
| Emballage | Recontamination | Migration | Bris verre/métal | Croisement | 4 | 2 | 8 | Atelier propre + détecteur métaux | CCP-3 |
| ... | ... | ... | ... | ... | ... | ... | ... | ... | ... |

## 6. Détermination des CCP (arbre de décision Codex)

Pour chaque danger non maîtrisé par PRP : appliquer l'arbre de décision (Q1 → Q4) → CCP / non CCP.

## 7. Limites critiques (par CCP)

| CCP | Paramètre | Limite critique | Justification scientifique |
|-----|-----------|-----------------|----------------------------|
| CCP-1 (stockage) | T° produit | ≤ 4 °C | Réglementation + réduction croissance Listeria |
| CCP-2 (pasteurisation) | T° × temps | 72 °C × 15s minimum | Codex / réduction 5 log de Salmonella |
| CCP-3 (détection métaux) | Sensibilité | Fe ≥ 1.5mm, Inox ≥ 2.5mm | Norme distributeur |

## 8. Surveillance

| CCP | Quoi | Comment | Fréquence | Qui |
|-----|------|---------|-----------|-----|
| CCP-1 | T° | Sonde IoT MQTT | Continu | Automatisé QualitOS |
| CCP-2 | T° × t | Enregistreur électronique | Chaque cycle | Opérateur ligne |
| CCP-3 | Détection | Test échantillon Fe/Inox | Toutes 4h + après changement lot | Opérateur |

## 9. Actions correctives

Pour chaque excursion : isolement lot, investigation root cause (Ishikawa), CAPA, ajustement procédé. Lots impactés mis en quarantaine jusqu'à libération qualité.

## 10. Vérification

- Tests microbiologiques aléatoires (semestriels).
- Audits internes HACCP (annuels).
- Étalonnages des équipements de surveillance (annuels).
- Revue du plan HACCP (annuelle + sur changement).

## 11. Documentation et enregistrements

Conservés ≥ 5 ans (ou durée légale supérieure). Tous ancrés blockchain.
