# QualitOS — Les modules metier et transverses

> Chapitre de la specification produit, extrait de `CLAUDE.md` pour
> n alourdir que les sessions qui en ont besoin. La numerotation des sections
> est celle de la specification.

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

