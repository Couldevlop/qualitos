import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { EudbService } from './eudb.service';
import { EudbDraftRequest, EudbView } from './eudb.types';

describe('EudbService', () => {
  let service: EudbService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/eudb`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(EudbService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded EUDB records (mock)', fakeAsync(() => {
    let items: EudbView[] | undefined;
    service.list().subscribe(r => (items = r));
    tick(120);
    expect(items!.length).toBeGreaterThan(0);
  }));

  it('resolves by reference (mock)', fakeAsync(() => {
    let r: EudbView | undefined;
    service.getByReference('EUDB-2026-DIAG-001').subscribe(x => (r = x));
    tick(80);
    expect(r!.reference).toBe('EUDB-2026-DIAG-001');
  }));

  it('get throws 404 for unknown id (mock)', fakeAsync(() => {
    let err: any;
    service.get('nope').subscribe({ error: e => (err = e) });
    tick(80);
    expect(err.status).toBe(404);
  }));

  it('draft creates a DRAFT record (mock)', fakeAsync(() => {
    const req: EudbDraftRequest = {
      reference: 'EUDB-NEW', aiSystemId: 'sys', providerEntityName: 'Acme',
      memberStateOfReference: 'FR', intendedPurposeSummary: 'p', createdByUserId: 'u'
    };
    let created: EudbView | undefined;
    service.draft(req).subscribe(r => (created = r));
    tick(140);
    expect(created!.status).toBe('DRAFT');
    expect(created!.reference).toBe('EUDB-NEW');
  }));

  it('draft rejects duplicate reference with 409 (mock)', fakeAsync(() => {
    let err: any;
    service.draft({
      reference: 'EUDB-2026-DIAG-001', aiSystemId: 's', createdByUserId: 'u'
    }).subscribe({ error: e => (err = e) });
    tick(140);
    expect(err.status).toBe(409);
  }));

  it('submit rejects when required fields missing (422, mock)', fakeAsync(() => {
    // seed-003 is DRAFT but lacks providerEntityName/memberState/intendedPurpose
    let err: any;
    service.submit('eudb-seed-003', { submittedByUserId: 'u' }).subscribe({ error: e => (err = e) });
    tick(120);
    expect(err.status).toBe(422);
  }));

  it('markRegistered moves SUBMITTED to REGISTERED (mock)', fakeAsync(() => {
    let r: EudbView | undefined;
    service.markRegistered('eudb-seed-002', {
      eudbId: 'EUDB-AI-ZZZ', registrationDate: new Date().toISOString()
    }).subscribe(x => (r = x));
    tick(120);
    expect(r!.status).toBe('REGISTERED');
    expect(r!.eudbId).toBe('EUDB-AI-ZZZ');
  }));

  it('reject sets REJECTED with reason (mock)', fakeAsync(() => {
    let r: EudbView | undefined;
    service.reject('eudb-seed-003', { reason: 'incomplet' }).subscribe(x => (r = x));
    tick(120);
    expect(r!.status).toBe('REJECTED');
    expect(r!.rejectionReason).toBe('incomplet');
  }));

  it('GET list hits the API in non-mock mode', () => {
    environment.useMockApi = false;
    service.list().subscribe();
    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('GET');
    req.flush([]);
    httpMock.verify();
  });
});
