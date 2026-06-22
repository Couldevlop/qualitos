import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { StandardsDocGenService } from './standards-doc-gen.service';
import { DossierView, NormDocView } from './standards-doc-gen.types';

describe('StandardsDocGenService', () => {
  let service: StandardsDocGenService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/v1/standards/doc-dossiers`;
  const normBase = `${environment.apiBaseUrl}/api/v1/standards/norm-documents`;

  const dossier: DossierView = {
    id: 'd1', tenantId: 't', standardId: 's1', standardCode: 'iso-9001',
    standardName: 'ISO 9001:2015', organizationName: 'ACME', language: 'fr',
    status: 'GENERE', aiProvider: 'ollama',
    documents: [], totalCount: 6, generatedCount: 6, failedCount: 0, progressPercent: 100,
    integritySha256: null, integritySignature: null, anchorTxRef: null,
    finalizedAt: null, finalizedByUserId: null, createdByUserId: 'u', createdAt: 'now',
    updatedAt: 'now'
  };

  const normDoc: NormDocView = {
    id: 'n1', tenantId: 't', standardId: 's1', standardCode: 'iso-9001',
    kind: 'MANUAL', title: 'Manuel', sections: [], status: 'BROUILLON_IA',
    aiProvider: 'ollama', markdown: '# Manuel', submittedAt: null, submittedByUserId: null,
    approvedAt: null, approvedByUserId: null, approvalNotes: null, humanSignature: null,
    rejectionReason: null, createdByUserId: 'u', createdAt: 'now', updatedAt: 'now'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(StandardsDocGenService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('GET le catalogue des pièces', () => {
    service.catalog().subscribe();
    const req = httpMock.expectOne(`${base}/catalog`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('GET la liste des dossiers', () => {
    let out: DossierView[] | undefined;
    service.list().subscribe(r => (out = r));
    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([dossier]);
    expect(out!.length).toBe(1);
  });

  it('GET un dossier par id', () => {
    service.get('d1').subscribe();
    const req = httpMock.expectOne(`${base}/d1`);
    expect(req.request.method).toBe('GET');
    req.flush(dossier);
  });

  it('POST démarre un dossier sans tenant_id dans le corps', () => {
    service.start({
      standardId: 's1',
      tenantProfile: { organizationName: 'ACME', industry: 'it' },
      documentKeys: ['manuel-qualite']
    }).subscribe();
    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.standardId).toBe('s1');
    expect(req.request.body.documentKeys).toEqual(['manuel-qualite']);
    // Invariant multi-tenant : pas de tenant_id dans le corps (JWT serveur).
    expect(req.request.body.tenantId).toBeUndefined();
    req.flush(dossier);
  });

  it('POST relance les pièces en échec', () => {
    service.retry('d1').subscribe();
    const req = httpMock.expectOne(`${base}/d1/retry`);
    expect(req.request.method).toBe('POST');
    req.flush(dossier);
  });

  it('POST finalise avec signature', () => {
    service.finalize('d1', { signature: 'sig', notes: 'ok' }).subscribe();
    const req = httpMock.expectOne(`${base}/d1/finalize`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.signature).toBe('sig');
    req.flush({ ...dossier, status: 'FINALISE' });
  });

  it('GET une pièce normative', () => {
    service.getNormDoc('n1').subscribe();
    const req = httpMock.expectOne(`${normBase}/n1`);
    expect(req.request.method).toBe('GET');
    req.flush(normDoc);
  });

  it('POST soumet une pièce à validation', () => {
    service.submitNormDoc('n1').subscribe();
    const req = httpMock.expectOne(`${normBase}/n1/submit`);
    expect(req.request.method).toBe('POST');
    req.flush({ ...normDoc, status: 'EN_VALIDATION' });
  });

  it('POST approuve une pièce avec signature', () => {
    service.approveNormDoc('n1', { signature: 'sig' }).subscribe();
    const req = httpMock.expectOne(`${normBase}/n1/approve`);
    expect(req.request.body.signature).toBe('sig');
    req.flush({ ...normDoc, status: 'APPROUVE' });
  });

  it('POST rejette une pièce avec motif', () => {
    service.rejectNormDoc('n1', { reason: 'sections vides' }).subscribe();
    const req = httpMock.expectOne(`${normBase}/n1/reject`);
    expect(req.request.body.reason).toBe('sections vides');
    req.flush({ ...normDoc, status: 'BROUILLON_IA' });
  });

  it('propage l\'erreur HTTP (ex. 503 IA indisponible)', () => {
    let err: { status: number } | undefined;
    service.start({ standardId: 's1', tenantProfile: { organizationName: 'ACME' } })
      .subscribe({ error: e => (err = e) });
    const req = httpMock.expectOne(base);
    req.flush('unavailable', { status: 503, statusText: 'Service Unavailable' });
    expect(err!.status).toBe(503);
  });
});
