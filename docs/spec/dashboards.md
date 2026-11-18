# QualitOS — Dashboards et rapports

> Chapitre de la specification produit, extrait de `CLAUDE.md` pour
> n alourdir que les sessions qui en ont besoin. La numerotation des sections
> est celle de la specification.

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

