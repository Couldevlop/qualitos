/** Le tableau de bord du système de management intégré, tel que `GET /api/v1/smi/dashboard` le rend. */

export interface StandardRef {
  adoptionId: string;
  code: string;
  name: string;
}

export interface StandardScore {
  code: string;
  name: string;
  score: number;
}

export interface SmiCompliance {
  global: number | null;
  perStandard: StandardScore[];
}

export interface OverdueActions {
  total: number;
  critical: number;
}

export interface MajorRisks {
  total: number;
  critical: number;
  high: number;
}

/** `gross[g - 1][p - 1]` : nombre de risques de gravité g et de probabilité p. */
export interface RiskMatrix {
  open: number;
  gross: number[][];
  residual: number[][];
}

export interface NextAudit {
  id: string;
  reference: string;
  title: string;
  type: string | null;
  standard: string | null;
  scheduledOn: string;
  daysUntil: number;
}

export type UpcomingKind = 'CAPA_ACTION' | 'CALIBRATION' | 'CHANGE' | 'RISK_REVIEW' | 'AUDIT';

export interface Upcoming {
  kind: UpcomingKind;
  targetId: string;
  reference: string | null;
  title: string;
  dueOn: string;
  /** Négatif : échue. */
  daysLeft: number;
}

export interface SmiDashboard {
  standards: StandardRef[];
  selected: string | null;
  compliance: SmiCompliance | null;
  overdueActions: OverdueActions;
  majorRisks: MajorRisks;
  riskMatrix: RiskMatrix;
  nextAudit: NextAudit | null;
  thisWeek: Upcoming[];
}

// ---------- matrice des exigences ----------

export type CoverageStatus = 'COVERED' | 'PARTIAL' | 'GAP' | 'NOT_APPLICABLE';

export interface MatrixCell {
  standardCode: string;
  status: CoverageStatus;
  covered: number;
  total: number;
  sectionTitle: string | null;
}

export interface ChapterRow {
  chapter: string;
  modules: string[];
  cells: MatrixCell[];
}

export interface RequirementsMatrix {
  standards: StandardRef[];
  rows: ChapterRow[];
}

/** Brute ou résiduelle : sur quelle cotation se lit la matrice. */
export type RatingView = 'gross' | 'residual';
