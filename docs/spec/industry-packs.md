# QualitOS — Adaptabilite par domaine d activite

> Chapitre de la specification produit, extrait de `CLAUDE.md` pour
> n alourdir que les sessions qui en ont besoin. La numerotation des sections
> est celle de la specification.

---

## 5. Adaptabilité par domaine d'activité (clé de la promesse "100 % adaptable")

### 5.1 Architecture d'adaptabilité — _Domain Adapter Layer_

Chaque secteur est packagé comme un **Industry Pack** activable côté tenant. Un Industry Pack contient :

- **Templates de workflows** (PDCA pré-configuré pour le secteur, audits 5S adaptés, projets DMAIC types).
- **Catalogue de KPIs** sectoriels (voir section 6).
- **Référentiels normatifs** pré-chargés (voir section 8).
- **Glossaire métier** propre au secteur.
- **Connecteurs natifs** (EHR pour santé, MES pour industrie, ITSM pour IT, etc.).
- **Exemples d'Ishikawa et de Poka-Yoke** spécifiques.
- **Parcours de formation** adaptés.

### 5.2 Industry Packs livrés en V1

| Pack                                      | Spécificités                                                                                                                           |
| ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| **Manufacturing / Industrie**             | SPC, FMEA, APQP/PPAP, IATF 16949, intégration MES (OPC-UA, MQTT), OEE, calibrations                                                    |
| **Santé / Hôpital / CHU**                 | ISO 13485, ISO 15189 (labos), HAS, Joint Commission, gestion incidents indésirables, EHR (HL7 FHIR), patient safety, infection control |
| **Pharmaceutique / Dispositifs médicaux** | GMP, FDA 21 CFR Part 11, ICH, validation GxP (IQ/OQ/PQ), eDHR, batch records                                                           |
| **Banque / Finance**                      | Bâle III/IV, DORA, MiFID II, LCB-FT, contrôles permanents, RCSA, Operational Risk                                                      |
| **IT / ITSM / SaaS**                      | ITIL 4, ISO 20000, ISO 27001, problem mgmt, change mgmt, incident postmortem, SRE error budgets                                        |
| **Agro-alimentaire**                      | HACCP, ISO 22000, FSSC 22000, IFS, BRC, traçabilité lot, allergènes                                                                    |
| **Aéronautique / Défense**                | AS9100, EN 9100, NADCAP, classification, traçabilité matière                                                                           |
| **Automobile**                            | IATF 16949, VDA 6.3, APQP, PPAP, 8D, exigences OEM (Renault, Stellantis, BMW, Toyota)                                                  |
| **BTP / Construction**                    | ISO 19650 (BIM), exigences chantier, PPSPS, levée de réserves                                                                          |
| **Énergie / Utilities**                   | ISO 55000 (asset mgmt), NERC CIP, gestion d'actifs critiques                                                                           |
| **Secteur Public / Administration**       | ISO 9001 service public, RGAA (accessibilité), Marianne, qualité d'accueil, démarche qualité usager                                    |
| **Éducation / EdTech**                    | ISO 21001, Qualiopi, satisfaction apprenants, taux de complétion, qualité pédagogique                                                  |
| **Retail / Distribution**                 | Qualité produit, gestion des retours, satisfaction client, audits magasin                                                              |
| **Logistique / Transport**                | ISO 39001, qualité de service, traçabilité, OTIF                                                                                       |

### 5.3 Comment un tenant active un domaine

```yaml
# Exemple : tenant "Hopital-XYZ" active le pack Santé
tenant_id: hopital-xyz
industry_packs:
  - healthcare-hospital
modules_enabled:
  - pdca, ishikawa, capa, audit, training
  - non-conformance, document-control, complaint
  - risk-fmea, calibration
custom_kpis:
  - inherit: healthcare-hospital
  - additional: [hospital-acquired-infections, surgery-cancellation-rate]
norms:
  - iso-9001, iso-13485, has-v2024, joint-commission
connectors:
  - hl7-fhir-ehr, his-system, lis-laboratory
language: fr-FR
```

Tout est **déclaratif**. Le Super Admin active, le tenant configure, l'IA suggère.

### 5.4 Personnalisation par tenant (au-delà du pack)

- **No-code form builder** (à la ETQ Reliance) pour créer des formulaires personnalisés.
- **Workflow designer drag & drop** (BPMN 2.0).
- **Custom KPIs** : éditeur d'expressions, formules SQL/Mongo, agrégations Kafka Streams.
- **Custom dashboards** : drag & drop de widgets, sauvegarde par utilisateur ou tenant.
- **Generative AI assistance** : "Crée-moi un formulaire d'audit 5S pour un atelier mécanique" → l'IA génère le formulaire, l'admin valide.

---

