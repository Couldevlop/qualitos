import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { InMemoryQueueStore, OfflineQueueStore } from '../../core/offline/offline-queue.store';
import { NcService } from './nc.service';

/** Connectivité pilotable : `navigator.onLine` est en lecture seule. */
class FakeConnectivity {
  online = true;
  private readonly subject = new Subject<boolean>();
  readonly online$ = this.subject.asObservable();
  isOnline(): boolean { return this.online; }
}

function configurer(): void {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      { provide: OfflineQueueStore, useClass: InMemoryQueueStore },
      { provide: ConnectivityService, useValue: new FakeConnectivity() }
    ]
  });
}

/**
 * Les quatre appels du rapport 8D.
 *
 * <p>Le point le moins évident est le PDF : sans `responseType: 'blob'`, Angular
 * lirait le document comme du JSON et échouerait sur le premier octet ; sans
 * `observe: 'response'`, le nom de fichier proposé par le serveur serait perdu.
 */
describe('NcService — rapport 8D (API réelle)', () => {

  let service: NcService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;

  const NC = 'ab3f1c22-0000-4000-8000-000000000001';
  const endpoint = `${environment.apiBaseUrl}/api/v1/nc`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    configurer();
    service = TestBed.inject(NcService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    environment.useMockApi = prevMock;
  });

  it('lit le rapport de la non-conformité', (done) => {
    service.getEightDReport(NC).subscribe(rapport => {
      expect(rapport.ncReference).toBe('NC-2026-0007');
      expect(rapport.disciplines.length).toBe(1);
      done();
    });

    const req = httpMock.expectOne(`${endpoint}/${NC}/8d`);
    expect(req.request.method).toBe('GET');
    req.flush({
      ncId: NC, ncReference: 'NC-2026-0007', ncTitle: 'Fuite', status: 'DRAFT',
      issuable: true, partial: true, missingCodes: ['D1'],
      team: null, containment: null, recognition: null,
      disciplines: [{
        code: 'D1', title: 'Équipe', sourced: false, sourceLabel: 'Aucune source',
        lines: [], editable: true
      }],
      seal: null
    });
  });

  it('envoie les trois disciplines saisies en PUT, et aucun tenant', (done) => {
    service.saveEightDReport(NC, { team: 'Ada', containment: 'Tri', recognition: 'Merci' })
      .subscribe(() => done());

    const req = httpMock.expectOne(`${endpoint}/${NC}/8d`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ team: 'Ada', containment: 'Tri', recognition: 'Merci' });
    // §18.2 #2 : le tenant vient du jeton, jamais du corps.
    expect(Object.keys(req.request.body as object)).not.toContain('tenantId');
    req.flush({});
  });

  it('émet le rapport par un POST sans corps utile', (done) => {
    service.issueEightDReport(NC).subscribe(() => done());

    const req = httpMock.expectOne(`${endpoint}/${NC}/8d/issue`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({});
  });

  it('télécharge le PDF en blob, réponse complète pour lire le nom de fichier', (done) => {
    service.downloadEightDPdf(NC).subscribe(reponse => {
      expect(reponse.status).toBe(200);
      expect(reponse.headers.get('Content-Disposition')).toContain('8d-nc-2026-0007');
      done();
    });

    const req = httpMock.expectOne(`${endpoint}/${NC}/8d/pdf`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['%PDF-1.6']), {
      status: 200,
      statusText: 'OK',
      headers: { 'Content-Disposition': 'attachment; filename="8d-nc-2026-0007-20260913-100000.pdf"' }
    });
  });

  it('propage le 409 d\'une émission avant clôture', (done) => {
    service.issueEightDReport(NC).subscribe({
      next: () => done.fail('ne devrait pas réussir'),
      error: err => {
        expect(err.status).toBe(409);
        done();
      }
    });

    httpMock.expectOne(`${endpoint}/${NC}/8d/issue`).flush(
      { type: 'https://qualitos.io/errors/eightd-report-invalid-state' },
      { status: 409, statusText: 'Conflict' });
  });
});

/**
 * Le même rapport en mode maquette : l'écran doit rester utilisable sans serveur,
 * et la maquette doit montrer le cas INTÉRESSANT — des disciplines sans source.
 */
describe('NcService — rapport 8D (mode maquette)', () => {

  let service: NcService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    configurer();
    service = TestBed.inject(NcService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('rend huit disciplines, dont trois à saisir', (done) => {
    service.getEightDReport('nc-1').subscribe(rapport => {
      expect(rapport.disciplines.length).toBe(8);
      expect(rapport.disciplines.filter(d => d.editable).map(d => d.code))
        .toEqual(['D1', 'D3', 'D8']);
      expect(rapport.partial).toBeTrue();
      done();
    });
  });

  it('la saisie rend les trois disciplines servies et recalcule le partiel', (done) => {
    service.saveEightDReport('nc-1', {
      team: 'Ada\nGrace', containment: 'Tri à 100 %', recognition: 'Merci'
    }).subscribe(rapport => {
      const d1 = rapport.disciplines.find(d => d.code === 'D1');
      expect(d1?.sourced).toBeTrue();
      expect(d1?.lines).toEqual(['Ada', 'Grace']);
      expect(rapport.missingCodes).not.toContain('D1');
      done();
    });
  });

  it('l\'émission pose un sceau et ferme la porte', (done) => {
    service.issueEightDReport('nc-2').subscribe(rapport => {
      expect(rapport.status).toBe('ISSUED');
      expect(rapport.issuable).toBeFalse();
      expect(rapport.seal?.sha256Hex.length).toBe(64);
      done();
    });
  });
});
