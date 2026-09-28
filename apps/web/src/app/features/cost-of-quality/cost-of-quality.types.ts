/** Les quatre familles du modèle PAF. */
export type CoqCategory = 'PREVENTION' | 'APPRAISAL' | 'INTERNAL_FAILURE' | 'EXTERNAL_FAILURE';

export const COQ_CATEGORIES: CoqCategory[] =
  ['PREVENTION', 'APPRAISAL', 'INTERNAL_FAILURE', 'EXTERNAL_FAILURE'];

/**
 * Une ligne du rapport.
 *
 * <p>Vue mois : une imputation, `entryId` renseigné — ou, `entryId` nul et
 * `entryCount` à 0, un libellé du catalogue resté sans imputation, affiché à
 * zéro. Vue année : le cumul d'un libellé, `entryId` nul et `entryCount` le
 * nombre d'imputations agrégées.
 */
export interface CoqLine {
  entryId: string | null;
  labelId: string;
  labelCode: string | null;
  labelName: string | null;
  partControl: boolean;
  amount: number;
  entryCount: number;
  responsible: string | null;
  imputationDate: string | null;
  comment: string | null;
  partReference: string | null;
  partQuantity: number | null;
  lot: string | null;
  receivedOrMadeOn: string | null;
}

export interface CoqBlock {
  category: CoqCategory;
  lines: CoqLine[];
  total: number;
}

export interface CoqMonth {
  month: number;
  conformance: number;
  nonConformance: number;
}

export interface CoqReport {
  year: number;
  month: number | null;
  currency: string;
  blocks: CoqBlock[];
  conformanceTotal: number;
  nonConformanceTotal: number;
  total: number;
  /** Conformité / non-conformité ; nul quand il n'y a aucune perte. */
  ratio: number | null;
  months: CoqMonth[];
}

export interface CoqLabel {
  id: string;
  category: CoqCategory;
  code: string | null;
  name: string;
  partControl: boolean;
  builtIn: boolean;
}

export interface CoqEntryRequest {
  labelId: string;
  amount: number;
  responsible: string;
  imputationDate: string;
  comment?: string;
  partReference?: string;
  partQuantity?: number;
  lot?: string;
  receivedOrMadeOn?: string;
}

export interface CoqLabelRequest {
  category: CoqCategory;
  name: string;
  partControl: boolean;
}
