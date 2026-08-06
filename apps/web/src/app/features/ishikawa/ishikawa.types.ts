import { SpringPage } from '../pdca/pdca.types';

export type IshikawaStatus = 'DRAFT' | 'IN_REVIEW' | 'VALIDATED' | 'ARCHIVED';
export type IshikawaMode = 'SIX_M' | 'SEVEN_M' | 'EIGHT_M';
export type CauseCategory =
  | 'METHODS' | 'MANPOWER' | 'MACHINES' | 'MATERIALS'
  | 'MEASUREMENTS' | 'ENVIRONMENT' | 'MANAGEMENT' | 'MONEY';

export interface IshikawaCauseResponse {
  id: string;
  diagramId: string;
  parentId?: string;
  category: CauseCategory;
  label: string;
  description?: string;
  rootCauseScore?: number;
  createdAt: string;
  updatedAt: string;
}

export interface IshikawaDiagramResponse {
  id: string;
  tenantId: string;
  problemStatement: string;
  description?: string;
  mode: IshikawaMode;
  status: IshikawaStatus;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
  causes: IshikawaCauseResponse[];
}

export type IshikawaPage = SpringPage<IshikawaDiagramResponse>;

export interface CreateIshikawaDiagramRequest {
  problemStatement: string;
  description?: string;
  mode: IshikawaMode;
  ownerId: string;
}

export interface CreateIshikawaCauseRequest {
  category: CauseCategory;
  label: string;
  description?: string;
  parentId?: string;
  rootCauseScore?: number;
}

/** Cause probable suggérée par l'IA (à valider/ajouter). §3.5 */
export interface SuggestedCause {
  category: CauseCategory;
  label: string;
  rationale?: string;
}

export interface UpdateIshikawaDiagramRequest {
  problemStatement?: string;
  description?: string;
  mode?: IshikawaMode;
  status?: IshikawaStatus;
}

/** Cycle PDCA créé par conversion d'un Ishikawa (§3.6). Champs utiles à la navigation. */
export interface ConvertedPdcaCycle {
  id: string;
  title: string;
  status: string;
}

/**
 * Avancement d'une action décidée devant le diagramme. Trois états seulement :
 * une décision de réunion se suit d'un coup d'œil. Un cycle plus fin — instruction,
 * validation, preuve d'efficacité — relève de la CAPA (§3.6).
 */
export type IshikawaActionStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

/** Une action décidée à partir du diagramme : quoi, par qui, décidée quand. */
export interface IshikawaActionResponse {
  id: string;
  diagramId: string;
  label: string;
  responsible?: string | null;
  /** Date de la DÉCISION (réunion, comité), et non une échéance. */
  decidedOn?: string | null;
  status: IshikawaActionStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateIshikawaActionRequest {
  label: string;
  responsible?: string;
  decidedOn?: string;
  status?: IshikawaActionStatus;
}

/** Édition cellule par cellule : un champ absent signifie « inchangé ». */
export interface UpdateIshikawaActionRequest {
  label?: string;
  responsible?: string;
  decidedOn?: string;
  status?: IshikawaActionStatus;
}
