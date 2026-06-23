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
