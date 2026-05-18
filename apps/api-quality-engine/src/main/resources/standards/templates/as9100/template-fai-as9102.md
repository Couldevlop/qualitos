# Template — First Article Inspection (FAI) — AS9102 Rev B

> Tenant : `{{tenant.name}}` — Pièce : `{{part.number}}` — Indice : `{{part.revision}}`.

## Form 1 — Part Number Accountability

| Champ | Valeur |
|-------|--------|
| 1. Part Number | `{{part.number}}` |
| 2. Part Name | `{{part.name}}` |
| 3. Serial Number | `{{part.serial}}` |
| 4. FAI Report Number | `{{fai.report_number}}` |
| 5. Part Revision Level | `{{part.revision}}` |
| 6. Drawing Number | `{{part.drawing}}` |
| 7. Drawing Revision Level | `{{part.drawing_revision}}` |
| 8. Additional Changes | `{{fai.eco_list}}` |
| 9. Manufacturing Process Reference | `{{process.id}}` |
| 10. Organization Name | `{{tenant.name}}` |
| 11. Supplier Code | `{{tenant.supplier_code}}` |
| 12. P.O. Number | `{{po.number}}` |
| 13. Detail FAI / Assembly FAI / Full FAI / Partial FAI | `{{fai.type}}` |
| 14. Baseline Part Number | `{{fai.baseline_part}}` |
| 15. Reason for Partial FAI | `{{fai.reason_partial}}` |
| 16-18. Réservés client | — |
| 19. FAI Complete | Y / N |
| 20. Signature | `{{signer}}` — `{{date.today}}` |
| 21. Reviewed By | `{{reviewer}}` — `{{date.today}}` |
| 22. Customer Approval (si requis) | `{{customer.approver}}` |
| 23. FAIR Documents | Form 2 + Form 3 + sub-tier FAIR |

## Form 2 — Product Accountability — Raw Material, Specifications & Special Process

Lister pour chaque ligne : Material/Process Name, Specification, Code, Supplier Code, Customer Approval, Certificate of Conformance.

## Form 3 — Characteristic Accountability, Verification & Compatibility

Pour chaque caractéristique du plan : Char. No, Reference Location, Char. Designator, Requirement, Results, Designed Tooling, Non-Conformance Number.

## Signature électronique

Signature 21 CFR 11 compatible. Hash SHA-256 du dossier complet ancré sur la chaîne Hyperledger Fabric (preuve d'horodatage opposable).
