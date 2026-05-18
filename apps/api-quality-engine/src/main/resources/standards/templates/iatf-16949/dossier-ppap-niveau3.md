# Dossier PPAP — Niveau 3 (IATF 16949 §8.3.4.4 — AIAG PPAP 4th Edition)

> Tenant : `{{tenant.name}}` — Client : `{{customer.name}}` — Pièce : `{{part.number}}` — `{{date.today}}`.

## Checklist PPAP niveau 3 (18 éléments)

- [ ] **1. Design Records** : plans de définition (CAD + 2D), version, ECN/ECO appliqués.
- [ ] **2. Authorized Engineering Change Documents** : tous les ECN/ECO actifs.
- [ ] **3. Customer Engineering Approval** : si modifications, preuve d'approbation client.
- [ ] **4. Design FMEA (DFMEA)** : à jour, signé, RPN traités.
- [ ] **5. Process Flow Diagram** : flux production, opérations, contrôles.
- [ ] **6. Process FMEA (PFMEA)** : couvre toutes les opérations critiques.
- [ ] **7. Control Plan** : par phase (prototype / pré-série / série).
- [ ] **8. Measurement System Analysis (MSA)** : R&R, biais, linéarité, stabilité ; %GRR < 10 %.
- [ ] **9. Dimensional Results** : layout complet (toutes cotes), conformité.
- [ ] **10. Material/Performance Test Results** : conformité matière, tests fonctionnels.
- [ ] **11. Initial Process Studies** : Ppk / Cpk préliminaires ≥ cible client.
- [ ] **12. Qualified Laboratory Documentation** : accréditations (ISO 17025 si applicable).
- [ ] **13. Appearance Approval Report (AAR)** : si exigé (pièces visibles client).
- [ ] **14. Sample Production Parts** : échantillons numérotés et tracés.
- [ ] **15. Master Sample** : conservé chez fournisseur, identifié.
- [ ] **16. Checking Aids** : moyens de contrôle spécifiques, étalonnés.
- [ ] **17. Customer-Specific Requirements (CSR)** : conformité aux CSR du client.
- [ ] **18. Part Submission Warrant (PSW)** : signé par responsable qualité fournisseur.

## Soumission

| Élément | Valeur |
|---------|--------|
| Type de soumission | Initiale / Re-soumission / Modification engineering |
| Quantité d'échantillons | `{{ppap.sample.qty}}` |
| Date de soumission | `{{date.today}}` |
| Statut souhaité | Production approval / Interim approval / Rejected |

## Validation interne avant envoi client

- [ ] Revue qualité interne complète.
- [ ] Signature électronique 21 CFR 11.
- [ ] Hash SHA-256 calculé et ancré blockchain.
- [ ] Archivage 15 ans minimum.
