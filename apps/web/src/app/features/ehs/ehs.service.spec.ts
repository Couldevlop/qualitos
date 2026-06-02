import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { EhsService } from './ehs.service';
import { IncidentPage, IncidentView, ReportRequest, Statistics } from './ehs.types';

describe('EhsService', () => {
  let service: EhsService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ehs/incidents`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(EhsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded incidents as a page (mock)', fakeAsync(() => {
    let page: IncidentPage | undefined;
    service.list().subscribe(r => (page = r));
    tick(120);
    expect(page!.content.length).toBeGreaterThan(0);
    expect(page!.totalElements).toBe(page!.content.length);
  }));

  it('filters by type (mock)', fakeAsync(() => {
    let page: IncidentPage | undefined;
    service.list(0, 50, undefined, 'NEAR_MISS').subscribe(r => (page = r));
    tick(120);
    expect(page!.content.every(i => i.type === 'NEAR_MISS')).toBeTrue();
  }));

  it('report creates a REPORTED incident (mock)', fakeAsync(() => {
    const req: ReportRequest = {
      code: 'EHS-NEW', title: 'Test', type: 'OTHER', reportedBy: 'u'
    };
    let created: IncidentView | undefined;
    service.report(req).subscribe(r => (created = r));
    tick(150);
    expect(created!.status).toBe('REPORTED');
    expect(created!.severity).toBe('MEDIUM'); // default applied
  }));

  it('investigate moves to INVESTIGATING and sets owner (mock)', fakeAsync(() => {
    let i: IncidentView | undefined;
    service.investigate('ehs-3', { ownerUserId: 'owner-1' }).subscribe(r => (i = r));
    tick(120);
    expect(i!.status).toBe('INVESTIGATING');
    expect(i!.ownerUserId).toBe('owner-1');
  }));

  it('mitigate records root cause and corrective actions (mock)', fakeAsync(() => {
    let i: IncidentView | undefined;
    service.mitigate('ehs-1', {
      rootCause: 'rc', correctiveActions: 'ca'
    }).subscribe(r => (i = r));
    tick(120);
    expect(i!.status).toBe('MITIGATED');
    expect(i!.rootCause).toBe('rc');
    expect(i!.mitigatedAt).toBeTruthy();
  }));

  it('linkCapa attaches a CAPA case (mock)', fakeAsync(() => {
    let i: IncidentView | undefined;
    service.linkCapa('ehs-1', { capaCaseId: 'capa-9' }).subscribe(r => (i = r));
    tick(120);
    expect(i!.capaCaseId).toBe('capa-9');
  }));

  it('statistics aggregates counts by status and type (mock)', fakeAsync(() => {
    let stats: Statistics | undefined;
    service.statistics().subscribe(r => (stats = r));
    tick(100);
    expect(stats!.reported + stats!.investigating + stats!.mitigated
      + stats!.closed + stats!.cancelled).toBeGreaterThan(0);
  }));

  it('GET list hits the API in non-mock mode', () => {
    environment.useMockApi = false;
    service.list(1, 10, 'CLOSED').subscribe();
    const req = httpMock.expectOne(r =>
      r.url === endpoint && r.params.get('page') === '1'
      && r.params.get('size') === '10' && r.params.get('status') === 'CLOSED');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 1, size: 10 } as IncidentPage);
    httpMock.verify();
  });
});
