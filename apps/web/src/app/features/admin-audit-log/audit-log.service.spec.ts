import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AuditLogService } from './audit-log.service';
import { AuditEvent, AuditEventPage, ChainVerification } from './audit-log.types';

/**
 * `/api/v1/audit/events` était sans consommateur : le journal §11.5 n'était lisible qu'en
 * appelant l'API à la main, et sa vérification d'intégrité restait invisible.
 */
describe('AuditLogService', () => {
  let service: AuditLogService;
  let http: HttpTestingController;

  const base = `${environment.apiBaseUrl}/api/v1/audit/events`;

  const event = (over: Partial<AuditEvent> = {}): AuditEvent => ({
    id: 'e-1', tenantId: 't-1', sequenceNo: 12,
    occurredAt: '2026-07-30T10:00:00Z', recordedAt: '2026-07-30T10:00:00Z',
    actorType: 'USER', actorUserId: '11111111-1111-1111-1111-111111111111',
    action: 'capa.closed', resourceType: 'capa-case', resourceId: null,
    summary: null, payloadJson: null, ipAddress: null, userAgent: null,
    integrityHash: 'abcdef0123456789', previousHash: null, blockchainTxRef: null,
    ...over
  });

  const page = (content: AuditEvent[]): AuditEventPage => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: 50
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuditLogService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pagine sans jamais envoyer de tri (le serveur impose sequenceNo DESC)', (done) => {
    service.list({}, 2, 25).subscribe(p => {
      expect(p.content.length).toBe(1);
      done();
    });

    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('25');
    expect(req.request.params.has('sort')).toBeFalse();
    req.flush(page([event()]));
  });

  it('n\'envoie que les critères réellement renseignés', (done) => {
    service.list({ action: 'capa.closed' }, 0, 50).subscribe(() => done());

    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('action')).toBe('capa.closed');
    expect(req.request.params.has('resourceType')).toBeFalse();
    expect(req.request.params.has('resourceId')).toBeFalse();
    expect(req.request.params.has('actorUserId')).toBeFalse();
    expect(req.request.params.has('from')).toBeFalse();
    expect(req.request.params.has('to')).toBeFalse();
    req.flush(page([]));
  });

  it('transmet chacun des six critères du contrat', (done) => {
    service.list({
      action: 'nc.created', resourceType: 'nc', resourceId: 'r-1',
      actorUserId: 'u-1', from: '2026-07-01T00:00:00.000Z', to: '2026-07-31T00:00:00.000Z'
    }, 0, 50).subscribe(() => done());

    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('action')).toBe('nc.created');
    expect(req.request.params.get('resourceType')).toBe('nc');
    expect(req.request.params.get('resourceId')).toBe('r-1');
    expect(req.request.params.get('actorUserId')).toBe('u-1');
    expect(req.request.params.get('from')).toBe('2026-07-01T00:00:00.000Z');
    expect(req.request.params.get('to')).toBe('2026-07-31T00:00:00.000Z');
    req.flush(page([]));
  });

  it('remonte une page vide sans erreur', (done) => {
    service.list({}, 0, 50).subscribe(p => {
      expect(p.content).toEqual([]);
      expect(p.totalElements).toBe(0);
      done();
    });
    http.expectOne(r => r.url === base).flush(page([]));
  });

  it('propage l\'erreur HTTP au lieu de la masquer', (done) => {
    service.list({}, 0, 50).subscribe({
      next: () => fail('ne doit pas émettre'),
      error: err => {
        expect(err.status).toBe(403);
        done();
      }
    });
    http.expectOne(r => r.url === base)
      .flush({ detail: 'nope' }, { status: 403, statusText: 'Forbidden' });
  });

  it('lit le détail d\'un événement', (done) => {
    service.get('e-1').subscribe(e => {
      expect(e.payloadJson).toBe('{"a":1}');
      done();
    });
    http.expectOne(`${base}/e-1`).flush(event({ payloadJson: '{"a":1}' }));
  });

  it('vérifie une fenêtre de séquences', (done) => {
    const verification: ChainVerification = {
      tenantId: 't-1', fromSequenceNo: 1, toSequenceNo: 10,
      verifiedCount: 10, valid: true, breaks: []
    };
    service.verifyChain(1, 10).subscribe(v => {
      expect(v.valid).toBeTrue();
      expect(v.verifiedCount).toBe(10);
      done();
    });

    const req = http.expectOne(r => r.url === `${base}/verify`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('fromSeq')).toBe('1');
    expect(req.request.params.get('toSeq')).toBe('10');
    req.flush(verification);
  });

  it('remonte les ruptures de chaîne détectées', (done) => {
    service.verifyChain(5, 6).subscribe(v => {
      expect(v.valid).toBeFalse();
      expect(v.breaks[0].reason).toContain('tamper');
      expect(v.breaks[0].eventId).toBeNull();
      done();
    });
    http.expectOne(r => r.url === `${base}/verify`).flush({
      tenantId: 't-1', fromSequenceNo: 5, toSequenceNo: 6, verifiedCount: 2, valid: false,
      breaks: [{ eventId: null, sequenceNo: 6, reason: 'Integrity hash mismatch (tamper)' }]
    } as ChainVerification);
  });

  describe('critère effectivement appliqué par le serveur', () => {
    it('donne la priorité à la ressource quand type ET id sont fournis', () => {
      expect(service.effectiveCriterion({
        resourceType: 'nc', resourceId: 'r-1', action: 'a', actorUserId: 'u', from: 'f', to: 't'
      })).toBe('resource');
    });

    it('ignore un type de ressource sans identifiant', () => {
      expect(service.effectiveCriterion({ resourceType: 'nc', action: 'a' })).toBe('action');
    });

    it('retient l\'acteur quand ni ressource ni action ne sont saisies', () => {
      expect(service.effectiveCriterion({ actorUserId: 'u-1', from: 'f', to: 't' })).toBe('actor');
    });

    it('retient la période seulement si les deux bornes sont posées', () => {
      expect(service.effectiveCriterion({ from: 'f', to: 't' })).toBe('period');
      expect(service.effectiveCriterion({ from: 'f' })).toBe('none');
      expect(service.effectiveCriterion({ to: 't' })).toBe('none');
    });

    it('retourne « aucun » sur un filtre vide', () => {
      expect(service.effectiveCriterion({})).toBe('none');
    });
  });
});
