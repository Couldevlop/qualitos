/** Types du module Natural Language Query (CLAUDE.md §7.3). */

export interface NlqAskRequest {
  question: string;
  maxRows?: number;
}

export interface NlqAskResponse {
  question: string;
  /** SQL généré et validé (affiché pour la transparence / explicabilité §12.3). */
  sql: string;
  tenantFilterApplied: boolean;
  tablesUsed: string[];
  functionsUsed: string[];
  rows: Array<Record<string, unknown>>;
  rowCount: number;
  confidence: number;
  chart: Record<string, unknown>;
  narrative: string;
}
