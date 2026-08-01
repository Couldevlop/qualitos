import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PmmService } from './pmm.service';
import { FREQUENCY_DAYS, PmmDraftRequest, PmmEditRequest, PmmPlanView } from './pmm.types';

/**
 * Surveillance après commercialisation des systèmes d'IA (AI Act, art. 72).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * DRAFT → ACTIVE ⇄ SUSPENDED → CLOSED et le mécanisme qui donne sa valeur au
 * module : chaque revue enregistrée reprogramme la suivante selon la fréquence
 * du plan, et un plan actif dont l'échéance est passée ressort en revue en
 * retard. Les deux modes sont testés.
 */
describe('PmmService', () => {

  const AUTHOR = '00000000-0000-0000-0000-000000000999';
  const BASE = `${environment.apiBaseUrl}/api/v1/ai-act/pmm`;
  const DAY = 86400000;

  const draftReq = (over: Partial<PmmDraftRequest> = {}): PmmDraftRequest => ({
    reference: 'PMM-2026-NEW-900',
    aiSystemId: '44444444-4444-4444-4444-444444444444',
    name: 'Surveillance post-marché — Détection de défauts en atelier',
    reviewFrequency: 'MONTHLY',
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<PmmEditRequest> = {}): PmmEditRequest => ({
    name: 'Surveillance post-marché — Aide diagnostic radiologique (v2)',
    reviewFrequency: 'QUARTERLY',
    ...over
  });

  // ------------------------------------------------------------------------
  // Barème des fréquences
  // ------------------------------------------------------------------------
  describe('fréquences de revue', () => {
    it('traduit chaque fréquence en nombre de jours', () => {
      expect(FREQUENCY_DAYS.WEEKLY).toBe(7);
      expect(FREQUENCY_DAYS.MONTHLY).toBe(30);
      expect(FREQUENCY_DAYS.QUARTERLY).toBe(90);
      expect(FREQUENCY_DAYS.SEMI_ANNUAL).toBe(182);
      expect(FREQUENCY_DAYS.ANNUAL).toBe(365);
    });
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: PmmService;
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
      service = TestBed.inject(PmmService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les plans pré-chargés et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).value?.length).toBe(3);

      expect(run(service.list('DRAFT')).value?.map(p => p.id)).toEqual(['pmm-seed-003']);
      expect(run(service.list('CLOSED')).value).toEqual([]);
    }));

    it('filtre par système d\'IA surveillé', fakeAsync(() => {
      expect(run(service.listByAiSystem('22222222-2222-2222-2222-222222222222')).value?.map(p => p.reference))
        .toEqual(['PMM-2026-HRGD-002']);
      expect(run(service.listByAiSystem('système-inconnu')).value).toEqual([]);
    }));

    it('résout un plan par identifiant et par référence, et refuse une clé inconnue', fakeAsync(() => {
      expect(run(service.get('pmm-seed-002')).value?.reference).toBe('PMM-2026-HRGD-002');
      expect(run(service.getByReference('PMM-2026-DIAG-001')).value?.id).toBe('pmm-seed-001');

      expect(run(service.get('pmm-inexistant')).error?.status).toBe(404);
      expect(run(service.getByReference('PMM-ABSENTE')).error?.status).toBe(404);
    }));

    it('rend une copie : modifier le résultat ne corrompt pas le magasin', fakeAsync(() => {
      const first = run(service.get('pmm-seed-001')).value!;
      first.name = 'nom falsifié';

      expect(run(service.get('pmm-seed-001')).value?.name).toContain('Aide diagnostic');
    }));

    // ---- Revues en retard --------------------------------------------------

    it('ne remonte en retard que les plans actifs dont l\'échéance est passée', fakeAsync(() => {
      // seed-001 est actif mais à échéance future ; seed-003 est encore un brouillon.
      expect(run(service.overdueReviews()).value?.map(p => p.id)).toEqual(['pmm-seed-002']);
    }));

    it('borne le nombre de retards remontés', fakeAsync(() => {
      expect(run(service.overdueReviews(0)).value).toEqual([]);
    }));

    it('sort un plan de la liste des retards dès qu\'il est suspendu', fakeAsync(() => {
      run(service.suspend('pmm-seed-002', { reason: 'Système gelé.' }));

      expect(run(service.overdueReviews()).value).toEqual([]);
    }));

    // ---- Création ----------------------------------------------------------

    it('refuse une référence déjà utilisée', fakeAsync(() => {
      expect(run(service.draft(draftReq({ reference: 'PMM-2026-DIAG-001' }))).error?.status).toBe(409);
    }));

    it('crée un brouillon en tête de liste, sans échéance de revue', fakeAsync(() => {
      const created = run(service.draft(draftReq())).value!;

      expect(created.status).toBe('DRAFT');
      expect(created.activatedAt).toBeNull();
      expect(created.nextReviewDueAt).toBeNull();
      expect(run(service.list()).value?.[0].reference).toBe('PMM-2026-NEW-900');
    }));

    it('normalise en null les champs facultatifs omis', fakeAsync(() => {
      const created = run(service.draft(draftReq())).value!;

      expect(created.description).toBeNull();
      expect(created.metricsMonitored).toBeNull();
      expect(created.collectionMethod).toBeNull();
      expect(created.responsiblePartyDescription).toBeNull();
      expect(created.triggerCriteria).toBeNull();
      expect(created.qmsLinkReference).toBeNull();
    }));

    // ---- Édition -----------------------------------------------------------

    it('n\'édite pas un plan clos, ni un plan inconnu', fakeAsync(() => {
      expect(run(service.edit('pmm-inexistant', editReq())).error?.status).toBe(404);

      run(service.close('pmm-seed-003', { reason: 'Projet abandonné.' }));
      expect(run(service.edit('pmm-seed-003', editReq())).error?.status).toBe(409);
    }));

    it('remplace les champs du plan, une omission valant effacement', fakeAsync(() => {
      const edited = run(service.edit('pmm-seed-001', editReq({
        metricsMonitored: 'Sensibilité, spécificité.'
      }))).value!;

      expect(edited.name).toContain('(v2)');
      expect(edited.metricsMonitored).toBe('Sensibilité, spécificité.');
      // L'édition porte l'état complet du formulaire.
      expect(edited.triggerCriteria).toBeNull();
    }));

    it('reprogramme l\'échéance quand la fréquence change sur un plan déjà revu', fakeAsync(() => {
      const before = run(service.get('pmm-seed-001')).value!;

      const edited = run(service.edit('pmm-seed-001', editReq({ reviewFrequency: 'WEEKLY' }))).value!;

      expect(new Date(edited.nextReviewDueAt!).getTime())
        .toBe(new Date(before.lastReviewedAt!).getTime() + 7 * DAY);
    }));

    it('ne programme pas d\'échéance sur un plan jamais revu', fakeAsync(() => {
      expect(run(service.edit('pmm-seed-003', editReq())).value?.nextReviewDueAt).toBeNull();
    }));

    // ---- Activation --------------------------------------------------------

    it('n\'active qu\'un plan en brouillon ou suspendu, et existant', fakeAsync(() => {
      expect(run(service.activate('pmm-inexistant')).error?.status).toBe(404);
      expect(run(service.activate('pmm-seed-001')).error?.status).toBe(409);
    }));

    it('date la mise en service à la première activation', fakeAsync(() => {
      const activated = run(service.activate('pmm-seed-003')).value!;

      expect(activated.status).toBe('ACTIVE');
      expect(activated.activatedAt).not.toBeNull();
    }));

    it('conserve la date de mise en service lors d\'une reprise après suspension', fakeAsync(() => {
      const first = run(service.activate('pmm-seed-003')).value!;
      run(service.suspend('pmm-seed-003', { reason: 'Système gelé.' }));

      const resumed = run(service.activate('pmm-seed-003')).value!;

      expect(resumed.activatedAt).toBe(first.activatedAt!);
      expect(resumed.suspendedAt).toBeNull();
      expect(resumed.suspensionReason).toBeNull();
    }));

    // ---- Revue -------------------------------------------------------------

    it('n\'enregistre une revue que sur un plan actif et existant', fakeAsync(() => {
      const req = { reviewedByUserId: AUTHOR };

      expect(run(service.recordReview('pmm-inexistant', req)).error?.status).toBe(404);
      expect(run(service.recordReview('pmm-seed-003', req)).error?.status).toBe(409);
    }));

    it('reprogramme la revue suivante selon la fréquence du plan', fakeAsync(() => {
      const reviewed = run(service.recordReview('pmm-seed-001', { reviewedByUserId: AUTHOR })).value!;

      expect(reviewed.lastReviewedByUserId).toBe(AUTHOR);
      expect(new Date(reviewed.nextReviewDueAt!).getTime())
        .toBe(new Date(reviewed.lastReviewedAt!).getTime() + FREQUENCY_DAYS.QUARTERLY * DAY);
    }));

    it('sort le plan des retards une fois la revue enregistrée', fakeAsync(() => {
      run(service.recordReview('pmm-seed-002', { reviewedByUserId: AUTHOR }));

      expect(run(service.overdueReviews()).value).toEqual([]);
    }));

    // ---- Suspension / clôture ----------------------------------------------

    it('ne suspend qu\'un plan actif et existant', fakeAsync(() => {
      const req = { reason: 'Système retiré temporairement.' };

      expect(run(service.suspend('pmm-inexistant', req)).error?.status).toBe(404);
      expect(run(service.suspend('pmm-seed-003', req)).error?.status).toBe(409);

      const suspended = run(service.suspend('pmm-seed-001', req)).value!;
      expect(suspended.status).toBe('SUSPENDED');
      expect(suspended.suspensionReason).toBe('Système retiré temporairement.');
      expect(suspended.suspendedAt).not.toBeNull();
    }));

    it('clôt un plan et date sa fin d\'effet, sans clôture en double', fakeAsync(() => {
      const req = { reason: 'Système décommissionné.' };

      expect(run(service.close('pmm-inexistant', req)).error?.status).toBe(404);

      const closed = run(service.close('pmm-seed-001', req)).value!;
      expect(closed.status).toBe('CLOSED');
      expect(closed.closureReason).toBe('Système décommissionné.');
      expect(closed.effectiveTo).not.toBeNull();

      expect(run(service.close('pmm-seed-001', req)).error?.status).toBe(409);
    }));

    // ---- Suppression -------------------------------------------------------

    it('supprime un plan et refuse une suppression inconnue', fakeAsync(() => {
      expect(run(service.delete('pmm-inexistant')).error?.status).toBe(404);

      run(service.delete('pmm-seed-003'));

      expect(run(service.list()).value?.length).toBe(2);
      expect(run(service.get('pmm-seed-003')).error?.status).toBe(404);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: PmmService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(PmmService);
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

      service.list('ACTIVE').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('ACTIVE');
      filtered.flush([]);
    });

    it('interroge les vues dérivées avec leurs paramètres', () => {
      service.listByAiSystem('sys-1').subscribe();
      const bySystem = http.expectOne(r => r.url === `${BASE}/by-system`);
      expect(bySystem.request.params.get('aiSystemId')).toBe('sys-1');
      bySystem.flush([]);

      service.getByReference('PMM-2026-DIAG-001').subscribe();
      const byRef = http.expectOne(r => r.url === `${BASE}/by-reference`);
      expect(byRef.request.params.get('reference')).toBe('PMM-2026-DIAG-001');
      byRef.flush({} as PmmPlanView);

      service.overdueReviews().subscribe();
      const byDefault = http.expectOne(r => r.url === `${BASE}/overdue-reviews`);
      expect(byDefault.request.params.get('limit')).toBe('200');
      byDefault.flush([]);

      service.overdueReviews(7).subscribe();
      const bounded = http.expectOne(r => r.url === `${BASE}/overdue-reviews`);
      expect(bounded.request.params.get('limit')).toBe('7');
      bounded.flush([]);
    });

    it('lit une fiche par identifiant', () => {
      service.get('p-1').subscribe();

      const req = http.expectOne(`${BASE}/p-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as PmmPlanView);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = draftReq();
      service.draft(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as PmmPlanView);

      const edit = editReq();
      service.edit('p-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/p-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as PmmPlanView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      const transitions: Array<[string, () => void, unknown]> = [
        // L'activation n'a pas de corps métier : on poste un objet vide plutôt
        // que `null`, un POST sans corps déclenchant des 415 sur certains serveurs.
        ['activate', () => service.activate('p-1').subscribe(), {}],
        ['record-review',
          () => service.recordReview('p-1', { reviewedByUserId: AUTHOR }).subscribe(),
          { reviewedByUserId: AUTHOR }],
        ['suspend', () => service.suspend('p-1', { reason: 'r' }).subscribe(), { reason: 'r' }],
        ['close', () => service.close('p-1', { reason: 'r' }).subscribe(), { reason: 'r' }]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/p-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as PmmPlanView);
      });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('p-1').subscribe();

      const req = http.expectOne(`${BASE}/p-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
