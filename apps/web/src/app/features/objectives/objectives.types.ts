/**
 * Les objectifs qualité (ISO 9001 §6.2) vus du navigateur.
 *
 * <p>L'unité et le sens d'amélioration ne sont jamais ENVOYÉS — ils appartiennent
 * au KPI — mais ils sont RENDUS, parce que l'écran doit afficher « ≤ 500 ppm »
 * sans aller rechercher la définition lui-même.
 *
 * <p>Voir l'ADR 0133.
 */

/** 1, 3, 6 ou 12 mois : l'unité dans laquelle se comptent les dépassements. */
export type ObjectivePeriodMonths = 1 | 3 | 6 | 12;

export type KpiDirection = 'HIGHER_IS_BETTER' | 'LOWER_IS_BETTER';

/** Où en est l'objectif face à son KPI réel. */
export type ObjectiveStatus = 'NOT_EVALUATED' | 'ON_TARGET' | 'WARNING' | 'CRITICAL';

/** Le niveau qui a déclenché une proposition. */
export type ObjectiveLevel = 'WARNING' | 'CRITICAL';

/** La vie de la DÉFINITION, à ne pas confondre avec {@link ObjectiveStatus}. */
export type ObjectiveLifecycle = 'ACTIVE' | 'ARCHIVED';

export type ProposalStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface ObjectiveAssessment {
  status: ObjectiveStatus;
  observedValue: number | null;
  deviationAbsolute: number | null;
  deviationPercent: number | null;
  warningBreaches: number;
  criticalBreaches: number;
  onTargetPeriods: number;
  triggeredLevel: ObjectiveLevel | null;
  /** Le retour durable sous la cible est-il constaté ? */
  recoveryAchieved: boolean;
  /** Le statut est-il MAINTENU par une CAPA encore ouverte ? */
  heldByOpenCapa: boolean;
  periodsConsidered: number;
  lastPeriodStart: string | null;
  lastPeriodEnd: string | null;
}

export interface QualityObjective {
  id: string;
  title: string;
  kpiId: string;
  kpiCode: string;
  kpiName: string;
  /** L'unité vient du KPI : l'objectif ne la porte pas. */
  kpiUnit: string | null;
  kpiDirection: KpiDirection;
  processId: string | null;
  processName: string | null;
  isoClause: string | null;
  periodMonths: number;
  targetValue: number;
  thresholdWarning: number;
  thresholdCritical: number;
  warningPeriods: number;
  criticalPeriods: number;
  recoveryPeriods: number;
  /**
   * Le critère d'efficacité que portera la CAPA née de cet objectif, rédigé par
   * la règle et verrouillé. Rendu par le serveur, jamais envoyé : la fenêtre
   * d'ouverture le montre, et c'est le serveur qui l'écrit (ADR 0133 §4).
   */
  effectivenessCriterion: string;
  lifecycle: ObjectiveLifecycle;
  assessment: ObjectiveAssessment;
  createdAt: string;
  updatedAt: string;
}

/** Une CAPA rédigée par la règle, prête à être acceptée telle quelle. */
export interface ObjectiveProposal {
  id: string;
  objectiveId: string;
  objectiveTitle: string | null;
  level: ObjectiveLevel;
  periodStart: string;
  periodEnd: string;
  observedValue: number;
  breachPeriods: number;
  title: string;
  description: string | null;
  /** Verrouillé : le retour du KPI sous la cible sur la durée définie. */
  effectivenessCriterion: string;
  status: ProposalStatus;
  capaCaseId: string | null;
  rejectionReason: string | null;
  decidedBy: string | null;
  decidedAt: string | null;
  createdAt: string;
}

/** Les quatre tuiles de la revue. */
export interface ObjectiveCounters {
  tracked: number;
  onTarget: number;
  warning: number;
  critical: number;
}

export interface ObjectiveReview {
  asOf: string | null;
  counters: ObjectiveCounters;
  objectives: QualityObjective[];
  pendingProposals: ObjectiveProposal[];
}

export interface CreateObjectiveRequest {
  title: string;
  kpiId: string;
  processId?: string | null;
  processName?: string | null;
  isoClause?: string | null;
  periodMonths: number;
  targetValue: number;
  thresholdWarning: number;
  thresholdCritical: number;
  warningPeriods: number;
  criticalPeriods: number;
  recoveryPeriods: number;
}

/**
 * « Ajouter une action » : ce que le pilote écrit dans la fenêtre CAPA pour
 * traiter l'écart d'un objectif.
 *
 * <p>Ni source, ni criticité, ni vérification : le serveur les impose
 * (ADR 0143). Rien n'est prérédigé — le dossier naît de ces mots-là.
 */
export interface OpenObjectiveCapaRequest {
  title: string;
  description?: string | null;
  ownerId: string;
  ownerName?: string | null;
  dueDate: string;
}

/** Le dossier qui vient de naître, et la proposition qu'il solde s'il y en avait une. */
export interface ObjectiveCapaOpened {
  capaId: string;
  proposalId: string | null;
}

export type UpdateObjectiveRequest = Partial<Omit<CreateObjectiveRequest, 'kpiId'>> & {
  lifecycle?: ObjectiveLifecycle;
};
