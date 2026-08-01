import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { Subject, firstValueFrom } from 'rxjs';

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

  it('retombe sur la première NC quand l\'identifiant est inconnu (démo sans backend)', async () => {
    const n = await firstValueFrom(service.getNc('inexistante'));
    expect(n.id).toBe('nc-1');
  });

  it('renvoie une page vide quand aucun filtre ne matche', async () => {
    const page = await firstValueFrom(service.listNcs(0, 50, { status: 'CLOSED' }));
    expect(page.content).toEqual([]);
    expect(page.totalElements).toBe(0);
  });

  it('updateNc ne modifie que les champs fournis et rafraîchit updatedAt', async () => {
    const before = await firstValueFrom(service.getNc('nc-2'));
    const updated = await firstValueFrom(
      service.updateNc('nc-2', { title: 'Fuite hydraulique presse 4 — reprise', zone: 'Atelier 2' }));

    expect(updated.title).toBe('Fuite hydraulique presse 4 — reprise');
    expect(updated.zone).toBe('Atelier 2');
    // les champs absents de la requête restent inchangés
    expect(updated.severity).toBe(before.severity);
    expect(updated.description).toBe(before.description);
    expect(updated.status).toBe(before.status);
    expect(updated.updatedAt).toBeTruthy();
  });

  it('updateNc sur un identifiant inconnu ne crée rien et retombe sur la première NC', async () => {
    const n = await firstValueFrom(service.updateNc('inexistante', { title: 'X' }));
    expect(n.id).toBe('nc-1');
    expect(n.title).not.toBe('X');
  });

  it('defineAction fait passer la NC en ACTION_DEFINED', async () => {
    const n = await firstValueFrom(service.defineAction('nc-1'));
    expect(n.status).toBe('ACTION_DEFINED');
  });

  it('close horodate la clôture, cancel marque l\'abandon', async () => {
    const closed = await firstValueFrom(service.close('nc-1'));
    expect(closed.status).toBe('CLOSED');
    expect(closed.closedAt).toBeTruthy();

    const cancelled = await firstValueFrom(service.cancel('nc-2'));
    expect(cancelled.status).toBe('CANCELLED');
  });

  it('les photos simulées sont cloisonnées par NC (ajout, lecture, suppression)', async () => {
    const file = new File([new Uint8Array([1, 2])], 'poste.jpg', { type: 'image/jpeg' });
    const photo = await firstValueFrom(service.uploadPhoto('nc-1', file));
    expect(photo.originalFilename).toBe('poste.jpg');
    expect(photo.sizeBytes).toBe(2);

    expect((await firstValueFrom(service.listPhotos('nc-1'))).length).toBe(1);
    // une autre NC ne voit pas la photo de la première
    expect(await firstValueFrom(service.listPhotos('nc-2'))).toEqual([]);

    await firstValueFrom(service.deletePhoto('nc-1', photo.id));
    expect(await firstValueFrom(service.listPhotos('nc-1'))).toEqual([]);
  });

  it('la suppression d\'une photo inconnue est sans effet (pas d\'erreur)', async () => {
    const file = new File([new Uint8Array([1])], 'a.png', { type: 'image/png' });
    await firstValueFrom(service.uploadPhoto('nc-1', file));
    await firstValueFrom(service.deletePhoto('nc-1', 'photo-absente'));
    expect((await firstValueFrom(service.listPhotos('nc-1'))).length).toBe(1);
  });

  it('analyzePhotoVision renvoie un résultat simulé déterministe', (done) => {
    const file = new File([new Uint8Array([1, 2, 3])], 'atelier.jpg', { type: 'image/jpeg' });
    service.analyzePhotoVision(file).subscribe(a1 => {
      expect(a1.score.overall).toBeGreaterThanOrEqual(0);
      expect(a1.score.overall).toBeLessThanOrEqual(100);
      expect(a1.findings.length).toBeGreaterThan(0);
      expect(a1.findings[0].pillar).toBe('SEIRI');
      // déterminisme : même fichier → même score global
      service.analyzePhotoVision(
        new File([new Uint8Array([1, 2, 3])], 'atelier.jpg', { type: 'image/jpeg' })
      ).subscribe(a2 => {
        expect(a2.score.overall).toBe(a1.score.overall);
        done();
      });
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

  it('listNcs pagine et n\'envoie que les filtres renseignés', () => {
    service.listNcs(3, 25, { severity: 'CRITICAL' }).subscribe();
    const req = httpMock.expectOne(r => r.url === endpoint);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('3');
    expect(req.request.params.get('size')).toBe('25');
    expect(req.request.params.get('severity')).toBe('CRITICAL');
    expect(req.request.params.has('status')).toBeFalse();
    expect(req.request.params.has('category')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 3, size: 25 });
  });

  it('listNcs transmet les trois filtres combinés', () => {
    service.listNcs(0, 20, { status: 'OPEN', severity: 'MAJOR', category: 'SAFETY' }).subscribe();
    const req = httpMock.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('status')).toBe('OPEN');
    expect(req.request.params.get('severity')).toBe('MAJOR');
    expect(req.request.params.get('category')).toBe('SAFETY');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('listNcs propage le 403 (module non activé / droits insuffisants)', (done) => {
    service.listNcs().subscribe({
      next: () => done.fail('ne devrait pas réussir'),
      error: err => { expect(err.status).toBe(403); done(); }
    });
    httpMock.expectOne(r => r.url === endpoint)
      .flush({ title: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });
  });

  it('getNc lit la fiche par identifiant', (done) => {
    service.getNc('a1').subscribe(n => { expect(n.id).toBe('a1'); done(); });
    const req = httpMock.expectOne(`${endpoint}/a1`);
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'a1', reference: 'NC-2026-1', title: 't', category: 'PROCESS',
      severity: 'MAJOR', status: 'OPEN', detectedAt: '', createdAt: '', updatedAt: ''
    });
  });

  it('updateNc envoie un PUT avec les seuls champs modifiés', (done) => {
    service.updateNc('a1', { title: 'Corrigé', zone: 'Zone B' }).subscribe(() => done());
    const req = httpMock.expectOne(`${endpoint}/a1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ title: 'Corrigé', zone: 'Zone B' });
    req.flush({
      id: 'a1', reference: 'NC-2026-1', title: 'Corrigé', category: 'PROCESS',
      severity: 'MAJOR', status: 'OPEN', detectedAt: '', createdAt: '', updatedAt: ''
    });
  });

  it('resolve envoie la note de résolution', (done) => {
    service.resolve('a1', { resolutionNote: 'Joint remplacé.' }).subscribe(n => {
      expect(n.status).toBe('RESOLVED');
      done();
    });
    const req = httpMock.expectOne(`${endpoint}/a1/resolve`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ resolutionNote: 'Joint remplacé.' });
    req.flush({
      id: 'a1', reference: 'NC-2026-1', title: 't', category: 'PROCESS',
      severity: 'MAJOR', status: 'RESOLVED', detectedAt: '', createdAt: '', updatedAt: ''
    });
  });

  it('close et cancel postent sur leurs sous-ressources avec un corps vide', (done) => {
    service.close('a1').subscribe();
    const closeReq = httpMock.expectOne(`${endpoint}/a1/close`);
    expect(closeReq.request.method).toBe('POST');
    expect(closeReq.request.body).toEqual({});
    closeReq.flush({
      id: 'a1', reference: 'NC-2026-1', title: 't', category: 'PROCESS',
      severity: 'MAJOR', status: 'CLOSED', detectedAt: '', createdAt: '', updatedAt: ''
    });

    service.cancel('a1').subscribe(n => { expect(n.status).toBe('CANCELLED'); done(); });
    const cancelReq = httpMock.expectOne(`${endpoint}/a1/cancel`);
    expect(cancelReq.request.body).toEqual({});
    cancelReq.flush({
      id: 'a1', reference: 'NC-2026-1', title: 't', category: 'PROCESS',
      severity: 'MAJOR', status: 'CANCELLED', detectedAt: '', createdAt: '', updatedAt: ''
    });
  });

  it('defineAction poste sur define-action et propage le 409 (transition interdite)', (done) => {
    service.defineAction('a1').subscribe({
      next: () => done.fail('la transition ne devrait pas aboutir'),
      error: err => { expect(err.status).toBe(409); done(); }
    });
    const req = httpMock.expectOne(`${endpoint}/a1/define-action`);
    expect(req.request.method).toBe('POST');
    req.flush({ title: 'Conflict' }, { status: 409, statusText: 'Conflict' });
  });

  it('une transition hors-ligne échoue au lieu d\'être mise en file (workflow online-only)', (done) => {
    connectivity.online = false;
    service.close('a1').subscribe({
      next: () => done.fail('ne devrait pas réussir hors-ligne'),
      error: err => { expect(err.status).toBe(0); done(); }
    });
    httpMock.expectOne(`${endpoint}/a1/close`).error(new ProgressEvent('error'), { status: 0 });
  });

  // --- photos (upload binaire, online-only) ---------------------------------

  it('listPhotos appelle GET .../photos et renvoie les métadonnées', (done) => {
    service.listPhotos('a1').subscribe(photos => {
      expect(photos.length).toBe(1);
      expect(photos[0].url).toBe('https://store/presigned/p1');
      done();
    });
    const req = httpMock.expectOne(`${endpoint}/a1/photos`);
    expect(req.request.method).toBe('GET');
    req.flush([
      { id: 'p1', url: 'https://store/presigned/p1', contentType: 'image/jpeg',
        sizeBytes: 1234, originalFilename: 'champ.jpg', createdAt: '2026-06-06T00:00:00Z' }
    ]);
  });

  it('uploadPhoto envoie un multipart avec le champ \'file\'', (done) => {
    const file = new File([new Uint8Array([1, 2, 3])], 'photo.png', { type: 'image/png' });
    service.uploadPhoto('a1', file).subscribe(photo => {
      expect(photo.id).toBe('p9');
      done();
    });
    const req = httpMock.expectOne(`${endpoint}/a1/photos`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    const body = req.request.body as FormData;
    const sent = body.get('file');
    expect(sent instanceof File).toBeTrue();
    expect((sent as File).name).toBe('photo.png');
    req.flush({
      id: 'p9', objectKey: 'tenant/a1/p9.png', contentType: 'image/png',
      sizeBytes: 3, originalFilename: 'photo.png', createdAt: '2026-06-06T00:00:00Z'
    }, { status: 201, statusText: 'Created' });
  });

  it('uploadPhoto propage proprement le 503 storage-disabled', (done) => {
    const file = new File([new Uint8Array([1])], 'p.webp', { type: 'image/webp' });
    service.uploadPhoto('a1', file).subscribe({
      next: () => done.fail('ne devrait pas réussir'),
      error: err => {
        expect(err.status).toBe(503);
        expect(err.error?.type).toContain('storage-disabled');
        done();
      }
    });
    const req = httpMock.expectOne(`${endpoint}/a1/photos`);
    req.flush({ type: 'https://qualitos.io/errors/storage-disabled' }, { status: 503, statusText: 'Service Unavailable' });
  });

  it('uploadPhoto propage proprement le 409 (NC clôturée/annulée)', (done) => {
    const file = new File([new Uint8Array([1])], 'p.jpg', { type: 'image/jpeg' });
    service.uploadPhoto('a1', file).subscribe({
      next: () => done.fail('ne devrait pas réussir'),
      error: err => {
        expect(err.status).toBe(409);
        done();
      }
    });
    const req = httpMock.expectOne(`${endpoint}/a1/photos`);
    req.flush({ title: 'Conflict' }, { status: 409, statusText: 'Conflict' });
  });

  it('deletePhoto appelle DELETE .../photos/{id} et renvoie 204', (done) => {
    service.deletePhoto('a1', 'p1').subscribe(() => done());
    const req = httpMock.expectOne(`${endpoint}/a1/photos/p1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  // --- analyse Vision 5S par IA (online-only, multipart 'image') -------------

  const visionEndpoint = `${environment.apiBaseUrl}/api/v1/vision/5s/analyze`;

  it('analyzePhotoVision POST multipart champ \'image\' + mappe la réponse', (done) => {
    const file = new File([new Uint8Array([1, 2, 3])], 'zone.jpg', { type: 'image/jpeg' });
    service.analyzePhotoVision(file).subscribe(result => {
      expect(result.imageSha256).toBe('abc123');
      expect(result.width).toBe(1280);
      expect(result.score.overall).toBe(78);
      expect(result.findings.length).toBe(1);
      expect(result.findings[0].pillar).toBe('SEIRI');
      expect(result.findings[0].bbox).toEqual([10, 20, 30, 40]);
      done();
    });
    const req = httpMock.expectOne(visionEndpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    const body = req.request.body as FormData;
    const sent = body.get('image');
    expect(sent instanceof File).toBeTrue();
    expect((sent as File).name).toBe('zone.jpg');
    req.flush({
      imageSha256: 'abc123', width: 1280, height: 720,
      score: { seiri: 70, seiton: 80, seiso: 90, seiketsu: 75, shitsuke: 75, overall: 78 },
      findings: [
        { pillar: 'SEIRI', description: 'Encombrement', severity: 'HIGH', confidence: 0.91, bbox: [10, 20, 30, 40] }
      ]
    });
  });

  it('analyzePhotoVision propage proprement le 503 vision-unavailable', (done) => {
    const file = new File([new Uint8Array([1])], 'z.png', { type: 'image/png' });
    service.analyzePhotoVision(file).subscribe({
      next: () => done.fail('ne devrait pas réussir'),
      error: err => {
        expect(err.status).toBe(503);
        expect(err.error?.type).toContain('vision-unavailable');
        done();
      }
    });
    const req = httpMock.expectOne(visionEndpoint);
    req.flush({ type: 'https://qualitos.io/errors/vision-unavailable' },
      { status: 503, statusText: 'Service Unavailable' });
  });

  it('analyzePhotoVision propage le 400 (vision-image-invalid)', (done) => {
    const file = new File([new Uint8Array([1])], 'z.txt', { type: 'text/plain' });
    service.analyzePhotoVision(file).subscribe({
      next: () => done.fail('ne devrait pas réussir'),
      error: err => { expect(err.status).toBe(400); done(); }
    });
    const req = httpMock.expectOne(visionEndpoint);
    req.flush({ type: 'https://qualitos.io/errors/vision-image-invalid' },
      { status: 400, statusText: 'Bad Request' });
  });

  it('analyzePhotoVision propage le 413 (vision-image-too-large)', (done) => {
    const file = new File([new Uint8Array([1])], 'huge.jpg', { type: 'image/jpeg' });
    service.analyzePhotoVision(file).subscribe({
      next: () => done.fail('ne devrait pas réussir'),
      error: err => { expect(err.status).toBe(413); done(); }
    });
    const req = httpMock.expectOne(visionEndpoint);
    req.flush({ type: 'https://qualitos.io/errors/vision-image-too-large' },
      { status: 413, statusText: 'Payload Too Large' });
  });
});
