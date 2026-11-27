import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ObjectivesService } from './objectives.service';
import { CreateObjectiveRequest } from './objectives.types';

describe('ObjectivesService — les objectifs qualité (ADR 0133)', () => {

  let service: ObjectivesService;
  let http: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/v1/quality-objectives`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ObjectivesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge la revue sans filtre : aucun paramètre superflu', () => {
    service.review().subscribe();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().length).toBe(0);
    req.flush({ asOf: null, counters: {}, objectives: [], pendingProposals: [] });
  });

  it('passe le processus et la date de revue en paramètres', () => {
    service.review('p-1', '2026-10-31').subscribe();
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('processId')).toBe('p-1');
    expect(req.request.params.get('asOf')).toBe('2026-10-31');
    req.flush({ asOf: '2026-10-31', counters: {}, objectives: [], pendingProposals: [] });
  });

  /**
   * Une chaîne vide n'est PAS un filtre : « Tous » dans la liste déroulante vaut
   * `''`, et l'envoyer ferait chercher un processus dont l'identifiant est vide.
   */
  it('ignore un filtre vide plutôt que de l\'envoyer', () => {
    service.review('', '').subscribe();
    const req = http.expectOne(base);
    expect(req.request.params.keys().length).toBe(0);
    req.flush({ asOf: null, counters: {}, objectives: [], pendingProposals: [] });
  });

  it('crée un objectif par POST sur la collection', () => {
    const body: CreateObjectiveRequest = {
      title: 'Taux de NC client', kpiId: 'k-1', periodMonths: 1,
      targetValue: 500, thresholdWarning: 600, thresholdCritical: 800,
      warningPeriods: 2, criticalPeriods: 1, recoveryPeriods: 1
    };
    service.create(body).subscribe();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Taux de NC client');
    req.flush({ id: 'o-1' });
  });

  it('modifie par PATCH : les champs omis restent inchangés', () => {
    service.update('o-1', { targetValue: 400 }).subscribe();
    const req = http.expectOne(`${base}/o-1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ targetValue: 400 });
    req.flush({ id: 'o-1' });
  });

  it('archive par DELETE — le serveur conserve, il n\'efface pas', () => {
    service.archive('o-1').subscribe();
    const req = http.expectOne(`${base}/o-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  /**
   * Le 204 du serveur dit « la règle ne déclenche pas ». L'écran doit recevoir
   * `null`, pas un objet vide : il affiche deux messages différents.
   */
  it('rend null quand l\'évaluation ne propose rien (204)', (done) => {
    service.evaluate('o-1').subscribe(p => {
      expect(p).toBeNull();
      done();
    });
    http.expectOne(`${base}/o-1/evaluate`).flush(null, { status: 204, statusText: 'No Content' });
  });

  it('rend la proposition quand la règle déclenche', (done) => {
    service.evaluate('o-1').subscribe(p => {
      expect(p?.level).toBe('CRITICAL');
      done();
    });
    http.expectOne(`${base}/o-1/evaluate`).flush({ id: 'pr-1', level: 'CRITICAL' });
  });

  it('évalue tous les objectifs et rend les propositions nées du passage', (done) => {
    service.evaluateAll().subscribe(list => {
      expect(list.length).toBe(2);
      done();
    });
    const req = http.expectOne(`${base}/evaluate`);
    expect(req.request.method).toBe('POST');
    req.flush([{ id: 'pr-1' }, { id: 'pr-2' }]);
  });

  /**
   * L'action se saisit dans la fenêtre CAPA puis s'envoie sur l'objectif, pas
   * sur la proposition : le bouton existe aussi quand la règle n'a encore rien
   * proposé (ADR 0143).
   */
  it('ouvre la CAPA sur l\'objectif, avec l\'action saisie', (done) => {
    service.openCapa('o-1', {
      title: 'Recaler la presse 3', ownerId: 'u-1', ownerName: 'Awa Diallo',
      dueDate: '2026-12-10'
    }).subscribe(out => {
      expect(out.capaId).toBe('c-9');
      done();
    });
    const req = http.expectOne(`${base}/o-1/capa`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Recaler la presse 3');
    expect(req.request.body.dueDate).toBe('2026-12-10');
    req.flush({ capaId: 'c-9', proposalId: 'pr-1' });
  });

  it('envoie le motif du rejet dans le corps : sans lui le serveur refuse', () => {
    service.reject('pr-1', 'arrêt planifié').subscribe();
    const req = http.expectOne(`${base}/proposals/pr-1/reject`);
    expect(req.request.body).toEqual({ reason: 'arrêt planifié' });
    req.flush({ id: 'pr-1', status: 'REJECTED' });
  });

  it('lit l\'historique des propositions d\'un objectif', () => {
    service.proposals('o-1').subscribe();
    const req = http.expectOne(`${base}/o-1/proposals`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
