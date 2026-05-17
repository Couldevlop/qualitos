# Validation Master Plan (VMP) — FDA 21 CFR Part 11 / GAMP 5 (2nd Ed)

> Tenant : `{{tenant.name}}` — Système : `{{system.name}}` — `{{date.today}}`.

## 1. Objet

Définir la stratégie de validation pour assurer que `{{system.name}}` est conforme à 21 CFR Part 11 §11.10(a) (validation), aux exigences GxP applicables (cGMP, GLP, GCP) et aux principes GAMP 5.

## 2. Périmètre

- Modules en scope : `{{vmp.modules}}`.
- Données GxP traitées : `{{vmp.data_types}}`.
- Catégorie GAMP 5 : `{{vmp.gamp_category}}` (1 infrastructure, 3 non configuré, 4 configuré, 5 spécifique).

## 3. Approche basée sur le risque

Évaluation du risque qualité (Quality Risk Assessment — QRA) selon ICH Q9. Score = Impact × Détection × Probabilité. Effort de validation proportionné au score.

## 4. Cycle de validation (V-Model)

| Phase | Livrable |
|-------|----------|
| User Requirements Specification (URS) | `{{vmp.urs.ref}}` |
| Functional Specification (FS) | `{{vmp.fs.ref}}` |
| Design Specification (DS) | `{{vmp.ds.ref}}` |
| IQ (Installation Qualification) | `{{vmp.iq.ref}}` |
| OQ (Operational Qualification) | `{{vmp.oq.ref}}` |
| PQ (Performance Qualification) | `{{vmp.pq.ref}}` |
| Validation Summary Report (VSR) | `{{vmp.vsr.ref}}` |

## 5. Exigences 21 CFR Part 11 couvertes

| Clause | Mécanisme |
|--------|-----------|
| §11.10(a) Validation | URS/FS/DS + IQ/OQ/PQ + VSR |
| §11.10(b) Generation of accurate copies | Export PDF/Native + checksums |
| §11.10(c) Protection of records | Stockage chiffré AES-256, RPO ≤ 15min |
| §11.10(d) Access control | RBAC + ABAC, Keycloak |
| §11.10(e) Audit trails | Audit trail technique non modifiable + revue |
| §11.10(f) Operational checks | Workflow engine avec séquençage |
| §11.10(g) Authority checks | Spring Security + permissions |
| §11.10(i) Training | Module Training de QualitOS |
| §11.30 Open systems | TLS 1.3 + signatures ML-DSA |
| §11.50 Signature manifestations | UI affiche nom + horodatage + raison |
| §11.100 Unique signature | Liaison utilisateur ↔ identité KYC |
| §11.200 Two distinct components | 2FA TOTP / WebAuthn |
| §11.300 Password controls | Rotation 90j, lockout 5 tentatives |

## 6. Rôles

| Rôle | Responsabilité |
|------|----------------|
| Validation Lead | Pilote la validation, signe le VSR |
| QA | Approuve les livrables |
| IT | Exécute IQ/OQ technique |
| Métier | Exécute PQ et signe l'acceptation |

## 7. Gestion du changement

Tout changement post-validation déclenche une analyse d'impact (Change Control) et, le cas échéant, une re-validation partielle ou complète.

## 8. Revue périodique

Revue annuelle de l'état validé du système. Re-validation tous les 3 ans ou après changement majeur.

## 9. Approbations

| Rôle | Signature 21 CFR 11 | Date |
|------|---------------------|------|
| Validation Lead | `{{signer.validation}}` | `{{date.today}}` |
| QA | `{{signer.qa}}` | `{{date.today}}` |
| IT | `{{signer.it}}` | `{{date.today}}` |

Hash SHA-256 du VMP ancré sur Hyperledger Fabric.
