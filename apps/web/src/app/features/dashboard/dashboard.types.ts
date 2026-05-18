export type KpiState = 'good' | 'warn' | 'bad' | 'neutral';

export interface KpiCard {
  id: string;
  label: string;
  value: number | string;
  unit: string;
  trend?: number;
  target?: number;
  description: string;
  icon: string;
  state: KpiState;
  /** true si une baisse est positive (ex : taux de NC, COQ). */
  trendInvertedIsGood?: boolean;
}

export interface AlignmentBar {
  standardCode: string;
  standardName: string;
  score: number;
  status: string;
}

export interface QualityTrendPoint { month: string; value: number; target: number; }
export interface DefectByCategory  { category: string; count: number; }
export interface ComplianceHeatCell { norm: string; clause: string; score: number; }

export interface TopRisk {
  id: string;
  title: string;
  source: string;
  severity: 'critical' | 'high' | 'medium';
  due?: string;
  owner?: string;
}

export interface AiPrediction {
  id: string;
  kind: 'objective' | 'drift' | 'supplier' | 'complaint';
  title: string;
  detail: string;
  confidence: number;   /* 0..1 */
  horizon: string;      /* texte libre ex. "14 jours" */
  state: 'good' | 'warn' | 'bad';
}
