import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AiConformityService } from './ai-conformity.service';
import { ConformityView, PlanRequest } from './ai-conformity.types';

describe('AiConformityService', () => {
  let service: AiConformityService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/conformity-assessments`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(AiConformityService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded conformity assessments (mock)', fakeAsync(() => {
    let items: ConformityView[] | undefined;
    service.list().subscribe(r => (items = r));
    tick(120);
    expect(items!.length).toBeGreaterThan(0);
    httpMock.verify();
  }));

  it('filters by status (mock)', fakeAsync(() => {
    let items: ConformityView[] | undefined;
    service.list('CERTIFIED').subscribe(r => (items = r));
    tick(120);
    expect(items!.every(c => c.status === 'CERTIFIED')).toBeTrue();
  }));

  it('lists by AI system (mock)', fakeAsync(() => {
    let items: ConformityView[] | undefined;
    service.listByAiSystem('00000000-0000-0000-0000-000000000001').subscribe(r => (items = r));
    tick(120);
    expect(items!.every(c => c.aiSystemId === '00000000-0000-0000-0000-000000000001')).toBeTrue();
  }));

  it('plan creates a PLANNED assessment (mock)', fakeAsync(() => {
    const req: PlanRequest = {
      reference: 'CA-NEW', aiSystemId: 'sys-x', procedure: 'INTERNAL_CONTROL',
      scope: 'scope', createdByUserId: 'u'
    };
    let created: ConformityView | undefined;
    service.plan(req).subscribe(r => (created = r));
    tick(150);
    expect(created!.status).toBe('PLANNED');
    expect(created!.reference).toBe('CA-NEW');
  }));

  it('certify transitions to CERTIFIED with certificate data (mock)', fakeAsync(() => {
    let c: ConformityView | undefined;
    service.certify('ca-2', {
      certificateNumber: 'CERT-1', euDeclarationReference: 'EU-1', validUntil: new Date().toISOString()
    }).subscribe(r => (c = r));
    tick(120);
    expect(c!.status).toBe('CERTIFIED');
    expect(c!.certificateNumber).toBe('CERT-1');
  }));

  it('revoke sets status REVOKED and reason (mock)', fakeAsync(() => {
    let c: ConformityView | undefined;
    service.revoke('ca-1', { reason: 'non-conformité majeure' }).subscribe(r => (c = r));
    tick(120);
    expect(c!.status).toBe('REVOKED');
    expect(c!.revokeReason).toBe('non-conformité majeure');
  }));

  it('start transitions to IN_PROGRESS (mock)', fakeAsync(() => {
    let c: ConformityView | undefined;
    service.start('ca-3').subscribe(r => (c = r));
    tick(120);
    expect(c!.status).toBe('IN_PROGRESS');
    expect(c!.startedAt).toBeTruthy();
  }));

  it('GET list hits the API in non-mock mode', () => {
    environment.useMockApi = false;
    service.list('PLANNED').subscribe();
    const req = httpMock.expectOne(r => r.url === endpoint && r.params.get('status') === 'PLANNED');
    expect(req.request.method).toBe('GET');
    req.flush([]);
    httpMock.verify();
  });

  it('POST plan hits the API in non-mock mode', () => {
    environment.useMockApi = false;
    const body: PlanRequest = {
      reference: 'CA-API', aiSystemId: 's', procedure: 'NOTIFIED_BODY', scope: 'x', createdByUserId: 'u'
    };
    service.plan(body).subscribe();
    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({} as ConformityView);
    httpMock.verify();
  });
});
