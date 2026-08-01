import {
  COMPLAINT_CATEGORIES, COMPLAINT_CATEGORY_LABEL, COMPLAINT_CHANNELS, COMPLAINT_CHANNEL_LABEL,
  COMPLAINT_SEVERITIES, COMPLAINT_SEVERITY_LABEL, COMPLAINT_STATUSES, COMPLAINT_STATUS_LABEL,
  Complaint, ComplaintStatus, awaitingFirstResponse, canAssignComplaint, canCloseComplaint,
  canDeleteComplaint, canEditComplaint, canRateComplaint, canRejectComplaint, canReopenComplaint,
  canResolveComplaint, canRespondToComplaint, isTerminalStatus, isUuid
} from './complaints.types';

/**
 * Ces règles décident des boutons affichés : si elles divergent du serveur,
 * l'utilisateur essuie des 409. Elles sont donc testées état par état, sur la
 * table ALLOWED de ComplaintService.
 */
describe('complaints.types — règles de cycle de vie', () => {

  const complaint = (over: Partial<Complaint> = {}): Complaint => ({
    id: 'c1', tenantId: 't1', code: 'REC-1', channel: 'EMAIL',
    customerName: null, customerEmail: null, customerExternalId: null,
    subject: 'Sujet', description: null,
    severity: 'MEDIUM', category: 'OTHER', status: 'RECEIVED',
    supplierId: null, capaCaseId: null, assignedToUserId: null,
    satisfactionScore: null, receivedAt: null, firstResponseAt: null,
    resolvedAt: null, closedAt: null, rejectionReason: null,
    createdBy: 'u1', createdAt: null, updatedAt: null, ...over
  });

  const open: ComplaintStatus[] = ['RECEIVED', 'UNDER_INVESTIGATION', 'RESPONDED', 'RESOLVED', 'REOPENED'];

  it('ne considère comme terminaux que CLOSED et REJECTED', () => {
    expect(isTerminalStatus('CLOSED')).toBeTrue();
    expect(isTerminalStatus('REJECTED')).toBeTrue();
    open.forEach(s => expect(isTerminalStatus(s)).withContext(s).toBeFalse());
  });

  it('autorise édition, assignation et réponse hors états terminaux', () => {
    open.forEach(s => {
      expect(canEditComplaint(s)).withContext(s).toBeTrue();
      expect(canAssignComplaint(s)).withContext(s).toBeTrue();
      expect(canRespondToComplaint(s)).withContext(s).toBeTrue();
    });
    (['CLOSED', 'REJECTED'] as ComplaintStatus[]).forEach(s => {
      expect(canEditComplaint(s)).withContext(s).toBeFalse();
      expect(canAssignComplaint(s)).withContext(s).toBeFalse();
      expect(canRespondToComplaint(s)).withContext(s).toBeFalse();
    });
  });

  it('ne permet le rejet que depuis RECEIVED ou UNDER_INVESTIGATION', () => {
    expect(canRejectComplaint('RECEIVED')).toBeTrue();
    expect(canRejectComplaint('UNDER_INVESTIGATION')).toBeTrue();
    (['RESPONDED', 'RESOLVED', 'CLOSED', 'REJECTED', 'REOPENED'] as ComplaintStatus[])
      .forEach(s => expect(canRejectComplaint(s)).withContext(s).toBeFalse());
  });

  it('exige une réponse client avant de résoudre', () => {
    const investigating = complaint({ status: 'UNDER_INVESTIGATION' });
    expect(canResolveComplaint(investigating)).toBeFalse();
    expect(awaitingFirstResponse(investigating)).toBeTrue();

    const answered = complaint({ status: 'RESPONDED', firstResponseAt: '2026-07-02T10:00:00Z' });
    expect(canResolveComplaint(answered)).toBeTrue();
    expect(awaitingFirstResponse(answered)).toBeFalse();
  });

  it('ne signale pas d\'attente de réponse dans les états où la résolution est hors sujet', () => {
    expect(awaitingFirstResponse(complaint({ status: 'RECEIVED' }))).toBeFalse();
    expect(awaitingFirstResponse(complaint({ status: 'CLOSED' }))).toBeFalse();
    expect(canResolveComplaint(complaint({ status: 'RECEIVED', firstResponseAt: '2026-07-02T10:00:00Z' }))).toBeFalse();
  });

  it('ne clôture que depuis RESOLVED et ne rouvre que depuis RESOLVED ou CLOSED', () => {
    expect(canCloseComplaint('RESOLVED')).toBeTrue();
    (['RECEIVED', 'UNDER_INVESTIGATION', 'RESPONDED', 'CLOSED', 'REJECTED', 'REOPENED'] as ComplaintStatus[])
      .forEach(s => expect(canCloseComplaint(s)).withContext(s).toBeFalse());

    expect(canReopenComplaint('RESOLVED')).toBeTrue();
    expect(canReopenComplaint('CLOSED')).toBeTrue();
    expect(canReopenComplaint('REJECTED')).toBeFalse();
    expect(canReopenComplaint('RECEIVED')).toBeFalse();
  });

  it('ne mesure la satisfaction qu\'une fois le traitement terminé', () => {
    expect(canRateComplaint('RESOLVED')).toBeTrue();
    expect(canRateComplaint('CLOSED')).toBeTrue();
    expect(canRateComplaint('RECEIVED')).toBeFalse();
    expect(canRateComplaint('REJECTED')).toBeFalse();
  });

  it('ne supprime que les réclamations non traitées ou rejetées', () => {
    expect(canDeleteComplaint('RECEIVED')).toBeTrue();
    expect(canDeleteComplaint('REJECTED')).toBeTrue();
    (['UNDER_INVESTIGATION', 'RESPONDED', 'RESOLVED', 'CLOSED', 'REOPENED'] as ComplaintStatus[])
      .forEach(s => expect(canDeleteComplaint(s)).withContext(s).toBeFalse());
  });

  it('valide la forme UUID des références', () => {
    expect(isUuid('3f1b5c8e-9a2d-4c7b-8e1f-0a2b3c4d5e6f')).toBeTrue();
    expect(isUuid('  3f1b5c8e-9a2d-4c7b-8e1f-0a2b3c4d5e6f  ')).toBeTrue();
    expect(isUuid('3f1b5c8e-9a2d-4c7b-8e1f')).toBeFalse();
    expect(isUuid('')).toBeFalse();
    expect(isUuid(null)).toBeFalse();
    expect(isUuid(undefined)).toBeFalse();
  });

  it('libelle chaque valeur des énumérations serveur', () => {
    COMPLAINT_STATUSES.forEach(s => expect(COMPLAINT_STATUS_LABEL[s]).withContext(s).toBeTruthy());
    COMPLAINT_CHANNELS.forEach(c => expect(COMPLAINT_CHANNEL_LABEL[c]).withContext(c).toBeTruthy());
    COMPLAINT_CATEGORIES.forEach(c => expect(COMPLAINT_CATEGORY_LABEL[c]).withContext(c).toBeTruthy());
    COMPLAINT_SEVERITIES.forEach(s => expect(COMPLAINT_SEVERITY_LABEL[s]).withContext(s).toBeTruthy());
  });
});
