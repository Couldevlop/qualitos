import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { fakeAsync, flushMicrotasks, TestBed, tick } from '@angular/core/testing';
import { firstValueFrom, Subject } from 'rxjs';

import { ConnectivityService } from './connectivity.service';
import { OfflineQueueEvent, OfflineQueueService } from './offline-queue.service';
import { InMemoryQueueStore, OfflineQueueStore } from './offline-queue.store';

/** Double de ConnectivityService pilotable par les tests. */
class FakeConnectivity {
  readonly onlineSubject = new Subject<boolean>();
  readonly online$ = this.onlineSubject.asObservable();
  private online = false;

  setOnline(value: boolean): void {
    this.online = value;
    this.onlineSubject.next(value);
  }

  isOnline(): boolean {
    return this.online;
  }
}

describe('OfflineQueueService', () => {
  let service: OfflineQueueService;
  let httpMock: HttpTestingController;
  let connectivity: FakeConnectivity;
  let events: OfflineQueueEvent[];

  beforeEach(() => {
    connectivity = new FakeConnectivity();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: OfflineQueueStore, useClass: InMemoryQueueStore },
        { provide: ConnectivityService, useValue: connectivity }
      ]
    });
    service = TestBed.inject(OfflineQueueService);
    httpMock = TestBed.inject(HttpTestingController);
    events = [];
    service.events$.subscribe(e => events.push(e));
  });

  afterEach(() => {
    httpMock.verify();
  });

  it("enregistre l'opération et émet l'événement queued + le compteur", fakeAsync(() => {
    let count = -1;
    service.pendingCount$.subscribe(c => (count = c));

    let opId = '';
    service.enqueue('PUT', '/api/v1/fives/audits/a1/score', { pillar: 'SEIRI', score: 7 },
        'Score 5S SEIRI')
      .subscribe(op => (opId = op.id));
    flushMicrotasks();

    expect(opId).not.toBe('');
    expect(count).toBe(1);
    expect(events.length).toBe(1);
    expect(events[0].type).toBe('queued');
    expect(events[0].operation.label).toBe('Score 5S SEIRI');
  }));

  it('rejoue la file dans l’ordre au retour du réseau et vide le compteur', fakeAsync(() => {
    service.enqueue('POST', '/api/v1/fives/audits', { zone: 'A' }, 'Création audit').subscribe();
    flushMicrotasks();
    service.enqueue('PUT', '/api/v1/fives/audits/a1/score', { score: 5 }, 'Score').subscribe();
    flushMicrotasks();

    connectivity.setOnline(true);
    flushMicrotasks();

    const first = httpMock.expectOne('/api/v1/fives/audits');
    expect(first.request.method).toBe('POST');
    first.flush({ id: 'a1' });
    flushMicrotasks();

    const second = httpMock.expectOne('/api/v1/fives/audits/a1/score');
    expect(second.request.method).toBe('PUT');
    second.flush({ id: 'i1' });
    flushMicrotasks();
    tick();

    let count = -1;
    service.pendingCount$.subscribe(c => (count = c));
    expect(count).toBe(0);
    expect(events.filter(e => e.type === 'replayed').length).toBe(2);
  }));

  it('stoppe le rejeu sur erreur réseau (status 0) et conserve les opérations', fakeAsync(() => {
    service.enqueue('POST', '/api/v1/fives/audits', { zone: 'A' }, 'Création audit').subscribe();
    flushMicrotasks();

    connectivity.setOnline(true);
    flushMicrotasks();

    const req = httpMock.expectOne('/api/v1/fives/audits');
    req.error(new ProgressEvent('error'), { status: 0 });
    flushMicrotasks();
    tick();

    let count = -1;
    service.pendingCount$.subscribe(c => (count = c));
    expect(count).toBe(1);
    expect(events.some(e => e.type === 'replay-failed')).toBeFalse();
  }));

  it("retire l'opération et émet replay-failed sur erreur applicative (4xx)", fakeAsync(() => {
    service.enqueue('POST', '/api/v1/fives/audits', { zone: '' }, 'Création audit').subscribe();
    flushMicrotasks();

    connectivity.setOnline(true);
    flushMicrotasks();

    const req = httpMock.expectOne('/api/v1/fives/audits');
    req.flush({ detail: 'zone obligatoire' }, { status: 400, statusText: 'Bad Request' });
    flushMicrotasks();
    tick();

    let count = -1;
    service.pendingCount$.subscribe(c => (count = c));
    expect(count).toBe(0);
    expect(events.some(e => e.type === 'replay-failed')).toBeTrue();
  }));

  it('ignore replay() quand hors-ligne', async () => {
    await firstValueFrom(service.enqueue('POST', '/x', {}, 'op'));
    await service.replay();   // offline : ne doit émettre aucune requête HTTP
    httpMock.expectNone('/x');
  });
});
