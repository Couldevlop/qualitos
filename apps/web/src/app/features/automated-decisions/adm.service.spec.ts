import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AdmService } from './adm.service';
import { AdmCreateRequest, AdmView } from './adm.types';

describe('AdmService', () => {
  let service: AdmService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/automated-decisions`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(AdmService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded decisions (mock)', fakeAsync(() => {
    let items: AdmView[] | undefined;
    service.list().subscribe(r => (items = r));
    tick(120);
    expect(items!.length).toBeGreaterThan(0);
  }));

  it('create a profiling-only DRAFT (mock)', fakeAsync(() => {
    const req: AdmCreateRequest = {
      reference: 'ADM-NEW', name: 'Segmentation', decisionType: 'PROFILING_ONLY',
      createdByUserId: 'u'
    };
    let created: AdmView | undefined;
    service.create(req).subscribe(r => (created = r));
    tick(140);
    expect(created!.status).toBe('DRAFT');
  }));

  it('create with legal-effect but no lawful basis throws Art.22 422 (mock)', () => {
    // checkArt22Invariants throws synchronously before the observable is returned.
    expect(() => service.create({
      reference: 'ADM-LEGAL', name: 'Crédit', decisionType: 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT',
      createdByUserId: 'u'
    })).toThrowMatching((e: any) => e.status === 422);
  });

  it('create rejects duplicate reference with 409 (mock)', fakeAsync(() => {
    let err: any;
    service.create({
      reference: 'ADM-CREDIT-001', name: 'dup', decisionType: 'PROFILING_ONLY', createdByUserId: 'u'
    }).subscribe({ error: e => (err = e) });
    tick(140);
    expect(err.status).toBe(409);
  }));

  it('activate moves DRAFT to ACTIVE for a valid record (mock)', fakeAsync(() => {
    // create a valid DRAFT then activate it
    let draft: AdmView | undefined;
    service.create({
      reference: 'ADM-OK', name: 'ok', decisionType: 'PROFILING_ONLY', createdByUserId: 'u'
    }).subscribe(r => (draft = r));
    tick(140);

    let activated: AdmView | undefined;
    service.activate(draft!.id).subscribe(r => (activated = r));
    tick(120);
    expect(activated!.status).toBe('ACTIVE');
    expect(activated!.effectiveFrom).toBeTruthy();
  }));

  it('deprecate then archive an ACTIVE decision (mock)', fakeAsync(() => {
    let dep: AdmView | undefined;
    service.deprecate('adm-seed-001').subscribe(r => (dep = r));
    tick(120);
    expect(dep!.status).toBe('DEPRECATED');

    let arch: AdmView | undefined;
    service.archive('adm-seed-001').subscribe(r => (arch = r));
    tick(120);
    expect(arch!.status).toBe('ARCHIVED');
    expect(arch!.effectiveTo).toBeTruthy();
  }));

  it('delete refuses a non-DRAFT decision with 409 (mock)', fakeAsync(() => {
    let err: any;
    service.delete('adm-seed-002').subscribe({ error: e => (err = e) });
    tick(100);
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
