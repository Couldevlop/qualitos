import { IMPACT_DOMAINS, ImpactLevel, MocDetail, MocImpact, MocSummary, MocValidation } from '../moc.types';

/** Les niveaux de la maquette : 13 points sur 24, score 54, critique. */
export const MAQUETTE: ImpactLevel[] = ['MEDIUM', 'LOW', 'LOW', 'HIGH', 'MEDIUM', 'MEDIUM', 'NONE', 'MEDIUM'];

export function impacts(levels: (ImpactLevel | null)[] = MAQUETTE, weight = 1): MocImpact[] {
  return IMPACT_DOMAINS.map((domain, i) => ({ domain, weight, level: levels[i] ?? null, notes: null }));
}

export function carte(over: Partial<MocValidation> = {}): MocValidation {
  return {
    id: 'v1', roleLabel: 'HSE', decision: 'PENDING', decidedByName: null, decidedAt: null, comment: null,
    holders: ['j.dupont@acme.test', 'm.martin@acme.test'], heldByMe: false, canDecide: false, preview: false,
    ...over
  };
}

export function demande(over: Partial<MocDetail> = {}): MocDetail {
  return {
    id: 'c1', code: 'MOC-2026-014', title: 'Remplacement du four 3', type: 'PERMANENT', site: 'Atelier 2',
    description: 'Four en fin de vie', justification: 'Dérive thermique', plannedFor: null,
    status: 'IN_ANALYSIS', requesterName: 'S. Benali', requestedByMe: true, canManage: true,
    createdAt: '2026-09-20T08:00:00Z', submittedAt: null, closedAt: null,
    score: 54, classification: 'CRITICAL', impacts: impacts(), validations: [], history: [],
    ...over
  };
}

export function ligne(over: Partial<MocSummary> = {}): MocSummary {
  return {
    id: 'c1', code: 'MOC-2026-014', title: 'Remplacement du four 3', type: 'PERMANENT', status: 'IN_VALIDATION',
    classification: 'CRITICAL', score: 54, requesterName: 'S. Benali', createdAt: '2026-09-20T08:00:00Z',
    awaitingMe: false, ...over
  };
}
