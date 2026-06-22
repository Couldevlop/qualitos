# ADR 0038 — Dashboard builder drag & drop avancé (angular-gridster2)

- **Statut** : Accepté
- **Date** : 2026-06-22
- **Owners** : @Couldevlop
- **Phase** : N1 1.4 (différenciateurs commerciaux, CLAUDE.md §7.3 « Drag & drop builder »)

## Contexte

CLAUDE.md §7.3 exige un **« Drag & drop builder » à la Power BI** : l'utilisateur
compose son tableau de bord en glissant/déposant des widgets, les déplace, les
redimensionne et les configure, puis sauvegarde la mise en page par
utilisateur/tenant.

La feature `apps/web/src/app/features/dashboard-builder/` existait déjà en Clean
Architecture (domain / application / infrastructure / presentation), avec :

- un **repository HTTP** (`DashboardHttpRepository`) branché sur l'endpoint
  `/api/v1/dashboards/custom` du `api-quality-engine` ;
- la **persistance backend complète** : table `dashboard_layouts` (migration
  **V55**), entité JPA, service tenant-strict (`tenant_id` du JWT, jamais body),
  contrôleur CRUD ; la mise en page est stockée en `layout_json` (jsonb) ;
- un éditeur **maquette CSS-grid** (sans drag & drop réel) et un `widget-host`
  affichant des **placeholders ECharts** (pas de rendu).

La dépendance `angular-gridster2 ^18.0.0` et `ngx-echarts`/`echarts` étaient
déjà déclarées dans `package.json` mais non utilisées. Le composant partagé
`qos-echart` (UiModule) rend déjà ECharts avec thème + cross-filtering.

## Décision

Porter le builder à **100 % drag & drop avancé**, **côté front uniquement**
(le backend persiste déjà tout l'état dans `layout_json` jsonb — aucune
migration, aucun changement d'API n'est nécessaire).

### Grille interactive — angular-gridster2

`GridsterModule` remplace la maquette CSS-grid. Configuration (`GridsterConfig`) :
grille fixe 12 colonnes, hauteur de ligne fixe (84 px), déplacement par poignée
(`dragHandleClass: 'drag-handle'`), redimensionnement activé, `pushItems`,
responsive (`mobileBreakpoint: 640`). Gridster mute `x/y/cols/rows` en place ;
le callback `itemChangeCallback` resynchronise un `DashboardLayout` **immuable**
pour la persistance.

### Palette glisser-déposer

Un `WidgetCatalogService` (application) déclare la palette (KPI, courbe,
histogramme, camembert, jauge, **carte de contrôle SPC**, tableau, heatmap,
récit IA) avec icône, libellé i18n, taille et config par défaut. La palette
supporte le **HTML5 drag-and-drop** vers une cellule vide
(`enableEmptyCellDrop` + `emptyCellDropCallback`) **et** l'ajout au clic
(accessibilité clavier). Le catalogue expose aussi les **options KPI**
(id + libellé + unité) pour le panneau de configuration (invariant §18.2.12 :
tout KPI affiché a une définition formelle).

### Rendu réel des widgets

`widget-host` rend désormais les graphiques via `qos-echart` (design system,
ECharts chargé en chunk paresseux). Un `WidgetRenderService` construit l'option
ECharts par type de widget. Les aperçus du builder sont **déterministes**
(graine FNV-1a dérivée de l'id KPI) : le builder sert à **composer la mise en
page** ; le câblage aux flux réels (KPI engine / time-series) se fait au rendu
du dashboard publié. Les widgets `kpi` et `narrative` ont un rendu dédié
(valeur + tendance + état de seuil ; texte/storyboard).

### Configuration par widget

Un panneau latéral `widget-config-panel` édite le titre, la source KPI, le
libellé, l'unité, le seuil (KPI) ou le texte (récit), et émet un `Widget`
**immuable** mis à jour. L'éditeur l'applique et resynchronise le layout.

## Conséquences

- **Pas de modification backend ni de migration Flyway** : `layout_json` (jsonb,
  V55) stocke positions / tailles / config ; tenant-strict via le JWT. La borne
  V92 réservée n'a pas eu à être consommée.
- **Conventions respectées** : Angular 18 **NgModules** (jamais standalone),
  HTML/SCSS **séparés**, lazy-loaded, design tokens premium, i18n par attributs
  `i18n` / `$localize` (unités listées dans le rapport de livraison).
- **Tests** : specs Jasmine/Karma pour les 5 unités nouvelles/modifiées
  (`WidgetCatalogService`, `WidgetRenderService`, `widget-host`,
  `widget-config-panel`, `dashboard-editor`) couvrant drag & drop, palette,
  configuration, persistance et chemins d'erreur. Suite front complète verte.
- **Accessibilité** : palette navigable au clavier (ajout au clic), poignées et
  boutons libellés (`aria-label`), panneau `role="dialog"`.

## Alternatives écartées

- **Angular CDK DragDrop** seul : pas de redimensionnement ni de grille
  responsive prête à l'emploi ; gridster2 est déjà dans `package.json` et couvre
  drag + resize + push + responsive.
- **Stocker la mise en page dans une nouvelle table** : superflu, `layout_json`
  jsonb absorbe déjà tout l'état de façon versionnée et tenant-strict.
