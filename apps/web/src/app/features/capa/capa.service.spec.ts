import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { CapaService } from './capa.service';
import { CapaCaseResponse } from './capa.types';

describe('CapaService (mock mode)', () => {
  let service: CapaService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(CapaService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded cases', (done) => {
    service.listCases().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      expect(page.totalElements).toBe(page.content.length);
      done();
    });
  });

  it('filters cases by status', (done) => {
    service.listCases(0, 50, 'OPEN').subscribe(page => {
      expect(page.content.every(c => c.status === 'OPEN')).toBeTrue();
      done();
    });
  });

  it('returns a case by id', (done) => {
    service.getCase('capa-1').subscribe(c => {
      expect(c.id).toBe('capa-1');
      done();
    });
  });

  it('creates a case in OPEN status', (done) => {
    service.createCase({
      title: 'Nouvelle action', type: 'CORRECTIVE', criticity: 'LOW', sourceType: 'INTERNAL', ownerId: 'u'
    }).subscribe(c => {
      expect(c.status).toBe('OPEN');
      expect(c.title).toBe('Nouvelle action');
      expect(c.actions).toEqual([]);
      done();
    });
  });

  it('adds an action to a case', (done) => {
    service.addAction('capa-1', { title: 'Recalibrer' }).subscribe(a => {
      expect(a.capaId).toBe('capa-1');
      expect(a.status).toBe('PENDING');
      done();
    });
  });

  it('updateAction advances an action to DONE and stamps completedAt (ANO-011)', (done) => {
    service.addAction('capa-1', { title: 'Recalibrer' }).subscribe(a => {
      service.updateAction('capa-1', a.id, { title: a.title, status: 'DONE' }).subscribe(updated => {
        expect(updated.status).toBe('DONE');
        expect(updated.completedAt).toBeTruthy();
        done();
      });
    });
  });

  it('suggests AI actions', (done) => {
    service.suggestActions('capa-1').subscribe(actions => {
      expect(actions.length).toBeGreaterThan(0);
      expect(actions[0].title).toBeTruthy();
      done();
    });
  });

  it('verifyEffectiveness closes the case when effective', (done) => {
    service.verifyEffectiveness('capa-1', true).subscribe(c => {
      expect(c.effectivenessVerified).toBeTrue();
      expect(c.status).toBe('CLOSED');
      done();
    });
  });

  it('startCase transitions to IN_PROGRESS', (done) => {
    service.startCase('capa-2').subscribe(c => {
      expect(c.status).toBe('IN_PROGRESS');
      done();
    });
  });

  it('ne modifie que les champs fournis lors d\'une mise à jour partielle', (done) => {
    service.updateCase('capa-1', { criticity: 'LOW', sourceRef: 'NC-2026-999' }).subscribe(c => {
      expect(c.criticity).toBe('LOW');
      expect(c.sourceRef).toBe('NC-2026-999');
      // Le titre n'était pas dans la requête : il doit rester intact.
      expect(c.title).toBe('Recalibration robot soudure cobot-3');
      done();
    });
  });

  it('applique titre, description et échéance quand ils sont fournis', (done) => {
    service.updateCase('capa-2', {
      title: 'Titre revu', description: 'Contexte étendu', dueDate: '2026-12-31'
    }).subscribe(c => {
      expect(c.title).toBe('Titre revu');
      expect(c.description).toBe('Contexte étendu');
      expect(c.dueDate).toBe('2026-12-31');
      done();
    });
  });

  it('retombe sur le premier cas de démonstration quand l\'identifiant est inconnu', (done) => {
    service.updateCase('inexistant', { title: 'ignoré' }).subscribe(c => {
      expect(c.id).toBe('capa-1');
      expect(c.title).toBe('Recalibration robot soudure cobot-3');
      done();
    });
  });

  it('supprime un cas et le retire de la liste', (done) => {
    service.deleteCase('capa-3').subscribe(() => {
      service.listCases().subscribe(page => {
        expect(page.content.some(c => c.id === 'capa-3')).toBeFalse();
        done();
      });
    });
  });

  it('ignore la suppression d\'un identifiant inconnu sans casser la liste', (done) => {
    service.deleteCase('inexistant').subscribe(() => {
      service.listCases().subscribe(page => {
        expect(page.content.length).toBe(3);
        done();
      });
    });
  });

  it('resolveCase horodate la résolution', (done) => {
    service.resolveCase('capa-2').subscribe(c => {
      expect(c.status).toBe('RESOLVED');
      expect(c.resolvedAt).toBeTruthy();
      done();
    });
  });

  it('rejectCase passe en REJECTED sans horodater de résolution', (done) => {
    service.rejectCase('capa-2').subscribe(c => {
      expect(c.status).toBe('REJECTED');
      expect(c.resolvedAt).toBeUndefined();
      done();
    });
  });

  it('une transition sur un identifiant inconnu ne corrompt aucun cas', (done) => {
    service.startCase('inexistant').subscribe(c => {
      expect(c.id).toBe('capa-1');
      // Le cas de repli garde son statut : rien n'a été écrit à tort.
      expect(c.status).toBe('IN_PROGRESS');
      done();
    });
  });

  it('verifyEffectiveness négatif conserve le cas en l\'état sans le clôturer', (done) => {
    service.resolveCase('capa-2').subscribe(() => {
      service.verifyEffectiveness('capa-2', false).subscribe(c => {
        expect(c.effectivenessVerified).toBeFalse();
        expect(c.status).toBe('RESOLVED');
        expect(c.closedAt).toBeUndefined();
        done();
      });
    });
  });

  it('verifyEffectiveness sur un identifiant inconnu retombe sur le cas de repli', (done) => {
    service.verifyEffectiveness('inexistant', true).subscribe(c => {
      expect(c.id).toBe('capa-1');
      done();
    });
  });

  it('updateAction sur une action inconnue renvoie une action synthétique sans planter', (done) => {
    service.updateAction('capa-1', 'act-inexistante', { title: 'Recalibrer' }).subscribe(a => {
      expect(a.id).toBe('act-inexistante');
      expect(a.status).toBe('PENDING');
      done();
    });
  });
});

/**
 * Mode API réelle : c'est le mode de production (`useMockApi=false` par défaut).
 * On y vérifie le contrat HTTP — verbe, URL, corps — puisque la moindre dérive
 * casse silencieusement l'écran sans qu'aucun test de mode mock ne le voie.
 */
describe('CapaService (API réelle)', () => {
  let service: CapaService;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/capa/cases`;

  const aCase = (over: Partial<CapaCaseResponse> = {}): CapaCaseResponse => ({
    id: 'c1', tenantId: 't1', title: 'Cas', type: 'CORRECTIVE', criticity: 'HIGH',
    status: 'OPEN', sourceType: 'AUDIT', ownerId: 'u1',
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
    actions: [], ...over
  });

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(CapaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('pagine la liste et omet le filtre de statut quand aucun n\'est choisi', (done) => {
    service.listCases(2, 25).subscribe(page => {
      expect(page.totalElements).toBe(0);
      done();
    });
    const req = http.expectOne(r => r.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('25');
    expect(req.request.params.has('status')).toBeFalse();
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 2, size: 25 });
  });

  it('transmet le filtre de statut au serveur', () => {
    service.listCases(0, 50, 'RESOLVED').subscribe();
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('status')).toBe('RESOLVED');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 50 });
  });

  it('lit un cas par son identifiant', (done) => {
    service.getCase('c1').subscribe(c => {
      expect(c.id).toBe('c1');
      done();
    });
    const req = http.expectOne(`${base}/c1`);
    expect(req.request.method).toBe('GET');
    req.flush(aCase());
  });

  it('crée un cas en POST sur la collection', () => {
    service.createCase({
      title: 'Nouveau', type: 'PREVENTIVE', criticity: 'LOW',
      sourceType: 'INTERNAL', ownerId: 'u1'
    }).subscribe();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Nouveau');
    expect(req.request.body.type).toBe('PREVENTIVE');
    req.flush(aCase());
  });

  it('met à jour un cas en PATCH (mise à jour partielle)', () => {
    service.updateCase('c1', { criticity: 'CRITICAL' }).subscribe();
    const req = http.expectOne(`${base}/c1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ criticity: 'CRITICAL' });
    req.flush(aCase({ criticity: 'CRITICAL' }));
  });

  it('supprime un cas en DELETE', (done) => {
    service.deleteCase('c1').subscribe(() => done());
    const req = http.expectOne(`${base}/c1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('ajoute une action sous la ressource du cas', () => {
    service.addAction('c1', { title: 'Recalibrer' }).subscribe();
    const req = http.expectOne(`${base}/c1/actions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 'Recalibrer' });
    req.flush({ id: 'a1', capaId: 'c1', title: 'Recalibrer', status: 'PENDING' });
  });

  it('met à jour une action en PATCH avec les seuls champs fournis', () => {
    service.updateAction('c1', 'a1', { title: 'Recalibrer', status: 'DONE' }).subscribe();
    const req = http.expectOne(`${base}/c1/actions/a1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ title: 'Recalibrer', status: 'DONE' });
    req.flush({ id: 'a1', capaId: 'c1', title: 'Recalibrer', status: 'DONE' });
  });

  it('avance un statut sans renvoyer le libellé (PATCH partiel)', () => {
    // Renvoyer le libellé à l'identique écraserait une correction faite
    // entre-temps par quelqu'un d'autre.
    service.updateAction('c1', 'a1', { status: 'IN_PROGRESS' }).subscribe();
    const req = http.expectOne(`${base}/c1/actions/a1`);
    expect(req.request.body).toEqual({ status: 'IN_PROGRESS' });
    req.flush({ id: 'a1', capaId: 'c1', title: 'Recalibrer', status: 'IN_PROGRESS' });
  });

  it('transmet la date de décision et le nom du porteur à la création', () => {
    service.addAction('c1', {
      title: 'Réviser le plan de contrôle',
      decidedOn: '2026-03-12',
      assigneeName: 'Amina Dridi'
    }).subscribe();
    const req = http.expectOne(`${base}/c1/actions`);
    expect(req.request.body).toEqual({
      title: 'Réviser le plan de contrôle',
      decidedOn: '2026-03-12',
      assigneeName: 'Amina Dridi'
    });
    req.flush({ id: 'a1', capaId: 'c1', title: 'Réviser le plan de contrôle', status: 'PENDING' });
  });

  it('demande les suggestions IA en POST avec un corps vide', (done) => {
    service.suggestActions('c1').subscribe(list => {
      expect(list.length).toBe(1);
      done();
    });
    const req = http.expectOne(`${base}/c1/suggest-actions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush([{ title: 'Auditer le fournisseur' }]);
  });

  it('remonte l\'indisponibilité du service IA à l\'appelant', (done) => {
    service.suggestActions('c1').subscribe({
      next: () => fail('un 503 ne doit pas être avalé par le service'),
      error: err => {
        expect(err.status).toBe(503);
        done();
      }
    });
    http.expectOne(`${base}/c1/suggest-actions`)
      .flush({ title: 'unavailable' }, { status: 503, statusText: 'Service Unavailable' });
  });

  it('vérifie l\'efficacité en PATCH sur la sous-ressource dédiée', () => {
    service.verifyEffectiveness('c1', false).subscribe();
    const req = http.expectOne(`${base}/c1/effectiveness`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ effective: false });
    req.flush(aCase({ effectivenessVerified: false }));
  });

  it('démarre un cas via le segment /start en PATCH', (done) => {
    service.startCase('c1').subscribe(c => {
      expect(c.status).toBe('IN_PROGRESS');
      done();
    });
    const req = http.expectOne(`${base}/c1/start`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({});
    req.flush(aCase({ status: 'IN_PROGRESS' }));
  });

  it('résout et rejette via des segments distincts', () => {
    const seen: string[] = [];
    service.resolveCase('c1').subscribe(c => seen.push(c.status));
    http.expectOne(`${base}/c1/resolve`).flush(aCase({ status: 'RESOLVED' }));

    service.rejectCase('c1').subscribe(c => seen.push(c.status));
    http.expectOne(`${base}/c1/reject`).flush(aCase({ status: 'REJECTED' }));

    expect(seen).toEqual(['RESOLVED', 'REJECTED']);
  });

  it('propage un 409 sur une transition interdite au lieu de l\'ignorer', (done) => {
    service.resolveCase('c1').subscribe({
      next: () => fail('une transition refusée ne doit pas produire de valeur'),
      error: err => {
        expect(err.status).toBe(409);
        done();
      }
    });
    http.expectOne(`${base}/c1/resolve`)
      .flush({ title: 'illegal state' }, { status: 409, statusText: 'Conflict' });
  });
});

/**
 * Preuves jointes au dossier (§4.2, ISO 9001 §10.2).
 *
 * <p>Le service ne porte aucune des bornes — elles sont tenues par le serveur, qui
 * seul connaît ce que le dossier contient déjà. Ce qui se teste ici est le
 * contrat : la bonne route, le bon verbe, et un fichier envoyé en multipart sous
 * le champ attendu.
 */
describe('CapaService — preuves du dossier', () => {
  let service: CapaService;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/capa/cases`;

  const fichier = () => new File(['%PDF-1.7 relevé'], 'releve.pdf', { type: 'application/pdf' });

  describe('en mode connecté', () => {
    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(CapaService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('liste les preuves du dossier', () => {
      service.listEvidences('c1').subscribe();

      const req = http.expectOne(`${base}/c1/evidences`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('dépose la pièce en multipart, sous le champ « file »', () => {
      service.uploadEvidence('c1', fichier()).subscribe();

      const req = http.expectOne(`${base}/c1/evidences`);
      expect(req.request.method).toBe('POST');
      const body = req.request.body as FormData;
      expect(body instanceof FormData).toBeTrue();
      expect((body.get('file') as File).name).toBe('releve.pdf');
      req.flush({});
    });

    it('retire une pièce par son identifiant', () => {
      service.deleteEvidence('c1', 'evd-9').subscribe();

      const req = http.expectOne(`${base}/c1/evidences/evd-9`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('propage le refus du serveur au lieu de l\'absorber', (done) => {
      // 409 = borne atteinte ou dossier clos. L'écran doit pouvoir le distinguer
      // d'un 413 ou d'un 400 : un service qui aplatit les refus lui retire ce moyen.
      service.uploadEvidence('c1', fichier()).subscribe({
        next: () => fail('un refus ne doit pas produire de valeur'),
        error: err => {
          expect(err.status).toBe(409);
          done();
        }
      });
      http.expectOne(`${base}/c1/evidences`)
        .flush({ title: 'limit reached' }, { status: 409, statusText: 'Conflict' });
    });
  });

  describe('en mode démonstration', () => {
    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(CapaService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démonstration ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    it('conserve la pièce dans l\'onglet, réellement consultable', (done) => {
      service.uploadEvidence('capa-1', fichier()).subscribe(e => {
        expect(e.originalFilename).toBe('releve.pdf');
        // L'URL d'objet rend le fichier consultable plutôt que de simuler une preuve.
        expect(e.url).toContain('blob:');
        service.listEvidences('capa-1').subscribe(list => {
          expect(list.map(x => x.id)).toEqual([e.id]);
          service.deleteEvidence('capa-1', e.id).subscribe(() => {
            service.listEvidences('capa-1').subscribe(apres => {
              expect(apres).toEqual([]);
              done();
            });
          });
        });
      });
    });

    it('isole les pièces par dossier', (done) => {
      service.uploadEvidence('capa-1', fichier()).subscribe(() => {
        service.listEvidences('capa-2').subscribe(list => {
          expect(list).toEqual([]);
          done();
        });
      });
    });
  });
});

/**
 * Preuves rattachées à UNE action (§4.2, ADR 0052).
 *
 * Mêmes bornes et mêmes refus que les preuves de dossier ; ce qui change, c'est
 * le chemin — et le fait qu'une action ne porte qu'une pièce. Ce qui se teste
 * ici, c'est que les deux niveaux ne se mélangent jamais : une pièce d'action
 * ne doit pas remonter dans le bloc « Preuves » du dossier, et inversement.
 */
describe('CapaService — preuves d\'action', () => {
  let service: CapaService;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/capa/cases`;
  const fichier = () => new File(['%PDF-1.7 constat'], 'constat.pdf', { type: 'application/pdf' });

  describe('en mode connecté', () => {
    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(CapaService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('lit toutes les pièces d\'actions du dossier en un appel', () => {
      service.listActionEvidences('c1').subscribe();

      // Une requête par ligne ferait autant d'aller-retours que d'actions.
      const req = http.expectOne(`${base}/c1/action-evidences`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('dépose la pièce sous la ressource de l\'action, en multipart', () => {
      service.uploadActionEvidence('c1', 'a1', fichier()).subscribe();

      const req = http.expectOne(`${base}/c1/actions/a1/evidences`);
      expect(req.request.method).toBe('POST');
      const body = req.request.body as FormData;
      expect(body instanceof FormData).toBeTrue();
      expect((body.get('file') as File).name).toBe('constat.pdf');
      req.flush({});
    });

    it('retire la pièce par le chemin de son action', () => {
      service.deleteActionEvidence('c1', 'a1', 'evd-9').subscribe();

      const req = http.expectOne(`${base}/c1/actions/a1/evidences/evd-9`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('propage le refus du serveur au lieu de l\'absorber', (done) => {
      service.uploadActionEvidence('c1', 'a1', fichier()).subscribe({
        error: err => {
          expect(err.status).toBe(409);
          done();
        }
      });
      http.expectOne(`${base}/c1/actions/a1/evidences`)
        .flush({ title: 'Invalid CAPA State' }, { status: 409, statusText: 'Conflict' });
    });
  });

  describe('en mode démonstration', () => {
    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(CapaService);
    });

    afterEach(() => { environment.useMockApi = prevMock; });

    it('ne mélange pas les pièces d\'actions avec celles du dossier', (done) => {
      service.uploadActionEvidence('capa-1', 'a1', fichier()).subscribe(e => {
        expect(e.actionId).toBe('a1');
        service.listActionEvidences('capa-1').subscribe(actions => {
          expect(actions.map(x => x.id)).toEqual([e.id]);
          // Le bloc « Preuves » du dossier reste vide : c'est le défaut exact
          // que le filtre serveur existe pour éviter.
          service.listEvidences('capa-1').subscribe(dossier => {
            expect(dossier).toEqual([]);
            done();
          });
        });
      });
    });

    it('retire la pièce d\'une action et la libère de la mémoire', (done) => {
      service.uploadActionEvidence('capa-1', 'a1', fichier()).subscribe(e => {
        expect(e.url).toContain('blob:');
        service.deleteActionEvidence('capa-1', 'a1', e.id).subscribe(() => {
          service.listActionEvidences('capa-1').subscribe(apres => {
            expect(apres).toEqual([]);
            done();
          });
        });
      });
    });

    it('isole les pièces d\'actions par dossier', (done) => {
      service.uploadActionEvidence('capa-1', 'a1', fichier()).subscribe(() => {
        service.listActionEvidences('capa-2').subscribe(list => {
          expect(list).toEqual([]);
          done();
        });
      });
    });

    it('déduit explicitement la date de décision à la création d\'une action', (done) => {
      service.addAction('capa-1', { title: 'Réviser le plan de contrôle' }).subscribe(a => {
        // Déduction assumée, jamais un champ technique renommé : la date est
        // celle du jour, et reste corrigeable.
        expect(a.decidedOn).toBe(new Date().toISOString().slice(0, 10));
        done();
      });
    });
  });
});
