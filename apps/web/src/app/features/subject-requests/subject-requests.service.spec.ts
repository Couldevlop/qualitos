import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SubjectRequestsService } from './subject-requests.service';
import { ReceiveSubjectRequest, SubjectRequestView } from './subject-requests.types';

/**
 * Demandes d'exercice de droits (RGPD art. 15 à 22).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue ce qui expose
 * l'organisation en cas de contrôle : le délai d'un mois de l'article 12§3
 * calculé à la réception, son dépassement recalculé À CHAQUE LECTURE plutôt que
 * figé, et la prolongation qui repousse l'échéance. L'identifiant de la personne
 * n'est par ailleurs jamais conservé en clair. Les deux modes sont testés.
 */
describe('SubjectRequestsService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/gdpr/subject-requests`;
  const DAY = 86400000;
  const inDays = (d: number) => new Date(Date.now() + d * DAY).toISOString();

  const receiveReq = (over: Partial<ReceiveSubjectRequest> = {}): ReceiveSubjectRequest => ({
    type: 'ACCESS',
    subjectIdentifier: 'erin@example.fr',
    requestedByUserId: AUTHOR,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: SubjectRequestsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    /** Les réponses simulées sont différées (`delay`) : on déroule le temps virtuel. */
    function run<T>(source: Observable<T>): T {
      let value: T | undefined;
      source.subscribe(v => (value = v));
      tick(300);
      return value as T;
    }

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(SubjectRequestsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les demandes pré-chargées et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).length).toBe(4);

      expect(run(service.list('RECEIVED')).map(r => r.id)).toEqual(['sr-3']);
      expect(run(service.list('REJECTED'))).toEqual([]);
    }));

    it('résout une demande par identifiant, avec repli si inconnue', fakeAsync(() => {
      expect(run(service.get('sr-2')).type).toBe('ERASURE');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('sr-inexistante')).id).toBe('sr-1');
    }));

    it('retrouve les demandes d\'une personne sans jamais stocker son identifiant', fakeAsync(() => {
      const rows = run(service.searchBySubject('alice@example.fr'));

      expect(rows.map(r => r.id)).toEqual(['sr-1']);
      expect(rows[0].subjectIdentifierHash.startsWith('fnv1a:')).toBeTrue();
      expect(JSON.stringify(rows)).not.toContain('alice@example.fr');
      expect(run(service.searchBySubject('inconnu@example.fr'))).toEqual([]);
    }));

    // ---- Délai de l'article 12§3 ---------------------------------------------

    it('fixe l\'échéance à un mois de la réception', fakeAsync(() => {
      const received = run(service.receive(receiveReq()));

      const ecart = new Date(received.deadlineAt).getTime()
        - new Date(received.receivedAt).getTime();
      expect(Math.round(ecart / DAY)).toBe(30);
      expect(received.status).toBe('RECEIVED');
      expect(received.extended).toBeFalse();
      expect(received.overdue).toBeFalse();
    }));

    it('ne remonte en retard que les demandes encore ouvertes', fakeAsync(() => {
      const overdue = run(service.overdue());

      // sr-2 a une échéance dépassée mais est CLÔTURÉE : elle n'est plus en
      // retard, elle est traitée. Seule sr-4 est réellement en souffrance.
      expect(overdue.map(r => r.id)).toEqual(['sr-4']);
    }));

    it('borne le nombre de retards remontés', fakeAsync(() => {
      expect(run(service.overdue(0))).toEqual([]);
    }));

    it('recalcule le retard à chaque lecture plutôt que de le figer', fakeAsync(() => {
      // sr-2 porte `overdue: false` dans le magasin ET une échéance dépassée :
      // c'est le statut qui tranche, pas le drapeau hérité.
      expect(run(service.get('sr-2')).overdue).toBeFalse();
      expect(run(service.get('sr-4')).overdue).toBeTrue();
    }));

    it('sort du retard une demande clôturée', fakeAsync(() => {
      run(service.complete('sr-4', { resolutionNotes: 'Opposition acceptée.' }));

      expect(run(service.overdue())).toEqual([]);
    }));

    it('sort du retard une demande rejetée', fakeAsync(() => {
      run(service.reject('sr-4', { reason: 'Demande manifestement infondée (art. 12§5).' }));

      expect(run(service.overdue())).toEqual([]);
    }));

    // ---- Prolongation ----------------------------------------------------------

    it('repousse l\'échéance et marque la demande comme prolongée', fakeAsync(() => {
      const extended = run(service.extend('sr-4', { newDeadline: inDays(60) }));

      expect(extended.extended).toBeTrue();
      // Article 12§3 : la prolongation de deux mois doit lever le retard, sinon
      // le tableau de bord signale une infraction qui n'existe pas.
      expect(run(service.overdue())).toEqual([]);
    }));

    it('prolonge sans effet de bord quand la demande visée n\'existe pas', fakeAsync(() => {
      run(service.extend('sr-inexistante', { newDeadline: inDays(60) }));

      expect(run(service.get('sr-3')).extended).toBeFalse();
    }));

    // ---- Cycle de vie ------------------------------------------------------------

    it('ouvre l\'instruction et désigne le responsable', fakeAsync(() => {
      const started = run(service.start('sr-3', { handledByUserId: 'juriste-1' }));

      expect(started.status).toBe('IN_PROGRESS');
      expect(started.handledByUserId).toBe('juriste-1');
      expect(started.inProgressAt).toBeTruthy();
    }));

    it('clôt la demande avec sa réponse et sa preuve', fakeAsync(() => {
      const completed = run(service.complete('sr-1', {
        resolutionNotes: 'Copie des données transmise par courrier recommandé.',
        evidenceUrl: 'https://preuve/sr-1.pdf',
        handledByUserId: 'juriste-1'
      }));

      expect(completed.status).toBe('COMPLETED');
      expect(completed.resolutionNotes).toContain('courrier recommandé');
      expect(completed.evidenceUrl).toBe('https://preuve/sr-1.pdf');
      expect(completed.handledByUserId).toBe('juriste-1');
      expect(completed.completedAt).toBeTruthy();
    }));

    it('conserve le responsable déjà désigné si la clôture n\'en fournit pas', fakeAsync(() => {
      const before = run(service.get('sr-1')).handledByUserId;

      const completed = run(service.complete('sr-1', { resolutionNotes: 'Traitée.' }));

      expect(completed.handledByUserId).toBe(before!);
    }));

    it('rejette la demande en conservant le motif', fakeAsync(() => {
      const rejected = run(service.reject('sr-3', {
        reason: 'Identité du demandeur non vérifiable (art. 12§6).',
        handledByUserId: 'juriste-1'
      }));

      expect(rejected.status).toBe('REJECTED');
      expect(rejected.rejectionReason).toContain('12§6');
      // Un rejet est une réponse : il clôt le dossier et doit être daté.
      expect(rejected.completedAt).toBeTruthy();
    }));

    it('laisse le magasin intact quand une transition vise une demande inconnue', fakeAsync(() => {
      run(service.start('sr-inexistante', { handledByUserId: AUTHOR }));
      run(service.complete('sr-inexistante', { resolutionNotes: 'n' }));
      run(service.reject('sr-inexistante', { reason: 'r' }));

      expect(run(service.list()).map(r => r.status))
        .toEqual(['IN_PROGRESS', 'COMPLETED', 'RECEIVED', 'IN_PROGRESS']);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: SubjectRequestsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(SubjectRequestsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('liste sans paramètre, et ajoute le statut quand il est demandé', () => {
      service.list().subscribe();
      const plain = http.expectOne(BASE);
      expect(plain.request.method).toBe('GET');
      expect(plain.request.params.has('status')).toBeFalse();
      plain.flush([]);

      service.list('IN_PROGRESS').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('IN_PROGRESS');
      filtered.flush([]);
    });

    it('interroge la recherche et les retards avec leurs paramètres', () => {
      service.searchBySubject('alice@example.fr').subscribe();
      const search = http.expectOne(r => r.url === `${BASE}/search`);
      expect(search.request.params.get('subjectIdentifier')).toBe('alice@example.fr');
      search.flush([]);

      service.overdue().subscribe();
      const byDefault = http.expectOne(r => r.url === `${BASE}/overdue`);
      expect(byDefault.request.params.get('limit')).toBe('100');
      byDefault.flush([]);

      service.overdue(15).subscribe();
      const bounded = http.expectOne(r => r.url === `${BASE}/overdue`);
      expect(bounded.request.params.get('limit')).toBe('15');
      bounded.flush([]);
    });

    it('lit une fiche par identifiant', () => {
      service.get('r-1').subscribe();

      const req = http.expectOne(`${BASE}/r-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as SubjectRequestView);
    });

    it('enregistre la réception en POST sur la collection', () => {
      const body = receiveReq();

      service.receive(body).subscribe();

      const req = http.expectOne(BASE);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({} as SubjectRequestView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      const complete = { resolutionNotes: 'n', evidenceUrl: 'u' };
      const extend = { newDeadline: '2026-12-31T00:00:00Z' };
      const transitions: Array<[string, () => void, unknown]> = [
        ['start',
          () => service.start('r-1', { handledByUserId: AUTHOR }).subscribe(),
          { handledByUserId: AUTHOR }],
        ['complete', () => service.complete('r-1', complete).subscribe(), complete],
        ['reject', () => service.reject('r-1', { reason: 'r' }).subscribe(), { reason: 'r' }],
        ['extend', () => service.extend('r-1', extend).subscribe(), extend]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/r-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as SubjectRequestView);
      });
    });
  });
});
