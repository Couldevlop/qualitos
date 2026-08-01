import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ComplaintsService } from './complaints.service';
import {
  Complaint, ComplaintResponseEntry, ComplaintStatistics, SpringPage
} from './complaints.types';

/**
 * `/api/v1/complaints` (15 routes) n'avait aucun consommateur : ces tests fixent
 * le contrat réellement appelé (chemins, verbes, corps) pour que l'écran ne
 * dérive pas du serveur.
 */
describe('ComplaintsService', () => {
  let service: ComplaintsService;
  let http: HttpTestingController;

  const base = `${environment.apiBaseUrl}/api/v1/complaints`;

  const complaint = (over: Partial<Complaint> = {}): Complaint => ({
    id: 'c1', tenantId: 't1', code: 'REC-1', channel: 'EMAIL',
    customerName: 'Acme', customerEmail: 'qa@acme.test', customerExternalId: null,
    subject: 'Colis endommagé', description: null,
    severity: 'MEDIUM', category: 'DELIVERY', status: 'RECEIVED',
    supplierId: null, capaCaseId: null, assignedToUserId: null,
    satisfactionScore: null, receivedAt: '2026-07-01T08:00:00Z', firstResponseAt: null,
    resolvedAt: null, closedAt: null, rejectionReason: null,
    createdBy: 'u1', createdAt: '2026-07-01T08:00:00Z', updatedAt: null, ...over
  });

  const entry = (over: Partial<ComplaintResponseEntry> = {}): ComplaintResponseEntry => ({
    id: 'r1', tenantId: 't1', complaintId: 'c1', authorUserId: 'u1', channel: 'EMAIL',
    body: 'Nous traitons votre demande.', internalNote: false,
    sentAt: '2026-07-02T08:00:00Z', createdAt: '2026-07-02T08:00:00Z', ...over
  });

  const page = <T>(content: T[]): SpringPage<T> => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: 20
  });

  const statistics = (over: Partial<ComplaintStatistics> = {}): ComplaintStatistics => ({
    tenantId: 't1', total: 3, received: 1, underInvestigation: 1, responded: 0,
    resolved: 1, closed: 0, rejected: 0,
    product: 1, service: 0, delivery: 2, billing: 0, quality: 0, safety: 0, other: 0, ...over
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ComplaintsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste sans filtre en ne transmettant que la pagination', (done) => {
    service.list({ page: 2, size: 50 }).subscribe(p => {
      expect(p.content.length).toBe(1);
      done();
    });
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('50');
    expect(req.request.params.has('status')).toBeFalse();
    req.flush(page([complaint()]));
  });

  it('filtre par statut', (done) => {
    service.list({ status: 'RESOLVED', page: 0, size: 20 }).subscribe(() => done());
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('status')).toBe('RESOLVED');
    req.flush(page([]));
  });

  it('n\'envoie que le statut quand plusieurs critères sont fournis (précédence serveur)', (done) => {
    service.list({ status: 'CLOSED', category: 'PRODUCT', supplierId: 's1', page: 0, size: 20 })
      .subscribe(() => done());
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('status')).toBe('CLOSED');
    expect(req.request.params.has('category')).toBeFalse();
    expect(req.request.params.has('supplierId')).toBeFalse();
    req.flush(page([]));
  });

  it('retombe sur la catégorie puis sur le fournisseur', (done) => {
    service.list({ category: 'BILLING', supplierId: 's1', page: 0, size: 20 }).subscribe();
    const byCategory = http.expectOne(r => r.url === base);
    expect(byCategory.request.params.get('category')).toBe('BILLING');
    expect(byCategory.request.params.has('supplierId')).toBeFalse();
    byCategory.flush(page([]));

    service.list({ supplierId: 's1', page: 0, size: 20 }).subscribe(() => done());
    const bySupplier = http.expectOne(r => r.url === base);
    expect(bySupplier.request.params.get('supplierId')).toBe('s1');
    bySupplier.flush(page([]));
  });

  it('joint la page de résultats aux compteurs du tenant', (done) => {
    service.overview({ page: 0, size: 20 }).subscribe(o => {
      expect(o.page.totalElements).toBe(1);
      expect(o.statistics.total).toBe(3);
      done();
    });
    http.expectOne(r => r.url === base).flush(page([complaint()]));
    http.expectOne(`${base}/statistics`).flush(statistics());
  });

  it('lit une réclamation', (done) => {
    service.get('c1').subscribe(c => {
      expect(c.code).toBe('REC-1');
      done();
    });
    http.expectOne(`${base}/c1`).flush(complaint());
  });

  it('lit le fil de réponses avec sa pagination par défaut', (done) => {
    service.responses('c1').subscribe(p => {
      expect(p.content[0].body).toContain('traitons');
      done();
    });
    const req = http.expectOne(r => r.url === `${base}/c1/responses`);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('100');
    req.flush(page([entry()]));
  });

  it('assemble le détail : réclamation + fil', (done) => {
    service.detail('c1').subscribe(d => {
      expect(d.complaint.id).toBe('c1');
      expect(d.responses.length).toBe(1);
      done();
    });
    http.expectOne(`${base}/c1`).flush(complaint());
    http.expectOne(r => r.url === `${base}/c1/responses`).flush(page([entry()]));
  });

  it('crée une réclamation', (done) => {
    service.create({
      code: 'REC-2', channel: 'PHONE', customerName: null, customerEmail: null,
      customerExternalId: null, subject: 'Retard', description: null,
      severity: 'HIGH', category: 'DELIVERY', supplierId: null, assignedToUserId: null,
      createdBy: 'u1', receivedAt: null
    }).subscribe(c => {
      expect(c.id).toBe('c1');
      done();
    });
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.code).toBe('REC-2');
    expect(req.request.body.createdBy).toBe('u1');
    req.flush(complaint());
  });

  it('met à jour une réclamation en PATCH', (done) => {
    service.update('c1', { subject: 'Nouveau sujet' }).subscribe(() => done());
    const req = http.expectOne(`${base}/c1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ subject: 'Nouveau sujet' });
    req.flush(complaint({ subject: 'Nouveau sujet' }));
  });

  it('supprime une réclamation', (done) => {
    service.delete('c1').subscribe(() => done());
    const req = http.expectOne(`${base}/c1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('assigne, rejette, clôture et rouvre', (done) => {
    service.assign('c1', 'u9').subscribe();
    const assign = http.expectOne(`${base}/c1/assign`);
    expect(assign.request.body).toEqual({ assigneeUserId: 'u9' });
    assign.flush(complaint({ status: 'UNDER_INVESTIGATION', assignedToUserId: 'u9' }));

    service.reject('c1', 'Hors périmètre').subscribe();
    const reject = http.expectOne(`${base}/c1/reject`);
    expect(reject.request.body).toEqual({ reason: 'Hors périmètre' });
    reject.flush(complaint({ status: 'REJECTED' }));

    service.close('c1').subscribe();
    const close = http.expectOne(`${base}/c1/close`);
    expect(close.request.body).toEqual({});
    close.flush(complaint({ status: 'CLOSED' }));

    service.reopen('c1').subscribe(() => done());
    const reopen = http.expectOne(`${base}/c1/reopen`);
    expect(reopen.request.body).toEqual({});
    reopen.flush(complaint({ status: 'UNDER_INVESTIGATION' }));
  });

  it('résout avec ou sans dossier CAPA', (done) => {
    service.resolve('c1').subscribe();
    const withoutCapa = http.expectOne(`${base}/c1/resolve`);
    expect(withoutCapa.request.body).toEqual({ capaCaseId: null });
    withoutCapa.flush(complaint({ status: 'RESOLVED' }));

    service.resolve('c1', 'capa-9').subscribe(() => done());
    const withCapa = http.expectOne(`${base}/c1/resolve`);
    expect(withCapa.request.body).toEqual({ capaCaseId: 'capa-9' });
    withCapa.flush(complaint({ status: 'RESOLVED', capaCaseId: 'capa-9' }));
  });

  it('enregistre une note de satisfaction', (done) => {
    service.setSatisfaction('c1', 9).subscribe(() => done());
    const req = http.expectOne(`${base}/c1/satisfaction`);
    expect(req.request.body).toEqual({ score: 9 });
    req.flush(complaint({ satisfactionScore: 9 }));
  });

  it('ajoute puis supprime un message du fil', (done) => {
    service.addResponse('c1', {
      authorUserId: 'u1', channel: null, body: 'Bonjour', internalNote: true
    }).subscribe();
    const add = http.expectOne(`${base}/c1/responses`);
    expect(add.request.method).toBe('POST');
    expect(add.request.body).toEqual({
      authorUserId: 'u1', channel: null, body: 'Bonjour', internalNote: true
    });
    add.flush(entry({ internalNote: true }));

    service.deleteResponse('c1', 'r1').subscribe(() => done());
    const del = http.expectOne(`${base}/c1/responses/r1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('propage l\'erreur HTTP au lieu de l\'avaler', (done) => {
    service.close('c1').subscribe({
      next: () => fail('la transition refusée ne doit pas émettre'),
      error: err => {
        expect(err.status).toBe(409);
        done();
      }
    });
    http.expectOne(`${base}/c1/close`)
      .flush({ title: 'Cannot close from status RECEIVED' }, { status: 409, statusText: 'Conflict' });
  });
});
