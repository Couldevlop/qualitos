import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { Subject } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { InMemoryQueueStore, OfflineQueueStore } from '../../core/offline/offline-queue.store';
import { FivesService } from './fives.service';

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

describe('FivesService (mock mode)', () => {
  let service: FivesService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    configure(new FakeConnectivity());
    service = TestBed.inject(FivesService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded audits', (done) => {
    service.listAudits().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters audits by status', (done) => {
    service.listAudits(0, 50, 'COMPLETED').subscribe(page => {
      expect(page.content.every(a => a.status === 'COMPLETED')).toBeTrue();
      done();
    });
  });

  it('creates a DRAFT audit', (done) => {
    service.createAudit({ zone: 'Atelier Z', auditorId: 'u' }).subscribe(a => {
      expect(a.status).toBe('DRAFT');
      expect(a.zone).toBe('Atelier Z');
      done();
    });
  });

  it('scorePillar adds an item and recomputes overall score', (done) => {
    service.createAudit({ zone: 'Zone score', auditorId: 'u' }).subscribe(a => {
      service.scorePillar(a.id, { pillar: 'SEIRI', score: 8 }).subscribe(item => {
        expect(item.pillar).toBe('SEIRI');
        service.getAudit(a.id).subscribe(reloaded => {
          expect(reloaded.overallScore).toBe(80);
          done();
        });
      });
    });
  });

  it('completeAudit transitions to COMPLETED and sets completedAt', (done) => {
    service.completeAudit('5s-2').subscribe(a => {
      expect(a.status).toBe('COMPLETED');
      expect(a.completedAt).toBeTruthy();
      done();
    });
  });
});

describe('FivesService (offline-first, API réelle)', () => {
  let service: FivesService;
  let httpMock: HttpTestingController;
  let connectivity: FakeConnectivity;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    connectivity = new FakeConnectivity();
    configure(connectivity);
    service = TestBed.inject(FivesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    httpMock.verify();
  });

  it('hors-ligne : createAudit met en file et répond de façon optimiste', (done) => {
    connectivity.online = false;
    service.createAudit({ zone: 'Zone blanche', auditorId: 'u' }).subscribe(a => {
      expect(a.pendingSync).toBeTrue();
      expect(a.id.startsWith('offline-')).toBeTrue();
      expect(a.zone).toBe('Zone blanche');
      // Aucune requête HTTP ne doit partir.
      httpMock.expectNone(`${environment.apiBaseUrl}/api/v1/fives/audits`);
      done();
    });
  });

  it('hors-ligne : scorePillar met en file et répond de façon optimiste', (done) => {
    connectivity.online = false;
    service.scorePillar('a1', { pillar: 'SEISO', score: 6 }).subscribe(item => {
      expect(item.pendingSync).toBeTrue();
      expect(item.pillar).toBe('SEISO');
      httpMock.expectNone(`${environment.apiBaseUrl}/api/v1/fives/audits/a1/score`);
      done();
    });
  });

  it('coupure pendant l’envoi (status 0) : bascule en file au lieu d’échouer', (done) => {
    connectivity.online = true;
    service.scorePillar('a1', { pillar: 'SEIRI', score: 9 }).subscribe(item => {
      expect(item.pendingSync).toBeTrue();
      done();
    });
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/fives/audits/a1/score`);
    req.error(new ProgressEvent('error'), { status: 0 });
  });

  it('en ligne : scorePillar appelle l’API normalement', (done) => {
    connectivity.online = true;
    service.scorePillar('a1', { pillar: 'SEIRI', score: 9 }).subscribe(item => {
      expect(item.id).toBe('srv-1');
      expect(item.pendingSync).toBeUndefined();
      done();
    });
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/fives/audits/a1/score`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 'srv-1', auditId: 'a1', pillar: 'SEIRI', score: 9 });
  });
});
