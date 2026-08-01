import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AdmService } from './adm.service';
import { AdmCreateRequest, AdmEditRequest, AdmView, BASIS_LABEL, TYPE_LABEL } from './adm.types';

/**
 * Registre des décisions individuelles automatisées (RGPD art. 22).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * DRAFT → ACTIVE → DEPRECATED → ARCHIVED et surtout les garanties de l'article 22 :
 * une décision produisant un effet juridique exige une base légale (art. 22.2) et
 * un mécanisme de révision humaine (art. 22.3), aussi bien à la création qu'à
 * l'édition et à l'activation. Les deux modes sont testés.
 */
describe('AdmService', () => {

  const AUTHOR = '00000000-0000-0000-0000-000000000999';
  const BASE = `${environment.apiBaseUrl}/api/v1/gdpr/automated-decisions`;

  const createReq = (over: Partial<AdmCreateRequest> = {}): AdmCreateRequest => ({
    reference: 'ADM-NOUVELLE-900',
    name: 'Attribution automatique de créneaux',
    decisionType: 'PROFILING_ONLY',
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<AdmEditRequest> = {}): AdmEditRequest => ({
    name: 'Détection fraude paiement temps réel — v2',
    decisionType: 'PROFILING_ONLY',
    ...over
  });

  /** Jeu minimal conforme à l'article 22 pour une décision à effet juridique. */
  const legalEffect = {
    decisionType: 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT' as const,
    art22LawfulBasis: 'EXPLICIT_CONSENT' as const,
    humanReviewMechanism: 'Révision par un agent sous 15 jours sur demande.'
  };

  // ------------------------------------------------------------------------
  // Libellés
  // ------------------------------------------------------------------------
  describe('libellés du registre', () => {
    it('nomme chaque type de décision et chaque base légale', () => {
      expect(TYPE_LABEL.PROFILING_ONLY).toBeTruthy();
      expect(TYPE_LABEL.AUTOMATED_DECISION).toBeTruthy();
      expect(TYPE_LABEL.AUTOMATED_DECISION_WITH_LEGAL_EFFECT).toContain('22.1');

      expect(BASIS_LABEL.EXPLICIT_CONSENT).toBeTruthy();
      expect(BASIS_LABEL.CONTRACTUAL_NECESSITY).toBeTruthy();
      expect(BASIS_LABEL.AUTHORIZED_BY_LAW).toBeTruthy();
    });
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: AdmService;
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
      service = TestBed.inject(AdmService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les décisions pré-chargées et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).value?.length).toBe(3);

      expect(run(service.list('DRAFT')).value?.map(r => r.reference)).toEqual(['ADM-FRAUD-003']);
      expect(run(service.list('ARCHIVED')).value).toEqual([]);
    }));

    it('résout une décision par identifiant et par référence, et refuse une clé inconnue', fakeAsync(() => {
      expect(run(service.get('adm-seed-002')).value?.reference).toBe('ADM-ABTEST-002');
      expect(run(service.getByReference('ADM-CREDIT-001')).value?.id).toBe('adm-seed-001');

      expect(run(service.get('adm-inexistante')).error?.status).toBe(404);
      expect(run(service.getByReference('ADM-ABSENTE')).error?.status).toBe(404);
    }));

    it('rend une copie : modifier le résultat ne corrompt pas le magasin', fakeAsync(() => {
      const first = run(service.get('adm-seed-001')).value!;
      first.name = 'nom falsifié';

      expect(run(service.get('adm-seed-001')).value?.name).toBe('Scoring crédit consommateur');
    }));

    // ---- Garanties de l'article 22 ------------------------------------------

    it('refuse une décision à effet juridique sans base légale (art. 22.2)', fakeAsync(() => {
      const refus = run(service.create(createReq({
        decisionType: 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT',
        humanReviewMechanism: 'Révision par un agent.'
      }))).error as { status: number; error: { detail: string } };

      expect(refus.status).toBe(422);
      expect(refus.error.detail).toContain('22.2');
    }));

    it('refuse une décision à effet juridique sans révision humaine (art. 22.3)', fakeAsync(() => {
      const refus = run(service.create(createReq({
        decisionType: 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT',
        art22LawfulBasis: 'EXPLICIT_CONSENT'
      }))).error as { status: number; error: { detail: string } };

      expect(refus.status).toBe(422);
      expect(refus.error.detail).toContain('22.3');
    }));

    it('ne se contente pas d\'un mécanisme de révision fait d\'espaces', fakeAsync(() => {
      expect(run(service.create(createReq({
        decisionType: 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT',
        art22LawfulBasis: 'EXPLICIT_CONSENT',
        humanReviewMechanism: '   '
      }))).error?.status).toBe(422);
    }));

    it('signale le refus par le canal d\'erreur, sans exception synchrone', fakeAsync(() => {
      // L'appelant est un dialogue qui pose `finalize` puis souscrit : une
      // exception synchrone le laisserait bloqué sur son indicateur de
      // chargement, sans jamais afficher le motif du refus.
      expect(() => {
        service.create(createReq({
          decisionType: 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT'
        })).subscribe({ error: () => undefined });
        tick(300);
      }).not.toThrow();
    }));

    it('n\'exige ces garanties que des décisions à effet juridique', fakeAsync(() => {
      // Un profilage simple n'entre pas dans le champ de l'article 22.
      expect(run(service.create(createReq())).value?.status).toBe('DRAFT');
      expect(run(service.create(createReq({
        reference: 'ADM-SANS-EFFET-901', decisionType: 'AUTOMATED_DECISION'
      }))).value?.status).toBe('DRAFT');
    }));

    it('applique les mêmes garanties à l\'édition', fakeAsync(() => {
      expect(run(service.edit('adm-seed-003', editReq({
        decisionType: 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT'
      }))).error?.status).toBe(422);

      expect(run(service.edit('adm-seed-003', editReq(legalEffect))).value?.decisionType)
        .toBe('AUTOMATED_DECISION_WITH_LEGAL_EFFECT');
    }));

    it('applique les mêmes garanties à l\'activation', fakeAsync(() => {
      // seed-003 est un brouillon à effet juridique, sans base légale ni révision.
      expect(run(service.activate('adm-seed-003')).error?.status).toBe(422);

      run(service.edit('adm-seed-003', editReq(legalEffect)));
      expect(run(service.activate('adm-seed-003')).value?.status).toBe('ACTIVE');
    }));

    // ---- Création ----------------------------------------------------------

    it('refuse une référence déjà utilisée', fakeAsync(() => {
      expect(run(service.create(createReq({ reference: 'ADM-CREDIT-001' }))).error?.status).toBe(409);
    }));

    it('crée un brouillon en tête de liste, sans période d\'effet', fakeAsync(() => {
      const created = run(service.create(createReq())).value!;

      expect(created.status).toBe('DRAFT');
      expect(created.effectiveFrom).toBeNull();
      expect(run(service.list()).value?.[0].reference).toBe('ADM-NOUVELLE-900');
    }));

    it('normalise les champs facultatifs omis', fakeAsync(() => {
      const created = run(service.create(createReq())).value!;

      expect(created.description).toBeNull();
      expect(created.art22LawfulBasis).toBeNull();
      expect(created.linkedDpiaId).toBeNull();
      expect(created.algorithmDescription).toBeNull();
      expect(created.inputDataCategories).toEqual([]);
      expect(created.linkedProcessingActivityIds).toEqual([]);
    }));

    // ---- Édition -----------------------------------------------------------

    it('n\'édite ni une archive, ni une décision inconnue', fakeAsync(() => {
      expect(run(service.edit('adm-inexistante', editReq())).error?.status).toBe(404);

      run(service.deprecate('adm-seed-002'));
      run(service.archive('adm-seed-002'));
      expect(run(service.edit('adm-seed-002', editReq())).error?.status).toBe(409);
    }));

    it('remplace les champs, une omission valant effacement', fakeAsync(() => {
      const edited = run(service.edit('adm-seed-001', editReq({
        inputDataCategories: ['identité']
      }))).value!;

      expect(edited.name).toContain('v2');
      expect(edited.inputDataCategories).toEqual(['identité']);
      // L'édition porte l'état complet du formulaire.
      expect(edited.objectionMechanism).toBeNull();
    }));

    // ---- Cycle de vie ------------------------------------------------------

    it('n\'active qu\'un brouillon existant', fakeAsync(() => {
      expect(run(service.activate('adm-inexistante')).error?.status).toBe(404);
      expect(run(service.activate('adm-seed-001')).error?.status).toBe(409);
    }));

    it('ouvre la période d\'effet à l\'activation', fakeAsync(() => {
      const created = run(service.create(createReq())).value!;

      const activated = run(service.activate(created.id)).value!;

      expect(activated.status).toBe('ACTIVE');
      expect(activated.effectiveFrom).not.toBeNull();
    }));

    it('ne déprécie qu\'une décision active et existante', fakeAsync(() => {
      expect(run(service.deprecate('adm-inexistante')).error?.status).toBe(404);
      expect(run(service.deprecate('adm-seed-003')).error?.status).toBe(409);

      expect(run(service.deprecate('adm-seed-001')).value?.status).toBe('DEPRECATED');
    }));

    it('n\'archive qu\'une décision dépréciée et existante', fakeAsync(() => {
      expect(run(service.archive('adm-inexistante')).error?.status).toBe(404);
      // On ne saute pas l'étape de dépréciation : une décision active reste opposable.
      expect(run(service.archive('adm-seed-001')).error?.status).toBe(409);

      run(service.deprecate('adm-seed-001'));
      const archived = run(service.archive('adm-seed-001')).value!;
      expect(archived.status).toBe('ARCHIVED');
      expect(archived.effectiveTo).not.toBeNull();
    }));

    // ---- Suppression -------------------------------------------------------

    it('ne supprime qu\'un brouillon : une décision appliquée reste traçable', fakeAsync(() => {
      expect(run(service.delete('adm-inexistante')).error?.status).toBe(404);
      expect(run(service.delete('adm-seed-001')).error?.status).toBe(409);

      run(service.delete('adm-seed-003'));

      expect(run(service.list()).value?.length).toBe(2);
      expect(run(service.get('adm-seed-003')).error?.status).toBe(404);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: AdmService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(AdmService);
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

    it('laisse le serveur juger de l\'article 22, sans contrôle client préalable', () => {
      // Le contrôle du magasin de démo ne doit pas court-circuiter l'appel réel :
      // le serveur est la seule autorité sur ses propres invariants.
      const body = createReq({ decisionType: 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT' });

      service.create(body).subscribe({ error: () => undefined });

      const req = http.expectOne(BASE);
      expect(req.request.body).toEqual(body);
      req.flush({ title: 'Art. 22.2' }, { status: 422, statusText: 'Unprocessable Entity' });
    });

    it('lit une fiche par identifiant et par référence', () => {
      service.get('a-1').subscribe();
      const byId = http.expectOne(`${BASE}/a-1`);
      expect(byId.request.method).toBe('GET');
      byId.flush({} as AdmView);

      service.getByReference('ADM-CREDIT-001').subscribe();
      const byRef = http.expectOne(r => r.url === `${BASE}/by-reference`);
      expect(byRef.request.params.get('reference')).toBe('ADM-CREDIT-001');
      byRef.flush({} as AdmView);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = createReq();
      service.create(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as AdmView);

      const edit = editReq();
      service.edit('a-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/a-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as AdmView);
    });

    it('poste chaque transition sur son propre sous-chemin, sans corps', () => {
      // Ces transitions n'ont pas de corps métier : on poste un objet vide plutôt
      // que `null`, un POST sans corps déclenchant des 415 sur certains serveurs.
      ['activate', 'deprecate', 'archive'].forEach(path => {
        const call = {
          activate: () => service.activate('a-1'),
          deprecate: () => service.deprecate('a-1'),
          archive: () => service.archive('a-1')
        }[path]!;
        call().subscribe();

        const req = http.expectOne(`${BASE}/a-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual({});
        req.flush({} as AdmView);
      });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('a-1').subscribe();

      const req = http.expectOne(`${BASE}/a-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
