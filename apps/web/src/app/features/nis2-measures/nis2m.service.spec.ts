import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Nis2MeasuresService } from './nis2m.service';
import { Nis2MeasureEditRequest, Nis2MeasurePlanRequest, Nis2MeasureView } from './nis2m.types';

/**
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue les règles de
 * l'article 21 que le back applique en production — cycle de vie PLANNED →
 * IN_PROGRESS → IMPLEMENTED → VERIFIED, justification obligatoire d'un risque
 * résiduel CRITICAL, recalcul de l'échéance de revue. Les deux modes sont testés.
 */
describe('Nis2MeasuresService', () => {

  const AUTHOR = '00000000-0000-0000-0000-000000000999';
  const DAY = 86400000;

  const planReq = (over: Partial<Nis2MeasurePlanRequest> = {}): Nis2MeasurePlanRequest => ({
    reference: 'NIS2-CRYPTO-900',
    category: 'CRYPTOGRAPHY',
    title: 'Chiffrement des sauvegardes au repos',
    maturityLevel: 2,
    residualRiskRating: 'MEDIUM',
    reviewIntervalDays: 180,
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<Nis2MeasureEditRequest> = {}): Nis2MeasureEditRequest => ({
    title: 'MFA obligatoire — périmètre étendu aux prestataires',
    maturityLevel: 5,
    residualRiskRating: 'LOW',
    reviewIntervalDays: 365,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: Nis2MeasuresService;
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
      service = TestBed.inject(Nis2MeasuresService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    it('liste les mesures pré-chargées et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).value?.length).toBe(3);

      const verified = run(service.list('VERIFIED')).value ?? [];
      expect(verified.length).toBe(2);
      expect(verified.every(m => m.status === 'VERIFIED')).toBeTrue();

      expect(run(service.list('PLANNED')).value).toEqual([]);
    }));

    it('filtre sur la catégorie de l\'article 21', fakeAsync(() => {
      const rows = run(service.listByCategory('BUSINESS_CONTINUITY')).value ?? [];

      expect(rows.map(m => m.reference)).toEqual(['NIS2-BCP-002']);
      expect(run(service.listByCategory('SECURE_DEVELOPMENT')).value).toEqual([]);
    }));

    it('ne remonte en revue en retard que les mesures vérifiées dont l\'échéance est passée', fakeAsync(() => {
      const overdue = run(service.reviewOverdue()).value ?? [];

      expect(overdue.map(m => m.id)).toEqual(['nis2m-seed-003']);
      expect(run(service.reviewOverdue(0)).value).toEqual([]);
    }));

    it('résout une mesure par identifiant, et refuse un identifiant inconnu', fakeAsync(() => {
      expect(run(service.get('nis2m-seed-001')).value?.reference).toBe('NIS2-MFA-001');
      expect(run(service.get('nis2m-inexistant')).error?.status).toBe(404);
    }));

    it('résout une mesure par référence, et refuse une référence inconnue', fakeAsync(() => {
      expect(run(service.getByReference('NIS2-BCP-002')).value?.id).toBe('nis2m-seed-002');
      expect(run(service.getByReference('NIS2-ABSENTE')).error?.status).toBe(404);
    }));

    it('rend une copie : modifier le résultat ne corrompt pas le magasin', fakeAsync(() => {
      const first = run(service.get('nis2m-seed-002')).value!;
      first.title = 'titre falsifié';

      expect(run(service.get('nis2m-seed-002')).value?.title).toContain('Plan de continuité');
    }));

    // ---- Planification -----------------------------------------------------

    it('refuse une référence déjà utilisée', fakeAsync(() => {
      expect(run(service.plan(planReq({ reference: 'NIS2-MFA-001' }))).error?.status).toBe(409);
    }));

    it('exige une justification pour un risque résiduel CRITICAL', fakeAsync(() => {
      expect(run(service.plan(planReq({ residualRiskRating: 'CRITICAL' }))).error?.status).toBe(422);
      expect(run(service.plan(planReq({
        residualRiskRating: 'CRITICAL', criticalRiskJustification: '   '
      }))).error?.status).toBe(422);
    }));

    it('accepte un risque CRITICAL justifié et le signale à la direction', fakeAsync(() => {
      const created = run(service.plan(planReq({
        residualRiskRating: 'CRITICAL',
        criticalRiskJustification: 'Dépendance à un fournisseur unique, arbitrage direction en cours.'
      }))).value!;

      expect(created.criticalResidualRisk).toBeTrue();
      expect(created.status).toBe('PLANNED');
    }));

    it('crée une mesure planifiée sans cycle de revue démarré', fakeAsync(() => {
      const created = run(service.plan(planReq())).value!;

      expect(created.status).toBe('PLANNED');
      expect(created.criticalResidualRisk).toBeFalse();
      expect(created.effectiveFrom).toBeNull();
      expect(created.lastReviewedAt).toBeNull();
      expect(created.nextReviewDueAt).toBeNull();
      expect(created.reviewOverdue).toBeFalse();
      expect(created.evidenceUrls).toEqual([]);
      expect(created.linkedProcessingActivityIds).toEqual([]);
      expect(created.linkedProcessorAgreementIds).toEqual([]);
      expect(created.ownerUserId).toBeNull();
      expect(created.description).toBeNull();
      expect(created.notes).toBeNull();
    }));

    it('place la nouvelle mesure en tête de liste', fakeAsync(() => {
      run(service.plan(planReq()));
      expect(run(service.list()).value?.[0].reference).toBe('NIS2-CRYPTO-900');
    }));

    // ---- Édition -----------------------------------------------------------

    it('met à jour les champs éditables sans toucher au statut', fakeAsync(() => {
      const updated = run(service.edit('nis2m-seed-002', editReq({
        description: 'PRA testé trimestriellement.',
        ownerUserId: AUTHOR,
        evidenceUrls: ['https://wiki.qualitos.local/pra.pdf'],
        notes: 'Validé DSI.'
      }))).value!;

      expect(updated.status).toBe('IN_PROGRESS');
      expect(updated.title).toContain('prestataires');
      expect(updated.maturityLevel).toBe(5);
      expect(updated.reviewIntervalDays).toBe(365);
      expect(updated.evidenceUrls).toEqual(['https://wiki.qualitos.local/pra.pdf']);
      expect(updated.criticalResidualRisk).toBeFalse();
    }));

    it('efface les champs optionnels que l\'édition ne fournit plus', fakeAsync(() => {
      const updated = run(service.edit('nis2m-seed-003', editReq())).value!;

      expect(updated.description).toBeNull();
      expect(updated.notes).toBeNull();
      expect(updated.ownerUserId).toBeNull();
      expect(updated.criticalRiskJustification).toBeNull();
      expect(updated.linkedProcessingActivityIds).toEqual([]);
    }));

    it('refuse d\'éditer une mesure désactivée ou inconnue', fakeAsync(() => {
      run(service.deprecate('nis2m-seed-002'));

      expect(run(service.edit('nis2m-seed-002', editReq())).error?.status).toBe(409);
      expect(run(service.edit('inconnue', editReq())).error?.status).toBe(404);
    }));

    it('exige aussi la justification CRITICAL à l\'édition', fakeAsync(() => {
      expect(run(service.edit('nis2m-seed-002', editReq({ residualRiskRating: 'CRITICAL' }))).error?.status).toBe(422);
    }));

    // ---- Cycle de vie ------------------------------------------------------

    it('déroule le cycle de vie PLANNED → IN_PROGRESS → IMPLEMENTED → VERIFIED', fakeAsync(() => {
      const planned = run(service.plan(planReq())).value!;

      expect(run(service.startImplementation(planned.id)).value?.status).toBe('IN_PROGRESS');

      const implemented = run(service.markImplemented(planned.id)).value!;
      expect(implemented.status).toBe('IMPLEMENTED');
      expect(implemented.effectiveFrom).not.toBeNull();

      const reviewedAt = new Date(Date.now() - DAY).toISOString();
      const verified = run(service.verify(planned.id, { reviewedByUserId: AUTHOR, reviewedAt })).value!;
      expect(verified.status).toBe('VERIFIED');
      expect(verified.lastReviewedAt).toBe(reviewedAt);
      expect(verified.reviewedByUserId).toBe(AUTHOR);
      expect(new Date(verified.nextReviewDueAt!).getTime())
        .toBe(new Date(reviewedAt).getTime() + 180 * DAY);
      expect(verified.reviewOverdue).toBeFalse();
    }));

    it('interdit de sauter une étape du cycle de vie', fakeAsync(() => {
      // seed-002 est IN_PROGRESS : ni démarrable, ni vérifiable.
      expect(run(service.startImplementation('nis2m-seed-002')).error?.status).toBe(409);
      expect(run(service.verify('nis2m-seed-002', { reviewedByUserId: AUTHOR, reviewedAt: new Date().toISOString() })).error?.status).toBe(409);
      // seed-001 est VERIFIED : plus rien à implémenter.
      expect(run(service.markImplemented('nis2m-seed-001')).error?.status).toBe(409);
    }));

    it('refuse toute transition sur une mesure inconnue', fakeAsync(() => {
      const at = { reviewedByUserId: AUTHOR, reviewedAt: new Date().toISOString() };

      expect(run(service.startImplementation('inconnue')).error?.status).toBe(404);
      expect(run(service.markImplemented('inconnue')).error?.status).toBe(404);
      expect(run(service.verify('inconnue', at)).error?.status).toBe(404);
      expect(run(service.review('inconnue', at)).error?.status).toBe(404);
      expect(run(service.deprecate('inconnue')).error?.status).toBe(404);
      expect(run(service.delete('inconnue')).error?.status).toBe(404);
    }));

    // ---- Revue périodique --------------------------------------------------

    it('reprogramme l\'échéance et lève le retard après une revue', fakeAsync(() => {
      expect(run(service.reviewOverdue()).value?.length).toBe(1);
      const reviewedAt = new Date().toISOString();

      const reviewed = run(service.review('nis2m-seed-003', { reviewedByUserId: AUTHOR, reviewedAt })).value!;

      expect(reviewed.status).toBe('VERIFIED');
      expect(reviewed.lastReviewedAt).toBe(reviewedAt);
      expect(new Date(reviewed.nextReviewDueAt!).getTime())
        .toBe(new Date(reviewedAt).getTime() + 90 * DAY);
      expect(run(service.reviewOverdue()).value).toEqual([]);
    }));

    it('refuse une revue périodique tant que la mesure n\'est pas vérifiée', fakeAsync(() => {
      expect(run(service.review('nis2m-seed-002', {
        reviewedByUserId: AUTHOR, reviewedAt: new Date().toISOString()
      })).error?.status).toBe(409);
    }));

    // ---- Désactivation et suppression --------------------------------------

    it('désactive une mesure en datant sa fin de validité', fakeAsync(() => {
      const deprecated = run(service.deprecate('nis2m-seed-001')).value!;

      expect(deprecated.status).toBe('DEPRECATED');
      expect(deprecated.effectiveTo).not.toBeNull();
      expect(run(service.deprecate('nis2m-seed-001')).error?.status).toBe(409);
    }));

    it('supprime définitivement une mesure du registre', fakeAsync(() => {
      run(service.delete('nis2m-seed-001'));

      expect(run(service.list()).value?.map(m => m.id)).toEqual(['nis2m-seed-002', 'nis2m-seed-003']);
      expect(run(service.get('nis2m-seed-001')).error?.status).toBe(404);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: Nis2MeasuresService;
    let http: HttpTestingController;
    let prevMock: boolean;

    const base = `${environment.apiBaseUrl}/api/v1/nis2/risk-measures`;
    const view = { id: 'm-1' } as Nis2MeasureView;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(Nis2MeasuresService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('n\'envoie pas de filtre de statut quand aucun n\'est demandé', () => {
      service.list().subscribe();
      const req = http.expectOne(r => r.url === base);
      expect(req.request.params.has('status')).toBeFalse();
      req.flush([]);
    });

    it('transmet le statut, la catégorie et la limite en paramètres de requête', () => {
      service.list('VERIFIED').subscribe();
      const list = http.expectOne(r => r.url === base);
      expect(list.request.params.get('status')).toBe('VERIFIED');
      list.flush([]);

      service.listByCategory('CRYPTOGRAPHY').subscribe();
      const byCat = http.expectOne(r => r.url === `${base}/by-category`);
      expect(byCat.request.params.get('category')).toBe('CRYPTOGRAPHY');
      byCat.flush([]);

      service.reviewOverdue(10).subscribe();
      const overdue = http.expectOne(r => r.url === `${base}/review-overdue`);
      expect(overdue.request.params.get('limit')).toBe('10');
      overdue.flush([]);
    });

    it('résout une fiche par identifiant et par référence', () => {
      service.get('m-1').subscribe(m => expect(m.id).toBe('m-1'));
      http.expectOne(`${base}/m-1`).flush(view);

      service.getByReference('NIS2-MFA-001').subscribe();
      const byRef = http.expectOne(r => r.url === `${base}/by-reference`);
      expect(byRef.request.params.get('reference')).toBe('NIS2-MFA-001');
      byRef.flush(view);
    });

    it('crée par POST sur la racine et modifie par PUT sur la fiche', () => {
      const created = planReq();
      service.plan(created).subscribe();
      const post = http.expectOne(r => r.url === base && r.method === 'POST');
      expect(post.request.body).toEqual(created);
      post.flush(view);

      const edited = editReq();
      service.edit('m-1', edited).subscribe();
      const put = http.expectOne(r => r.url === `${base}/m-1` && r.method === 'PUT');
      expect(put.request.body).toEqual(edited);
      put.flush(view);
    });

    it('adresse chaque transition à sa propre route', () => {
      const at = { reviewedByUserId: 'u-1', reviewedAt: '2026-07-01T00:00:00Z' };
      const routes: Array<[() => void, string, unknown]> = [
        [() => service.startImplementation('m-1').subscribe(), 'start', {}],
        [() => service.markImplemented('m-1').subscribe(), 'implemented', {}],
        [() => service.verify('m-1', at).subscribe(), 'verify', at],
        [() => service.review('m-1', at).subscribe(), 'review', at],
        [() => service.deprecate('m-1').subscribe(), 'deprecate', {}]
      ];

      for (const [call, path, body] of routes) {
        call();
        const req = http.expectOne(`${base}/m-1/${path}`);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(body);
        req.flush(view);
      }
    });

    it('supprime par DELETE sur la fiche', () => {
      service.delete('m-1').subscribe();
      const req = http.expectOne(`${base}/m-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('propage l\'erreur serveur au lieu de la masquer', () => {
      let status = 0;
      service.get('m-1').subscribe({ error: e => (status = e.status) });
      http.expectOne(`${base}/m-1`).flush({}, { status: 409, statusText: 'Conflict' });
      expect(status).toBe(409);
    });
  });
});
