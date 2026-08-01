import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { BreachService } from './breach.service';
import { BreachDetectRequest, BreachView } from './breach.types';

/**
 * Violations de données personnelles (RGPD art. 33 et 34).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue les règles que
 * le back applique en production — cycle DETECTED → ASSESSING → CONTAINED →
 * CLOSED, avec REJECTED en sortie anticipée ; délai de 72 h pour la notification
 * à l'autorité de contrôle ; notification aux personnes réservée aux sévérités
 * HIGH/CRITICAL (art. 34, « risque élevé »). Les deux modes sont testés.
 */
describe('BreachService', () => {

  const AUTHOR = '00000000-0000-0000-0000-000000000999';
  const BASE = `${environment.apiBaseUrl}/api/v1/gdpr/breaches`;
  const HOUR = 3600000;

  const detectReq = (over: Partial<BreachDetectRequest> = {}): BreachDetectRequest => ({
    internalReference: 'BR-2026-900',
    title: 'Fuite d\'un export commercial',
    detectedAt: new Date(Date.now() - 2 * HOUR).toISOString(),
    severity: 'MEDIUM',
    affectedSubjectsCount: 12,
    reportedByUserId: AUTHOR,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: BreachService;
    let http: HttpTestingController;
    let prevMock: boolean;

    /** Les réponses simulées sont différées (`delay`) : on déroule le temps virtuel. */
    function run<T>(source: Observable<T>): { value?: T; error?: { status: number } } {
      let value: T | undefined;
      let error: { status: number } | undefined;
      source.subscribe({ next: v => (value = v), error: e => (error = e) });
      tick(300);
      return { value, error };
    }

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(BreachService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les violations pré-chargées et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).value?.length).toBe(3);

      const assessing = run(service.list('ASSESSING')).value ?? [];
      expect(assessing.map(b => b.internalReference)).toEqual(['BR-2026-001']);

      expect(run(service.list('CLOSED')).value).toEqual([]);
    }));

    it('résout une violation par identifiant, et refuse un identifiant inconnu', fakeAsync(() => {
      expect(run(service.get('br-seed-001')).value?.internalReference).toBe('BR-2026-001');
      expect(run(service.get('br-inexistante')).error?.status).toBe(404);
    }));

    it('rend une copie : modifier le résultat ne corrompt pas le magasin', fakeAsync(() => {
      const first = run(service.get('br-seed-001')).value!;
      first.title = 'titre falsifié';

      expect(run(service.get('br-seed-001')).value?.title).toContain('Email contenant');
    }));

    // ---- Délai de 72 h (art. 33) -------------------------------------------

    it('ne signale aucun retard tant que le délai de 72 h court', fakeAsync(() => {
      expect(run(service.dpaOverdue()).value).toEqual([]);
    }));

    it('signale en retard une violation non notifiée dont les 72 h sont écoulées', fakeAsync(() => {
      const late = run(service.detect(detectReq({
        internalReference: 'BR-2026-901',
        detectedAt: new Date(Date.now() - 5 * 24 * HOUR).toISOString()
      }))).value!;

      const overdue = run(service.dpaOverdue()).value ?? [];

      expect(overdue.map(b => b.id)).toEqual([late.id]);
      // Le drapeau est recalculé à la lecture, jamais figé à la création.
      expect(run(service.get(late.id)).value?.dpaOverdue).toBeTrue();
    }));

    it('borne le nombre de retards remontés', fakeAsync(() => {
      run(service.detect(detectReq({
        internalReference: 'BR-2026-902',
        detectedAt: new Date(Date.now() - 5 * 24 * HOUR).toISOString()
      })));

      expect(run(service.dpaOverdue(0)).value).toEqual([]);
    }));

    it('lève le retard dès que l\'autorité est notifiée', fakeAsync(() => {
      const late = run(service.detect(detectReq({
        internalReference: 'BR-2026-903',
        detectedAt: new Date(Date.now() - 5 * 24 * HOUR).toISOString()
      }))).value!;

      run(service.notifyDpa(late.id, {
        notifiedAt: new Date().toISOString(), reference: 'CNIL-NOT-2026-99999'
      }));

      expect(run(service.dpaOverdue()).value).toEqual([]);
    }));

    // ---- Déclaration -------------------------------------------------------

    it('refuse une référence interne déjà utilisée', fakeAsync(() => {
      expect(run(service.detect(detectReq({ internalReference: 'BR-2026-001' }))).error?.status).toBe(409);
    }));

    it('calcule l\'échéance à 72 h de la détection et normalise les champs omis', fakeAsync(() => {
      const detectedAt = new Date(Date.now() - 2 * HOUR).toISOString();

      const created = run(service.detect(detectReq({ detectedAt }))).value!;

      expect(created.status).toBe('DETECTED');
      expect(new Date(created.dpaDeadlineAt!).getTime())
        .toBe(new Date(detectedAt).getTime() + 72 * HOUR);
      expect(created.description).toBeNull();
      expect(created.occurredAt).toBeNull();
      expect(created.riskOfHarmDescription).toBeNull();
      expect(created.affectedDataCategories).toEqual([]);
    }));

    it('déduit de la sévérité si les personnes doivent être informées (art. 34)', fakeAsync(() => {
      const low = run(service.detect(detectReq({
        internalReference: 'BR-LOW', severity: 'LOW'
      }))).value!;
      const critical = run(service.detect(detectReq({
        internalReference: 'BR-CRIT', severity: 'CRITICAL'
      }))).value!;

      expect(low.subjectNotificationRequired).toBeFalse();
      expect(critical.subjectNotificationRequired).toBeTrue();
    }));

    // ---- Instruction -------------------------------------------------------

    it('ne démarre l\'analyse que depuis DETECTED, sur une violation existante', fakeAsync(() => {
      const req = { handledByUserId: AUTHOR };

      expect(run(service.startAssessment('br-inexistante', req)).error?.status).toBe(404);
      expect(run(service.startAssessment('br-seed-001', req)).error?.status).toBe(409);

      const started = run(service.startAssessment('br-seed-003', req)).value!;
      expect(started.status).toBe('ASSESSING');
      expect(started.handledByUserId).toBe(AUTHOR);
    }));

    it('n\'endigue que depuis ASSESSING, sur une violation existante', fakeAsync(() => {
      const req = { containmentMeasures: 'Révocation des accès, purge des boîtes.' };

      expect(run(service.contain('br-inexistante', req)).error?.status).toBe(404);
      expect(run(service.contain('br-seed-003', req)).error?.status).toBe(409);

      const contained = run(service.contain('br-seed-001', req)).value!;
      expect(contained.status).toBe('CONTAINED');
      expect(contained.containmentMeasures).toBe('Révocation des accès, purge des boîtes.');
    }));

    it('ne change le responsable à l\'endiguement que s\'il est fourni', fakeAsync(() => {
      const before = run(service.get('br-seed-001')).value!.handledByUserId;

      const kept = run(service.contain('br-seed-001', { containmentMeasures: 'm' })).value!;
      expect(kept.handledByUserId).toBe(before!);

      run(service.startAssessment('br-seed-003', { handledByUserId: AUTHOR }));
      const reassigned = run(service.contain('br-seed-003', {
        containmentMeasures: 'm', handledByUserId: 'autre-responsable'
      })).value!;
      expect(reassigned.handledByUserId).toBe('autre-responsable');
    }));

    // ---- Notifications -----------------------------------------------------

    it('refuse toute notification sur un incident terminal', fakeAsync(() => {
      run(service.reject('br-seed-003', { reason: 'Fausse alerte.' }));

      expect(run(service.notifyDpa('br-seed-003', {
        notifiedAt: new Date().toISOString(), reference: 'X'
      })).error?.status).toBe(409);
      expect(run(service.notifySubjects('br-seed-003', {
        notifiedAt: new Date().toISOString(), channel: 'email'
      })).error?.status).toBe(409);
      expect(run(service.updateSeverity('br-seed-003', { severity: 'LOW' })).error?.status).toBe(409);
    }));

    it('refuse une notification sur une violation inconnue', fakeAsync(() => {
      const now = new Date().toISOString();

      expect(run(service.notifyDpa('br-inexistante', { notifiedAt: now, reference: 'X' })).error?.status).toBe(404);
      expect(run(service.notifySubjects('br-inexistante', { notifiedAt: now, channel: 'email' })).error?.status).toBe(404);
      expect(run(service.updateSeverity('br-inexistante', { severity: 'LOW' })).error?.status).toBe(404);
    }));

    it('enregistre la notification à l\'autorité avec sa référence', fakeAsync(() => {
      const notifiedAt = new Date().toISOString();

      const done = run(service.notifyDpa('br-seed-001', {
        notifiedAt, reference: 'CNIL-NOT-2026-77777'
      })).value!;

      expect(done.dpaNotifiedAt).toBe(notifiedAt);
      expect(done.dpaReference).toBe('CNIL-NOT-2026-77777');
      expect(done.dpaOverdue).toBeFalse();
    }));

    it('n\'informe les personnes que si le risque l\'exige (art. 34)', fakeAsync(() => {
      const notifiedAt = new Date().toISOString();

      // seed-002 est MEDIUM : pas de risque élevé, donc pas de notification due.
      expect(run(service.notifySubjects('br-seed-002', {
        notifiedAt, channel: 'email'
      })).error?.status).toBe(409);

      const done = run(service.notifySubjects('br-seed-001', {
        notifiedAt, channel: 'courrier recommandé'
      })).value!;
      expect(done.subjectsNotifiedAt).toBe(notifiedAt);
      expect(done.subjectsNotificationChannel).toBe('courrier recommandé');
    }));

    it('réaligne l\'obligation d\'informer quand la sévérité est révisée', fakeAsync(() => {
      const releve = run(service.updateSeverity('br-seed-002', { severity: 'HIGH' })).value!;
      expect(releve.subjectNotificationRequired).toBeTrue();

      const abaisse = run(service.updateSeverity('br-seed-002', { severity: 'LOW' })).value!;
      expect(abaisse.subjectNotificationRequired).toBeFalse();
    }));

    // ---- Sorties -----------------------------------------------------------

    it('ne clôture qu\'une violation endiguée', fakeAsync(() => {
      expect(run(service.close('br-inexistante', {})).error?.status).toBe(404);
      expect(run(service.close('br-seed-001', {})).error?.status).toBe(409);

      const closed = run(service.close('br-seed-002', { closureNotes: 'Aucun impact avéré.' })).value!;
      expect(closed.status).toBe('CLOSED');
      expect(closed.closureNotes).toBe('Aucun impact avéré.');
      expect(closed.closedAt).not.toBeNull();
    }));

    it('accepte une clôture sans note et la stocke à null', fakeAsync(() => {
      expect(run(service.close('br-seed-002', {})).value?.closureNotes).toBeNull();
    }));

    it('ne rejette qu\'une violation encore en instruction', fakeAsync(() => {
      expect(run(service.reject('br-inexistante', { reason: 'r' })).error?.status).toBe(404);
      expect(run(service.reject('br-seed-002', { reason: 'r' })).error?.status).toBe(409);

      const rejected = run(service.reject('br-seed-001', { reason: 'Fausse alerte.' })).value!;
      expect(rejected.status).toBe('REJECTED');
      expect(rejected.rejectionReason).toBe('Fausse alerte.');
      expect(rejected.closedAt).not.toBeNull();
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: BreachService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(BreachService);
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

      service.list('CONTAINED').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('CONTAINED');
      filtered.flush([]);
    });

    it('demande les retards avec la borne par défaut, ou celle fournie', () => {
      service.dpaOverdue().subscribe();
      const byDefault = http.expectOne(r => r.url === `${BASE}/dpa-overdue`);
      expect(byDefault.request.params.get('limit')).toBe('100');
      byDefault.flush([]);

      service.dpaOverdue(5).subscribe();
      const bounded = http.expectOne(r => r.url === `${BASE}/dpa-overdue`);
      expect(bounded.request.params.get('limit')).toBe('5');
      bounded.flush([]);
    });

    it('lit une fiche par identifiant', () => {
      service.get('b-1').subscribe();

      const req = http.expectOne(`${BASE}/b-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as BreachView);
    });

    it('déclare la violation en POST sur la collection', () => {
      const body = detectReq();

      service.detect(body).subscribe();

      const req = http.expectOne(BASE);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({} as BreachView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      const at = '2026-07-01T10:00:00Z';
      const transitions: Array<[string, () => void, unknown]> = [
        ['start-assessment',
          () => service.startAssessment('b-1', { handledByUserId: AUTHOR }).subscribe(),
          { handledByUserId: AUTHOR }],
        ['contain',
          () => service.contain('b-1', { containmentMeasures: 'm' }).subscribe(),
          { containmentMeasures: 'm' }],
        ['notify-dpa',
          () => service.notifyDpa('b-1', { notifiedAt: at, reference: 'C-1' }).subscribe(),
          { notifiedAt: at, reference: 'C-1' }],
        ['notify-subjects',
          () => service.notifySubjects('b-1', { notifiedAt: at, channel: 'email' }).subscribe(),
          { notifiedAt: at, channel: 'email' }],
        ['close',
          () => service.close('b-1', { closureNotes: 'n' }).subscribe(),
          { closureNotes: 'n' }],
        ['reject',
          () => service.reject('b-1', { reason: 'r' }).subscribe(),
          { reason: 'r' }],
        ['severity',
          () => service.updateSeverity('b-1', { severity: 'HIGH' }).subscribe(),
          { severity: 'HIGH' }]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/b-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as BreachView);
      });
    });
  });
});
