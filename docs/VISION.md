# VISION — QualitOS (positionnement, benchmark, différenciation)

> Ce document regroupe le **contenu stratégique et marketing** de QualitOS, extrait de `CLAUDE.md` pour alléger les instructions chargées à chaque session.
> `CLAUDE.md` reste la source des **règles opérationnelles** (stack, architecture, invariants, conventions, DoD). Ce fichier est la source du **pourquoi** (marché, positionnement, promesse).
>
> Pour l'identité courte du projet et le slogan, voir `CLAUDE.md §0`.

---

## 1. Benchmark concurrentiel (état du marché 2026)

### 1.1 Panorama des leaders

| Plateforme | Positionnement | Forces clés | Faiblesses observées (avis G2 / Capterra / Gartner) |
| --- | --- | --- | --- |
| **MasterControl Quality Excellence** | Leader Life Sciences / FDA | Document control puissant, validation pré-livrée, 21 CFR Part 11, modules CAPA/audits matures | Très cher, courbe d'apprentissage abrupte, déconnecté du shop-floor (pas d'IoT/MES), implémentation longue, complexité élevée, "forced upsell" sur la nouvelle plateforme |
| **ETQ Reliance (Octave)** | Configurabilité enterprise (40+ apps) | Designer no-code drag & drop, modules out-of-the-box, mobile, customisation profonde | Pricing par licence opaque et croissant, support décevant, pas d'autosave, cumbersome au démarrage, fonctionnalités gratuites devenues payantes, peu d'IA réelle, difficile pour non-IT |
| **Siemens QMS (Opcenter / Polarion)** | Industrie / aéronautique | APQP/PPAP, FMEA, SPC, MSA, intégration PLM | Lourd, écosystème Siemens-centric, peu accessible aux PME, coût élevé |
| **Qualio** | Life Sciences agile | Setup rapide, UX moderne, ISO/FDA en quelques semaines | Centré documentation, ne couvre pas la fabrication, peu d'IA, modules limités |
| **Greenlight Guru** | Dispositifs médicaux | Spécialisation MedTech, ISO 13485 natif | Mono-secteur, peu adapté hors médical |
| **QT9 QMS** | PME tous secteurs | 25+ modules inclus, tarif par licence concurrente, validé out-of-the-box | UI vieillissante, reporting complexe, templating difficile |
| **TrackWise Digital (Honeywell)** | Enterprise régulé | Configuration point-and-click, intégration shop-floor, scale élevé | Cher, écosystème propriétaire |
| **Intellect QMS AI** | PME/ETI avec IA | IA pour automatisation, FDA/ISO/GDPR | IA limitée à de l'automatisation simple, RAG/LLM peu avancés |
| **ComplianceQuest** | Salesforce-native | Intégration native Salesforce, EHS + qualité | Verrouillage Salesforce, coût total élevé |
| **Qualityze** | Cloud "all-in-one" | Centralisation NC/CAPA/audits, IA marketing | Faible profondeur sectorielle |
| **AlisQI** | SPC + EHS no-code | Implémentation autonome, no-code, modulaire | Périmètre limité (pas de cercle qualité, pas de DMAIC complet) |
| **EASE** | Audits Gemba mobiles | Audits terrain, layered process audits, mobile | Audit-only, pas de couverture qualité totale |
| **GoAudits** | Audits checklists | Templates customisables, mobile | Audit-only, pas de méthodologie qualité |
| **SafetyCulture (iAuditor)** | Audits/inspections terrain | UX mobile excellente, photos annotées | **Crée des silos de données**, pas de lien avec machines/CMMS |
| **Fabrico** | Quality + Maintenance unifiés | Computer Vision, OEE, lien quality↔maintenance | Mono-secteur (manufacturing), pas de méthodologie qualité formalisée |
| **SAP Cloud ERP / Oracle PLM** | ERP avec module qualité | Intégration ERP profonde | Module qualité accessoire, pas spécialiste, ergonomie ERP héritée |
| **Wrike / Monday + plugins qualité** | Project mgmt avec usage qualité | Souplesse, collaboration | Pas de moteur qualité, pas de référentiel normes, IA générique |

### 1.2 Synthèse des faiblesses récurrentes du marché

À partir de 1 500+ avis Gartner / G2 / Capterra / TrustRadius / SoftwareAdvice, sept faiblesses systémiques :

1. **Silos de données** : la qualité vit séparément de l'IoT/MES/ERP/CMMS/ITSM. La cause-racine d'un défaut (vibration machine, charge serveur) est invisible depuis le QMS.
2. **Sectorisation** : un outil pour MedTech, un autre pour l'auto, un autre pour l'IT. Aucun n'embrasse réellement _tous_ les domaines.
3. **Mono-méthode** : un outil fait du CAPA/document control, mais pas le PDCA, le 5S terrain, l'Ishikawa collaboratif et le DMAIC analytique avec la même cohérence.
4. **Pricing opaque et croissant** : les fonctionnalités basculent en premium, les coûts explosent à l'échelle.
5. **IA superficielle** : l'IA est souvent un "co-pilote" de rédaction plutôt qu'un moteur prédictif et explicatif.
6. **Implémentation lourde** : 6 à 18 mois pour MasterControl/ETQ. Validation et formation explosent les budgets.
7. **Support et UX vieillissants** : "outdated UI", "logs out too quickly", "no autosave", déconnexion intempestive, pas de mode hors-ligne robuste.

### 1.3 Stratégie QualitOS — neutralisation des faiblesses

| Faiblesse marché | Réponse QualitOS | Mécanisme technique |
| --- | --- | --- |
| Silos de données | **Référentiel commun** Problèmes→Causes→Actions→KPIs→Preuves→Normes | Modèle de domaine unique, event sourcing Kafka, connecteurs IoT/MES/ERP/ITSM/EHR natifs |
| Sectorisation | **Domain Adapter Layer** : packs sectoriels activables | Configuration déclarative YAML + plugins Java (SPI) par secteur |
| Mono-méthode | **5 méthodes natives + données partagées** | Une cause Ishikawa devient un cycle PDCA en 1 clic ; un audit 5S déclenche un Cercle de Qualité |
| Pricing opaque | **Tarification transparente par module + concurrent licensing** | Activation/désactivation atomique par tenant, billing usage-based en option |
| IA superficielle | **IA bout-en-bout, prédictive, explicable** | LLM + RAG + ML classique + CV + NLP + UEBA (cf. `CLAUDE.md §12`) |
| Implémentation lourde | **MVP en 3 semaines, pré-configurations sectorielles** | Templates by industry, validation packs livrés (IQ/OQ/PQ Life Sciences) |
| UX vieillissante | **Angular 18 + Material 3 + design system premium + offline-first PWA** | Mobile-first, dark mode, WCAG 2.2 AA, INP < 200 ms |

### 1.4 Fonctionnalités best-of-breed extraites du marché et améliorées par l'IA

| Fonctionnalité concurrente | Leader connu | Version QualitOS améliorée par IA |
| --- | --- | --- |
| Document control + 21 CFR Part 11 | MasterControl | + détection LLM des incohérences entre documents, suggestion de mise à jour transverse |
| Drag & drop workflow designer | ETQ Reliance | + génération de workflow par description en langage naturel |
| Validation pré-livrée | MasterControl | + validation packs auto-générés par l'IA selon le secteur du tenant |
| Layered Process Audits mobiles | EASE | + CV YOLOv8 détectant les non-conformités sur photo (encombrement 5S, EPI, étiquette) |
| Photos annotées sur défauts | SafetyCulture | + classification auto du type de défaut, cause-racine probable, lien vers cas similaires |
| OEE + qualité unifiés | Fabrico | + prédiction LSTM de la dérive qualité à partir des données IoT, alertes pré-incident |
| FMEA / risk management | Siemens | + scoring de risque dynamique réévalué à chaque nouvelle donnée |
| CAPA workflow | tous | + suggestion auto de causes-racines (RAG) et recommandation d'actions selon l'efficacité passée |
| Gemba walks | Lean tools | + transcription Whisper + résumé LLM + extraction d'actions avec assignation |
| Audit reporting | tous | + génération de rapport final en 30 s par LLM avec citations vers preuves blockchain |
| Training management | tous | + personnalisation IA des parcours, simulateurs, certificats blockchain vérifiables |
| Supplier quality management | ETQ, MasterControl | + scoring auto des fournisseurs, prédiction de risque, alertes proactives |
| Non-conformance management | tous | + clustering auto des NC similaires pour détecter des patterns invisibles |
| Statistical Process Control (SPC) | Siemens, AlisQI | + détection d'anomalies multivariées (autoencoders) en plus des règles WECO/Nelson |
| Référentiels normes | rare et limité | + moteur d'alignement automatique aux exigences ISO/IATF en continu |

### 1.5 Promesse de différenciation

> **"Tout ce que MasterControl fait en compliance + tout ce que ETQ fait en configurabilité + tout ce que Fabrico fait en shop-floor + tout ce que Lean offre en méthodologie — dans une seule plateforme, multi-secteurs, IA-native, à un coût 3 à 5 × inférieur."**

---

## 2. Promesses fonctionnelles (rappel)

> Les cibles chiffrées correspondantes (SLO, RTO/RPO, couverture, p95…) sont des **invariants opérationnels** maintenus dans `CLAUDE.md` (§2, §10, §11, §14, §20).

1. **100 % fiable** — tests > 85 %, SLO 99,95 %, RTO < 1 h, RPO < 5 min.
2. **100 % sécurisée** — OWASP ASVS L3, OWASP Top 10 + LLM Top 10, ISO 27001 alignée, post-quantique ready.
3. **100 % modulable** — activation/désactivation à chaud par tenant.
4. **100 % adaptable au domaine** — Industry Packs (templates, KPIs, normes pré-configurés).
5. **100 % auditable** — chaque action journalisée, signée ML-DSA, ancrée blockchain.
6. **IA-native** — l'IA n'est pas une feature, c'est l'épine dorsale.
7. **Design premium** — Material 3 + design system propriétaire, WCAG 2.2 AA.
8. **Time-to-Value court** — MVP fonctionnel en 3 semaines vs 6-18 mois chez les concurrents.

---

## 3. Conclusion — synthèse stratégique

Le marché des QMS est dominé par des plateformes **mono-secteur, mono-méthode, vieillissantes, chères, fermées et déconnectées du terrain**. QualitOS attaque chaque faiblesse :

- **Mono-secteur** → **Industry Packs** pour 14 secteurs livrés.
- **Mono-méthode** → **5 méthodes natives** + référentiel commun.
- **Faible couverture normative** → **Module Standards Hub** : 60+ normes documentées avec roadmap de certification, modèles de documents, processus types, audit blanc IA.
- **Déconnecté du terrain** → **Module IoT & Edge Connectivity** : multi-protocoles (OPC-UA, MQTT, HL7 FHIR, LoRaWAN, ISOBUS…), Edge Gateway K3s, IA embarquée locale.
- **Vieillissant** → **Angular 18 + Material 3 + PWA offline**.
- **Cher** → **modèle modulaire transparent**, MVP en 3 semaines.
- **Fermé** → **OpenAPI + webhooks + 15 connecteurs natifs**.
- **Sans IA réelle** → **IA bout-en-bout, prédictive, explicable**.
- **Sans intégrité forte** → **blockchain + post-quantique**.

> **QualitOS = (MasterControl ∪ ETQ ∪ Fabrico ∪ Lean BoK ∪ AssurX ∪ Greenlight Guru) × IA × Multi-secteur × IoT-natif × Standards Hub ÷ 5 (coût) ÷ 6 (time-to-value).**

QualitOS n'est pas un QMS. C'est **le système d'exploitation universel de la qualité totale** : il agrège les méthodes (PDCA, 5S, Cercle, DMAIC, Ishikawa), les modules transverses (CAPA, NC, Audit, Document Control, Risk, Supplier, Training…), les normes (60+ référentiels avec dossier de certification clé en main), les capteurs (IoT multi-domaines), l'IA (prédictive et explicable), et la confiance (blockchain + post-quantique) dans une seule plateforme.

Les choix techniques sont arrêtés. La roadmap est tranchée. Les invariants sont gravés. _Maintenant, on construit._

---

_Extrait de `CLAUDE.md` v3.0 (Mai 2026). Toute évolution majeure passe par un ADR dans `/docs/adr/`._
