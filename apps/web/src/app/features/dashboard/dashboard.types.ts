export type KpiState = 'good' | 'warn' | 'bad' | 'neutral';

// ---------------------------------------------------------------------------
// Contrat de `GET /api/v1/dashboards/executive` (agrégat serveur, §7.1).
// Les types ci-dessous décrivent la charge utile brute ; le service les projette
// ensuite sur les modèles de vue (KpiCard, TopRisk…) consommés par la page.
// ---------------------------------------------------------------------------

export type KpiHealth = 'OK' | 'WARNING' | 'CRITICAL' | 'UNKNOWN';
export type KpiDirection = 'HIGHER_IS_BETTER' | 'LOWER_IS_BETTER';

export interface ExecutiveKpiCardResponse {
  kpiId: string;
  code: string;
  name: string;
  description: string | null;
  category: string | null;
  unit: string | null;
  direction: KpiDirection;
  value: number | null;
  targetValue: number | null;
  trendDelta: number | null;
  health: KpiHealth;
  latestPeriodStart: string | null;
  latestPeriodEnd: string | null;
}

export interface TrendPointResponse {
  periodStart: string | null;
  value: number | null;
  targetValue: number | null;
  health: KpiHealth;
}

export interface DefectByCategoryResponse { category: string; count: number; }

export interface TopRiskResponse {
  id: string;
  title: string;
  source: string;
  severity: string;
  rpn: number | null;
  dueDate: string | null;
}

export interface SectionScoreResponse { sectionCode: string; score: number; }

export interface AlignmentBarResponse {
  adoptionId: string;
  standardCode: string;
  standardName: string;
  score: number;
  status: string;
  sections: SectionScoreResponse[];
}

export interface ExecutiveDashboardResponse {
  kpis: ExecutiveKpiCardResponse[];
  qualityTrend: TrendPointResponse[];
  defectsByCategory: DefectByCategoryResponse[];
  topRisks: TopRiskResponse[];
  alignment: AlignmentBarResponse[];
  generatedAt: string;
}

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
  /** Libellé affiché (ex. « FMEA · RPN 240 »). */
  source: string;
  /** Origine machine, pour router vers la ressource au clic. */
  sourceType: 'FMEA' | 'CAPA';
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
