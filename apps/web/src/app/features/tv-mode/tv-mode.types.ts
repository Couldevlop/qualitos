import type { EChartsCoreOption } from 'echarts/core';

import { AiPrediction, KpiCard, TopRisk } from '../dashboard/dashboard.types';

/**
 * Types du Mode TV / Salle qualité (§7.3).
 *
 * Le mode TV agrège les données du dashboard exécutif en « slides »
 * affichées plein écran, en rotation automatique, pour un écran mural
 * (hall d'usine, pôle qualité). Chaque slide est typée pour un rendu
 * lisible à distance (gros chiffres, contraste fort).
 */

/** Type discriminant de slide. */
export type TvSlideKind = 'kpis' | 'risks' | 'predictions' | 'trend' | 'empty';

/** Slide de base : titre + sous-titre affichés en bandeau. */
interface TvSlideBase {
  /** Identifiant stable (clé *ngFor, navigation). */
  id: string;
  kind: TvSlideKind;
  /** Libellé déjà localisé (résolu côté composant via $localize). */
  title: string;
  subtitle: string;
}

/** Slide « KPIs stratégiques » : grille de gros indicateurs. */
export interface TvKpiSlide extends TvSlideBase {
  kind: 'kpis';
  kpis: KpiCard[];
}

/** Slide « Risques critiques » : liste priorisée. */
export interface TvRiskSlide extends TvSlideBase {
  kind: 'risks';
  risks: TopRisk[];
}

/** Slide « Prévisions IA » : cartes de prédiction explicables. */
export interface TvPredictionSlide extends TvSlideBase {
  kind: 'predictions';
  predictions: AiPrediction[];
}

/** Slide « Tendance qualité » : graphe ECharts plein écran. */
export interface TvTrendSlide extends TvSlideBase {
  kind: 'trend';
  option: EChartsCoreOption;
}

/** Slide de repli affichée quand aucune donnée n'est disponible. */
export interface TvEmptySlide extends TvSlideBase {
  kind: 'empty';
}

export type TvSlide =
  | TvKpiSlide
  | TvRiskSlide
  | TvPredictionSlide
  | TvTrendSlide
  | TvEmptySlide;

/** Intervalles de rotation proposés (secondes). */
export const TV_INTERVALS_SEC = [8, 10, 15, 20, 30] as const;
