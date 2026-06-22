/**
 * Dashboard-builder domain models — pure TS, NO Angular imports.
 * CLAUDE.md §7.1 / §7.3.
 */

export type WidgetType =
  | 'kpi'
  | 'line'
  | 'bar'
  | 'pie'
  | 'gauge'
  | 'control-chart'
  | 'table'
  | 'heatmap'
  | 'narrative';

/**
 * Position d'un widget dans la grille gridster (unités de grille, pas pixels).
 * Mutable : angular-gridster2 met à jour x/y/cols/rows en place lors d'un
 * drag/resize. La couche éditeur reconstruit ensuite un {@link Widget} immuable
 * pour la persistance.
 */
export interface WidgetPosition {
  x: number;
  y: number;
  cols: number;
  rows: number;
}

/**
 * Configuration d'un widget. Champs optionnels typés pour le panneau de
 * configuration ; `extra` reste ouvert pour des réglages spécifiques.
 */
export interface WidgetConfig {
  /** Référence KPI du catalogue (widgets kpi / charts data-driven). */
  kpiId?: string;
  /** Libellé court affiché sous la valeur KPI. */
  kpiLabel?: string;
  /** Seuil de bascule d'état (warn/critical) pour un widget KPI. */
  threshold?: number;
  /** Unité affichée à côté de la valeur KPI (%, j, ppm…). */
  unit?: string;
  /** Texte markdown/narratif pour un widget narrative. */
  text?: string;
  /** Palette de couleurs ou option d'affichage libre. */
  [extra: string]: unknown;
}

export interface Widget {
  readonly id: string;
  readonly type: WidgetType;
  readonly title: string;
  readonly position: WidgetPosition;
  /** Widget-specific config (data source, ECharts option, KPI ref...). */
  readonly config: WidgetConfig;
}

export interface DashboardLayout {
  readonly id?: string;
  readonly tenantId?: string;
  readonly userId?: string;
  readonly name: string;
  readonly description?: string;
  readonly widgets: ReadonlyArray<Widget>;
  readonly shared: boolean;
  readonly signatureHash?: string;
  readonly version?: number;
}

export interface CrossFilter {
  readonly sourceWidgetId: string;
  readonly field: string;
  readonly value: string | number | null;
}

/**
 * Définition d'un type de widget dans la palette (drag &amp; drop §7.3).
 * Métadonnées d'affichage + gabarit par défaut (taille + config initiale).
 */
export interface WidgetCatalogEntry {
  readonly type: WidgetType;
  /** Libellé i18n affiché dans la palette. */
  readonly label: string;
  /** Icône Material. */
  readonly icon: string;
  /** Courte description i18n (tooltip / aide). */
  readonly description: string;
  /** Taille initiale lors de l'ajout (unités de grille). */
  readonly defaultCols: number;
  readonly defaultRows: number;
  /** Config initiale appliquée au nouveau widget. */
  readonly defaultConfig: WidgetConfig;
}

/** Option de catalogue KPI proposée dans le panneau de configuration. */
export interface KpiOption {
  readonly id: string;
  readonly label: string;
  readonly unit: string;
}

/** A widget snapshot sent to the signed-PDF export endpoint (§7.4). */
export interface ExportWidgetSnapshot {
  readonly title: string;
  readonly type: string;
  readonly dataLines: ReadonlyArray<string>;
}

/** Result of a signed PDF export: the blob plus integrity metadata from headers. */
export interface DashboardExportResult {
  readonly blob: Blob;
  readonly fileName: string;
  readonly verificationCode: string;
  readonly sha256: string;
  readonly anchorRef: string;
}
