import {
  canDecommission, canDelete, canEdit, canPutInUse, canRegister, canWithdraw,
  inUseRequirements, isProhibited, isStuckBeforeUse, isTerminal, missingInUseRequirements
} from './ai-systems.rules';
import { AiSystemView } from './ai-systems.types';

/**
 * Ces règles recopient l'agrégat serveur `AiSystem`. Les tests fixent ce contrat :
 * un bouton ne doit jamais être proposé pour une action que le serveur refuserait.
 */
describe('ai-systems.rules', () => {

  const system = (over: Partial<AiSystemView> = {}): AiSystemView => ({
    id: 'id-1', tenantId: 't-1', reference: 'AISYS-A', name: 'Système',
    description: null, providerName: null, intendedPurpose: 'Finalité',
    riskClassification: 'MINIMAL_OR_NO', role: 'DEPLOYER', generalPurpose: false,
    status: 'DRAFT',
    conformityAssessmentEvidenceUrl: null, ceMarkingNumber: null,
    humanOversightDescription: null, transparencyMeasures: null, dataGovernanceNotes: null,
    linkedDpiaId: null, linkedProcessingActivityIds: [], linkedAutomatedDecisionIds: [],
    effectiveFrom: null, effectiveTo: null, withdrawalReason: null,
    createdByUserId: 'u-1', createdAt: '2026-07-01T09:00:00Z', updatedAt: '2026-07-01T09:00:00Z',
    prohibited: false, requiresConformityAssessment: false, requiresTransparency: false,
    ...over
  });

  const compliantHigh = (over: Partial<AiSystemView> = {}): AiSystemView => system({
    riskClassification: 'HIGH',
    conformityAssessmentEvidenceUrl: 'https://exemple.tld/dossier',
    humanOversightDescription: 'Arrêt manuel par le superviseur',
    transparencyMeasures: 'Bandeau d\'information affiché',
    requiresConformityAssessment: true, requiresTransparency: true,
    ...over
  });

  // ---- Obligations -----------------------------------------------------------

  it('n\'impose aucune obligation à un risque minimal', () => {
    expect(inUseRequirements('MINIMAL_OR_NO', {
      conformityAssessmentEvidenceUrl: null,
      humanOversightDescription: null,
      transparencyMeasures: null
    })).toEqual([]);
  });

  it('impose la seule transparence au risque limité (Art. 50)', () => {
    const requirements = inUseRequirements('LIMITED', {
      conformityAssessmentEvidenceUrl: null,
      humanOversightDescription: null,
      transparencyMeasures: 'Mention « réponse générée par IA »'
    });
    expect(requirements.map(r => r.key)).toEqual(['transparency']);
    expect(requirements[0].satisfied).toBeTrue();
  });

  it('impose conformité, supervision et transparence au haut risque', () => {
    const requirements = inUseRequirements('HIGH', {
      conformityAssessmentEvidenceUrl: null,
      humanOversightDescription: null,
      transparencyMeasures: null
    });
    expect(requirements.map(r => r.key))
      .toEqual(['conformity-evidence', 'human-oversight', 'transparency']);
    expect(requirements.every(r => r.satisfied)).toBeFalse();
  });

  it('ne compte pas une chaîne d\'espaces comme une obligation satisfaite', () => {
    const requirements = inUseRequirements('LIMITED', {
      conformityAssessmentEvidenceUrl: null,
      humanOversightDescription: null,
      transparencyMeasures: '   '
    });
    expect(requirements[0].satisfied).toBeFalse();
  });

  it('ne retient que les obligations manquantes pour l\'alerte', () => {
    const partial = system({
      riskClassification: 'HIGH',
      conformityAssessmentEvidenceUrl: 'https://exemple.tld/dossier'
    });
    expect(missingInUseRequirements(partial).map(r => r.key))
      .toEqual(['human-oversight', 'transparency']);
    expect(missingInUseRequirements(compliantHigh())).toEqual([]);
  });

  // ---- Pratiques interdites ---------------------------------------------------

  it('reconnaît une pratique interdite par le drapeau serveur ou par la classification', () => {
    expect(isProhibited(system({ prohibited: true }))).toBeTrue();
    expect(isProhibited(system({ riskClassification: 'UNACCEPTABLE', prohibited: false }))).toBeTrue();
    expect(isProhibited(system())).toBeFalse();
  });

  it('n\'offre ni enregistrement ni mise en service à un système interdit (Art. 5)', () => {
    const banned = system({ riskClassification: 'UNACCEPTABLE', prohibited: true });
    expect(canRegister(banned)).toBeFalse();
    expect(canPutInUse(system({
      ...banned, status: 'REGISTERED'
    }))).toBeFalse();
    // Il reste néanmoins gérable : on doit pouvoir l'abandonner ou le supprimer.
    expect(canWithdraw(banned)).toBeTrue();
    expect(canDelete(banned)).toBeTrue();
  });

  // ---- Cycle de vie ------------------------------------------------------------

  it('ne rend modifiable et supprimable qu\'un brouillon', () => {
    for (const status of ['REGISTERED', 'IN_USE', 'DECOMMISSIONED', 'WITHDRAWN'] as const) {
      expect(canEdit(system({ status }))).toBeFalse();
      expect(canDelete(system({ status }))).toBeFalse();
    }
    expect(canEdit(system())).toBeTrue();
    expect(canDelete(system())).toBeTrue();
  });

  it('n\'enregistre que depuis le brouillon', () => {
    expect(canRegister(system())).toBeTrue();
    expect(canRegister(system({ status: 'REGISTERED' }))).toBeFalse();
  });

  it('ne met en service qu\'un système enregistré ET conforme', () => {
    expect(canPutInUse(compliantHigh({ status: 'REGISTERED' }))).toBeTrue();
    expect(canPutInUse(compliantHigh({ status: 'DRAFT' }))).toBeFalse();
    expect(canPutInUse(system({ status: 'REGISTERED', riskClassification: 'HIGH' }))).toBeFalse();
  });

  it('ne retire du service que ce qui est en service', () => {
    expect(canDecommission(system({ status: 'IN_USE' }))).toBeTrue();
    expect(canDecommission(system({ status: 'REGISTERED' }))).toBeFalse();
  });

  it('n\'abandonne que depuis le brouillon ou l\'enregistrement', () => {
    expect(canWithdraw(system({ status: 'DRAFT' }))).toBeTrue();
    expect(canWithdraw(system({ status: 'REGISTERED' }))).toBeTrue();
    expect(canWithdraw(system({ status: 'IN_USE' }))).toBeFalse();
    expect(canWithdraw(system({ status: 'WITHDRAWN' }))).toBeFalse();
  });

  it('détecte l\'impasse : enregistré, incomplet, donc plus modifiable', () => {
    const stuck = system({ status: 'REGISTERED', riskClassification: 'HIGH' });
    expect(isStuckBeforeUse(stuck)).toBeTrue();
    expect(canEdit(stuck)).toBeFalse();
    expect(canPutInUse(stuck)).toBeFalse();
    expect(canWithdraw(stuck)).toBeTrue();

    expect(isStuckBeforeUse(compliantHigh({ status: 'REGISTERED' }))).toBeFalse();
    expect(isStuckBeforeUse(system({ status: 'DRAFT', riskClassification: 'HIGH' }))).toBeFalse();
  });

  it('marque les états terminaux', () => {
    expect(isTerminal('DECOMMISSIONED')).toBeTrue();
    expect(isTerminal('WITHDRAWN')).toBeTrue();
    expect(isTerminal('IN_USE')).toBeFalse();
  });
});
