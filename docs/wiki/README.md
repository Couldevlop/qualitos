# Espace de documentation utilisateur — QualitOS

Bienvenue dans le guide utilisateur de **QualitOS**, la plateforme qui agrège les 5 méthodes
fondamentales de la qualité (PDCA, 5S, Ishikawa, DMAIC, Cercle de Qualité), les modules
transverses (CAPA, non-conformités, audits, documents…), un référentiel de normes
(**Standards Hub**), des **packs sectoriels** et une **IA explicable** dans une seule interface.

Cet espace s'adresse aux utilisateurs finaux — pas aux développeurs. Chaque page décrit
**à quoi sert** une fonctionnalité, **comment l'utiliser pas à pas**, **où la trouver dans
l'application** (la route, ex. `/spc`) et les **bonnes pratiques** associées.

> L'espace de documentation **technique** (architecture, API, runbooks) est documenté séparément
> dans `docs/adr/`, `docs/architecture/` et l'OpenAPI du backend.

---

## 1. Naviguer par rôle

Selon votre profil, vous n'accédez pas aux mêmes écrans ni aux mêmes actions. Commencez par
la page de votre rôle :

| Rôle | Pour qui | Page |
|---|---|---|
| Super Admin (plateforme) | Exploitant de la plateforme multi-tenant | [roles/super-admin.md](roles/super-admin.md) |
| Admin Tenant | Administrateur d'une organisation | [roles/admin-tenant.md](roles/admin-tenant.md) |
| Directeur Qualité | Pilotage stratégique, validation, signature | [roles/directeur-qualite.md](roles/directeur-qualite.md) |
| Manager Qualité | Pilotage opérationnel des cycles et actions | [roles/manager-qualite.md](roles/manager-qualite.md) |
| Auditeur | Audits, lecture étendue, rapports indépendants | [roles/auditeur.md](roles/auditeur.md) |
| Utilisateur | Terrain : audits 5S, idées, tâches | [roles/utilisateur.md](roles/utilisateur.md) |

> Le rôle **Externe (auditeur tiers)** est un accès limité dans le temps : il est décrit en fin
> de la page [Auditeur](roles/auditeur.md).

---

## 2. Naviguer par module

### Les 5 méthodes qualité

- [PDCA — Roue de Deming](modules/pdca.md) · `/pdca`
- [5S — Excellence opérationnelle](modules/fives.md) · `/fives`
- [Ishikawa — Diagramme de causes](modules/ishikawa.md) · `/ishikawa`
- [DMAIC + Poka-Yoke — Six Sigma](modules/dmaic.md) · `/dmaic`
- [Cercle de Qualité](modules/circles.md) · `/circles`

### Qualité opérationnelle

- [Non-conformités (NC)](modules/non-conformites.md) · `/nc`
- [CAPA — Actions correctives & préventives](modules/capa.md) · `/capa`
- [Audits](modules/audits.md) · `/audits`

### Capacités IA livrées (explicables)

- [SPC — Cartes de contrôle & règles de Nelson](modules/spc.md) · `/spc`
- [Détection d'anomalies IA](modules/anomaly.md) · `/anomaly`
- [Prévision KPI](modules/forecast.md) · `/forecast`
- [Clustering de non-conformités](modules/nc-clusters.md) · `/nc-clusters`
- [Analyse NLP des réclamations](modules/complaints-nlp.md) · `/complaints-nlp`
- [Explicabilité (SHAP) et human-in-the-loop](modules/explicabilite-ia.md) — transverse

### Normes & adaptabilité sectorielle

- [Standards Hub — Référentiel des normes](modules/standards-hub.md) · `/standards`
- [Packs sectoriels (Industry Packs)](modules/industry-packs.md) · `/industry-packs`

---

## 3. Naviguer par secteur d'activité

Chaque secteur dispose d'un **guide dédié** (enjeux qualité, normes, KPIs clés, modules recommandés,
connecteurs IoT, exemple de parcours), aligné sur les **14 Industry Packs** livrés :

- [Index des secteurs et activation d'un pack](secteurs/README.md)
- Industrie / Manufacturing · Santé / Hôpital · Pharma / MedTech · Banque / Finance · IT / ITSM ·
  Agro-alimentaire · Aéronautique / Défense · Automobile · BTP / Construction · Énergie / Utilities ·
  Secteur public · Éducation · Retail / Distribution · Logistique / Transport
  (voir l'[index des secteurs](secteurs/README.md)).

---

## 4. Questions fréquentes

Consultez la [FAQ](faq.md) pour les 15 questions les plus courantes (connexion, langues,
mode hors-ligne, signature, partage de rapports, etc.).

---

## 5. Glossaire qualité (l'essentiel)

| Terme | Définition courte |
|---|---|
| **PDCA** | *Plan-Do-Check-Act*, la « roue de Deming » : cycle d'amélioration continue en 4 étapes. |
| **5S** | Méthode d'organisation des postes de travail : *Seiri* (trier), *Seiton* (ranger), *Seiso* (nettoyer), *Seiketsu* (standardiser), *Shitsuke* (respecter). |
| **Ishikawa** | Diagramme « en arête de poisson » qui classe les causes d'un problème par catégories (les « M » : Main-d'œuvre, Méthode, Matière, Matériel, Milieu, Mesure…). |
| **DMAIC** | Démarche Six Sigma en 5 phases : *Define-Measure-Analyze-Improve-Control*. |
| **Poka-Yoke** | Dispositif « anti-erreur » qui empêche ou détecte une erreur dès qu'elle se produit. |
| **Cercle de Qualité** | Petit groupe (5 à 10 personnes) qui se réunit pour identifier et résoudre des problèmes de son périmètre. |
| **CAPA** | *Corrective And Preventive Actions* : plan d'actions correctives (traiter la cause d'un problème) et préventives (éviter qu'il survienne). |
| **NC (Non-Conformité)** | Écart constaté entre une exigence (norme, procédure, spécification) et la réalité. |
| **SPC** | *Statistical Process Control* : maîtrise statistique des procédés via des cartes de contrôle et des règles de détection (Nelson). |
| **KPI** | *Key Performance Indicator* : indicateur clé de performance, défini par une formule, une cible et des seuils. |
| **FMEA / AMDEC** | Analyse des modes de défaillance, de leurs effets et de leur criticité (RPN). |
| **Cp / Cpk** | Indices de capabilité d'un procédé : capacité à produire dans les tolérances. |
| **Standards Hub** | Le module qui regroupe les référentiels de normes (ISO, IATF, FDA…) et leur dossier de certification. |
| **Industry Pack** | Pack sectoriel pré-configuré (KPIs, templates, normes) activable pour un domaine d'activité. |
| **Tenant** | Une organisation cliente isolée dans la plateforme multi-tenant. |
| **Ancrage blockchain** | Empreinte cryptographique (hash) d'une preuve enregistrée de façon inaltérable, sans donnée personnelle. |
| **SHAP** | Méthode qui explique une prédiction IA en attribuant à chaque variable sa contribution. |

---

_Documentation utilisateur QualitOS — maintenue avec le produit. Les routes citées (`/pdca`,
`/spc`…) correspondent aux écrans réellement présents dans l'application._
