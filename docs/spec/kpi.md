# QualitOS — Indicateurs de performance

> Chapitre de la specification produit, extrait de `CLAUDE.md` pour
> n alourdir que les sessions qui en ont besoin. La numerotation des sections
> est celle de la specification.

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

