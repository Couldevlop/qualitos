# Procédure APQP — Advanced Product Quality Planning (IATF 16949 §8.3.2.1)

> Référence : AIAG APQP 2nd Edition + IATF 16949:2016 §8.3.2.1 et §8.3.4.4 (PPAP).
> Tenant : `{{tenant.name}}` — Version 1.0 — `{{date.today}}`.

## 1. Objet

Décrire la méthode APQP en 5 phases appliquée à tout nouveau produit ou modification majeure, afin de garantir une qualité prouvée avant le lancement série et un PPAP approuvé client.

## 2. Phases APQP

### Phase P0 — Pré-planification (entrée projet)

Livrables : voice of the customer (VoC), benchmarks, hypothèses produit/processus, project charter.

### Phase P1 — Planification & définition du programme (P1)

Livrables : design goals, fiabilité/qualité cibles, liste préliminaire des matériaux, diagramme de flux préliminaire, liste des caractéristiques spéciales préliminaires, plan de support produit/processus.

### Phase P2 — Conception et développement du produit (P2)

Livrables : DFMEA, design for manufacturing & assembly (DFM/DFA), revue de conception, vérification de conception, plans de prototypes, équipements et outillage spécifiés, plan d'essais de qualification matériaux.

### Phase P3 — Conception et développement du processus (P3)

Livrables : standards d'emballage, revue système qualité produit/processus, diagramme de flux processus, plan d'aménagement (layout), characteristics matrix, PFMEA, plan de contrôle pré-série, instructions de travail, plan d'analyse du système de mesure (MSA), plan d'études de capabilité préliminaire.

### Phase P4 — Validation du produit et du processus (P4)

Livrables : production trial run, MSA, études de capabilité préliminaires (Pp/Ppk), PPAP, plan d'évaluation packaging, plan de contrôle de production, support qualité production, signoff de validation.

### Phase P5 — Lancement, retour d'expérience et actions correctives (P5)

Livrables : variation réduite, satisfaction client, livraison & service améliorés, lessons learned. Bouclage avec PDCA pour pérenniser les améliorations.

## 3. Jalons de revue (gates)

| Jalon | Phase | Livrables minimums |
|-------|-------|--------------------|
| G0 | P0 | Project charter + business case validés |
| G1 | P1 | Caractéristiques spéciales préliminaires identifiées |
| G2 | P2 | DFMEA + plan de prototypes |
| G3 | P3 | PFMEA + plan de contrôle pré-série |
| G4 | P4 | PPAP signé client |
| G5 | P5 | Production stabilisée + Cpk ≥ cible client |

## 4. PPAP (§8.3.4.4) — Niveaux

| Niveau | Documents soumis au client |
|--------|----------------------------|
| 1 | Warrant uniquement |
| 2 | Warrant + échantillons + données limitées |
| 3 | Warrant + échantillons + données complètes (par défaut) |
| 4 | Warrant + exigences spécifiques définies par le client |
| 5 | Warrant + échantillons + données complètes disponibles chez le fournisseur |

## 5. Caractéristiques spéciales

- Symboles : ⌷ (critique), ⌬ (significative), ⊕ (sécurité), ⊗ (réglementaire).
- Toutes les caractéristiques spéciales sont identifiées au DFMEA et tracées jusqu'au plan de contrôle.

## 6. Workflow QualitOS

1. Création du projet APQP dans QualitOS (module Quality Engine).
2. Chaque jalon génère un cycle PDCA dédié.
3. Tous les livrables sont versionnés dans Document Control + ancrés blockchain.
4. Le PPAP final est signé électroniquement (21 CFR 11 compatible) + ancré.
