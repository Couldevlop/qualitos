import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { FriaService } from './fria.service';
import { FriaDraftRequest, FriaView } from './fria.types';

describe('FriaService', () => {
  let service: FriaService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/fria`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(FriaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded FRIA (mock)', fakeAsync(() => {
    let items: FriaView[] | undefined;
    service.list().subscribe(r => (items = r));
    tick(120);
    expect(items!.length).toBeGreaterThan(0);
  }));

  it('resolves by reference (mock)', fakeAsync(() => {
    let f: FriaView | undefined;
    service.getByReference('FRIA-2026-DIAG-001').subscribe(r => (f = r));
    tick(80);
    expect(f!.reference).toBe('FRIA-2026-DIAG-001');
  }));

  it('draft creates a DRAFT FRIA (mock)', fakeAsync(() => {
    const req: FriaDraftRequest = {
      reference: 'FRIA-NEW', aiSystemId: 'sys', processDescription: 'p',
      affectedPersonsCategories: 'c', specificRisks: 'r', createdByUserId: 'u'
    };
    let created: FriaView | undefined;
    service.draft(req).subscribe(r => (created = r));
    tick(140);
    expect(created!.status).toBe('DRAFT');
  }));

  it('draft rejects duplicate reference with 409 (mock)', fakeAsync(() => {
    let err: any;
    service.draft({
      reference: 'FRIA-2026-DIAG-001', aiSystemId: 's', processDescription: 'p',
      affectedPersonsCategories: 'c', specificRisks: 'r', createdByUserId: 'u'
    }).subscribe({ error: e => (err = e) });
    tick(140);
    expect(err.status).toBe(409);
  }));

  it('approve transitions SUBMITTED to APPROVED (mock)', fakeAsync(() => {
    // seed-002 is SUBMITTED
    let f: FriaView | undefined;
    service.approve('fria-seed-002', {
      approvedByUserId: 'a', approvalNotes: 'ok'
    }).subscribe(r => (f = r));
    tick(120);
    expect(f!.status).toBe('APPROVED');
    expect(f!.approvedByUserId).toBe('a');
  }));

  it('approve refuses non-SUBMITTED FRIA with 409 (mock)', fakeAsync(() => {
    // seed-001 is APPROVED already
    let err: any;
    service.approve('fria-seed-001', { approvedByUserId: 'a' }).subscribe({ error: e => (err = e) });
    tick(120);
    expect(err.status).toBe(409);
  }));

  it('returnToDraft resets a SUBMITTED FRIA (mock)', fakeAsync(() => {
    let f: FriaView | undefined;
    service.returnToDraft('fria-seed-002', { reason: 'incomplet' }).subscribe(r => (f = r));
    tick(120);
    expect(f!.status).toBe('DRAFT');
    expect(f!.submittedAt).toBeNull();
  }));

  it('archive refuses a non-APPROVED FRIA with 409 (mock)', fakeAsync(() => {
    let err: any;
    service.archive('fria-seed-002', { reason: 'x' }).subscribe({ error: e => (err = e) });
    tick(120);
    expect(err.status).toBe(409);
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
