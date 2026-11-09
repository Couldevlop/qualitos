import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { fakeAsync, flushMicrotasks, TestBed, tick } from '@angular/core/testing';
import { firstValueFrom, Subject } from 'rxjs';

import { ConnectivityService } from './connectivity.service';
import { OfflineQueueEvent, OfflineQueueService, payloadOf } from './offline-queue.service';
import { InMemoryQueueStore, OfflineQueueStore, QueuedOperation } from './offline-queue.store';

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

  // --- envoi de fichier (photo 5S de terrain) ---------------------------------

  it("garde les octets d'une photo et le nom du champ, sans FormData en file", fakeAsync(() => {
    const photo = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'zone.jpg', { type: 'image/jpeg' });
    let op!: QueuedOperation;

    service.enqueue('POST', '/api/v1/fives/audits/a1/photos/SEIRI', photo,
        'Photo 5S SEIRI', { field: 'image', filename: 'zone.jpg' }).subscribe(o => (op = o));
    flushMicrotasks();

    // Un FormData n'est pas clonable : il ne survivrait ni a IndexedDB ni a la
    // fermeture de la PWA. On garde le Blob, et le rejeu assemble le formulaire.
    expect(op.body instanceof File).toBeTrue();
    expect(op.multipart).toEqual({ field: 'image', filename: 'zone.jpg' });
  }));

  it('assemble le formulaire multipart au moment du rejeu', fakeAsync(() => {
    const photo = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'zone.jpg', { type: 'image/jpeg' });
    service.enqueue('POST', '/api/v1/fives/audits/a1/photos/SEIRI', photo,
        'Photo 5S SEIRI', { field: 'image', filename: 'zone.jpg' }).subscribe();
    flushMicrotasks();

    connectivity.setOnline(true);
    flushMicrotasks();

    const req = httpMock.expectOne('/api/v1/fives/audits/a1/photos/SEIRI');
    expect(req.request.body instanceof FormData).toBeTrue();
    // Le navigateur recopie le fichier en l'ajoutant au formulaire : on vérifie
    // ce qui compte, le nom de la partie et les octets annoncés.
    const partie = (req.request.body as FormData).get('image') as File;
    expect(partie.name).toBe('zone.jpg');
    expect(partie.type).toBe('image/jpeg');
    expect(partie.size).toBe(photo.size);
    req.flush({ id: 'p-1' });
    flushMicrotasks();
    tick();

    expect(events.some(e => e.type === 'replayed')).toBeTrue();
  }));

  it("laisse le corps JSON intact quand l'operation n'est pas un envoi de fichier", () => {
    const ordinaire: QueuedOperation = {
      id: 'op-1', method: 'PUT', url: '/x', body: { score: 7 }, label: 'Score',
      queuedAt: '2026-11-09T10:00:00.000Z'
    };

    expect(payloadOf(ordinaire)).toEqual({ score: 7 });
  });

  it('ignore replay() quand hors-ligne', async () => {
    await firstValueFrom(service.enqueue('POST', '/x', {}, 'op'));
    await service.replay();   // offline : ne doit émettre aucune requête HTTP
    httpMock.expectNone('/x');
  });
});
