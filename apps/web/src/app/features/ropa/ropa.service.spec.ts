import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RopaService } from './ropa.service';
import {
  CreateProcessingActivityRequest,
  EditProcessingActivityRequest,
  ProcessingActivityView
} from './ropa.types';

/**
 * Registre des activités de traitement (RGPD art. 30).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * DRAFT → ACTIVE → ARCHIVED avec ses dates d'effet, et le traitement des listes
 * du registre — catégories de personnes, de données, de destinataires, transferts
 * hors UE — dont l'omission vaut effacement, l'article 30 exigeant un registre
 * qui reflète l'état réel du traitement et non un cumul historique. Les deux
 * modes sont testés.
 */
describe('RopaService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/gdpr/processing-activities`;

  const createReq = (
    over: Partial<CreateProcessingActivityRequest> = {}
  ): CreateProcessingActivityRequest => ({
    reference: 'SUPPORT-TICKETS',
    name: 'Gestion des demandes de support client',
    purposes: 'Traiter et suivre les demandes d\'assistance.',
    lawfulBasis: 'CONTRACT',
    controllerName: 'QualitOS SAS',
    controllerContact: 'dpo@qualitos.io',
    specialCategoriesProcessed: false,
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (
    over: Partial<EditProcessingActivityRequest> = {}
  ): EditProcessingActivityRequest => ({
    name: 'Vidéosurveillance du siège — périmètre révisé',
    purposes: 'Sécurité des biens et des personnes.',
    lawfulBasis: 'LEGITIMATE_INTERESTS',
    controllerName: 'QualitOS SAS',
    controllerContact: 'dpo@qualitos.io',
    specialCategoriesProcessed: false,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: RopaService;
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
      service = TestBed.inject(RopaService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les traitements pré-chargés et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).length).toBe(3);

      expect(run(service.list('DRAFT')).map(a => a.reference)).toEqual(['CCTV-SIEGE']);
      expect(run(service.list('ARCHIVED'))).toEqual([]);
    }));

    it('résout un traitement par identifiant et par référence', fakeAsync(() => {
      expect(run(service.get('ropa-2')).reference).toBe('CRM-PROSPECTS');
      expect(run(service.getByReference('RH-PAYROLL-FR')).id).toBe('ropa-1');
    }));

    it('retombe sur le premier traitement quand la clé est inconnue', fakeAsync(() => {
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('ropa-inexistant')).id).toBe('ropa-1');
      expect(run(service.getByReference('REFERENCE-ABSENTE')).id).toBe('ropa-1');
    }));

    // ---- Création ----------------------------------------------------------

    it('crée un brouillon en tête de registre, sans période d\'effet', fakeAsync(() => {
      const created = run(service.create(createReq()));

      expect(created.status).toBe('DRAFT');
      expect(created.effectiveFrom).toBeUndefined();
      expect(run(service.list())[0].reference).toBe('SUPPORT-TICKETS');
    }));

    it('normalise en tableaux vides les listes du registre non fournies', fakeAsync(() => {
      const created = run(service.create(createReq()));

      expect(created.dataSubjectCategories).toEqual([]);
      expect(created.dataCategories).toEqual([]);
      expect(created.recipientCategories).toEqual([]);
      expect(created.thirdCountryTransfers).toEqual([]);
      expect(created.linkedRetentionRuleIds).toEqual([]);
    }));

    it('retient les catégories particulières et leur justification', fakeAsync(() => {
      const created = run(service.create(createReq({
        reference: 'SANTE-SUIVI',
        specialCategoriesProcessed: true,
        specialCategoriesJustification: 'Art. 9.2.h — médecine du travail.',
        dataCategories: ['santé']
      })));

      // Traiter une donnée de l'article 9 sans justification consignée serait
      // un manquement direct : les deux champs voyagent ensemble.
      expect(created.specialCategoriesProcessed).toBeTrue();
      expect(created.specialCategoriesJustification).toContain('Art. 9.2.h');
    }));

    it('retient les transferts hors UE et leurs garanties', fakeAsync(() => {
      const created = run(service.create(createReq({
        reference: 'ANALYTICS-US',
        thirdCountryTransfers: ['US'],
        transferSafeguards: 'Clauses contractuelles types 2021/914.'
      })));

      expect(created.thirdCountryTransfers).toEqual(['US']);
      expect(created.transferSafeguards).toContain('2021/914');
    }));

    // ---- Édition -----------------------------------------------------------

    it('remplace les champs du traitement édité', fakeAsync(() => {
      const edited = run(service.edit('ropa-3', editReq({
        technicalMeasures: 'Chiffrement des flux, conservation 30 jours.'
      })));

      expect(edited.name).toContain('périmètre révisé');
      expect(edited.lawfulBasis).toBe('LEGITIMATE_INTERESTS');
      expect(edited.technicalMeasures).toContain('Chiffrement');
    }));

    it('remet à vide les listes absentes de l\'édition', fakeAsync(() => {
      run(service.edit('ropa-1', editReq({
        dataCategories: ['identité', 'salaire'], recipientCategories: ['URSSAF']
      })));
      expect(run(service.get('ropa-1')).dataCategories).toEqual(['identité', 'salaire']);

      run(service.edit('ropa-1', editReq()));
      const after = run(service.get('ropa-1'));
      // L'article 30 exige un registre qui reflète l'état RÉEL du traitement :
      // une catégorie retirée par l'utilisateur doit disparaître, pas s'ajouter
      // à un cumul historique.
      expect(after.dataCategories).toEqual([]);
      expect(after.recipientCategories).toEqual([]);
      expect(after.thirdCountryTransfers).toEqual([]);
      expect(after.linkedRetentionRuleIds).toEqual([]);
    }));

    it('édite sans effet de bord quand le traitement visé n\'existe pas', fakeAsync(() => {
      const before = run(service.get('ropa-1')).name;

      run(service.edit('ropa-inexistant', editReq()));

      expect(run(service.get('ropa-1')).name).toBe(before);
    }));

    // ---- Cycle de vie ------------------------------------------------------

    it('ouvre la période d\'effet à l\'activation', fakeAsync(() => {
      const activated = run(service.activate('ropa-3'));

      expect(activated.status).toBe('ACTIVE');
      expect(activated.effectiveFrom).toBeTruthy();
    }));

    it('clôt la période d\'effet à l\'archivage', fakeAsync(() => {
      const archived = run(service.archive('ropa-1'));

      expect(archived.status).toBe('ARCHIVED');
      expect(archived.effectiveTo).toBeTruthy();
    }));

    it('laisse le magasin intact quand une transition vise un traitement inconnu', fakeAsync(() => {
      run(service.activate('ropa-inexistant'));
      run(service.archive('ropa-inexistant'));

      expect(run(service.list()).map(a => a.status)).toEqual(['ACTIVE', 'ACTIVE', 'DRAFT']);
    }));

    // ---- Suppression -------------------------------------------------------

    it('supprime un traitement, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('ropa-3'));
      expect(run(service.list()).length).toBe(2);

      run(service.delete('ropa-inexistant'));
      expect(run(service.list()).length).toBe(2);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: RopaService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(RopaService);
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

    it('lit une fiche par identifiant et par référence', () => {
      service.get('a-1').subscribe();
      const byId = http.expectOne(`${BASE}/a-1`);
      expect(byId.request.method).toBe('GET');
      byId.flush({} as ProcessingActivityView);

      service.getByReference('RH-PAYROLL-FR').subscribe();
      const byRef = http.expectOne(r => r.url === `${BASE}/by-reference`);
      expect(byRef.request.params.get('reference')).toBe('RH-PAYROLL-FR');
      byRef.flush({} as ProcessingActivityView);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = createReq();
      service.create(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as ProcessingActivityView);

      const edit = editReq();
      service.edit('a-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/a-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as ProcessingActivityView);
    });

    it('poste chaque transition sur son propre sous-chemin, sans corps', () => {
      // Ces transitions n'ont pas de corps métier : on poste un objet vide plutôt
      // que `null`, un POST sans corps déclenchant des 415 sur certains serveurs.
      service.activate('a-1').subscribe();
      const activate = http.expectOne(`${BASE}/a-1/activate`);
      expect(activate.request.method).toBe('POST');
      expect(activate.request.body).toEqual({});
      activate.flush({} as ProcessingActivityView);

      service.archive('a-1').subscribe();
      const archive = http.expectOne(`${BASE}/a-1/archive`);
      expect(archive.request.method).toBe('POST');
      expect(archive.request.body).toEqual({});
      archive.flush({} as ProcessingActivityView);
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('a-1').subscribe();

      const req = http.expectOne(`${BASE}/a-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
