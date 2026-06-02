import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ItsmService } from './itsm.service';
import {
  ConnectionPage, ConnectionResponse, CreateConnectionRequest, MappingPage, SyncReport
} from './itsm.types';

describe('ItsmService', () => {
  let service: ItsmService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/itsm`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ItsmService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded connections as a page (mock)', fakeAsync(() => {
    let page: ConnectionPage | undefined;
    service.list().subscribe(r => (page = r));
    tick(120);
    expect(page!.content.length).toBeGreaterThan(0);
  }));

  it('create never echoes the secret back (OWASP A02) (mock)', fakeAsync(() => {
    const req: CreateConnectionRequest = {
      name: 'New conn', provider: 'SERVICENOW', baseUrl: 'https://x.service-now.com',
      secret: 'super-secret', createdBy: 'u'
    };
    let created: ConnectionResponse | undefined;
    service.create(req).subscribe(r => (created = r));
    tick(150);
    expect(created!.status).toBe('ACTIVE');
    expect(JSON.stringify(created)).not.toContain('super-secret');
  }));

  it('update drops the secret from the locally recorded patch (mock)', fakeAsync(() => {
    let updated: ConnectionResponse | undefined;
    service.update('itsm-1', { name: 'Renamed', secret: 'rotated-secret' }).subscribe(r => (updated = r));
    tick(120);
    expect(updated!.name).toBe('Renamed');
    expect(JSON.stringify(updated)).not.toContain('rotated-secret');
  }));

  it('sync returns a report and resets failures (mock)', fakeAsync(() => {
    let report: SyncReport | undefined;
    service.sync('itsm-3').subscribe(r => (report = r));
    tick(400);
    expect(report!.connectionId).toBe('itsm-3');
    expect(report!.totalFetched).toBe(report!.newImports + report!.alreadyKnown);

    let conn: ConnectionResponse | undefined;
    service.get('itsm-3').subscribe(r => (conn = r));
    tick(100);
    expect(conn!.consecutiveFailures).toBe(0);
  }));

  it('listMappings filters by connection (mock)', fakeAsync(() => {
    let page: MappingPage | undefined;
    service.listMappings('itsm-1').subscribe(r => (page = r));
    tick(120);
    expect(page!.content.length).toBe(2);
    expect(page!.content.every(m => m.connectionId === 'itsm-1')).toBeTrue();
  }));

  it('listMappings without connection returns all (mock)', fakeAsync(() => {
    let page: MappingPage | undefined;
    service.listMappings().subscribe(r => (page = r));
    tick(120);
    expect(page!.content.length).toBe(3);
  }));

  it('POST create hits the API in non-mock mode', () => {
    environment.useMockApi = false;
    const body: CreateConnectionRequest = {
      name: 'n', provider: 'JIRA_SM', baseUrl: 'https://x', secret: 's', createdBy: 'u'
    };
    service.create(body).subscribe();
    const req = httpMock.expectOne(`${endpoint}/connections`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({} as ConnectionResponse);
    httpMock.verify();
  });
});
