import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  RetentionService, daysToDuration, describeDuration, durationToDays
} from './retention.service';
import {
  CreateRetentionRuleRequest, EditRetentionRuleRequest, RetentionRuleView
} from './retention.types';

/**
 * Règles de conservation des données (RGPD art. 5 §1.e).
 *
 * Deux sujets distincts sont couverts ici.
 *
 * 1. La conversion des durées. Le back sérialise en `Duration` Java, qui n'accepte
 *    que des unités temps — jours et en dessous, jamais de mois ni d'années. Les
 *    helpers traduisent dans les deux sens pour offrir une saisie naturelle ; une
 *    erreur à cet endroit décale silencieusement toutes les dates d'effacement.
 *
 * 2. Le service lui-même, dans ses deux implémentations (magasin de démo et HTTP
 *    réel), avec l'invariant qui fait la valeur du registre : une seule règle
 *    active par catégorie de données à un instant donné.
 */
describe('RetentionService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/gdpr/retention-rules`;
  const DAY = 86400000;

  const createReq = (over: Partial<CreateRetentionRuleRequest> = {}): CreateRetentionRuleRequest => ({
    dataCategoryCode: 'support.ticket',
    dataCategoryLabel: 'Tickets de support client',
    retentionPeriod: 'P365D',
    legalBasis: 'Intérêt légitime — suivi de la relation client (RGPD art. 6.1.f).',
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<EditRetentionRuleRequest> = {}): EditRetentionRuleRequest => ({
    retentionPeriod: 'P90D',
    legalBasis: 'Durée réduite après avis du délégué à la protection des données.',
    ...over
  });

  // ------------------------------------------------------------------------
  // Conversion des durées
  // ------------------------------------------------------------------------
  describe('conversion des durées ISO-8601', () => {

    it('lit une durée en jours', () => {
      expect(durationToDays('P30D')).toBe(30);
      expect(durationToDays('P1825D')).toBe(1825);
    });

    it('convertit les heures en jours entiers, en tronquant le reste', () => {
      expect(durationToDays('PT48H')).toBe(2);
      // 36 h ne font pas deux jours : on ne prolonge jamais une conservation.
      expect(durationToDays('PT36H')).toBe(1);
      expect(durationToDays('P1DT24H')).toBe(2);
    });

    it('rend zéro pour une durée absente ou non reconnue', () => {
      expect(durationToDays('')).toBe(0);
      expect(durationToDays('3 ans')).toBe(0);
      // Les mois et années n'existent pas dans une Duration Java : refusés.
      expect(durationToDays('P1Y')).toBe(0);
      expect(durationToDays('P6M')).toBe(0);
    });

    it('écrit une durée à partir d\'un nombre et d\'une unité', () => {
      expect(daysToDuration(30, 'DAY')).toBe('P30D');
      expect(daysToDuration(6, 'MONTH')).toBe('P180D');
      expect(daysToDuration(3, 'YEAR')).toBe('P1095D');
    });

    it('arrondit et refuse les durées négatives', () => {
      expect(daysToDuration(1.4, 'DAY')).toBe('P1D');
      expect(daysToDuration(1.6, 'DAY')).toBe('P2D');
      // Une durée négative effacerait rétroactivement : ramenée à zéro.
      expect(daysToDuration(-5, 'DAY')).toBe('P0D');
    });

    it('fait l\'aller-retour sans perte sur les unités naturelles', () => {
      expect(durationToDays(daysToDuration(5, 'YEAR'))).toBe(1825);
      expect(durationToDays(daysToDuration(18, 'MONTH'))).toBe(540);
    });

    it('décrit la durée dans l\'unité la plus lisible', () => {
      expect(describeDuration('P1095D')).toBe('3 ans (1095 j)');
      expect(describeDuration('P365D')).toBe('1 an (365 j)');
      expect(describeDuration('P180D')).toBe('6 mois (180 j)');
      expect(describeDuration('P30D')).toBe('1 mois (30 j)');
    });

    it('retombe sur les jours quand la durée ne tombe pas juste', () => {
      expect(describeDuration('P400D')).toBe('400 jours');
      expect(describeDuration('P45D')).toBe('45 jours');
      expect(describeDuration('P1D')).toBe('1 jour');
      expect(describeDuration('')).toBe('0 jour');
    });
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: RetentionService;
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
      service = TestBed.inject(RetentionService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les règles pré-chargées et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).length).toBe(3);

      expect(run(service.list('DRAFT')).map(r => r.id)).toEqual(['ret-3']);
      expect(run(service.list('ARCHIVED'))).toEqual([]);
    }));

    it('résout une règle par identifiant, avec repli sur la première si inconnue', fakeAsync(() => {
      expect(run(service.get('ret-2')).dataCategoryCode).toBe('crm.prospect');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('ret-inexistante')).id).toBe('ret-1');
    }));

    // ---- Création / édition -------------------------------------------------

    it('crée un brouillon en tête de liste, sans date d\'effet', fakeAsync(() => {
      const created = run(service.create(createReq()));

      expect(created.status).toBe('DRAFT');
      expect(created.effectiveFrom).toBeUndefined();
      expect(run(service.list())[0].dataCategoryCode).toBe('support.ticket');
    }));

    it('remplace la durée et le fondement juridique de la règle éditée', fakeAsync(() => {
      const edited = run(service.edit('ret-3', editReq({
        dataCategoryLabel: 'Enregistrements de vidéosurveillance (hall)'
      })));

      expect(edited.retentionPeriod).toBe('P90D');
      expect(edited.legalBasis).toContain('avis du délégué');
      expect(edited.dataCategoryLabel).toContain('hall');
    }));

    it('édite sans effet de bord quand la règle visée n\'existe pas', fakeAsync(() => {
      const before = run(service.get('ret-1')).retentionPeriod;

      run(service.edit('ret-inexistante', editReq()));

      expect(run(service.get('ret-1')).retentionPeriod).toBe(before);
    }));

    // ---- Activation : une seule règle active par catégorie -------------------

    it('active la règle et ouvre sa période d\'effet', fakeAsync(() => {
      const activated = run(service.activate('ret-3'));

      expect(activated.status).toBe('ACTIVE');
      expect(activated.effectiveFrom).toBeTruthy();
    }));

    it('archive la règle active de la même catégorie — une seule à la fois', fakeAsync(() => {
      const next = run(service.create(createReq({
        dataCategoryCode: 'crm.prospect', retentionPeriod: 'P730D'
      })));

      run(service.activate(next.id));

      const previous = run(service.get('ret-2'));
      expect(previous.status).toBe('ARCHIVED');
      expect(previous.effectiveTo).toBeTruthy();
    }));

    it('ne touche pas aux règles des autres catégories', fakeAsync(() => {
      const next = run(service.create(createReq({ dataCategoryCode: 'crm.prospect' })));

      run(service.activate(next.id));

      // La règle « bulletins de paie » relève d'une autre catégorie : intacte.
      expect(run(service.get('ret-1')).status).toBe('ACTIVE');
    }));

    it('active sans effet de bord quand la règle visée n\'existe pas', fakeAsync(() => {
      run(service.activate('ret-inexistante'));

      expect(run(service.get('ret-3')).status).toBe('DRAFT');
    }));

    // ---- Archivage / suppression -------------------------------------------

    it('archive une règle et date sa fin d\'effet', fakeAsync(() => {
      const archived = run(service.archive('ret-1'));

      expect(archived.status).toBe('ARCHIVED');
      expect(archived.effectiveTo).toBeTruthy();
    }));

    it('archive sans effet de bord quand la règle visée n\'existe pas', fakeAsync(() => {
      run(service.archive('ret-inexistante'));

      expect(run(service.get('ret-1')).status).toBe('ACTIVE');
    }));

    it('supprime une règle, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('ret-3'));
      expect(run(service.list()).length).toBe(2);

      run(service.delete('ret-inexistante'));
      expect(run(service.list()).length).toBe(2);
    }));

    // ---- Évaluation de l'échéance d'effacement ------------------------------

    it('calcule la date d\'effacement à partir de la règle active', fakeAsync(() => {
      const createdAt = new Date(Date.now() - 100 * DAY).toISOString();

      const evaluation = run(service.evaluateErasure('crm.prospect', createdAt))!;

      expect(evaluation.ruleId).toBe('ret-2');
      expect(evaluation.retentionPeriod).toBe('P1095D');
      expect(new Date(evaluation.erasureAt).getTime())
        .toBe(new Date(createdAt).getTime() + 1095 * DAY);
      expect(evaluation.dueNow).toBeFalse();
    }));

    it('signale l\'effacement dû quand la durée est écoulée', fakeAsync(() => {
      const createdAt = new Date(Date.now() - 2000 * DAY).toISOString();

      expect(run(service.evaluateErasure('crm.prospect', createdAt))?.dueNow).toBeTrue();
    }));

    it('ne conclut rien sans règle active pour la catégorie', fakeAsync(() => {
      // cctv.video n'existe qu'à l'état de brouillon : aucune règle opposable.
      expect(run(service.evaluateErasure('cctv.video', new Date().toISOString()))).toBeNull();
      expect(run(service.evaluateErasure('categorie.inconnue', new Date().toISOString()))).toBeNull();
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: RetentionService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(RetentionService);
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

    it('lit une fiche par identifiant', () => {
      service.get('r-1').subscribe();

      const req = http.expectOne(`${BASE}/r-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as RetentionRuleView);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = createReq();
      service.create(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as RetentionRuleView);

      const edit = editReq();
      service.edit('r-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/r-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as RetentionRuleView);
    });

    it('poste l\'activation et l\'archivage sans corps', () => {
      service.activate('r-1').subscribe();
      const activate = http.expectOne(`${BASE}/r-1/activate`);
      expect(activate.request.method).toBe('POST');
      expect(activate.request.body).toEqual({});
      activate.flush({} as RetentionRuleView);

      service.archive('r-1').subscribe();
      const archive = http.expectOne(`${BASE}/r-1/archive`);
      expect(archive.request.method).toBe('POST');
      expect(archive.request.body).toEqual({});
      archive.flush({} as RetentionRuleView);
    });

    it('interroge l\'échéance d\'effacement avec ses deux paramètres', () => {
      const createdAt = '2026-01-01T00:00:00Z';

      service.evaluateErasure('crm.prospect', createdAt).subscribe();

      const req = http.expectOne(r => r.url === `${BASE}/erasure-evaluation`);
      expect(req.request.params.get('dataCategoryCode')).toBe('crm.prospect');
      expect(req.request.params.get('recordCreatedAt')).toBe(createdAt);
      req.flush(null);
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('r-1').subscribe();

      const req = http.expectOne(`${BASE}/r-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
