# Manuel Qualité — AS9100D / EN 9100:2018

> Standard SMQ aéronautique, spatial et défense. Tenant : `{{tenant.name}}`.

## 1. Champ d'application

Activités aéronautiques couvertes : `{{scope.aero_activities}}`. Clients : `{{scope.aero_customers}}` (Airbus, Boeing, Safran, Dassault, Thales, MBDA…). Sites : `{{scope.sites}}`. Programmes : `{{scope.programs}}`.

## 2. Sécurité produit (§4.4.2)

L'organisme identifie les caractéristiques de sécurité produit (items critiques / Flight Safety Critical) et applique une maîtrise renforcée. Le registre des items de sécurité est revu au minimum annuellement.

## 3. Risk-Based Thinking (§6.1)

Évaluation des risques à trois niveaux : produit, processus, programme. Tous les risques majeurs sont reliés à une FMEA. Le RPN est réévalué dynamiquement via le module Risk/FMEA de QualitOS.

## 4. Prévention des pièces contrefaites (§8.1.2)

- Approvisionnement uniquement chez OCM (Original Component Manufacturer) ou distributeur autorisé.
- Liste des fournisseurs autorisés tenue par les Achats.
- Tests de validation matière (XRF, certification) pour les lots à risque.
- Traçabilité matière complète conservée 15 ans (ou durée du programme).

## 5. Prévention des FOD (Foreign Object Debris) (§8.1.4)

- Programme FOD piloté avec audits visuels quotidiens des zones critiques.
- Outillage tracé (shadow boards, FOD count avant/après opération).
- Sensibilisation 100 % opérateurs (e-learning + quiz annuel).

## 6. Procédés spéciaux & NADCAP (§8.5.1.2)

Les procédés spéciaux (soudure, traitement thermique, NDT, processing chimique) sont qualifiés NADCAP (Nadcap accreditation par groupe). Re-qualification ≥ 1/an. Opérateurs certifiés et tracés.

## 7. First Article Inspection (§8.5.1.3)

FAI conforme AS9102 pour toute pièce nouvelle ou modification engineering. Trois formulaires : Form 1 (part number accountability), Form 2 (product accountability), Form 3 (characteristics accountability). FAI partiel ou complet selon nature du changement.

## 8. Traçabilité (§8.5.2)

Traçabilité 100 % pour pièces critiques : matière → lot → ordre de fabrication → numéro de série → expédition.

## 9. Escape rate (§10.2.1.1)

Indicateur clé : taux de défauts livrés au client. Objectif : `{{escape_rate.target}}` PPM. Plan d'action immédiat en cas de dépassement.

## 10. Signature & ancrage blockchain

Validé par la Direction Qualité Aéro. Hash SHA-256 ancré Hyperledger Fabric.
