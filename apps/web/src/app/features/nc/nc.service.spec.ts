import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { Subject } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { InMemoryQueueStore, OfflineQueueStore } from '../../core/offline/offline-queue.store';
import { NcService } from './nc.service';

/** Connectivité pilotable (navigator.onLine est read-only). */
class FakeConnectivity {
  online = true;
  private readonly subject = new Subject<boolean>();
  readonly online$ = this.subject.asObservable();
  isOnline(): boolean { return this.online; }
}

function configure(connectivity: FakeConnectivity): void {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      { provide: OfflineQueueStore, useClass: InMemoryQueueStore },
      { provide: ConnectivityService, useValue: connectivity }
    ]
  });
}

describe('NcService (mock mode)', () => {
  let service: NcService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    configure(new FakeConnectivity());
    service = TestBed.inject(NcService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded non-conformances', (done) => {
    service.listNcs().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters by status', (done) => {
    service.listNcs(0, 50, { status: 'OPEN' }).subscribe(page => {
      expect(page.content.every(n => n.status === 'OPEN')).toBeTrue();
      done();
    });
  });

  it('filters by severity and category combined', (done) => {
    service.listNcs(0, 50, { severity: 'CRITICAL', category: 'SAFETY' }).subscribe(page => {
      expect(page.content.every(n => n.severity === 'CRITICAL' && n.category === 'SAFETY')).toBeTrue();
      done();
    });
  });

  it('creates an OPEN non-conformance', (done) => {
    service.createNc({ title: 'Test NC', category: 'PRODUCT', severity: 'MAJOR' }).subscribe(n => {
      expect(n.status).toBe('OPEN');
      expect(n.title).toBe('Test NC');
      expect(n.reference).toContain('NC-2026-');
      done();
    });
  });

  it('resolve sets RESOLVED status, note and resolvedAt', (done) => {
    service.resolve('nc-1', { resolutionNote: 'Fixed.' }).subscribe(n => {
      expect(n.status).toBe('RESOLVED');
      expect(n.resolutionNote).toBe('Fixed.');
      expect(n.resolvedAt).toBeTruthy();
      done();
    });
  });

  it('startAnalysis transitions to UNDER_ANALYSIS', (done) => {
    service.startAnalysis('nc-1').subscribe(n => {
      expect(n.status).toBe('UNDER_ANALYSIS');
      done();
    });
  });

  it('escalateToCapa links a CAPA case', (done) => {
    service.escalateToCapa('nc-1', { ownerId: 'u' }).subscribe(n => {
      expect(n.capaCaseId).toBeTruthy();
      done();
    });
  });
});

describe('NcService (offline-first, API réelle)', () => {
  let service: NcService;
  let httpMock: HttpTestingController;
  let connectivity: FakeConnectivity;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/nc`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    connectivity = new FakeConnectivity();
    configure(connectivity);
    service = TestBed.inject(NcService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    httpMock.verify();
  });

  it('hors-ligne : createNc met en file et répond de façon optimiste', (done) => {
    connectivity.online = false;
    service.createNc({ title: 'Zone blanche', category: 'PROCESS', severity: 'MINOR' }).subscribe(n => {
      expect(n.pendingSync).toBeTrue();
      expect(n.id.startsWith('offline-')).toBeTrue();
      expect(n.reference).toBe('NC-EN-ATTENTE');
      expect(n.title).toBe('Zone blanche');
      expect(n.status).toBe('OPEN');
      httpMock.expectNone(endpoint);
      done();
    });
  });

  it('coupure pendant l’envoi (status 0) : bascule en file au lieu d’échouer', (done) => {
    connectivity.online = true;
    service.createNc({ title: 'Coupure', category: 'OTHER', severity: 'MAJOR' }).subscribe(n => {
      expect(n.pendingSync).toBeTrue();
      expect(n.id.startsWith('offline-')).toBeTrue();
      done();
    });
    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    req.error(new ProgressEvent('error'), { status: 0 });
  });

  it('en ligne : createNc appelle l’API normalement', (done) => {
    connectivity.online = true;
    service.createNc({ title: 'En ligne', category: 'PRODUCT', severity: 'CRITICAL' }).subscribe(n => {
      expect(n.id).toBe('srv-1');
      expect(n.pendingSync).toBeUndefined();
      done();
    });
    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    req.flush({
      id: 'srv-1', reference: 'NC-2026-9001', title: 'En ligne',
      category: 'PRODUCT', severity: 'CRITICAL', status: 'OPEN',
      detectedAt: new Date().toISOString(), createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });
  });

  it('erreur applicative (400) : ne bascule PAS en file et propage l’erreur', (done) => {
    connectivity.online = true;
    service.createNc({ title: '', category: 'PRODUCT', severity: 'MAJOR' }).subscribe({
      next: () => done.fail('ne devrait pas réussir'),
      error: err => {
        expect(err.status).toBe(400);
        done();
      }
    });
    const req = httpMock.expectOne(endpoint);
    req.flush({ title: 'Validation failed' }, { status: 400, statusText: 'Bad Request' });
  });

  it('transitions de workflow restent online-only (POST direct)', (done) => {
    connectivity.online = true;
    service.startAnalysis('a1', { rootCause: 'usure' }).subscribe(n => {
      expect(n.status).toBe('UNDER_ANALYSIS');
      done();
    });
    const req = httpMock.expectOne(`${endpoint}/a1/start-analysis`);
    expect(req.request.method).toBe('POST');
    req.flush({
      id: 'a1', reference: 'NC-2026-1', title: 't', category: 'PROCESS',
      severity: 'MAJOR', status: 'UNDER_ANALYSIS', detectedAt: '', createdAt: '', updatedAt: ''
    });
  });

  it('escalateToCapa envoie ownerId dans le body (online-only)', (done) => {
    connectivity.online = true;
    service.escalateToCapa('a1', { ownerId: 'owner-9' }).subscribe(() => done());
    const req = httpMock.expectOne(`${endpoint}/a1/escalate-capa`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ ownerId: 'owner-9' });
    req.flush({
      id: 'a1', reference: 'NC-2026-1', title: 't', category: 'PROCESS',
      severity: 'MAJOR', status: 'ACTION_DEFINED', detectedAt: '', createdAt: '',
      updatedAt: '', capaCaseId: 'capa-1'
    });
  });
});
