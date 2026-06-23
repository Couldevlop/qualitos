import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { Subject } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { InMemoryQueueStore, OfflineQueueStore } from '../../core/offline/offline-queue.store';
import { AuditsService } from './audits.service';

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

describe('AuditsService (mock mode)', () => {
  let service: AuditsService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    configure(new FakeConnectivity());
    service = TestBed.inject(AuditsService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded plans', (done) => {
    service.listPlans().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters plans by status', (done) => {
    service.listPlans(0, 50, 'PLANNED').subscribe(page => {
      expect(page.content.every(p => p.status === 'PLANNED')).toBeTrue();
      done();
    });
  });

  it('creates a plan in PLANNED status', (done) => {
    service.createPlan({ title: 'Audit X', type: 'INTERNAL', leadAuditorId: 'u' }).subscribe(p => {
      expect(p.status).toBe('PLANNED');
      expect(p.checklist).toEqual([]);
      done();
    });
  });

  it('adds a checklist item then responds, computing conformity score', (done) => {
    service.addChecklistItem('a2', { question: 'Procédure documentée ?' }).subscribe(item => {
      service.respondChecklistItem('a2', item.id, { conformant: true }).subscribe(updated => {
        expect(updated.conformant).toBeTrue();
        service.getPlan('a2').subscribe(plan => {
          expect(plan.conformityScore).toBe(100);
          done();
        });
      });
    });
  });

  it('startPlan transitions to IN_PROGRESS and sets startedAt', (done) => {
    service.startPlan('a3').subscribe(p => {
      expect(p.status).toBe('IN_PROGRESS');
      expect(p.startedAt).toBeTruthy();
      done();
    });
  });

  it('completePlan stores the report summary', (done) => {
    service.completePlan('a3', 'RAS').subscribe(p => {
      expect(p.status).toBe('COMPLETED');
      expect(p.reportSummary).toBe('RAS');
      done();
    });
  });

  it('generateReport fills the report summary via LLM (ANO-012)', (done) => {
    service.generateReport('a1').subscribe(p => {
      expect(p.reportSummary).toBeTruthy();
      expect(p.reportSummary!.length).toBeGreaterThan(10);
      done();
    });
  });

  it('addFinding attaches a finding to the plan', (done) => {
    service.addFinding('a1', { type: 'MINOR_NC', description: 'Étiquette manquante', raisedBy: 'u' }).subscribe(f => {
      expect(f.planId).toBe('a1');
      expect(f.type).toBe('MINOR_NC');
      done();
    });
  });
});

describe('AuditsService (offline-first, API réelle)', () => {
  let service: AuditsService;
  let httpMock: HttpTestingController;
  let connectivity: FakeConnectivity;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    connectivity = new FakeConnectivity();
    configure(connectivity);
    service = TestBed.inject(AuditsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    httpMock.verify();
  });

  it('hors-ligne : respondChecklistItem met en file et répond de façon optimiste', (done) => {
    connectivity.online = false;
    service.respondChecklistItem('a1', 'cli1', { conformant: true, response: 'OK' }).subscribe(item => {
      expect(item.pendingSync).toBeTrue();
      expect(item.conformant).toBeTrue();
      expect(item.response).toBe('OK');
      httpMock.expectNone(`${environment.apiBaseUrl}/api/v1/audits/plans/a1/checklist/cli1/response`);
      done();
    });
  });

  it('hors-ligne : addFinding met en file et répond de façon optimiste', (done) => {
    connectivity.online = false;
    service.addFinding('a1', { type: 'MAJOR_NC', description: 'EPI manquant', raisedBy: 'u' }).subscribe(f => {
      expect(f.pendingSync).toBeTrue();
      expect(f.id.startsWith('offline-')).toBeTrue();
      expect(f.type).toBe('MAJOR_NC');
      httpMock.expectNone(`${environment.apiBaseUrl}/api/v1/audits/plans/a1/findings`);
      done();
    });
  });

  it('coupure pendant l’envoi (status 0) : respondChecklistItem bascule en file', (done) => {
    connectivity.online = true;
    service.respondChecklistItem('a1', 'cli1', { conformant: false }).subscribe(item => {
      expect(item.pendingSync).toBeTrue();
      done();
    });
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/audits/plans/a1/checklist/cli1/response`);
    req.error(new ProgressEvent('error'), { status: 0 });
  });

  it('coupure pendant l’envoi (status 0) : addFinding bascule en file', (done) => {
    connectivity.online = true;
    service.addFinding('a1', { type: 'OBSERVATION', description: 'Zone encombrée', raisedBy: 'u' }).subscribe(f => {
      expect(f.pendingSync).toBeTrue();
      done();
    });
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/audits/plans/a1/findings`);
    req.error(new ProgressEvent('error'), { status: 0 });
  });

  it('en ligne : respondChecklistItem appelle l’API normalement', (done) => {
    connectivity.online = true;
    service.respondChecklistItem('a1', 'cli1', { conformant: true }).subscribe(item => {
      expect(item.id).toBe('srv-1');
      expect(item.pendingSync).toBeUndefined();
      done();
    });
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/audits/plans/a1/checklist/cli1/response`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 'srv-1', planId: 'a1', question: 'Q', conformant: true,
      createdAt: 'now', updatedAt: 'now' });
  });

  it('en ligne : addFinding appelle l’API normalement', (done) => {
    connectivity.online = true;
    service.addFinding('a1', { type: 'MINOR_NC', description: 'Doc obsolète', raisedBy: 'u' }).subscribe(f => {
      expect(f.id).toBe('srv-f1');
      expect(f.pendingSync).toBeUndefined();
      done();
    });
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/audits/plans/a1/findings`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'srv-f1', planId: 'a1', type: 'MINOR_NC', description: 'Doc obsolète',
      raisedBy: 'u', raisedAt: 'now', createdAt: 'now', updatedAt: 'now' });
  });
});
