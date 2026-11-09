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

  it('renoter un pilier déjà noté remplace le score au lieu d\'ajouter une ligne', (done) => {
    service.scorePillar('5s-2', { pillar: 'SEISO', score: 4, note: 'Premier passage' }).subscribe(() => {
      service.scorePillar('5s-2', { pillar: 'SEISO', score: 9, note: 'Après remise en état' })
        .subscribe(item => {
          expect(item.score).toBe(9);
          expect(item.note).toBe('Après remise en état');
          service.getAudit('5s-2').subscribe(a => {
            expect(a.items.length).toBe(1);
            expect(a.overallScore).toBe(90);
            done();
          });
        });
    });
  });

  it('noter un audit inconnu ne crée pas d\'audit fantôme', (done) => {
    service.scorePillar('inconnu', { pillar: 'SEIRI', score: 7 }).subscribe(item => {
      expect(item.id).toBe('orphan');
      expect(item.auditId).toBe('inconnu');
      done();
    });
  });

  it('met à jour zone, description et date prévue', (done) => {
    service.updateAudit('5s-3', {
      zone: 'Entrepôt sud', description: 'Réaffecté', scheduledAt: '2026-09-01T08:00:00Z'
    }).subscribe(a => {
      expect(a.zone).toBe('Entrepôt sud');
      expect(a.description).toBe('Réaffecté');
      expect(a.scheduledAt).toBe('2026-09-01T08:00:00Z');
      done();
    });
  });

  it('ne touche pas aux champs absents d\'une mise à jour partielle', (done) => {
    service.updateAudit('5s-1', { description: 'Note revue' }).subscribe(a => {
      expect(a.description).toBe('Note revue');
      expect(a.zone).toBe('Atelier mécanique A');
      done();
    });
  });

  it('mettre à jour un audit inconnu retombe sur l\'audit de repli', (done) => {
    service.updateAudit('inconnu', { zone: 'ignorée' }).subscribe(a => {
      expect(a.id).toBe('5s-1');
      expect(a.zone).toBe('Atelier mécanique A');
      done();
    });
  });

  it('supprime un audit et le retire de la liste', (done) => {
    service.deleteAudit('5s-3').subscribe(() => {
      service.listAudits().subscribe(page => {
        expect(page.content.some(a => a.id === '5s-3')).toBeFalse();
        done();
      });
    });
  });

  it('supprimer un identifiant inconnu ne retire rien', (done) => {
    service.deleteAudit('inconnu').subscribe(() => {
      service.listAudits().subscribe(page => {
        expect(page.content.length).toBe(3);
        done();
      });
    });
  });

  it('startAudit et cancelAudit basculent le statut sans horodater de clôture', (done) => {
    service.startAudit('5s-3').subscribe(started => {
      expect(started.status).toBe('IN_PROGRESS');
      service.cancelAudit('5s-3').subscribe(cancelled => {
        expect(cancelled.status).toBe('CANCELLED');
        expect(cancelled.completedAt).toBeUndefined();
        done();
      });
    });
  });

  it('une transition sur un audit inconnu ne corrompt pas l\'audit de repli', (done) => {
    service.cancelAudit('inconnu').subscribe(a => {
      expect(a.id).toBe('5s-1');
      expect(a.status).toBe('COMPLETED');
      done();
    });
  });

  it('l\'analyse vision simulée est déterministe pour un même fichier', (done) => {
    const file = new File(['pixels'], 'zone-a.jpg', { type: 'image/jpeg' });
    service.analyzeAuditPhoto('5s-1', file).subscribe(first => {
      expect(first.score.overall).toBeGreaterThan(0);
      expect(first.score.overall).toBeLessThanOrEqual(100);
      expect(first.findings.length).toBe(2);
      expect(first.findings[0].pillar).toBe('SEITON');
      service.analyzeAuditPhoto('5s-1', file).subscribe(second => {
        // Déterminisme : sans lui, une démo produirait un score différent à chaque clic.
        expect(second).toEqual(first);
        done();
      });
    });
  });
  it('porte un pilier au cercle, et cite la zone et la note', (done) => {
    service.listAudits().subscribe(page => {
      const a = page.content[0];
      service.scorePillar(a.id, { pillar: 'SEIRI', score: 3 }).subscribe(() => {
        service.proposeToCircle(a.id, {
          circleId: 'c-1', pillar: 'SEIRI', title: 'Vider la zone'
        }).subscribe(idee => {
          expect(idee.status).toBe('PROPOSED');
          expect(idee.circleId).toBe('c-1');
          expect(idee.originRef).toContain(a.zone);
          expect(idee.originRef).toContain('3/10');
          done();
        });
      });
    });
  });
  it("en demonstration, l'apercu vient du fichier verse", (done) => {
    const fichier = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'zone.jpg',
      { type: 'image/jpeg' });
    service.uploadItemPhoto('5s-1', 'SEIRI', fichier).subscribe(photo => {
      expect(photo.pillar).toBe('SEIRI');
      expect(photo.localUrl).toContain('blob:');
      service.listPhotos('5s-1').subscribe(liste => {
        expect(liste.map(p => p.id)).toContain(photo.id);
        service.deleteItemPhoto('5s-1', photo.id).subscribe(() => {
          service.listPhotos('5s-1').subscribe(apres => {
            expect(apres.length).toBe(0);
            done();
          });
        });
      });
    });
  });
});

/**
 * Mode API réelle sans coupure réseau : le contrat HTTP des routes qui ne
 * passent jamais par la file offline (lectures, transitions, vision).
 */
describe('FivesService (API réelle, en ligne)', () => {
  let service: FivesService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/fives/audits`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    configure(new FakeConnectivity());
    service = TestBed.inject(FivesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    httpMock.verify();
  });

  it('pagine la liste et n\'envoie le statut que s\'il est filtré', () => {
    service.listAudits(3, 10).subscribe();
    const req = httpMock.expectOne(r => r.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('3');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.has('status')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 3, size: 10 });
  });

  it('transmet le filtre de statut', () => {
    service.listAudits(0, 50, 'IN_PROGRESS').subscribe();
    const req = httpMock.expectOne(r => r.url === base);
    expect(req.request.params.get('status')).toBe('IN_PROGRESS');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 50 });
  });

  it('lit un audit par identifiant', (done) => {
    service.getAudit('a1').subscribe(a => {
      expect(a.zone).toBe('Atelier A');
      done();
    });
    const req = httpMock.expectOne(`${base}/a1`);
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'a1', tenantId: 't1', zone: 'Atelier A', status: 'DRAFT', auditorId: 'u1',
      createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z', items: []
    });
  });

  it('envoie la photo en multipart sous le champ « image » attendu par le serveur', (done) => {
    const file = new File(['pixels'], 'zone.jpg', { type: 'image/jpeg' });
    service.analyzeAuditPhoto('a1', file).subscribe(res => {
      expect(res.score.overall).toBe(72);
      done();
    });
    const req = httpMock.expectOne(`${base}/a1/vision`);
    expect(req.request.method).toBe('POST');
    const body = req.request.body as FormData;
    expect(body instanceof FormData).toBeTrue();
    expect((body.get('image') as File).name).toBe('zone.jpg');
    req.flush({
      imageSha256: 'abc', width: 1280, height: 720,
      score: { seiri: 70, seiton: 70, seiso: 70, seiketsu: 70, shitsuke: 70, overall: 72 },
      findings: []
    });
  });

  it('l\'analyse vision n\'est pas mise en file : un 503 remonte à l\'UI', (done) => {
    const file = new File(['pixels'], 'zone.jpg', { type: 'image/jpeg' });
    service.analyzeAuditPhoto('a1', file).subscribe({
      next: () => fail('l\'indisponibilité du service vision doit remonter'),
      error: err => {
        expect(err.status).toBe(503);
        done();
      }
    });
    httpMock.expectOne(`${base}/a1/vision`)
      .flush({ title: 'unavailable' }, { status: 503, statusText: 'Service Unavailable' });
  });

  it('crée un audit en POST quand le réseau est disponible', () => {
    service.createAudit({ zone: 'Atelier A', auditorId: 'u1' }).subscribe();
    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ zone: 'Atelier A', auditorId: 'u1' });
    req.flush({
      id: 'a1', tenantId: 't1', zone: 'Atelier A', status: 'DRAFT', auditorId: 'u1',
      createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z', items: []
    });
  });

  it('met à jour un audit en PATCH', () => {
    service.updateAudit('a1', { zone: 'Atelier B' }).subscribe();
    const req = httpMock.expectOne(`${base}/a1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ zone: 'Atelier B' });
    req.flush({
      id: 'a1', tenantId: 't1', zone: 'Atelier B', status: 'DRAFT', auditorId: 'u1',
      createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z', items: []
    });
  });

  it('supprime un audit en DELETE', (done) => {
    service.deleteAudit('a1').subscribe(() => done());
    const req = httpMock.expectOne(`${base}/a1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('mappe chaque transition d\'audit sur son segment d\'URL', () => {
    const flushed = {
      id: 'a1', tenantId: 't1', zone: 'Atelier A', status: 'IN_PROGRESS', auditorId: 'u1',
      createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z', items: []
    };
    service.startAudit('a1').subscribe();
    const start = httpMock.expectOne(`${base}/a1/start`);
    expect(start.request.method).toBe('PATCH');
    expect(start.request.body).toEqual({});
    start.flush(flushed);

    service.completeAudit('a1').subscribe();
    httpMock.expectOne(`${base}/a1/complete`).flush({ ...flushed, status: 'COMPLETED' });

    service.cancelAudit('a1').subscribe();
    httpMock.expectOne(`${base}/a1/cancel`).flush({ ...flushed, status: 'CANCELLED' });
  });

  it('hors-ligne, la création est mise en file même si l\'envoi échoue en cours de route', (done) => {
    service.createAudit({ zone: 'Zone coupée', auditorId: 'u1' }).subscribe(a => {
      expect(a.pendingSync).toBeTrue();
      expect(a.zone).toBe('Zone coupée');
      done();
    });
    // status 0 = la requête n'a jamais atteint le serveur.
    httpMock.expectOne(base).error(new ProgressEvent('error'), { status: 0 });
  });

  it('un refus applicatif (400) n\'est PAS mis en file : la saisie est invalide', (done) => {
    service.createAudit({ zone: '', auditorId: 'u1' }).subscribe({
      next: () => fail('une requête refusée par le serveur ne doit pas être rejouée'),
      error: err => {
        expect(err.status).toBe(400);
        done();
      }
    });
    httpMock.expectOne(base).flush({ title: 'zone blank' }, { status: 400, statusText: 'Bad Request' });
  });
  it('poste l’idée sur l’audit, et ne la met jamais en file hors ligne', () => {
    // Déposer une idée engage d'autres personnes : une demande qui repartirait
    // des heures plus tard sur un cercle entre-temps archivé échouerait sans
    // que personne ne le voie.
    service.proposeToCircle('a1', {
      circleId: 'c-1', pillar: 'SEISO', title: 'Nettoyer', description: 'Sol gras'
    }).subscribe();
    const req = httpMock.expectOne(`${base}/a1/circle-proposals`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      circleId: 'c-1', pillar: 'SEISO', title: 'Nettoyer', description: 'Sol gras'
    });
    req.flush({ id: 'p-1', circleId: 'c-1', title: 'Nettoyer', status: 'PROPOSED' });
  });
  it('poste la photo en multipart sur le pilier, et lit les vignettes de l’audit', () => {
    const fichier = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'zone.jpg',
      { type: 'image/jpeg' });

    service.uploadItemPhoto('a1', 'SEITON', fichier).subscribe();
    const envoi = httpMock.expectOne(`${base}/a1/photos/SEITON`);
    expect(envoi.request.method).toBe('POST');
    expect(envoi.request.body instanceof FormData).toBeTrue();
    expect((envoi.request.body as FormData).get('image')).toBeTruthy();
    envoi.flush({ id: 'p-1', auditId: 'a1', pillar: 'SEITON' });

    service.listPhotos('a1').subscribe();
    const lecture = httpMock.expectOne(`${base}/a1/photos`);
    expect(lecture.request.method).toBe('GET');
    lecture.flush([]);

    service.deleteItemPhoto('a1', 'p-1').subscribe();
    const retrait = httpMock.expectOne(`${base}/a1/photos/p-1`);
    expect(retrait.request.method).toBe('DELETE');
    retrait.flush(null);
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
  it('hors-ligne : la photo attend dans la file, avec un apercu local', (done) => {
    connectivity.online = false;
    const fichier = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'zone.jpg',
      { type: 'image/jpeg' });

    service.uploadItemPhoto('a1', 'SEIRI', fichier).subscribe(photo => {
      expect(photo.pendingSync).toBeTrue();
      expect(photo.localUrl).toContain('blob:');
      expect(photo.sizeBytes).toBe(fichier.size);
      // Rien ne part : c'est la file qui enverra au retour du reseau.
      httpMock.expectNone(`${environment.apiBaseUrl}/api/v1/fives/audits/a1/photos/SEIRI`);
      done();
    });
  });

  it('coupure pendant l’envoi d’une photo : bascule en file au lieu d’echouer', (done) => {
    connectivity.online = true;
    const fichier = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'zone.jpg',
      { type: 'image/jpeg' });

    service.uploadItemPhoto('a1', 'SEIRI', fichier).subscribe(photo => {
      expect(photo.pendingSync).toBeTrue();
      done();
    });
    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/v1/fives/audits/a1/photos/SEIRI`);
    req.error(new ProgressEvent('error'), { status: 0 });
  });

  it('une erreur applicative sur la photo remonte, elle ne part pas en file', (done) => {
    connectivity.online = true;
    const fichier = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'zone.jpg',
      { type: 'image/jpeg' });

    service.uploadItemPhoto('a1', 'SEIRI', fichier).subscribe({
      next: () => fail('un refus du serveur ne doit pas devenir une mise en file'),
      error: err => {
        expect(err.status).toBe(409);
        done();
      }
    });
    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/v1/fives/audits/a1/photos/SEIRI`);
    req.flush({ detail: 'no score' }, { status: 409, statusText: 'Conflict' });
  });
});
