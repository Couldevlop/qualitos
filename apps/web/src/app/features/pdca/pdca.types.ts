export type PdcaStatus = 'PLAN' | 'DO' | 'CHECK' | 'ACT' | 'COMPLETED' | 'CANCELLED';
export type PdcaPhase = 'PLAN' | 'DO' | 'CHECK' | 'ACT';
export type StepStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE';

export interface CreatePdcaCycleRequest {
  title: string;
  description?: string;
  ownerId: string;
}

export interface CreatePdcaStepRequest {
  title: string;
  description?: string;
  phase: PdcaPhase;
  status?: StepStatus;
  assigneeId?: string;
  dueDate?: string;
}

export interface PdcaStepResponse {
  id: string;
  cycleId: string;
  phase: PdcaPhase;
  title: string;
  description?: string;
  status: StepStatus;
  assigneeId?: string;
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PdcaCycleResponse {
  id: string;
  tenantId: string;
  title: string;
  description?: string;
  status: PdcaStatus;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
  steps: PdcaStepResponse[];
}

/**
 * Pièce jointe apportée en preuve à UNE étape du cycle (§3.1, ADR 0061).
 *
 * La preuve de la mise en place d'une action est toujours un document : c'est
 * lui qu'affiche la colonne « Preuve » du tableau des étapes.
 */
export interface PdcaStepEvidence {
  id: string;
  cycleId: string;
  /** Étape justifiée par la pièce — c'est ce champ qui la range sur la bonne ligne. */
  stepId: string;
  contentType: string;
  sizeBytes: number;
  originalFilename?: string;
  /** Auteur du dépôt ; absent si le jeton ne portait pas d'identifiant exploitable. */
  uploadedBy?: string;
  createdAt: string;
  /**
   * Lien de lecture présigné, à durée de vie courte. Absent sur la réponse de
   * dépôt (le client relit par la liste) et quand le stockage n'a pas su en
   * produire : la cellule affiche alors la pièce sans lien plutôt qu'un lien mort.
   */
  url?: string;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
