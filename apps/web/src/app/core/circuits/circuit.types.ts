/** Les circuits de validation (ADR 0080), tels que `/api/v1/circuits` les rend. */

/** Le type d'objet qu'un circuit fait approuver, par exemple `document-version`. */
export type CircuitSubject = 'document-version';

export interface CircuitStep {
  name: string;
  /** Le rôle qui approuve cette étape : un rôle du client, système ou sur mesure. */
  roleCode: string;
  /** Approbations distinctes exigées à cette étape (1 à 10). */
  minApprovals: number;
}

/** Le circuit d'un type d'objet. Sans étape, l'approbation simple d'avant s'applique. */
export interface CircuitView {
  subject: CircuitSubject;
  steps: CircuitStep[];
}

export type RunStatus = 'IN_PROGRESS' | 'APPROVED' | 'REJECTED';

export interface CircuitDecision {
  stepIndex: number;
  actorId: string;
  approved: boolean;
  comment: string | null;
  at: string;
}

/** Le passage d'un objet dans son circuit : les étapes figées au départ, et les décisions. */
export interface CircuitRun {
  id: string;
  subject: CircuitSubject;
  subjectId: string;
  status: RunStatus;
  currentStep: number;
  steps: CircuitStep[];
  decisions: CircuitDecision[];
  startedAt: string;
  endedAt: string | null;
}

export const MAX_STEPS = 10;
export const MAX_APPROVALS = 10;
