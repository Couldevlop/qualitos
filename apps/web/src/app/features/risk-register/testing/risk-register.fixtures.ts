import { OpportunityView, RiskView } from '../risk-register.types';

/** Des lignes de registre réalistes, pour les bancs de test de l'écran. */
export function risque(o: Partial<RiskView> = {}): RiskView {
  return {
    id: 'r1', reference: 'R-014', title: 'Dérive du procédé de soudure', type: 'QUALITY', process: 'Production',
    site: 'Usine A', owner: 'M. Kone', cause: 'Usure buse', effect: 'Fuite', origin: 'FMEA',
    originRef: 'PFMEA-7 #3', sourceId: null, grossSeverity: 4, grossProbability: 3, grossScore: 12,
    grossLevel: 'HIGH', residualSeverity: 4, residualProbability: 2, residualScore: 8, residualLevel: 'MEDIUM',
    decision: 'REDUCE', status: 'IN_TREATMENT', requirements: ['ISO_9001_6_1', 'IATF_16949_6_1_2'],
    nextReviewOn: '2027-01-15', effectivenessCriterion: 'Cpk > 1,33', createdAt: '2026-10-01T08:00:00Z',
    updatedAt: '2026-10-01T08:00:00Z', ...o
  };
}

export function opportunite(o: Partial<OpportunityView> = {}): OpportunityView {
  return {
    id: 'o1', reference: 'O-003', title: 'Automatiser la saisie SPC', type: 'QUALITY', process: 'Production',
    site: null, owner: 'Mme Diallo', targetDate: '2027-03-31', context: 'Instruments connectables',
    benefit: 'Gain de temps', origin: 'MANAGEMENT_REVIEW', originRef: 'RDD-2026', gain: 4, feasibility: 4,
    score: 16, level: 'PRIORITY', decision: 'STUDY', status: 'UNDER_STUDY',
    requirements: ['ISO_9001_6_1', 'ISO_9001_10_3'], benefitCriterion: null,
    createdAt: '2026-10-01T08:00:00Z', updatedAt: '2026-10-01T08:00:00Z', ...o
  };
}
