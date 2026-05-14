# CLAUDE.md — Plateforme **QualitOS**

> **La seule plateforme SaaS qui agrège les 5 méthodes fondamentales de la qualité totale, augmentée par l'IA, certifiée par blockchain, résistante au quantique, et 100 % adaptable à tout secteur d'activité.**

---

## 0. Identité du projet

| Élément                 | Valeur                                                                                                                                                                                                                                                                                        |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Nom de code**         | QualitOS — _Quality Operating System_                                                                                                                                                                                                                                                         |
| **Slogan**              | _"One platform. Five methods. Every industry. Zero compromise."_                                                                                                                                                                                                                              |
| **Positionnement**      | Plateforme SaaS multi-tenant qui agrège **PDCA, 5S, Cercle de Qualité, DMAIC + Poka-Yoke, Ishikawa** dans un référentiel unique, piloté par l'IA, et qui s'adapte à **tout domaine d'activité** (industrie, santé, banque, IT/ITSM, agro, défense, services, secteur public, éducation, BTP). |
| **Cible**               | Toute organisation qui gère la qualité — du PME industriel au CHU multi-sites, du SAV ITIL à l'usine pharmaceutique, en passant par les administrations et les acteurs financiers.                                                                                                            |
| **Mode de déploiement** | SaaS multi-tenant + on-premise + hybride + cluster dédié (selon contraintes réglementaires).                                                                                                                                                                                                  |
| **Différenciation**     | Le marché propose des QMS **mono-méthode, mono-secteur** ou **trop chers, trop lourds, trop fermés**. QualitOS est **multi-méthodes, multi-secteurs, IA-natif, modulaire et accessible**.                                                                                                     |

---

## 1. Benchmark concurrentiel (état du marché 2026)

### 1.1 Panorama des leaders

| Plateforme                            | Positionnement                        | Forces clés                                                                                              | Faiblesses observées (avis utilisateurs G2 / Capterra / Gartner)                                                                                                                                                                            |
| ------------------------------------- | ------------------------------------- | -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **MasterControl Quality Excellence**  | Leader Life Sciences / FDA            | Document control puissant, validation pré-livrée, conformité 21 CFR Part 11, modules CAPA/audits matures | Très cher, courbe d'apprentissage abrupte, déconnecté du shop-floor (pas d'IoT/MES), implémentation longue, complexité élevée, "feels like forced upsell" sur la nouvelle plateforme                                                        |
| **ETQ Reliance (Octave)**             | Configurabilité enterprise (40+ apps) | Designer no-code drag & drop, modules out-of-the-box, mobile, customisation profonde                     | Pricing par licence opaque qui augmente, support client jugé décevant ("system limitation"), pas d'autosave, cumbersome pour démarrer, fonctionnalités payantes qui étaient gratuites, peu d'IA réelle, difficulté pour utilisateurs non-IT |
| **Siemens QMS (Opcenter / Polarion)** | Industrie / aéronautique              | APQP/PPAP, FMEA, SPC, MSA, intégration PLM                                                               | Lourd, écosystème Siemens-centric, peu accessible aux PME, coût élevé                                                                                                                                                                       |
| **Qualio**                            | Life Sciences agile                   | Setup rapide, UX moderne, ISO/FDA en quelques semaines                                                   | Centré documentation, ne couvre pas la fabrication, peu d'IA, modules limités                                                                                                                                                               |
| **Greenlight Guru**                   | Dispositifs médicaux                  | Spécialisation MedTech, ISO 13485 natif                                                                  | Mono-secteur, peu adapté hors médical                                                                                                                                                                                                       |
| **QT9 QMS**                           | PME tous secteurs                     | 25+ modules inclus, tarif par licence concurrente, validé out-of-the-box                                 | UI vieillissante, reporting complexe, templating difficile à apprendre                                                                                                                                                                      |
| **TrackWise Digital (Honeywell)**     | Enterprise régulé                     | Configuration point-and-click, intégration shop-floor, scale élevé                                       | Cher, écosystème propriétaire                                                                                                                                                                                                               |
| **Intellect QMS AI**                  | PME/ETI avec IA                       | IA pour automatisation, FDA/ISO/GDPR                                                                     | IA limitée à de l'automatisation simple, RAG/LLM peu avancés                                                                                                                                                                                |
| **ComplianceQuest**                   | Salesforce-native                     | Intégration native Salesforce, EHS + qualité                                                             | Verrouillage Salesforce, coût total élevé                                                                                                                                                                                                   |
| **Qualityze**                         | Cloud "all-in-one"                    | Centralisation NC/CAPA/audits, IA marketing                                                              | Faible profondeur sectorielle                                                                                                                                                                                                               |
| **AlisQI**                            | SPC + EHS no-code                     | Implémentation autonome, no-code, modulaire                                                              | Périmètre limité (pas de cercle qualité, pas de DMAIC complet)                                                                                                                                                                              |
| **EASE**                              | Audits Gemba mobiles                  | Audits terrain, layered process audits, mobile                                                           | Audit-only, pas de couverture qualité totale                                                                                                                                                                                                |
| **GoAudits**                          | Audits checklists                     | Templates customisables, mobile                                                                          | Audit-only, pas de méthodologie qualité                                                                                                                                                                                                     |
| **SafetyCulture (iAuditor)**          | Audits/inspections terrain            | UX mobile excellente, photos annotées                                                                    | **Crée des silos de données**, pas de lien avec les machines/CMMS                                                                                                                                                                           |
| **Fabrico**                           | Quality+Maintenance unifiés           | Computer Vision, OEE, lien quality↔maintenance                                                           | Mono-secteur (manufacturing), pas de méthodologie qualité formalisée                                                                                                                                                                        |
| **SAP Cloud ERP / Oracle PLM**        | ERP avec module qualité               | Intégration ERP profonde                                                                                 | Module qualité accessoire, pas spécialiste, ergonomie ERP héritée                                                                                                                                                                           |
| **Wrike / Monday + plugins qualité**  | Project mgmt avec usage qualité       | Souplesse, collaboration                                                                                 | Pas de moteur qualité, pas de référentiel normes, IA générique                                                                                                                                                                              |

### 1.2 Synthèse des **faiblesses récurrentes** du marché

À partir de 1 500+ avis Gartner / G2 / Capterra / TrustRadius / SoftwareAdvice, on extrait sept faiblesses systémiques :

1. **Silos de données** : la qualité vit séparément de l'IoT/MES/ERP/CMMS/ITSM. La cause-racine d'un défaut (vibration machine, charge serveur) est invisible depuis le QMS.
2. **Sectorisation** : un outil pour MedTech, un autre pour l'auto, un autre pour l'IT. Aucun n'embrasse réellement _tous_ les domaines.
3. **Mono-méthode** : un outil fait du CAPA/document control, mais pas le PDCA, le 5S terrain, l'Ishikawa collaboratif et le DMAIC analytique avec la même cohérence.
4. **Pricing opaque et croissant** : les fonctionnalités basculent en premium, les coûts explosent à l'échelle.
5. **IA superficielle** : l'IA est souvent un "co-pilote" de rédaction (Auto-fill, Draft) plutôt qu'un moteur prédictif et explicatif.
6. **Implémentation lourde** : 6 à 18 mois pour MasterControl/ETQ. La validation et la formation explosent les budgets.
7. **Support et UX vieillissants** : "outdated UI", "logs out too quickly", "no autosave", "training materials limited", déconnexion intempestive, pas de mode hors-ligne robuste.

### 1.3 Stratégie QualitOS — capitalisation sur les forces, neutralisation des faiblesses

Pour chaque faiblesse identifiée, QualitOS apporte une **réponse architecturale** (pas juste marketing) :

| Faiblesse marché      | Réponse QualitOS                                                        | Mécanisme technique                                                                                           |
| --------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Silos de données      | **Référentiel commun** Problèmes→Causes→Actions→KPIs→Preuves→Normes     | Modèle de domaine unique, event sourcing Kafka, connecteurs IoT/MES/ERP/ITSM/EHR natifs                       |
| Sectorisation         | **Domain Adapter Layer** : packs sectoriels activables                  | Configuration déclarative YAML + plugins Java (SPI) pour chaque secteur                                       |
| Mono-méthode          | **5 méthodes natives + données partagées**                              | Une cause d'Ishikawa devient un problème de cycle PDCA en 1 clic ; un audit 5S déclenche un Cercle de Qualité |
| Pricing opaque        | **Tarification transparente par module + concurrent licensing**         | Activation/désactivation atomique par tenant, billing usage-based en option                                   |
| IA superficielle      | **IA bout-en-bout, prédictive, explicable**                             | Voir section 12 (LLM + RAG + ML classique + CV + NLP + UEBA)                                                  |
| Implémentation lourde | **MVP en 3 semaines, pré-configurations sectorielles**                  | Templates by industry, validation packs livrés (IQ/OQ/PQ pour Life Sciences)                                  |
| UX vieillissante      | **Angular 18 + Material 3 + design system premium + offline-first PWA** | Mobile-first, dark mode, WCAG 2.2 AA, INP < 200 ms                                                            |

### 1.4 Fonctionnalités "best-of-breed" extraites du marché et améliorées par l'IA

QualitOS prend les **meilleures idées** des leaders et les pousse plus loin :

| Fonctionnalité concurrente        | Leader connu       | Version QualitOS améliorée par IA                                                                                                                        |
| --------------------------------- | ------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Document control + 21 CFR Part 11 | MasterControl      | + **détection automatique** par LLM des incohérences entre documents, suggestion de mise à jour transverse quand une procédure change                    |
| Drag & drop workflow designer     | ETQ Reliance       | + **génération de workflow** par description en langage naturel ("Je veux un processus CAPA en 4 étapes avec validation manager")                        |
| Validation pré-livrée             | MasterControl      | + **validation packs auto-générés** par l'IA selon le secteur du tenant                                                                                  |
| Layered Process Audits mobiles    | EASE               | + **CV YOLOv8** qui détecte automatiquement les non-conformités sur photo (encombrement 5S, EPI manquant, étiquette absente)                             |
| Photos annotées sur défauts       | SafetyCulture      | + **classification automatique** du type de défaut, suggestion de cause-racine probable, lien vers cas similaires historiques                            |
| OEE + qualité unifiés             | Fabrico            | + **prédiction LSTM** de la dérive de qualité à partir des données IoT, alertes pré-incident                                                             |
| FMEA / risk management            | Siemens            | + **scoring de risque dynamique** réévalué par l'IA à chaque nouvelle donnée, pas seulement lors d'une revue annuelle                                    |
| CAPA workflow                     | tous               | + **suggestion automatique de causes-racines** (RAG sur historique tenant + corpus public) et **recommandation d'actions** basée sur l'efficacité passée |
| Gemba walks                       | Lean tools         | + **transcription Whisper** + **résumé LLM** + extraction automatique d'actions avec assignation                                                         |
| Audit reporting                   | tous               | + **génération de rapport final en 30 secondes** par LLM avec citations vers preuves blockchain                                                          |
| Training management               | tous               | + **personnalisation IA** des parcours selon les rôles, simulateurs interactifs, certificats blockchain vérifiables                                      |
| Supplier quality management       | ETQ, MasterControl | + **scoring automatique des fournisseurs**, prédiction de risque, alertes proactives                                                                     |
| Non-conformance management        | tous               | + **clustering automatique** des NC similaires pour détecter des patterns invisibles à l'œil humain                                                      |
| Statistical Process Control (SPC) | Siemens, AlisQI    | + **détection d'anomalies multivariées** (autoencoders) en plus des règles WECO/Nelson classiques                                                        |
| Référentiels normes               | rare et limité     | + **moteur d'alignement automatique** : l'IA mappe vos pratiques aux exigences ISO/IATF/etc. en continu                                                  |

### 1.5 Promesse de différenciation

> **"Tout ce que MasterControl fait en compliance + tout ce que ETQ fait en configurabilité + tout ce que Fabrico fait en shop-floor + tout ce que Lean offre en méthodologie — dans une seule plateforme, multi-secteurs, IA-native, à un coût 3 à 5 × inférieur."**

---

## 2. Vision stratégique

### 2.1 Promesses fonctionnelles

1. **100 % fiable** : tests automatisés > 85 % de couverture, SLO 99,95 %, RTO < 1 h, RPO < 5 min.
2. **100 % sécurisée** : OWASP ASVS L3, OWASP Top 10 + LLM Top 10, ISO 27001 alignée, post-quantique ready.
3. **100 % modulable** : un tenant active uniquement les modules dont il a besoin, désactivation à chaud.
4. **100 % adaptable au domaine** : packs sectoriels (Industrie, Santé, Banque, IT/ITSM, Agro, Défense, BTP, Public, Éducation, Énergie) avec templates, KPIs et normes pré-configurés.
5. **100 % auditable** : chaque action journalisée, signée ML-DSA, ancrée blockchain.
6. **IA-native** : l'IA n'est pas une feature, c'est l'épine dorsale.
7. **Design premium** : Angular Material 3 + design system propriétaire, accessibilité WCAG 2.2 AA.
8. **Time-to-Value court** : MVP fonctionnel en 3 semaines vs 6-18 mois chez les concurrents.

---

## 3. Les 5 modules métier (cœur fonctionnel)

Chaque module embarque : **(a)** un **wizard guidé** étape par étape, **(b)** un **assistant IA contextuel**, **(c)** des **templates** sectoriels, **(d)** un **moteur d'export** (PDF signé, Word, blockchain hash), **(e)** un **dashboard dédié**, **(f)** un mode **mobile/offline-first** pour le terrain.

### 3.1 Module **PDCA** — Roue de Deming

- Cycles **Plan / Do / Check / Act** avec KPIs cibles à chaque étape.
- Liaison native avec les **registres ITIL CSI** (Continual Service Improvement).
- IA : prédiction de l'atteinte des objectifs en fonction des actions planifiées (LSTM).
- IA : détection de cycles "qui patinent" (Plan répétés sans Do, ou Check sans Act).
- Sortie : registre versionné, ancré blockchain à chaque transition d'étape.

### 3.2 Module **5S** — Excellence opérationnelle

- Audit terrain mobile (Angular PWA + caméra, **mode offline complet**) sur les **cinq piliers** Seiri / Seiton / Seiso / Seiketsu / Shitsuke.
- IA de **vision par ordinateur** (YOLOv8 fine-tuné) pour détecter automatiquement encombrement, étiquetage manquant, zones non conformes à partir de photos.
- Score 5S calculé, **heatmap par zone**, plan d'action généré automatiquement.
- Programmation d'audits récurrents, notifications, signature blockchain de chaque rapport.

### 3.3 Module **Cercle de Qualité**

- Constitution de groupes (5 à 10 personnes) avec rôles (animateur, secrétaire, membres).
- Salle virtuelle (WebRTC + intégration Jitsi/Teams), **comptes-rendus auto-générés** par LLM (Whisper + résumé).
- Suivi des propositions : statut, validation hiérarchique, mise en œuvre, mesure d'impact.
- IA : extraction automatique des actions, assignation suggérée, détection de propositions similaires déjà traitées dans d'autres cercles.

### 3.4 Module **DMAIC + Poka-Yoke**

- Pilotage Six Sigma : **Define / Measure / Analyze / Improve / Control**.
- Bibliothèque de **dispositifs Poka-Yoke** par secteur avec moteur de recommandation IA.
- Calculs statistiques natifs : capabilité (Cp/Cpk), tests d'hypothèse, ANOVA, **cartes de contrôle SPC** (X-R, X-S, p, np, c, u, EWMA, CUSUM).
- ML pour la détection d'anomalies multivariées (Isolation Forest, Autoencoders, LSTM-AE).
- Couplage automatique avec FMEA dynamique.

### 3.5 Module **Ishikawa**

- Éditeur visuel collaboratif (Angular + JointJS) avec **branches 6M / 7M / 8M** configurables selon le secteur.
- IA générative : à partir de la description d'un problème, suggère les causes probables par catégorie (RAG sur cas historiques tenant + corpus public).
- Couplage automatique avec **Pareto** et **5 Pourquoi** pour creuser les causes racines.
- Conversion en 1 clic : un Ishikawa devient un Plan d'action PDCA ou un projet DMAIC.

### 3.6 Référentiel transverse partagé (le vrai différenciateur)

Tous les modules écrivent dans un **référentiel commun** :

```
Problèmes ←→ Causes ←→ Actions ←→ Indicateurs ←→ Preuves ←→ Normes
```

- Une cause identifiée dans Ishikawa peut être traitée par un cycle PDCA.
- Une action DMAIC peut générer un Poka-Yoke.
- Un audit 5S peut alimenter un Cercle de Qualité.
- Une non-conformité (NC/CAPA) peut s'instancier en cycle PDCA ou en projet DMAIC selon la gravité.
- **L'agrégation est dans la donnée, pas dans l'UI.** C'est ce qu'aucun concurrent ne propose aujourd'hui.

---

## 4. Modules transverses (au-delà des 5 méthodes)

Inspirés des leaders, repensés et augmentés :

### 4.1 Document Control & GED qualité

- Versioning, e-signature ML-DSA, workflow d'approbation configurable, lecture obligatoire avec preuve.
- IA : détection de doublons, suggestion de mise à jour transverse quand une procédure change.

### 4.2 CAPA (Corrective and Preventive Actions)

- Workflow configurable, escalade automatique, lien vers cause-racine Ishikawa.
- IA : suggestion de causes-racines probables, recommandation d'actions basée sur l'efficacité passée.

### 4.3 Non-Conformance Management (NC)

- Saisie mobile avec photos, vidéos, géoloc.
- IA : clustering automatique des NC similaires, détection de patterns invisibles, prédiction d'impact.

### 4.4 Audit Management (interne / externe / fournisseurs)

- Programmation, checklists configurables, exécution mobile offline, rapport généré par LLM.
- Audits **Layered Process Audits (LPA)** terrain à la EASE.

### 4.5 Risk Management & FMEA

- FMEA dynamique réévalué automatiquement par l'IA à chaque nouvelle donnée.
- Bow-tie analysis pour les risques critiques.

### 4.6 Supplier Quality Management

- Scorecards fournisseurs, audits, NC, certificats matière.
- IA : scoring de risque fournisseur, prédiction de défaillance, alertes proactives.

### 4.7 Training & Competency Management

- Matrices de compétences, parcours par rôle, certifications, rappels automatiques.
- IA : personnalisation des parcours, simulateurs (Ishikawa virtuel, audit 5S simulé).
- Certificats blockchain vérifiables publiquement (QR code).

### 4.8 Change Management

- Demandes de changement, analyse d'impact, validation multi-niveaux.
- Lien automatique avec documents impactés, formations à mettre à jour, fournisseurs à notifier.

### 4.9 Customer Complaints & Voice of Customer

- Saisie multi-canal (email, formulaire, API, importer ITSM).
- IA : NLP de classification, sentiment analysis, détection de réclamations critiques.

### 4.10 Calibration & Equipment Management

- Plans de calibration, alertes échéances, registres MSA (Measurement System Analysis).
- Intégration IoT pour capteurs auto-rapportés.

### 4.11 EHS (Environnement, Hygiène, Sécurité)

- Module activable qui réutilise audit, NC, CAPA pour les sujets EHS.
- Conformité ISO 14001 / ISO 45001.

### 4.12 Référentiel des normes & moteur de conformité

Voir section 8.

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

## 6. Indicateurs de performance (KPI) — la richesse au cœur de la plateforme

### 6.1 Philosophie KPI

Inspirée des bonnes pratiques (NetSuite, ThoughtSpot, Bold BI, Databox, recherche académique sur les dashboards de performance) :

- **Catalogue éditorialisé** > liste à plat.
- **Définitions explicites** : nom, formule, seuils, sources de données, fréquence, propriétaire (KPI owner).
- **Hiérarchisation** : KPI stratégiques (10-15) → tactiques (50+) → opérationnels (200+).
- **Benchmarking** : comparaison anonymisée avec d'autres tenants du même secteur (opt-in).
- **Drill-down** systématique : du KPI agrégé jusqu'à l'événement source.
- **Pas plus de 8-12 KPIs** sur le dashboard exécutif (anti-pattern : "tracking too many KPIs leads to analysis paralysis").

### 6.2 KPI transverses (tous secteurs)

#### Qualité globale

- **DPMO** (Defects Per Million Opportunities), **niveau Sigma**
- **First Pass Yield (FPY)** / **First Time Right (FTR)**
- **Coût d'Obtention de la Qualité (COQ)** = prévention + détection + défaillances internes + défaillances externes
- **Taux de non-conformités** (par 1 000 unités/transactions)
- **NC répétitives** (taux, top 10)
- **Taux de réclamations clients**
- **NPS qualité** interne et externe

#### Pilotage qualité

- **Avancement des cycles PDCA** (% complétion, retards, blocages)
- **Score 5S** par zone, tendance
- **Taux de complétude Ishikawa** (par projet)
- **Cp / Cpk / Pp / Ppk** (capabilité processus DMAIC)
- **Taux de Poka-Yoke déployés** vs identifiés
- **Nombre d'idées des Cercles de Qualité** / taux de mise en œuvre / impact mesuré

#### CAPA & actions

- **Délai moyen de clôture CAPA** (par criticité)
- **Taux de récidive** post-CAPA
- **CAPA en retard** (par responsable, par site)
- **Efficacité des actions** (mesurée à 3/6/12 mois)

#### Conformité & audits

- **Taux d'alignement normatif** (par norme, par clause) — _score IA_
- **Audits réalisés vs planifiés**
- **Findings d'audit** (criticité, délai de résolution)
- **Documents à jour** (% du référentiel)
- **Formations complétées** (par rôle, par site)

#### Fournisseurs

- **Score qualité fournisseurs** (IA)
- **Taux de conformité matière**
- **Délai de traitement des NC fournisseurs**

#### Risques (FMEA)

- **RPN moyen / max / par processus**
- **Risques résiduels critiques**
- **Couverture FMEA** (% processus couverts)

### 6.3 KPI sectoriels — extraits

#### Industrie / Manufacturing

- OEE (Disponibilité × Performance × Qualité), TRS, MTBF, MTTR
- Scrap rate, rework rate, customer reject rate (PPM)
- Audit by Layer (LPA) score

#### Santé / Hôpital

- Taux d'infections nosocomiales, taux de réadmission 30j, taux de mortalité ajusté, durée moyenne de séjour
- Délai d'attente urgences, taux d'annulation chirurgie, taux de satisfaction patient (HCAHPS)
- Événements indésirables graves (EIG), taux de chute patient, erreurs médicamenteuses
- Taux d'occupation lits (cible ~85%), staffing ratio

#### IT / ITSM

- MTTR incidents, MTBF, taux de réouverture tickets, taux de respect SLA
- First Call Resolution (FCR), CSAT support
- Change Success Rate, Failed Changes
- SRE error budget consumption, % uptime

#### Banque / Finance

- Taux d'erreurs opérationnelles, pertes opérationnelles (Bâle), écart de réconciliation
- KRI (Key Risk Indicators) RCSA, taux d'incidents DORA
- Taux de fraude détectée, faux positifs LCB-FT
- Taux de résolution des réclamations clients

#### Pharma / MedTech

- Batch right first time, deviation rate, OOS (Out of Specification) rate
- Audit findings 21 CFR Part 11
- Délai de libération de lot

#### Agro-alimentaire

- Taux de rappel produit, taux de non-conformité matière première
- HACCP CCP excursions, taux de contamination
- Traçabilité amont/aval (temps de remontée)

#### Aéronautique

- Escape rate, Right First Time (RFT), supplier defect rate
- AS9100 audit findings

#### Public / Administration

- Délai moyen de traitement de dossier, taux de satisfaction usager
- Réclamations Marianne, accessibilité RGAA
- Taux de réponse aux courriers (cible 15j)

### 6.4 KPI calculés en temps réel

- Stack : **Kafka Streams + Apache Flink** pour les agrégations en flux.
- Stockage time-series : **TimescaleDB** (extension PostgreSQL) pour conserver l'unicité du datastore et garantir le multi-tenant via RLS.
- Cache de KPI précalculés : **Redis** (TTL configurable par KPI).
- API GraphQL pour la consultation par les dashboards (réduction du sur-fetching côté Angular).

### 6.5 KPI prédictifs (différenciation IA)

Là où les concurrents montrent du **passé**, QualitOS prédit l'**avenir** :

- **Prédiction d'atteinte d'objectif** : "Ton cycle PDCA atteindra 87 % de probabilité d'atteindre son KPI cible à la date X" (LSTM).
- **Prédiction de dérive qualité** : "Le processus Y va sortir des limites SPC dans 4 jours" (autoencoder + EWMA).
- **Prédiction de défaillance fournisseur** : "Le fournisseur Z présente un risque accru de NC dans le prochain mois" (XGBoost).
- **Prédiction de réclamation** : "Ton produit/service a 30 % de risque de générer une réclamation cluster dans les 14 jours" (NLP + séries temporelles).

Chaque prédiction est **explicable** (SHAP) et **actionnable** (suggestion d'action concrète).

### 6.6 Catalogue de KPI — modèle de définition

Chaque KPI du catalogue suit cette fiche :

```yaml
kpi_id: capa_closure_time_avg
name: Délai moyen de clôture CAPA
category: capa-actions
formula: AVG(closed_at - created_at) WHERE status='closed' AND criticity IN ('high','critical')
unit: days
target: < 30
threshold_warning: 30-45
threshold_critical: > 45
data_source: postgres.capa_events
refresh_frequency: realtime (Kafka stream)
owner: quality_manager
applicable_industries: [all]
related_kpis: [capa_recurrence_rate, capa_overdue_count]
explainability: |
  Indicateur pivot du processus d'amélioration.
  Cible alignée sur ISO 9001 §10.2.
```

---

## 7. Dashboards — vue 360° riche et premium

### 7.1 Hiérarchie de dashboards (3 niveaux)

#### Niveau 1 — **Executive Dashboard** (Direction Qualité, Direction Générale)

Vue stratégique en une page :

- 8-12 KPIs stratégiques (cible vs réalisé, tendance 12 mois)
- Heatmap conformité par norme
- Carte des sites avec score qualité
- Top 5 risques critiques
- Top 5 actions urgentes
- Indicateur **COQ** avec décomposition
- Prévisions IA sur 3 mois

#### Niveau 2 — **Tactical Dashboard** (Manager Qualité, par module/secteur)

Vue de pilotage opérationnel :

- KPIs du module (PDCA, 5S, DMAIC, etc.)
- Listes filtrables (cycles en cours, audits planifiés, NC ouvertes)
- Cartes de contrôle SPC live
- Alertes IA contextuelles

#### Niveau 3 — **Operational Dashboard** (équipes terrain)

Vue d'action :

- "Mes tâches du jour"
- Audits 5S à réaliser
- Formations à compléter
- Notifications IA personnalisées

### 7.2 Composants graphiques (éprouvés et premium)

| Besoin                       | Bibliothèque                         | Pourquoi                                                               |
| ---------------------------- | ------------------------------------ | ---------------------------------------------------------------------- |
| **Charts généralistes**      | **Apache ECharts** (via ngx-echarts) | Performance, customisation, large communauté, gratuit                  |
| **Charts Angular natifs**    | **NGX-Charts**                       | Intégration Angular fluide                                             |
| **Tableaux denses**          | **AG Grid Enterprise**               | Référence absolue pour data-grid pro (filtrage, pivot, virtualization) |
| **Diagrammes interactifs**   | **JointJS / GoJS**                   | Ishikawa, BPMN, workflows                                              |
| **Cartes**                   | **Mapbox GL JS** ou **Leaflet**      | Heatmaps de sites, audits géolocalisés                                 |
| **Composants UI riches**     | **Angular Material 18 + PrimeNG**    | Material pour la cohérence, PrimeNG pour Tree, Gantt, OrgChart         |
| **Datavisualisation custom** | **D3.js**                            | Pour les vues exotiques (Sankey, voronoi, custom)                      |
| **3D / Pareto avancé**       | **Plotly**                           | Quand c'est requis                                                     |
| **Real-time**                | **WebSockets + RxJS**                | Mises à jour push, pas de polling                                      |

### 7.3 Fonctionnalités dashboards "premium"

- **Drag & drop builder** : l'utilisateur compose son dashboard (à la Power BI).
- **Drill-down infini** : du KPI agrégé jusqu'à l'événement source unique.
- **Cross-filtering** : cliquer sur un point d'un graphique filtre tous les autres.
- **Time travel** : "Affiche-moi l'état du dashboard au 15 mars 2025" (event-sourcing).
- **Annotations collaboratives** : commentaires sur graphiques, partageables.
- **Export** : PNG, PDF (signé blockchain), Excel/CSV, lien direct partageable (avec signature courte).
- **Alerts personnalisables** : "Préviens-moi si COQ > 3,2 % du CA".
- **Natural Language Query (NLQ)** : "Combien de NC en mars dans l'usine A ?" → réponse + graphique généré (LLM + text-to-SQL).
- **Mode présentation** plein écran rotatif pour salle qualité.
- **Mode TV** : écran mural en hall d'usine ou pôle qualité avec rotation auto.
- **Embed** : intégration via iframe sécurisée (JWT signé) dans portails tiers.
- **Mobile-first** : tous les dashboards sont responsives et optimisés tactile.

### 7.4 Rapports

- Génération à la demande : PDF (avec **signature ML-DSA** + QR code blockchain), Word, Excel, CSV.
- Templates configurables par tenant (rapport d'audit ISO, comité qualité, revue de direction, rapport CAPA, rapport mensuel).
- Planification : envoi automatique hebdo/mensuel par email signé.
- **Storyboards IA** : l'IA génère un récit narratif autour des chiffres ("Ce mois, vos NC ont baissé de 12 % grâce aux actions issues du Cercle Qualité X. Attention cependant à la dérive du processus Y détectée le 18.").

---

## 8. Module **Standards Hub** — Référentiel des normes & moteur de certification

> **Promesse du module : "Quelle que soit la norme visée, QualitOS contient TOUT ce qu'il faut pour la certification — référentiel, documentation, processus, étapes, preuves, audit blanc, audit final."**

C'est un **module à part entière** (activable, désactivable, facturable indépendamment) qui constitue l'un des piliers différenciateurs de la plateforme. Aucun concurrent ne propose une couverture aussi complète, structurée et actionnable.

### 8.1 Vision et périmètre

Là où les concurrents listent quelques normes en checklist, QualitOS livre, **pour chaque norme**, un **dossier de certification clé en main** :

1. **Catalogue normatif** — fiche détaillée de la norme.
2. **Bibliothèque documentaire** — modèles de tous les documents exigés.
3. **Cartographie des processus** — processus types alignés sur les exigences.
4. **Roadmap de certification** — étapes chronologiques avec livrables, durées, responsables.
5. **Moteur de preuves** — collecte, liaison, scoring d'alignement.
6. **Audit blanc IA** — simulation d'audit avant le réel, avec écarts et plan de remédiation.
7. **Suivi continu post-certification** — alertes de dérive, gestion des non-conformités majeures/mineures, préparation aux audits de surveillance.

### 8.2 Référentiel intégré complet (livré progressivement)

#### A. Normes systèmes de management (ISO/IEC HLS — High Level Structure)

- **ISO 9001:2015** — Management de la qualité
- **ISO 14001:2015** — Management environnemental
- **ISO 45001:2018** — Santé & sécurité au travail
- **ISO 27001:2022** — Sécurité de l'information (SMSI)
- **ISO 22301:2019** — Continuité d'activité (PCA)
- **ISO 50001:2018** — Management de l'énergie
- **ISO 37001:2016** — Anti-corruption
- **ISO 37301:2021** — Compliance management
- **ISO 31000:2018** — Management du risque
- **ISO 19011:2018** — Audit des systèmes de management
- **ISO 10002:2018** — Traitement des réclamations clients
- **ISO 9004:2018** — Excellence durable
- **ISO 26000:2010** — Responsabilité sociétale (RSE)

#### B. Normes industrielles & sectorielles

- **IATF 16949:2016** — Automobile
- **AS9100D / EN 9100** — Aéronautique
- **AS9110 / AS9120** — Maintenance aéro & distributeurs
- **NADCAP** — Procédés spéciaux aéro
- **VDA 6.3 / VDA 6.5** — Audit processus & produit (constructeurs allemands)
- **API Q1 / API Q2** — Pétrole & gaz
- **ISO 19650** — BIM / BTP
- **ISO 17025:2017** — Compétence laboratoires d'essais

#### C. Santé & dispositifs médicaux

- **ISO 13485:2016** — Dispositifs médicaux
- **ISO 14971:2019** — Gestion du risque MD
- **ISO 15189:2022** — Laboratoires médicaux
- **MDR (UE) 2017/745** — Règlement européen MD
- **IVDR (UE) 2017/746** — Diagnostic in vitro
- **FDA 21 CFR Part 820** — Quality System Regulation (US)
- **FDA 21 CFR Part 11** — Signatures électroniques
- **FDA 21 CFR Part 210/211** — cGMP médicaments
- **ICH Q7-Q14** — Qualité pharmaceutique
- **GAMP 5 (2nd edition)** — Validation systèmes informatisés
- **HAS Certification V2024** — Établissements de santé France
- **Joint Commission / JCI** — Certification hôpitaux internationale
- **HL7 FHIR / HIPAA** — Interopérabilité & confidentialité santé

#### D. Agroalimentaire & agriculture

- **ISO 22000:2018** — Sécurité des denrées alimentaires
- **FSSC 22000 v6** — Schéma certification GFSI
- **IFS Food v8** — Standard distributeurs européens
- **BRCGS Food v9** — Standard britannique
- **GlobalG.A.P.** — Bonnes pratiques agricoles
- **HACCP Codex Alimentarius** — Maîtrise des points critiques
- **Halal / Casher / Bio** — Certifications confessionnelles & bio (FR Bio, USDA Organic, EU Organic)

#### E. IT / Cybersécurité / Cloud

- **ISO/IEC 20000-1:2018** — Management des services IT
- **ISO/IEC 27017** — Sécurité cloud
- **ISO/IEC 27018** — Protection PII dans le cloud
- **ISO/IEC 27701** — Privacy Information Management
- **ITIL 4** — Cadre de gestion des services
- **CMMI v3.0** — Maturité processus
- **SOC 1 / SOC 2 / SOC 3 (AICPA)** — Audits Trust Services Criteria
- **HDS (Hébergeur Données de Santé)** — France
- **SecNumCloud** — ANSSI France
- **PCI DSS v4.0** — Données cartes bancaires
- **NIST CSF 2.0** — Cybersecurity Framework
- **CIS Controls v8** — Critical Security Controls

#### F. Finance / Régulation

- **DORA (UE) 2022/2554** — Résilience opérationnelle numérique
- **Bâle III/IV / CRR3** — Banque
- **Solvabilité II** — Assurance
- **MiFID II / MiFIR** — Marchés financiers
- **EMIR / SFTR** — Dérivés & repo
- **LCB-FT (5e/6e directive AML)** — Anti-blanchiment

#### G. Données personnelles & numérique

- **RGPD (UE) 2016/679** — Protection des données personnelles
- **CCPA / CPRA** — Californie
- **AI Act (UE) 2024** — Intelligence artificielle
- **NIS 2 (UE) 2022/2555** — Cybersécurité critique
- **Cyber Resilience Act (UE)** — Produits numériques

#### H. Énergie / Environnement / Durabilité

- **ISO 55001** — Gestion d'actifs
- **ISO 14064** — Gaz à effet de serre
- **CSRD / ESRS** — Reporting durabilité UE
- **GRI Standards** — Reporting RSE
- **TCFD / ISSB IFRS S1-S2** — Climat
- **NERC CIP** — Critical Infrastructure Protection (énergie US)

#### I. Éducation / Formation

- **ISO 21001:2018** — Organismes éducatifs
- **Qualiopi** — France (formation professionnelle)

#### J. Service public / Accessibilité

- **ISO 18091** — Qualité service public local
- **Marianne** (France) — Engagement service public
- **RGAA 4.1 / WCAG 2.2** — Accessibilité numérique

### 8.3 Modélisation d'une norme — modèle de données universel

Chaque norme est modélisée selon un **schéma unique** qui permet le moteur d'alignement IA :

```yaml
norm:
  id: iso-9001
  full_name: "ISO 9001:2015 - Systèmes de management de la qualité - Exigences"
  publisher: ISO
  publication_date: 2015-09-15
  current_version: "2015"
  next_revision_planned: "2025-2026"
  family: HLS # High Level Structure
  certification_body_required: true # certification accréditée par COFRAC/UKAS/etc.
  applicable_industries: [all]
  related_norms: [iso-9000, iso-9004, iso-19011]
  languages: [en, fr, es, de, ar, ja, zh]

  structure:
    - section: "4. Contexte de l'organisme"
      clauses:
        - id: "4.1"
          title: "Compréhension de l'organisme et de son contexte"
          requirements:
            - id: "4.1.1"
              text: "L'organisme doit déterminer les enjeux externes et internes…"
              must_or_should: must
              evidence_types: [analyse_swot, pestel, doc_strategique]
              measurable_criteria:
                - "Enjeux documentés"
                - "Revue au moins annuelle"
              risk_if_missing: high
              ai_inference_hooks:
                - module: pdca
                  trigger: "Cycle PDCA de revue stratégique annuelle"
              related_documents:
                - template_id: "TPL-CTX-001"
                  name: "Analyse de contexte SWOT/PESTEL"

  documents_required:
    mandatory:
      - id: "DOC-MQ"
        name: "Manuel Qualité (recommandé même si non obligatoire en 2015)"
        template: "/templates/iso-9001/manuel-qualite.docx"
      - id: "DOC-POL-Q"
        name: "Politique Qualité"
      - id: "DOC-PROC"
        name: "Procédures documentées (clauses spécifiques)"
    recommended: [...]

  processes_required:
    - id: "PROC-DIRE"
      name: "Processus de direction (revue de direction)"
      maps_to_clauses: ["9.3"]
      template_bpmn: "/processes/iso-9001/revue-direction.bpmn"
    - id: "PROC-AUDIT"
      name: "Audit interne"
      maps_to_clauses: ["9.2"]
    - id: "PROC-AC"
      name: "Actions correctives"
      maps_to_clauses: ["10.2"]
    # ... 15+ processus types

  certification_path:
    estimated_duration_months: 9-18
    estimated_cost_range_eur: [10000, 80000] # selon taille org
    stages: [...] # voir 8.5

  surveillance_audit_frequency: annual
  recertification_cycle_years: 3
```

### 8.4 La fiche complète d'une norme dans le module

Pour chaque norme, l'utilisateur accède à un **dossier complet** structuré en 8 onglets :

#### Onglet 1 — **Vue d'ensemble**

Périmètre, applicabilité, autorité de certification, cycle de recertification, coût estimé, durée moyenne, niveau de difficulté (1-5).

#### Onglet 2 — **Exigences détaillées**

Arborescence Sections → Clauses → Exigences avec, pour chaque item :

- Texte de l'exigence
- Caractère (obligatoire / recommandé / informatif)
- Critères mesurables
- Types de preuves attendues
- Score de conformité actuel du tenant (calculé en continu par l'IA)
- Suggestions IA pour combler les écarts

#### Onglet 3 — **Bibliothèque documentaire**

Tous les **modèles de documents** prêts à utiliser :

- Manuel Qualité, Politique Qualité, Cartographie des processus
- Procédures documentées (audit, AC, AP, maîtrise des documents…)
- Enregistrements (PV, comptes-rendus, rapports d'audit)
- Modes opératoires, instructions de travail
- Fiches de fonction, organigrammes
- Templates en .docx, .pptx, .xlsx, .bpmn, .md, signés et versionnés.
- **Génération IA** : à partir du contexte du tenant, l'IA pré-remplit le document avec son nom, son secteur, ses processus connus.

#### Onglet 4 — **Cartographie des processus**

- Bibliothèque de processus types liés à chaque norme (BPMN 2.0).
- Importables et adaptables dans le module Workflow Designer.
- Liaison automatique entre processus et clauses qu'ils couvrent.
- Heatmap : "Quels processus couvrent quelles clauses ?"

#### Onglet 5 — **Roadmap de certification (étape par étape)**

Voir section 8.5.

#### Onglet 6 — **Mes preuves**

- Vue agrégée de tous les éléments du tenant (documents, audits, formations, équipements, KPIs) liés à cette norme.
- Pour chaque clause : preuves rattachées + score d'alignement IA + alertes si preuve obsolète.
- Génération en 1 clic du **Dossier de certification** complet (PDF zippé, signé ML-DSA, ancré blockchain).

#### Onglet 7 — **Audit blanc IA**

Avant l'audit officiel, l'IA simule un audit :

- Génère 30 à 100 questions ciblées sur les clauses à risque.
- Confronte les réponses du tenant aux preuves disponibles.
- Restitue un **rapport d'écarts** (gap analysis) avec criticité, recommandations et plan de remédiation auto-créé.

#### Onglet 8 — **Veille normative**

- Suivi de l'évolution de la norme (révisions, amendements, addendums).
- Notification automatique aux tenants concernés.
- Migration assistée IA : _"La nouvelle version de ISO 9001 ajoute la clause X.Y, voici les ajustements suggérés pour ton SMQ"_.

### 8.5 Roadmap de certification générique (étape par étape)

Pour chaque norme, le module embarque un **plan de certification chronologique** adapté, basé sur la trame ci-dessous (instanciée par l'IA selon la norme et la maturité du tenant) :

| #   | Étape                                                     | Durée typique | Livrables clés                                             | Acteurs                                 | Module(s) QualitOS impliqué(s)               |
| --- | --------------------------------------------------------- | ------------- | ---------------------------------------------------------- | --------------------------------------- | -------------------------------------------- |
| 1   | **Cadrage & engagement direction**                        | 2-4 sem       | Lettre d'engagement direction, périmètre, budget, planning | Direction, Pilote certif                | Document Control, PDCA                       |
| 2   | **Diagnostic initial / gap analysis**                     | 3-5 sem       | Rapport d'écarts, matrice de conformité initiale           | Pilote + Auditeur interne ou consultant | Audit, Standards Hub (audit blanc IA)        |
| 3   | **Définition de la politique & objectifs**                | 2 sem         | Politique signée, objectifs SMART, indicateurs cibles      | Direction Qualité                       | Document Control, KPI Catalog                |
| 4   | **Analyse du contexte & parties intéressées**             | 2-3 sem       | SWOT/PESTEL, registre parties intéressées                  | Pilote + équipes                        | Document Control, Templates IA               |
| 5   | **Analyse des risques & opportunités**                    | 3-4 sem       | Cartographie risques, FMEA, plans de traitement            | Risk Manager                            | Risk/FMEA, Ishikawa                          |
| 6   | **Cartographie des processus**                            | 4-6 sem       | Cartographie globale, fiches processus, indicateurs        | Pilotes processus                       | Workflow Designer (BPMN), Standards Hub      |
| 7   | **Documentation système**                                 | 6-10 sem      | Manuel, procédures, modes opératoires, enregistrements     | Tous pilotes                            | Document Control + génération IA             |
| 8   | **Sensibilisation & formation**                           | 4-6 sem       | Plan de formation, support, tests                          | RH + Formation                          | Module Formation (gamification + e-learning) |
| 9   | **Mise en œuvre opérationnelle**                          | 8-16 sem      | Application réelle des processus, premiers enregistrements | Toute l'organisation                    | Tous modules                                 |
| 10  | **Audits internes (1er cycle)**                           | 4-8 sem       | Programme d'audit, rapports, écarts                        | Auditeurs internes                      | Audit Management                             |
| 11  | **Actions correctives**                                   | 4-12 sem      | CAPA, preuves d'efficacité                                 | Pilotes + responsables                  | CAPA, NC                                     |
| 12  | **Revue de direction**                                    | 1-2 sem       | Compte-rendu de revue, décisions, ressources allouées      | Direction                               | PDCA, Document Control                       |
| 13  | **Pré-audit (optionnel mais recommandé)**                 | 1 sem         | Rapport d'audit blanc, écarts résiduels                    | Auditeur tiers ou IA                    | Standards Hub (audit blanc IA)               |
| 14  | **Audit de certification — Étape 1 (revue documentaire)** | 1-2 j         | Rapport étape 1, points d'attention                        | Organisme certificateur                 | Standards Hub, Document Control              |
| 15  | **Audit de certification — Étape 2 (audit terrain)**      | 2-5 j         | Rapport étape 2, NC majeures/mineures, certificat          | Organisme certificateur                 | Standards Hub, tous modules                  |
| 16  | **Traitement des NC d'audit**                             | 1-3 mois      | Plan d'action, preuves de levée                            | Pilote                                  | CAPA, Audit                                  |
| 17  | **Obtention du certificat**                               | —             | Certificat valide 3 ans                                    | —                                       | Standards Hub (registre certifs)             |
| 18  | **Audits de surveillance annuels**                        | 1-2 j/an      | Rapports, levée NC                                         | Organisme certificateur                 | Standards Hub                                |
| 19  | **Recertification (an 3)**                                | 3-5 j         | Audit complet renouvellement                               | Organisme certificateur                 | Standards Hub                                |

Chaque étape inclut : checklist, livrables attendus (avec templates), responsable assigné, alerte de retard, lien vers les modules concernés, intégration dans un cycle PDCA global.

### 8.6 Roadmaps spécifiques (exemples)

Le module embarque des **variantes spécifiques** par norme, avec leurs particularités :

- **ISO 13485 + MDR** : ajoute la classification du dispositif, la documentation technique (Annexe II/III MDR), l'évaluation clinique, le PMS (post-market surveillance), la vigilance.
- **FDA 21 CFR Part 11** : ajoute la qualification d'infrastructure (IQ), opérationnelle (OQ), de performance (PQ), validation des systèmes informatisés (CSV), gestion des audit trails électroniques.
- **HACCP / ISO 22000** : ajoute l'analyse des dangers (microbio, chimique, physique, allergènes), CCP, PRP, PRPo, plans de surveillance.
- **ISO 27001** : ajoute la SoA (Statement of Applicability), traitement du risque selon Annexe A, ISMS scope, déclaration ANSSI/CNIL si applicable.
- **IATF 16949** : ajoute APQP, PPAP, MSA, SPC obligatoires, exigences clients spécifiques (CSR Renault, Stellantis, BMW, Toyota…).
- **AS9100** : ajoute counterfeit parts prevention, configuration management, FAI (First Article Inspection), risk-based thinking renforcé.
- **DORA** : ajoute le registre des tiers TIC, tests de résilience (TLPT), gestion des incidents majeurs avec reporting régulateur.
- **CSRD / ESRS** : ajoute la double matérialité, indicateurs E1-S4-G1, taxonomie verte UE, audit limité de durabilité.
- **HDS** : ajoute les exigences ANS (Agence du Numérique en Santé), ISO 27001 + cadre national, hébergement physique en UE.

### 8.7 Moteur d'alignement IA — fonctionnement détaillé

L'IA cartographie automatiquement les **pratiques observées** (audits, cycles, projets, documents, formations, équipements, KPIs) avec les **exigences normatives** :

```
Exigence normative (ex: ISO 9001 §7.5.3 "Maîtrise des informations documentées")
     ↓
Critères mesurables (procédure de gestion documentaire en place, versioning actif, accès contrôlé…)
     ↓
Recherche IA (RAG) dans le datastore tenant
     ↓
Score de couverture (%) + liste de preuves identifiées + écarts détectés
     ↓
Recommandations actionnables (création d'un cycle PDCA, génération d'une procédure, planification d'une formation)
```

**Sortie en continu** : score de conformité par norme, par section, par clause, mis à jour à chaque événement (nouvelle preuve, document mis à jour, audit réalisé). Tableau de bord dédié.

**Audit-ready** : à tout moment, l'auditeur externe peut recevoir un **dossier complet** (PDF + ZIP de preuves + lien blockchain) avec preuves liées aux clauses, signé ML-DSA.

### 8.8 Génération IA assistée des documents normatifs

L'IA exploite :

- Le contexte du tenant (secteur, taille, sites, langues).
- Les modèles de référence du Standards Hub.
- Les bonnes pratiques publiques (corpus open-source).
- L'historique du tenant (versions précédentes, autres normes déjà couvertes — réutilisation transversale).

Pour générer en quelques minutes : Manuel Qualité, Politique, Procédures, Modes opératoires, Plans d'audit, Comptes-rendus de revue de direction, Cartographies de processus.

L'utilisateur **valide** chaque document (workflow d'approbation), aucun document n'est publié sans signature humaine + ancrage blockchain.

### 8.9 Multi-certifications & système intégré (IMS)

QualitOS gère nativement les **systèmes de management intégrés** (Integrated Management Systems) :

- Un même tenant peut viser ISO 9001 + ISO 14001 + ISO 45001 + ISO 27001 simultanément.
- L'IA détecte les **clauses communes (HLS)** et **mutualise les preuves** : un même document de "Politique" couvre plusieurs normes ; un audit unique peut couvrir plusieurs systèmes.
- Matrice de **co-couverture** : "Cette procédure couvre §5.2 ISO 9001 + §5.2 ISO 14001 + §5.2 ISO 45001".
- Économie de 30 à 50 % d'effort par rapport à une approche silotée.

### 8.10 Mise à jour & veille normative

- Un éditeur (rôle interne ou super admin) peut versionner les référentiels (Git-like, avec branches, diff, merge).
- Notification automatique aux tenants impactés lors d'une révision normative (avec analyse d'impact générée par l'IA).
- Migration assistée par IA : _"La révision de ISO 9001:2025 ajoute la clause X.Y sur le climat ; voici les 4 ajustements suggérés à ton SMQ avec impact estimé en jours/homme"_.
- Publication d'**Addenda éditoriaux** (commentaires d'experts internes, exemples sectoriels) pour aider à l'interprétation.

### 8.11 Marketplace de packs normatifs

À terme, le module accueille un **marketplace** :

- Packs sectoriels avancés produits par des consultants partenaires (ex. "Pack ISO 13485 pour startup MedTech", "Pack HACCP pour restauration collective").
- Validation Anthropic / éditeur principal avant publication.
- Modèle de revenu partagé.
- Permet de **scaler** l'offre normative sans grossir l'équipe interne.

---

## 9. Module **IoT & Edge Connectivity** — universalité multi-domaines

> **Promesse du module : "Quel que soit le domaine — usine, hôpital, exploitation agricole, laboratoire, smart city, datacenter — QualitOS s'interface nativement avec les capteurs, équipements et systèmes du terrain."**

C'est le module qui transforme QualitOS d'un QMS classique (déconnecté du shop-floor, comme les concurrents) en un **système nerveux qualité connecté en temps réel** au monde physique.

### 9.1 Pourquoi un module IoT natif

La principale faiblesse identifiée chez les leaders du marché (cf. section 1.2) est la **déconnexion entre le QMS et le terrain**. MasterControl gère les documents, ETQ gère les workflows, mais **ni l'un ni l'autre ne lit la vibration de la machine ou la température du réfrigérateur médical**. La cause-racine d'un défaut reste invisible.

QualitOS comble ce vide par un **IoT Hub natif, multi-protocoles, multi-domaines**.

### 9.2 Domaines couverts (universalité par design)

| Domaine                       | Sources de données IoT typiques                                                                                             | Cas d'usage QualitOS                                                                        |
| ----------------------------- | --------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| **Industrie / Manufacturing** | OPC-UA (PLC Siemens, Schneider, Rockwell), MQTT, Modbus, capteurs vibration/température/pression, MES, SCADA, robots cobots | SPC en temps réel, prédiction de dérive, détection panne, lien défaut→machine               |
| **Santé / Hôpital**           | DICOM (imagerie), HL7 FHIR (EHR), capteurs biomédicaux, frigos pharma, autoclaves, biocompatibilité                         | Surveillance T° médicaments/vaccins, traçabilité dispositifs, détection anomalies cliniques |
| **Pharma / Biotech**          | Bioreactors, chromatographes, spectro, monitoring environnement (T°, HR, particules), Building Management System            | GxP en continu, batch right first time, détection excursion CCP                             |
| **Agriculture / AgriTech**    | Stations météo, capteurs sol (humidité, NPK), drones, GPS tracteurs, capteurs élevage, irrigation connectée, serres         | Traçabilité GlobalG.A.P., HACCP au champ, optimisation phytosanitaire, bien-être animal     |
| **Agro-alimentaire**          | Capteurs T° chaîne du froid, pH, débitmètres, étiqueteuses, RFID lots, systèmes pesage                                      | HACCP CCP en continu, traçabilité lot complète, alertes excursion                           |
| **Laboratoire**               | LIMS, automates de paillasse, balances connectées, thermo-cycleurs, microscopes                                             | ISO 15189, traçabilité analytique, calibration auto                                         |
| **Énergie / Utilities**       | Compteurs, smart grid, capteurs environnementaux, SCADA énergie                                                             | ISO 50001, NERC CIP, surveillance assets critiques                                          |
| **BTP / Smart Buildings**     | BIM, BMS, capteurs structure (béton, acier, déformation), badges chantier, drones                                           | ISO 19650, qualité chantier, sécurité, levée de réserves                                    |
| **Logistique / Transport**    | GPS, RFID conteneurs, capteurs T° transport, ELD (Electronic Logging Devices)                                               | ISO 39001, OTIF, chaîne du froid, qualité service                                           |
| **Datacenter / IT**           | Sondes T°/HR salles, métriques serveurs (Prometheus), APM, logs sécurité                                                    | ISO 27001, ISO 50001 datacenter, SLA, SRE                                                   |
| **Retail / Magasin**          | Caméras, capteurs étagères (poids/présence), sondes T° vitrine, files d'attente vidéo                                       | Audit magasin auto, fraîcheur produits, qualité service                                     |
| **Smart City / Public**       | Capteurs qualité air, bruit, trafic, propreté urbaine                                                                       | ISO 18091, qualité service public, environnement                                            |

### 9.3 Architecture IoT — vue d'ensemble

```
┌──────────────────────────────────────────────────────────────┐
│  Capteurs / Équipements / Systèmes terrain (multi-domaines)  │
│  (PLC, biomed, agro, BMS, GPS, RFID, caméras, LIMS, MES...)  │
└──────────────────────────────┬───────────────────────────────┘
                               │
                  ┌────────────┴────────────┐
                  │                         │
        Protocoles legacy/proprio      Protocoles modernes
        (Modbus, BACnet, KNX,         (MQTT 5, OPC-UA, AMQP,
         Profinet, EtherCAT)          CoAP, HL7 FHIR, DICOM,
                  │                    Sparkplug B, LoRaWAN)
                  ▼                         ▼
        ┌─────────────────────────────────────────┐
        │     QualitOS Edge Gateway (K3s)         │
        │  - Containerisé, multi-arch (ARM/x86)   │
        │  - Buffer local, store-and-forward      │
        │  - Modèles IA embarqués (TFLite/ONNX)   │
        │  - Sécurité : mTLS, signed firmware     │
        └────────────────────┬────────────────────┘
                             │ TLS 1.3 + mTLS
                             ▼
        ┌─────────────────────────────────────────┐
        │     QualitOS IoT Hub (Cloud/On-prem)    │
        │  - Apache Kafka + MQTT broker (EMQX)    │
        │  - Schema Registry (Avro)               │
        │  - Device Registry (provisioning)       │
        │  - Twin / Shadow management             │
        └────────────────────┬────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
    ┌─────────────┐  ┌──────────────┐  ┌──────────────┐
    │ Stream      │  │ Time-series  │  │ AI Service   │
    │ Processing  │  │ Storage      │  │ (anomaly,    │
    │ (Flink,     │  │ (TimescaleDB │  │  prediction) │
    │  Kafka      │  │  + InfluxDB  │  │              │
    │  Streams)   │  │  + Mongo)    │  │              │
    └──────┬──────┘  └──────┬───────┘  └──────┬───────┘
           │                │                  │
           └────────┬───────┴──────────────────┘
                    ▼
         ┌─────────────────────────┐
         │  Quality Engine         │
         │  - SPC en continu       │
         │  - Auto-déclenche NC    │
         │  - Alimente KPIs        │
         │  - Génère évidences     │
         │    blockchain           │
         └─────────────────────────┘
```

### 9.4 Protocoles supportés (V1)

#### Industriels

- **OPC-UA** (IEC 62541) avec PubSub, méthode standard pour PLCs modernes.
- **MQTT 3.1.1 / 5.0** (broker EMQX en interne) — IoT généraliste.
- **Sparkplug B** sur MQTT — modèle de données industriel unifié.
- **Modbus TCP / RTU** — équipements legacy.
- **Profinet, EtherCAT** — via passerelles dédiées.
- **BACnet, KNX, LonWorks** — building/HVAC.
- **CAN-bus / CANopen** — véhicules, automatisation.

#### Santé / Médical

- **HL7 v2 / HL7 FHIR R5** — EHR/HIS.
- **DICOM** — imagerie.
- **IEEE 11073** — dispositifs médicaux personnels.
- **SDC (IEEE 11073-20701)** — interopérabilité équipements bloc opératoire.
- **ASTM E1394 / LIS01-A2** — automates labo.

#### Agriculture / IoT longue portée

- **LoRaWAN** (MAC v1.0.4) — capteurs autonomes longue portée.
- **Sigfox** — capteurs ultra-low-power.
- **NB-IoT / LTE-M** — IoT cellulaire.
- **ISOBUS (ISO 11783)** — engins agricoles.

#### Web / Cloud

- **REST + Webhooks**, **GraphQL**, **WebSocket**, **gRPC**.
- **AMQP 1.0** (RabbitMQ).
- **Kafka Connect** — connecteurs ERP, CMMS, MES.

#### Légerrement / contraints

- **CoAP** (IETF RFC 7252).
- **DTLS** pour le transport sécurisé sur UDP.

### 9.5 QualitOS Edge Gateway

Composant déployable **sur site** (usine, hôpital, ferme, datacenter, magasin) :

- **Distribution** : K3s (Kubernetes léger) sur Linux, multi-arch (ARM64 pour Raspberry Pi/Jetson, x86 pour serveurs industriels).
- **Footprint** : 512 Mo RAM minimum, fonctionne sur appliance type Intel NUC, Siemens IPC, Dell Edge Gateway.
- **Fonctions** :
  - Découverte automatique des équipements (auto-discovery OPC-UA, mDNS, BACnet broadcast).
  - Buffer local (24-72h) pour résilience aux coupures réseau (store-and-forward).
  - Filtrage/agrégation locale pour réduire la bande passante (ex : moyenne 1-min vs flux 10 Hz).
  - **Inférence IA locale** : modèles ONNX/TFLite embarqués pour détection d'anomalies sans aller-retour cloud (cas usage : détection visuelle 5S, alarme T° frigo médical, alerte vibration anormale).
  - **Sécurité** : firmware signé Cosign, TLS 1.3 + mTLS vers le hub, attestation TPM 2.0 si disponible.
  - **Mode déconnecté** : fonctionne 100 % offline, sync différée intelligente.
  - **OTA updates** : mise à jour gérée par GitOps (ArgoCD edge).

### 9.6 Device Registry & Digital Twin

- **Provisioning automatique** des équipements : QR code, certificats X.509 par device, rotation auto.
- **Digital Twin / Device Shadow** : représentation logique de chaque équipement avec son état (modèle Eclipse Ditto ou équivalent open-source).
- **Hiérarchie ISA-95** pour l'industrie (Enterprise → Site → Area → Work Center → Equipment), exposée naturellement aux dashboards.
- **Tagging multi-dimensionnel** : par site, ligne, criticité, norme couverte, propriétaire.
- **Lifecycle complet** : provisioning → exploitation → maintenance → décommissionnement, traçé en blockchain.

### 9.7 IA & IoT — convergence

L'IA exploite massivement les flux IoT pour :

- **Détection d'anomalies multivariées** sur signaux temps réel (Autoencoders, Isolation Forest, LSTM-AE) — au-delà des règles SPC classiques.
- **Maintenance prédictive** (RUL — Remaining Useful Life) — anticipe les pannes avant qu'elles ne génèrent des défauts qualité.
- **Vision industrielle / médicale / agricole** : YOLOv8 sur flux caméra pour détecter encombrements 5S, EPI manquants, défauts visuels produits, mauvaises herbes au champ, détection d'anomalies en imagerie médicale (avec validation médecin).
- **Corrélation cross-domaines** : "La hausse du taux de défaut produit X corrèle à la dérive du capteur de pression Y depuis 6 jours" — analyse causale automatique.
- **Acoustique** (sonomètres, micros industriels) : détection roulements défaillants, anomalies process.
- **Audit automatisé** : un audit 5S peut être pré-rempli par les capteurs (T°, propreté capteur poussière, présence détectée).

### 9.8 Sécurité IoT (zero-trust)

- **mTLS obligatoire** entre Edge Gateway et IoT Hub.
- **Certificats X.509 par device**, rotation 90j max, révocation immédiate possible (CRL/OCSP).
- **Signed firmware** sur les Edge Gateways (Cosign + Sigstore).
- **Network segmentation** : VLAN dédié IoT, ACLs strictes, pare-feu applicatif.
- **Aucune connexion entrante** sur l'Edge Gateway : seul le sortant TLS est autorisé (pattern reverse-tunnel).
- **Audit complet** : chaque commande envoyée à un équipement (write OPC-UA, FHIR PUT, etc.) est journalisée, signée, ancrée blockchain.
- **AI Act compliance** : si l'IA prend une décision sur un système physique critique (ex. : arrêt machine), traçabilité complète + supervision humaine obligatoire.
- **Cyber Resilience Act** : SBOM des Edge Gateways, gestion des vulnérabilités.

### 9.9 Cas d'usage emblématiques

#### Industriel — usine

Capteur vibration cobot → MQTT → Edge Gateway → IoT Hub → Stream Processing → détection anomalie → **création automatique d'une NC + alerte au manager qualité + lien vers la fiche FMEA + déclenchement d'un Cycle PDCA**.

#### Santé — hôpital

Frigo médicaments → sonde T° HL7 → Hub → seuil dépassé → **NC auto + alerte pharmacien + lot médicaments mis en quarantaine + preuve blockchain pour ANSM/ARS**.

#### Agriculture — exploitation céréalière

Capteurs sol + station météo + drone NDVI → Edge Gateway local (4G/LoRa) → Hub → IA agronomique → **alerte stress hydrique + plan d'irrigation + traçabilité GlobalG.A.P. + preuve d'application du cahier des charges Bio**.

#### Pharma — bioréacteur

Sonde pH/O₂/T° → OPC-UA → Hub → batch en cours → **excursion CCP détectée → arrêt batch + NC GxP + investigation + dossier audit FDA pré-rempli + signature 21 CFR Part 11**.

#### Smart Building — datacenter

Sondes salles + métriques serveurs → MQTT/Prometheus → Hub → corrélation IA → **alerte risque thermique + ouverture incident ITSM + lien ISO 27001 §A.7.5 (sécurité physique)**.

#### Logistique — transport pharma

GPS + sonde T° conteneur → NB-IoT → Hub → trajectoire suivie + **alerte excursion T° → quarantaine au déchargement + preuves blockchain pour assurance et BPDG**.

### 9.10 Adaptation aisée à n'importe quel domaine

Le module est conçu pour qu'un nouveau domaine puisse être supporté **sans développement core** :

1. **Adapter de protocole** : si un protocole exotique est nécessaire (ex : un bus propriétaire industriel), il est codé comme un **plugin Java SPI** et déployé sur l'Edge Gateway.
2. **Mapping de données** : un fichier YAML déclare comment les variables du protocole sont mappées dans le modèle QualitOS (point de mesure → KPI cible).
3. **Industry Pack** (cf. §5) : fournit les seuils, normes, processus types du domaine.
4. **Modèles IA pré-entraînés** par domaine : disponibles dans le **Model Hub** (TorchHub interne du tenant), ré-entraînables sur les données locales.

Exemple : ajouter le support d'une exploitation viticole (nouveau sous-domaine agro) prend 2 à 4 semaines pour un intégrateur, sans toucher au cœur de la plateforme.

---

## 10. Architecture technique

### 10.1 Vue d'ensemble (modulith → microservices)

```
┌─────────────────────────────────────────────────────────────┐
│  Angular 18 SPA + PWA (Material 3, Design System QualitOS)  │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTPS / mTLS
┌──────────────────────────────▼──────────────────────────────┐
│         API Gateway (Spring Cloud Gateway + WAF)            │
│   Rate-limit · OAuth2/OIDC · Validation OpenAPI · CORS      │
└──┬────────────┬────────────┬────────────┬─────────┬─────────┘
   │            │            │            │         │
┌──▼──┐     ┌───▼───┐    ┌───▼───┐    ┌───▼───┐ ┌───▼────┐
│Core │     │Quality│    │ IA    │    │Block- │ │Industry│
│API  │     │Engine │    │Service│    │chain  │ │Adapter │
│J25  │     │J25    │    │Python │    │Service│ │Service │
└──┬──┘     └───┬───┘    └───┬───┘    └───┬───┘ └───┬────┘
   │            │            │            │         │
   └────┬───────┴────────────┴────────────┴─────────┘
        │
   ┌────▼────────┬───────────┬───────────┬─────────────┐
   │ PostgreSQL  │ MongoDB 7 │ Redis 7   │ TimescaleDB │
   │ + RLS multi-│ (docs/IA  │ (cache,   │ (KPI time-  │
   │ tenant      │ logs)     │ sessions) │ series)     │
   └─────────────┴───────────┴───────────┴─────────────┘
        │
   ┌────▼─────────┐  ┌──────────────┐  ┌────────────────┐
   │ Keycloak 25  │  │ Kafka 3.8    │  │ Hyperledger    │
   │ + SPI LDAP   │  │ + Schema Reg │  │ Fabric 2.5     │
   │ + OIDC/OAuth2│  │ (event src)  │  │                │
   └──────────────┘  └──────────────┘  └────────────────┘
        │
   ┌────▼──────────────────────────────────────────────────┐
   │  Connecteurs : MES (OPC-UA, MQTT) · ERP (SAP, Oracle) │
   │  ITSM (ServiceNow, Jira) · EHR (HL7 FHIR) · IoT       │
   │  GED (SharePoint, Alfresco) · Comm (Slack, Teams)     │
   └───────────────────────────────────────────────────────┘
```

### 10.2 Stack technique imposée

| Couche                    | Technologie                                                                                        | Justification                                                                  |
| ------------------------- | -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| **Frontend**              | Angular 18+, TypeScript 5.5+, Angular Material 18, NGRX, Cypress, Storybook                        | Stable, typé, écosystème mature                                                |
| **Backend Core**          | Java 25 LTS, Spring Boot 3.5, Spring Security 6.4, Spring Modulith, Hibernate 6.6                  | Java 25 GA septembre 2025, support long terme                                  |
| **IA**                    | Python 3.13, FastAPI, PyTorch 2.5, scikit-learn, LangChain, LlamaIndex, ONNX Runtime, Hugging Face | Standard de fait pour ML/DL                                                    |
| **Auth**                  | Keycloak 25+, OIDC, OAuth2, **SPI LDAP custom**, MFA TOTP/WebAuthn, FIDO2                          | Open-source, extensible, certifié                                              |
| **Bases**                 | PostgreSQL 17 (RLS multi-tenant) + TimescaleDB, MongoDB 7, Redis 7                                 | PostgreSQL pour transactionnel, TimescaleDB pour KPIs, Mongo pour documents IA |
| **Messaging**             | Apache Kafka 3.8 + Schema Registry (Avro) + Kafka Streams + Flink                                  | Event-driven, audit-ready, agrégations temps réel                              |
| **Blockchain**            | Hyperledger Fabric 2.5 (consortium privé)                                                          | Permissionné, conforme RGPD                                                    |
| **Crypto post-quantique** | Open Quantum Safe (liboqs), **ML-KEM + ML-DSA** (NIST 2024)                                        | Standard NIST FIPS 203/204                                                     |
| **Documentation**         | Wiki.js 2.x, OpenAPI 3.1, AsyncAPI 3.0, ADR                                                        | Couvre technique + utilisateur                                                 |
| **Observabilité**         | OpenTelemetry, Prometheus, Grafana, Loki, Tempo, ELK (OpenSearch)                                  | Standard CNCF                                                                  |
| **CI/CD**                 | GitLab CI ou GitHub Actions, ArgoCD (GitOps), SonarQube, Trivy, Snyk, OWASP DC, Semgrep            | Sécurité shift-left                                                            |
| **Conteneurs**            | Images **distroless** (gcr.io/distroless) ou **Chainguard** ; signées Cosign ; SBOM SPDX           | Surface d'attaque minimale                                                     |
| **Orchestration**         | Kubernetes 1.31+, Helm, Istio (service mesh, mTLS), Cert-Manager                                   | Production-ready                                                               |
| **Secrets**               | HashiCorp Vault + External Secrets Operator + SOPS                                                 | Aucun secret en clair                                                          |

### 10.3 Multi-tenancy : modèle hybride

- **Niveau 1 — base partagée, schéma isolé** par défaut (PostgreSQL Row-Level Security + schéma par tenant).
- **Niveau 2 — base dédiée** pour les tenants régulés (santé, défense, finance).
- **Niveau 3 — déploiement isolé** (namespace K8s + cluster dédié) pour les comptes Enterprise.

Toutes les requêtes portent un `tenant_id` propagé via JWT et un filtre Hibernate global (`@TenantId`). Les violations sont impossibles côté code (interceptor) et côté base (RLS PostgreSQL).

### 10.4 Modularité par tenant

Chaque module (PDCA, 5S, Cercle, DMAIC, Ishikawa, Document Control, CAPA, Audit, Risk, Supplier, Training, Change, Complaints, Calibration, EHS, IA Avancée, Blockchain, Référentiel Normes…) est :

- Activable / désactivable par le **Super Admin** via la console (table `tenant_modules`).
- Exposé via **feature flags** (Unleash) pour rollout progressif.
- Chargé dynamiquement côté Angular (lazy loading) — un tenant qui n'a pas le module 5S ne télécharge pas le bundle correspondant.
- Facturé à l'unité (pricing transparent).

---

## 11. Sécurité — _security by design_

### 11.1 OWASP ASVS Level 3

| Catégorie OWASP               | Mise en œuvre QualitOS                                                                             |
| ----------------------------- | -------------------------------------------------------------------------------------------------- |
| A01 Broken Access Control     | RBAC + ABAC (Spring Security + OPA/Rego), test d'autorisation systématique, RLS PostgreSQL         |
| A02 Cryptographic Failures    | TLS 1.3 obligatoire, AES-256-GCM au repos, Vault Transit, rotation 90j                             |
| A03 Injection                 | Requêtes paramétrées (JPA/Criteria), validation Jakarta + OpenAPI, échappement Angular             |
| A04 Insecure Design           | Threat modeling STRIDE par module, ADR sécurité, abuse cases dans les tests                        |
| A05 Security Misconfiguration | Distroless, CIS Kubernetes Benchmark, CSP stricte, OWASP Secure Headers                            |
| A06 Vulnerable Components     | OWASP DC + Snyk + Trivy à chaque build, SBOM signé, **0 CVE Critique/Haute** en prod               |
| A07 Auth Failures             | Keycloak + MFA obligatoire admin, WebAuthn, blocage brute-force, sessions 15 min + refresh rotatif |
| A08 Software & Data Integrity | Signatures Cosign, SLSA Level 3, blockchain pour intégrité applicative                             |
| A09 Logging & Monitoring      | Logs structurés JSON, OpenTelemetry, SIEM (Wazuh/Elastic), alertes SOC                             |
| A10 SSRF                      | Allow-list destinations sortantes, proxy egress, validation stricte URLs                           |

### 11.2 OWASP **LLM Top 10** (spécifique IA)

- **LLM01 Prompt Injection** : sanitization, séparation system/user, bouclier sémantique (Rebuff).
- **LLM02 Insecure Output** : validation et échappement avant affichage Angular.
- **LLM03 Training Data Poisoning** : provenance datasets, signatures, MLflow + DVC.
- **LLM04 Model DoS** : quotas par tenant, rate-limit, timeouts, circuit breaker.
- **LLM06 Sensitive Info Disclosure** : filtres PII (Microsoft Presidio), redaction auto.
- **LLM07 Insecure Plugin Design** : tool-use sandboxé, allow-list stricte, audit appels.

### 11.3 Blockchain : ancrage et preuves

- **Hyperledger Fabric** (consortium permissionné).
- **Smart contracts (chaincode Go)** :
  - `AnchorAudit(hash, tenant_id, timestamp, type)`
  - `VerifyEvidence(hash)`
- **RGPD-compatible** : uniquement des **hashes** sur la chaîne, jamais de données personnelles.
- Cas d'usage : signature d'un rapport d'audit, traçabilité d'une NC, preuve d'horodatage d'une décision qualité, certificats de formation vérifiables.

### 11.4 Cryptographie post-quantique

- **TLS hybride** (X25519 + ML-KEM-768) sur les flux entrants.
- **Signatures hybrides** (Ed25519 + ML-DSA-65) pour rapports critiques et blocs blockchain.
- **Bouncy Castle FIPS Java + liboqs Python**.
- **Crypto-agility** : algorithmes configurables, **CBOM** maintenu.

### 11.5 Audit & traçabilité

- **Tout** est journalisé : connexion, lecture, modification, export, action IA, transition de cycle.
- Logs **immuables** (append-only) en PostgreSQL + ancrage périodique en blockchain.
- Recherche full-text (OpenSearch).
- Rapports d'audit générés en un clic, signés ML-DSA, ancrés blockchain.

---

## 12. L'IA, épine dorsale de la plateforme

### 12.1 Capacités IA (matrice complète)

| Capacité                           | Modèle / Approche                              | Cas d'usage                                                |
| ---------------------------------- | ---------------------------------------------- | ---------------------------------------------------------- |
| **Recommandation de méthode**      | Classification (XGBoost) + RAG                 | "Décris ton problème" → suggère PDCA, DMAIC ou Ishikawa    |
| **Génération causes Ishikawa**     | LLM + RAG sur cas historiques                  | Suggestions par branche 6M                                 |
| **Génération de workflow no-code** | LLM (Claude/Mistral) avec output structuré     | "Crée-moi un workflow CAPA en 4 étapes"                    |
| **Suggestion CAPA**                | RAG + similarité cosinus                       | "Pour cette NC, voici les actions qui ont marché ailleurs" |
| **Clustering de NC**               | HDBSCAN + embeddings                           | Détection de patterns invisibles                           |
| **Détection anomalies SPC**        | Isolation Forest, Autoencoders, LSTM-AE        | Dérive d'indicateurs, signaux faibles                      |
| **Vision 5S/EHS**                  | YOLOv8 fine-tuné                               | Encombrement, EPI, étiquetage, zones                       |
| **NLP audits**                     | BERT multilingue + Whisper                     | Analyse de comptes-rendus, transcription Cercle            |
| **Prédiction KPI**                 | LSTM / Prophet / Temporal Fusion Transformer   | "Atteindras-tu ton KPI cible ?"                            |
| **Conformité normative**           | RAG sur référentiel ISO + scoring              | "Es-tu aligné ISO 9001 §8.5 ?"                             |
| **Aide à la décision**             | LLM avec garde-fous (guardrails)               | Synthèse multi-source, recommandations                     |
| **Sécurité (UEBA)**                | Détection comportements anormaux               | Alertes SOC, fraude interne                                |
| **Sentiment réclamations**         | NLP (BERT)                                     | Priorisation auto                                          |
| **Génération de rapports**         | LLM avec citations                             | Rapport d'audit en 30 secondes                             |
| **Natural Language Query**         | Text-to-SQL/Mongo                              | "Combien de NC ce mois ?"                                  |
| **Génération de formulaires**      | LLM structured output                          | "Formulaire d'audit 5S atelier méca"                       |
| **Storyboards**                    | LLM narratif                                   | Récit autour des chiffres mensuels                         |
| **Personnalisation formation**     | RecSys (filtrage collaboratif + content-based) | Parcours adaptatifs                                        |

### 12.2 Architecture IA

- **Inférence** : modèles servis via **TorchServe** (modèles propriétaires) + **vLLM** ou **Ollama** (LLM open-source) ; appel optionnel à Anthropic/OpenAI/Mistral via passerelle abstraite (`AIProvider` interface).
- **MLOps** : MLflow (registre), DVC (datasets), Kubeflow Pipelines, monitoring de drift (Evidently AI).
- **RAG** : **Qdrant** comme vector store, embeddings multilingues (BGE-M3, E5).
- **Garde-fous** : Guardrails AI + NeMo Guardrails pour valider entrées/sorties, anti-prompt-injection.
- **Souveraineté** : option **100 % on-premise** (Ollama + Mistral 7B/Llama 3.1 70B local) pour les tenants régulés.

### 12.3 Explicabilité

Chaque décision IA est **explicable** :

- SHAP / LIME pour modèles classiques.
- Citations de sources pour le RAG.
- Logs des prompts (anonymisés) pour les LLM.
- Visualisation des poids dans le dashboard.

**L'IA suggère, l'humain décide.** Tout effet décisionnel critique passe par validation humaine + signature ML-DSA + ancrage blockchain.

### 12.4 Apprentissage continu (privacy-preserving)

- **Federated learning** optionnel : un tenant peut bénéficier de modèles entraînés sur les données agrégées et anonymisées d'autres tenants du même secteur (opt-in).
- **Differential privacy** pour les benchmarks.
- Garantie : aucune donnée de tenant A n'est jamais exposée à un tenant B.

---

## 13. Intégrations tierces (interopérabilité)

### 13.1 API REST publique

- OpenAPI 3.1 documentée, versionnée (`/api/v1/`).
- Authentification OAuth2 client_credentials.
- Pagination cursor-based, idempotency keys, rate-limiting par tenant.

### 13.2 Webhooks sortants

- Événements souscriptibles : `cycle.created`, `audit.signed`, `nonconformity.detected`, `kpi.threshold.breached`, etc.
- Signatures HMAC-SHA256 + horodatage anti-replay.
- Retry exponentiel, DLQ Kafka.

### 13.3 Connecteurs natifs (V1)

- **ITSM** : ServiceNow, Jira Service Management, Zendesk → import incidents, export actions.
- **ERP** : SAP, Oracle Fusion, Microsoft Dynamics → indicateurs production, achats, fournisseurs.
- **MES / IoT** : OPC-UA, MQTT, Sparkplug B → données capteurs pour DMAIC/SPC.
- **EHR/HIS** (santé) : HL7 FHIR R5, DICOM.
- **Bureautique** : Microsoft 365, Google Workspace.
- **Communication** : Slack, Teams, Mattermost.
- **GED** : SharePoint, Alfresco, Nextcloud, Confluence.
- **CMMS / GMAO** : Maximo, SAP PM, Carl Source.
- **PLM** : Windchill, Teamcenter, Aras.
- **BI** : Power BI, Tableau, Qlik (export embed).

### 13.4 Keycloak SPI LDAP custom

- SPI Java implémentant `UserStorageProviderFactory` pour brancher des annuaires LDAP non standards.
- Mapping d'attributs configurable, cache local, fallback transparent.
- Support **Active Directory**, **OpenLDAP**, **FreeIPA** out-of-the-box.

---

## 14. Qualité logicielle & DevSecOps

### 14.1 Pratiques

- **Trunk-based development** + feature flags ; pas de longues branches.
- **Pull Request** obligatoire, 2 reviewers, checks CI verts, signature commit GPG.
- **Conventional Commits** + changelog auto (release-please).
- **Code review IA** assistée.

### 14.2 Pipeline CI/CD

```
commit → lint → SAST (SonarQube + Semgrep) → tests unitaires (>85%)
       → SCA (Snyk + OWASP DC) → build → image scan (Trivy + Grype)
       → tests intégration (Testcontainers) → DAST (OWASP ZAP)
       → signature Cosign → SBOM SPDX → push registre → ArgoCD sync
       → smoke tests prod → rollout progressif (Argo Rollouts canary)
```

### 14.3 Tests

- **Unitaires** : JUnit 5, Mockito, AssertJ ; pytest ; Jest.
- **Intégration** : Testcontainers (PostgreSQL, Mongo, Kafka, Keycloak).
- **Contrat** : Pact.
- **E2E** : Cypress + Playwright.
- **Performance** : Gatling, k6 (objectifs : p95 < 300 ms API, p99 < 800 ms).
- **Sécurité** : OWASP ZAP automatisé + pentest manuel trimestriel.
- **Chaos engineering** : Litmus, Chaos Mesh.

### 14.4 Couverture & qualité

- Couverture > 85 % (lignes), > 75 % (branches).
- Quality Gate SonarQube : 0 bug bloquant, 0 vulnérabilité, dette < 5 %.

---

## 15. UI/UX — design premium et épuré

### 15.1 Principes

- **Sobriété** : peu de couleurs, beaucoup d'espace blanc, hiérarchie typographique forte (Inter + IBM Plex Mono).
- **Cohérence** : design system propriétaire (tokens Style Dictionary), Storybook publié.
- **Densité** adaptable (compact / cosy / comfortable).
- **Mode clair / sombre** (préférence système + override).
- **Accessibilité WCAG 2.2 AA** vérifiée Axe + tests manuels NVDA/VoiceOver.
- **Internationalisation** dès V1 : FR, EN, ES, AR (RTL), JA, ZH. Angular i18n + ICU MessageFormat.

### 15.2 Performances UI

- Lazy loading des modules, code splitting, preload intelligent.
- LCP < 2 s, INP < 200 ms, CLS < 0,1 (Core Web Vitals).
- Service Worker (PWA) pour mode dégradé et **audits 5S offline complet**.
- Sync différée intelligente quand la connectivité revient.

### 15.3 Mobile-first pour le terrain

- App PWA installable (iOS, Android, desktop).
- Caméra native, géoloc, signature digitale, voice-to-text (Whisper local pour offline).
- Écrans adaptés aux gants industriels (boutons larges, contrastes forts).

---

## 16. Modèle de rôles (par tenant)

| Rôle                         | Permissions clés                                                                                    |
| ---------------------------- | --------------------------------------------------------------------------------------------------- |
| **Super Admin (plateforme)** | Crée/désactive tenants, active modules + Industry Packs, gère facturation, accès logs globaux       |
| **Admin Tenant**             | Gère utilisateurs, rôles, paramètres tenant, intégrations LDAP/SSO, modules actifs                  |
| **Directeur Qualité**        | Vue 360°, validation des plans d'action stratégiques, rapports vers direction, signature blockchain |
| **Manager Qualité**          | Lance et pilote cycles (PDCA, DMAIC, Cercles), assigne actions, valide niveau opérationnel          |
| **Auditeur**                 | Lecture seule étendue + génération de rapports d'audit indépendants                                 |
| **Utilisateur**              | Participe aux cycles, remplit audits 5S, propose idées, consulte ses tâches                         |
| **Externe (auditeur tiers)** | Accès limité dans le temps, lecture preuves, signature attestations                                 |

RBAC enrichi par **ABAC** (attributs : site, service, projet, criticité) via OPA/Rego.

---

## 17. Roadmap d'exécution (réaliste, pas de tergiversation)

| Phase                              | Durée   | Livrables                                                                                                                                                                                                               |
| ---------------------------------- | ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **P0 — Cadrage & socle**           | 6 sem   | ADR, threat model, design system, monorepo, CI/CD, Keycloak, multi-tenant base                                                                                                                                          |
| **P1 — MVP**                       | 12 sem  | **PDCA + Ishikawa + 5S** + Document Control + CAPA + Audit + dashboard de base + IA recommandation + ancrage blockchain + **Standards Hub** (ISO 9001 + ISO 27001 documentées avec roadmap certif)                      |
| **P2 — Extension méthodes**        | 10 sem  | **DMAIC + Poka-Yoke + Cercle de Qualité**, extension Standards Hub (ISO 14001, 45001, 22301), webhooks, connecteurs ITSM                                                                                                |
| **P3 — Industrie + IoT**           | 10 sem  | Industry Packs Manufacturing + IT/ITSM + Santé, **Module IoT/Edge Connectivity** (OPC-UA, MQTT, HL7 FHIR, LoRaWAN), Edge Gateway K3s, Vision 5S (CV), formation/gamification, post-quantique TLS hybride, OWASP ASVS L3 |
| **P4 — Multi-secteur**             | 14 sem  | Industry Packs Banque, Pharma, Agro, Aéro, Auto, Public + connecteurs ERP/MES/EHR/PLM, extension Standards Hub (IATF, AS9100, ISO 13485, FDA, HACCP, DORA…)                                                             |
| **P5 — Excellence opérationnelle** | continu | Federated learning, NLQ, dashboards drag&drop, marketplace de packs normatifs, certification ISO 27001 SaaS, conformité SOC 2 Type II                                                                                   |

**Décision tranchée** : MVP = **PDCA + Ishikawa + 5S + Standards Hub (2 normes pilotes)** (12 semaines). DMAIC et Cercle viennent en P2. IoT et Industry Packs s'empilent en P3. Les normes additionnelles s'ajoutent au catalogue Standards Hub en continu.

---

## 18. Conventions de développement

### 18.1 Structure du monorepo (Nx)

```
qualitos/
├── apps/
│   ├── web/                   # Angular SPA + PWA
│   ├── api-core/              # Spring Boot core (auth, tenant, RBAC)
│   ├── api-quality-engine/    # Moteur qualité (PDCA, 5S, etc.)
│   ├── api-audit-reporting/   # Audit/reporting/blockchain
│   ├── ai-service/            # Python FastAPI
│   ├── blockchain-service/    # Java + Fabric SDK
│   ├── industry-adapter/      # Service de packs sectoriels
│   └── mobile-pwa/            # PWA dédiée audits terrain
├── libs/
│   ├── ui-design-system/      # Composants Angular partagés + tokens
│   ├── shared-types/          # Types TS générés depuis OpenAPI
│   ├── domain-quality/        # Modèle métier qualité (Java)
│   ├── industry-packs/        # Packs YAML + plugins SPI
│   └── security-commons/      # Utils sécurité (Java + Python)
├── infra/
│   ├── k8s/                   # Helm charts
│   ├── terraform/             # IaC cloud
│   └── ansible/               # On-premise
├── docs/
│   ├── adr/                   # Architecture Decision Records
│   ├── architecture/
│   └── runbooks/
└── CLAUDE.md
```

### 18.2 Règles non négociables

1. **Aucune dépendance avec CVE Critique/Haute** ne franchit la CI.
2. **Aucun `tenant_id` lu depuis le body** d'une requête : toujours depuis le JWT validé.
3. **Aucun secret en clair** : Vault HashiCorp + ESO + SOPS.
4. **Aucun appel IA externe** sans passer par la passerelle `AIProvider` (logging + redaction PII).
5. **Aucune action critique** (validation rapport, signature audit) sans **MFA** + **ancrage blockchain**.
6. **Aucun déploiement** sans SBOM signé + scan d'image vert.
7. **Aucune feature** livrée sans tests + entrée Wiki utilisateur + entrée Wiki technique.
8. **Aucun KPI** affiché sans définition explicite (formule + seuil + source + propriétaire).
9. **Aucune feature sectorielle** codée en dur : tout passe par les Industry Packs.

---

## 19. Documentation & wiki

### 19.1 Wiki.js — deux espaces

#### A. Espace **Technique**

- Architecture C4 (niveaux 1 à 4), ADR, runbooks SRE.
- API : OpenAPI 3.1 + Redoc, AsyncAPI pour Kafka.
- Schémas BDD (DBML), diagrammes de séquence, threat models.
- Guides Helm, ArgoCD, rollback, DR plan.

#### B. Espace **Utilisateur**

- **Par rôle** : Super Admin, Admin Tenant, Directeur, Manager, Auditeur, Utilisateur.
- **Par module** : tutoriel pas-à-pas, vidéos < 3 min, FAQ.
- **Par secteur** : guides spécifiques (industrie, santé, IT, banque, agro…).
- **Glossaire qualité** en 6 langues.

### 19.2 Aide contextuelle in-app

- Icône `?` sur chaque écran → page Wiki correspondante.
- Assistant IA conversationnel embarqué (RAG sur le wiki).

### 19.3 Module Formation & Sensibilisation

- Parcours par rôle + secteur, vidéos + quiz + simulations.
- Gamification : badges, classements, niveaux (Yellow Belt → Black Belt qualité).
- Simulateurs : "Construis un Ishikawa" sur cas fictif avec correction IA.
- Certificats PDF, signés ML-DSA, ancrés blockchain (vérifiables QR).
- LMS-light intégré ou intégration SCORM 2004 / xAPI vers LMS externe.

---

## 20. Critères de succès (Definition of Done global)

Une feature est _Done_ si **et seulement si** :

- ✅ Code mergé, revue 2 pairs, CI verte.
- ✅ Tests unit + intégration + E2E, couverture ≥ 85 %.
- ✅ SAST + SCA + DAST + scan image : 0 critique, 0 haute.
- ✅ Threat model mis à jour si surface modifiée.
- ✅ Documentation Wiki technique **et** utilisateur publiée.
- ✅ Telemetry (métriques + logs + traces) instrumentée.
- ✅ Feature flag défini, rollback possible en < 5 min.
- ✅ ADR rédigé pour toute décision structurante.
- ✅ Données sensibles chiffrées (au repos + en transit).
- ✅ Action auditable + ancrable blockchain si critique.
- ✅ SLO respecté (p95 API < 300 ms).
- ✅ Accessibilité : score Axe ≥ 95.
- ✅ Si feature sectorielle : packagée en Industry Pack, pas en code dur.
- ✅ Si nouvel KPI : défini dans le catalogue, formule + seuil + source explicites.

---

## 21. Risques majeurs & mitigation

| Risque                                 | Probabilité  | Impact        | Mitigation                                                                                    |
| -------------------------------------- | ------------ | ------------- | --------------------------------------------------------------------------------------------- |
| Adoption faible faute de pédagogie     | Moyenne      | Critique      | Formation/gamification dès P3, onboarding guidé, success manager                              |
| Complexité IA → effet boîte noire      | Haute        | Élevé         | Explicabilité systématique (SHAP, citations RAG), human-in-the-loop                           |
| Hallucination LLM sur conseils qualité | Haute        | Élevé         | RAG strict + guardrails + disclaimer + validation humaine                                     |
| Coût infra (LLM, blockchain)           | Moyenne      | Élevé         | Modèles open-source on-prem, blockchain ancrage horaire, tarification par module              |
| Migration crypto post-quantique        | Faible court | Critique long | Crypto-agility, CBOM tenu à jour                                                              |
| RGPD sur blockchain                    | Moyenne      | Critique      | **Jamais** de données personnelles on-chain, hashes uniquement                                |
| Verrouillage Keycloak                  | Faible       | Moyen         | Abstraction `IdentityProvider`, OIDC standard, alternatives Authentik/Ory                     |
| Multi-secteur trop ambitieux           | Moyenne      | Élevé         | Industry Packs livrés par vagues (P3 puis P4), framework testé sur 3 secteurs avant extension |
| Pricing perçu comme cher               | Moyenne      | Élevé         | Modèle transparent, free tier généreux, comparaison frontale avec MasterControl/ETQ           |

---

## 22. Engagements pour le développement (à Claude / aux assistants IA)

Quand tu génères du code ou de la documentation pour ce projet, **respecte ces invariants** :

1. **Sécurité d'abord** : aucune ligne ne contourne les règles 18.2.
2. **Multi-tenancy invariant** : tout accès données filtre par `tenant_id` issu du JWT.
3. **Tests d'abord** ou en parallèle.
4. **Pas de magie** : pas de configuration cachée, pas de dépendance non documentée.
5. **Cohérence** : si une convention existe (libs partagées, types générés), on la suit.
6. **Documentation simultanée** : toute API publique a OpenAPI + Wiki.
7. **Pas de tergiversation** : si un choix est tranché ici, on l'applique. Si on le remet en cause, on rédige un ADR.
8. **Performance et sobriété** : code lisible > code malin ; O(n) au pire sur les chemins chauds.
9. **Logging structuré** : JSON, niveaux explicites, jamais de PII en clair.
10. **Accessibilité** : tout composant Angular testé clavier + lecteur d'écran.
11. **Industry-agnostic par défaut** : aucune logique sectorielle hard-codée ; passe par Industry Packs.
12. **KPI définis** : tout indicateur a une définition formelle dans le catalogue avant d'être affiché.
13. **IA explicable** : toute prédiction/recommandation montre ses sources et sa confiance.

---

## 23. Conclusion

Le marché des QMS est dominé par des plateformes **mono-secteur, mono-méthode, vieillissantes, chères, fermées et déconnectées du terrain**. QualitOS attaque chaque faiblesse :

- **Mono-secteur** → **Industry Packs** pour 14 secteurs livrés.
- **Mono-méthode** → **5 méthodes natives** + référentiel commun.
- **Faible couverture normative** → **Module Standards Hub** : 60+ normes documentées avec roadmap de certification, modèles de documents, processus types, audit blanc IA.
- **Déconnecté du terrain** → **Module IoT & Edge Connectivity** : universalité multi-protocoles (OPC-UA, MQTT, HL7 FHIR, LoRaWAN, ISOBUS…), Edge Gateway K3s, IA embarquée locale.
- **Vieillissant** → **Angular 18 + Material 3 + PWA offline**.
- **Cher** → **modèle modulaire transparent**, MVP en 3 semaines.
- **Fermé** → **OpenAPI + webhooks + 15 connecteurs natifs**.
- **Sans IA réelle** → **IA bout-en-bout, prédictive, explicable**.
- **Sans intégrité forte** → **blockchain + post-quantique**.

> **QualitOS = (MasterControl ∪ ETQ ∪ Fabrico ∪ Lean BoK ∪ AssurX ∪ Greenlight Guru) × IA × Multi-secteur × IoT-natif × Standards Hub ÷ 5 (coût) ÷ 6 (time-to-value).**

QualitOS n'est pas un QMS. C'est **le système d'exploitation universel de la qualité totale** : il agrège les méthodes (PDCA, 5S, Cercle, DMAIC, Ishikawa), les modules transverses (CAPA, NC, Audit, Document Control, Risk, Supplier, Training…), les normes (60+ référentiels avec dossier de certification clé en main), les capteurs (IoT multi-domaines), l'IA (prédictive et explicable), et la confiance (blockchain + post-quantique) dans une seule plateforme.

Les choix techniques sont arrêtés. La roadmap est tranchée. Les invariants sont gravés. _Maintenant, on construit._

---

_Document maintenu par l'architecte principal. Toute évolution majeure passe par un ADR référencé dans `/docs/adr/`. Version 3.0 — Mai 2026._
