export interface KpiCard {
  id: string;
  label: string;
  value: number;
  unit: string;
  trend?: number;     // %, signe positif = en hausse
  target?: number;    // valeur cible
  description: string;
  icon: string;
  state: 'good' | 'warn' | 'bad';
}

export interface AlignmentBar {
  standardCode: string;
  standardName: string;
  score: number;      // 0..100
  status: string;
}
