import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { PaService } from './pa.service';
import { CreatePaRequest, PaView } from './pa.types';

describe('PaService', () => {
  let service: PaService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/processor-agreements`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(PaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded agreements (mock)', fakeAsync(() => {
    let items: PaView[] | undefined;
    service.list().subscribe(r => (items = r));
    tick(120);
    expect(items!.length).toBeGreaterThan(0);
  }));

  it('recomputes EXPIRED on list — seed-003 is past expiration (mock)', fakeAsync(() => {
    let items: PaView[] | undefined;
    service.list('EXPIRED').subscribe(r => (items = r));
    tick(120);
    expect(items!.some(p => p.id === 'pa-3')).toBeTrue();
    expect(items!.every(p => p.status === 'EXPIRED')).toBeTrue();
  }));

  it('create makes a DRAFT agreement (mock)', fakeAsync(() => {
    const req: CreatePaRequest = {
      reference: 'DPA-NEW', processorName: 'Acme', servicesDescription: 's',
      breachNotificationCommitmentHours: 24, auditRights: true, createdByUserId: 'u'
    };
    let created: PaView | undefined;
    service.create(req).subscribe(r => (created = r));
    tick(150);
    expect(created!.status).toBe('DRAFT');
    expect(created!.subProcessorCategories).toEqual([]);
  }));

  it('activate sets ACTIVE and defaults effectiveFrom (mock)', fakeAsync(() => {
    let p: PaView | undefined;
    service.activate('pa-2').subscribe(r => (p = r));
    tick(120);
    expect(p!.status).toBe('ACTIVE');
    expect(p!.effectiveFrom).toBeTruthy();
  }));

  it('terminate sets TERMINATED with reason (mock)', fakeAsync(() => {
    let p: PaView | undefined;
    service.terminate('pa-1', { reason: 'fin de contrat' }).subscribe(r => (p = r));
    tick(120);
    expect(p!.status).toBe('TERMINATED');
    expect(p!.terminationReason).toBe('fin de contrat');
  }));

  it('expireDue reports how many ACTIVE agreements lapsed (mock)', fakeAsync(() => {
    let result: { expired: number } | undefined;
    service.expireDue().subscribe(r => (result = r));
    tick(200);
    expect(result!.expired).toBeGreaterThanOrEqual(0);
  }));

  it('GET list hits the API in non-mock mode', () => {
    environment.useMockApi = false;
    service.list('ACTIVE').subscribe();
    const req = httpMock.expectOne(r => r.url === endpoint && r.params.get('status') === 'ACTIVE');
    expect(req.request.method).toBe('GET');
    req.flush([]);
    httpMock.verify();
  });
});
