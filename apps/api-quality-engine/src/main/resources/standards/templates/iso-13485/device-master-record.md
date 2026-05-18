# Device Master Record (DMR) — ISO 13485 §4.2.3

> Tenant : `{{tenant.name}}` — Famille DM : `{{device.family}}` — `{{date.today}}`.

## 1. Identification du dispositif

| Champ | Valeur |
|-------|--------|
| Nom commercial | `{{device.commercial_name}}` |
| Référence catalogue | `{{device.catalog_ref}}` |
| Classification UE (MDR) | `{{device.mdr_class}}` (I, IIa, IIb, III) |
| Classification US (FDA) | `{{device.fda_class}}` (I, II, III) |
| Code GMDN | `{{device.gmdn}}` |
| UDI-DI | `{{device.udi_di}}` |
| Statut commercialisation | `{{device.market_status}}` |

## 2. Spécifications produit

- Description physique, dimensions, matériaux, biocompatibilité.
- Performance attendue, indication thérapeutique, contre-indications, mode d'emploi.
- Conditions environnementales (T°, HR, stérilité).

## 3. Conception (référence files)

| Élément | Référence document |
|---------|--------------------|
| User Requirements Specification (URS) | `{{dmr.urs.ref}}` |
| Design Inputs | `{{dmr.design_inputs.ref}}` |
| Design Outputs (plans, BOM) | `{{dmr.design_outputs.ref}}` |
| Design Verification Reports | `{{dmr.dv.refs}}` |
| Design Validation Reports | `{{dmr.dval.refs}}` |
| Design Transfer Report | `{{dmr.dt.ref}}` |
| Design Review Records | `{{dmr.reviews.refs}}` |

## 4. Production

- Procédés de fabrication (BOM matières + procédés spéciaux).
- Plans de contrôle / plans d'inspection.
- Validation processus (IQ/OQ/PQ) — référence rapports.
- Maîtrise des environnements (salles blanches, ESD).

## 5. Étiquetage & UDI

- Format étiquette (langue par marché).
- Notice d'utilisation (IFU) par langue (EUDAMED conforme).
- Code UDI-DI + UDI-PI.

## 6. Servicing & vigilance

- Procédure de servicing (si applicable).
- Procédure vigilance (lien §8.2.1 / §8.2.3).
- Plan PMS (Post-Market Surveillance) / PSUR.

## 7. Gestion du risque (ISO 14971)

- Risk Management Plan référence : `{{rmf.plan.ref}}`.
- Risk Management File référence : `{{rmf.file.ref}}`.
- Évaluation clinique référence (MEDDEV 2.7/1) : `{{cer.ref}}`.

## 8. Documents réglementaires

- Déclaration de conformité UE / 510(k) ou PMA US.
- Certificats organismes notifiés (CE, FDA).
- Rapports d'essais notifiés (laboratoires accrédités).

## 9. Validation, signature et ancrage

DMR validé par Responsable Affaires Réglementaires. Signature 21 CFR 11. Hash SHA-256 ancré.
