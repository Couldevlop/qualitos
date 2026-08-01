import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { EudbService } from './eudb.service';
import { EudbDraftRequest, EudbView } from './eudb.types';

/**
 * Enregistrement à la base de données européenne (AI Act, art. 49 et 71).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle de vie
 * que le back applique en production — DRAFT → SUBMITTED → REGISTERED → UPDATED,
 * REJECTED et RETIRED en sorties — ainsi que les règles qui protègent le registre :
 * unicité de la référence et de l'identifiant EUDB, complétude obligatoire avant
 * soumission. Les deux modes sont testés.
 */
describe('EudbService', () => {

  const AUTHOR = '00000000-0000-0000-0000-000000000999';
  const BASE = `${environment.apiBaseUrl}/api/v1/ai-act/eudb`;

  const draftReq = (over: Partial<EudbDraftRequest> = {}): EudbDraftRequest => ({
    reference: 'EUDB-2026-NEW-900',
    aiSystemId: '44444444-4444-4444-4444-444444444444',
    providerEntityName: 'QualitOS SAS',
    memberStateOfReference: 'FR',
    intendedPurposeSummary: 'Détection de dérive qualité en atelier.',
    createdByUserId: AUTHOR,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: EudbService;
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

    /** Amène un dossier neuf jusqu'à REGISTERED — état de départ de plusieurs règles. */
    function registered(): EudbView {
      const created = run(service.draft(draftReq())).value!;
      run(service.submit(created.id, { submittedByUserId: AUTHOR }));
      return run(service.markRegistered(created.id, {
        eudbId: 'EUDB-AI-NEW00001', registrationDate: '2026-07-01'
      })).value!;
    }

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(EudbService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les enregistrements pré-chargés et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).value?.length).toBe(3);

      const registeredRows = run(service.list('REGISTERED')).value ?? [];
      expect(registeredRows.map(r => r.reference)).toEqual(['EUDB-2026-DIAG-001']);

      expect(run(service.list('RETIRED')).value).toEqual([]);
    }));

    it('filtre par système d\'IA rattaché', fakeAsync(() => {
      const rows = run(service.listByAiSystem('22222222-2222-2222-2222-222222222222')).value ?? [];

      expect(rows.map(r => r.reference)).toEqual(['EUDB-2026-HRGD-002']);
      expect(run(service.listByAiSystem('système-inconnu')).value).toEqual([]);
    }));

    it('résout un enregistrement par identifiant, et refuse un identifiant inconnu', fakeAsync(() => {
      expect(run(service.get('eudb-seed-001')).value?.reference).toBe('EUDB-2026-DIAG-001');
      expect(run(service.get('eudb-inexistant')).error?.status).toBe(404);
    }));

    it('résout par référence, et refuse une référence inconnue', fakeAsync(() => {
      expect(run(service.getByReference('EUDB-2026-HRGD-002')).value?.id).toBe('eudb-seed-002');
      expect(run(service.getByReference('EUDB-ABSENTE')).error?.status).toBe(404);
    }));

    it('résout par identifiant EUDB, et refuse un identifiant EUDB inconnu', fakeAsync(() => {
      expect(run(service.getByEudbId('EUDB-AI-A4F92K71')).value?.id).toBe('eudb-seed-001');
      expect(run(service.getByEudbId('EUDB-AI-INCONNU')).error?.status).toBe(404);
    }));

    it('rend une copie : modifier le résultat ne corrompt pas le magasin', fakeAsync(() => {
      const first = run(service.get('eudb-seed-001')).value!;
      first.reference = 'référence falsifiée';

      expect(run(service.get('eudb-seed-001')).value?.reference).toBe('EUDB-2026-DIAG-001');
    }));

    // ---- Création ----------------------------------------------------------

    it('refuse une référence déjà utilisée', fakeAsync(() => {
      expect(run(service.draft(draftReq({ reference: 'EUDB-2026-DIAG-001' }))).error?.status).toBe(409);
    }));

    it('crée un brouillon en tête de liste, sans identifiant EUDB', fakeAsync(() => {
      const created = run(service.draft(draftReq())).value!;

      expect(created.status).toBe('DRAFT');
      expect(created.eudbId).toBeNull();
      expect(created.submittedAt).toBeNull();
      expect(run(service.list()).value?.[0].reference).toBe('EUDB-2026-NEW-900');
    }));

    it('normalise en null les champs facultatifs omis', fakeAsync(() => {
      const created = run(service.draft({
        reference: 'EUDB-2026-MINIMAL-901',
        aiSystemId: '55555555-5555-5555-5555-555555555555',
        createdByUserId: AUTHOR
      })).value!;

      expect(created.providerEntityName).toBeNull();
      expect(created.providerEuRepresentative).toBeNull();
      expect(created.memberStateOfReference).toBeNull();
      expect(created.intendedPurposeSummary).toBeNull();
      expect(created.technicalDocumentationReference).toBeNull();
    }));

    // ---- Édition -----------------------------------------------------------

    it('n\'édite qu\'un brouillon, et seulement s\'il existe', fakeAsync(() => {
      expect(run(service.edit('eudb-inexistant', {})).error?.status).toBe(404);
      // seed-002 est SUBMITTED : le dossier est parti au régulateur, il se fige.
      expect(run(service.edit('eudb-seed-002', {})).error?.status).toBe(409);
    }));

    it('remplace les champs du brouillon, une omission valant effacement', fakeAsync(() => {
      const edited = run(service.edit('eudb-seed-003', {
        providerEntityName: 'QualitOS SAS — Lyon',
        technicalDocumentationReference: 'TDOC-CREDIT-v0.2'
      })).value!;

      expect(edited.providerEntityName).toBe('QualitOS SAS — Lyon');
      expect(edited.technicalDocumentationReference).toBe('TDOC-CREDIT-v0.2');
      // L'édition porte l'état COMPLET du formulaire : ce qui n'est pas transmis
      // est effacé, sans quoi un champ vidé par l'utilisateur resterait en base.
      expect(edited.intendedPurposeSummary).toBeNull();
    }));

    // ---- Soumission --------------------------------------------------------

    it('ne soumet qu\'un brouillon existant', fakeAsync(() => {
      expect(run(service.submit('eudb-inexistant', { submittedByUserId: AUTHOR })).error?.status).toBe(404);
      expect(run(service.submit('eudb-seed-002', { submittedByUserId: AUTHOR })).error?.status).toBe(409);
    }));

    it('refuse une soumission incomplète en énumérant les champs manquants', fakeAsync(() => {
      // seed-003 n'a pas d'État membre de référence, mais a bien fournisseur et finalité.
      const refus = run(service.submit('eudb-seed-003', { submittedByUserId: AUTHOR })).error as
        { status: number; error: { detail: string } };

      expect(refus.status).toBe(422);
      expect(refus.error.detail).toContain('État membre de référence');
      expect(refus.error.detail).not.toContain('Finalité prévue');
    }));

    it('soumet un brouillon complet et horodate la soumission', fakeAsync(() => {
      const created = run(service.draft(draftReq())).value!;

      const submitted = run(service.submit(created.id, { submittedByUserId: AUTHOR })).value!;

      expect(submitted.status).toBe('SUBMITTED');
      expect(submitted.submittedByUserId).toBe(AUTHOR);
      expect(submitted.submittedAt).not.toBeNull();
    }));

    // ---- Enregistrement effectif -------------------------------------------

    it('ne marque enregistré qu\'un dossier soumis et existant', fakeAsync(() => {
      const req = { eudbId: 'EUDB-AI-ZZZZZZZZ', registrationDate: '2026-07-01' };

      expect(run(service.markRegistered('eudb-inexistant', req)).error?.status).toBe(404);
      expect(run(service.markRegistered('eudb-seed-003', req)).error?.status).toBe(409);
    }));

    it('refuse un identifiant EUDB déjà attribué à un autre dossier', fakeAsync(() => {
      expect(run(service.markRegistered('eudb-seed-002', {
        eudbId: 'EUDB-AI-A4F92K71', registrationDate: '2026-07-01'
      })).error?.status).toBe(409);
    }));

    it('enregistre le dossier soumis avec sa date officielle', fakeAsync(() => {
      const done = run(service.markRegistered('eudb-seed-002', {
        eudbId: 'EUDB-AI-B7X31Q02', registrationDate: '2026-07-01'
      })).value!;

      expect(done.status).toBe('REGISTERED');
      expect(done.eudbId).toBe('EUDB-AI-B7X31Q02');
      expect(done.registrationDate).toBe('2026-07-01');
    }));

    // ---- Mise à jour déclarée ----------------------------------------------

    it('ne déclare une mise à jour que sur un dossier enregistré', fakeAsync(() => {
      const req = { updateSummary: 'Nouveau jeu d\'entraînement', updateDate: '2026-07-15' };

      expect(run(service.declareUpdate('eudb-inexistant', req)).error?.status).toBe(404);
      expect(run(service.declareUpdate('eudb-seed-003', req)).error?.status).toBe(409);
    }));

    it('accepte des mises à jour successives (REGISTERED puis UPDATED)', fakeAsync(() => {
      const first = run(service.declareUpdate('eudb-seed-001', {
        updateSummary: 'Nouveau jeu d\'entraînement', updateDate: '2026-07-15'
      })).value!;
      expect(first.status).toBe('UPDATED');

      const second = run(service.declareUpdate('eudb-seed-001', {
        updateSummary: 'Seuil de confiance relevé', updateDate: '2026-08-01'
      })).value!;

      expect(second.status).toBe('UPDATED');
      expect(second.lastUpdateSummary).toBe('Seuil de confiance relevé');
      expect(second.lastUpdateDate).toBe('2026-08-01');
    }));

    // ---- Rejet / retrait ---------------------------------------------------

    it('ne rejette qu\'un dossier encore instruit (DRAFT ou SUBMITTED)', fakeAsync(() => {
      const req = { reason: 'Documentation technique absente.' };

      expect(run(service.reject('eudb-inexistant', req)).error?.status).toBe(404);
      expect(run(service.reject('eudb-seed-001', req)).error?.status).toBe(409);

      const rejected = run(service.reject('eudb-seed-002', req)).value!;
      expect(rejected.status).toBe('REJECTED');
      expect(rejected.rejectionReason).toBe('Documentation technique absente.');
      expect(rejected.rejectedAt).not.toBeNull();
    }));

    it('ne retire qu\'un dossier effectivement enregistré', fakeAsync(() => {
      const req = { reason: 'Système retiré du marché.' };

      expect(run(service.retire('eudb-inexistant', req)).error?.status).toBe(404);
      expect(run(service.retire('eudb-seed-003', req)).error?.status).toBe(409);

      const retired = run(service.retire('eudb-seed-001', req)).value!;
      expect(retired.status).toBe('RETIRED');
      expect(retired.retirementReason).toBe('Système retiré du marché.');
      expect(retired.retiredAt).not.toBeNull();
    }));

    it('retire aussi un dossier passé par une mise à jour', fakeAsync(() => {
      const dossier = registered();
      run(service.declareUpdate(dossier.id, {
        updateSummary: 'Correctif', updateDate: '2026-07-20'
      }));

      expect(run(service.retire(dossier.id, { reason: 'Fin de vie.' })).value?.status).toBe('RETIRED');
    }));

    // ---- Suppression -------------------------------------------------------

    it('supprime un enregistrement et refuse une suppression inconnue', fakeAsync(() => {
      expect(run(service.delete('eudb-inexistant')).error?.status).toBe(404);

      run(service.delete('eudb-seed-003'));

      expect(run(service.list()).value?.length).toBe(2);
      expect(run(service.get('eudb-seed-003')).error?.status).toBe(404);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: EudbService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(EudbService);
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

      service.list('REGISTERED').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('REGISTERED');
      filtered.flush([]);
    });

    it('interroge chaque point d\'entrée de résolution avec son paramètre', () => {
      service.listByAiSystem('sys-1').subscribe();
      const bySystem = http.expectOne(r => r.url === `${BASE}/by-system`);
      expect(bySystem.request.params.get('aiSystemId')).toBe('sys-1');
      bySystem.flush([]);

      service.getByReference('EUDB-2026-DIAG-001').subscribe();
      const byRef = http.expectOne(r => r.url === `${BASE}/by-reference`);
      expect(byRef.request.params.get('reference')).toBe('EUDB-2026-DIAG-001');
      byRef.flush({} as EudbView);

      service.getByEudbId('EUDB-AI-A4F92K71').subscribe();
      const byEudb = http.expectOne(r => r.url === `${BASE}/by-eudb-id`);
      expect(byEudb.request.params.get('eudbId')).toBe('EUDB-AI-A4F92K71');
      byEudb.flush({} as EudbView);
    });

    it('lit une fiche par identifiant', () => {
      service.get('r-1').subscribe();

      const req = http.expectOne(`${BASE}/r-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as EudbView);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = draftReq();
      service.draft(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as EudbView);

      service.edit('r-1', { memberStateOfReference: 'BE' }).subscribe();
      const put = http.expectOne(`${BASE}/r-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual({ memberStateOfReference: 'BE' });
      put.flush({} as EudbView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      const transitions: Array<[string, () => void, unknown]> = [
        ['submit',
          () => service.submit('r-1', { submittedByUserId: AUTHOR }).subscribe(),
          { submittedByUserId: AUTHOR }],
        ['mark-registered',
          () => service.markRegistered('r-1', { eudbId: 'E-1', registrationDate: '2026-07-01' }).subscribe(),
          { eudbId: 'E-1', registrationDate: '2026-07-01' }],
        ['declare-update',
          () => service.declareUpdate('r-1', { updateSummary: 's', updateDate: '2026-07-02' }).subscribe(),
          { updateSummary: 's', updateDate: '2026-07-02' }],
        ['reject',
          () => service.reject('r-1', { reason: 'r' }).subscribe(),
          { reason: 'r' }],
        ['retire',
          () => service.retire('r-1', { reason: 'r' }).subscribe(),
          { reason: 'r' }]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/r-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as EudbView);
      });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('r-1').subscribe();

      const req = http.expectOne(`${BASE}/r-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
