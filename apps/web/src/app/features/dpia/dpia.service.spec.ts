import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { DpiaService } from './dpia.service';
import { CreateDpiaRequest, DpiaView, EditDpiaRequest } from './dpia.types';

/**
 * Analyses d'impact relatives à la protection des données (RGPD art. 35).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * DRAFT → IN_PROGRESS → DPO_REVIEW → APPROVED/REJECTED → ARCHIVED, le retour en
 * correction, et l'invariant de l'article 36 : un risque résiduel HIGH ou SEVERE
 * déclenche l'obligation de consultation préalable de l'autorité. Les deux modes
 * sont testés.
 */
describe('DpiaService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/gdpr/dpias`;

  const createReq = (over: Partial<CreateDpiaRequest> = {}): CreateDpiaRequest => ({
    reference: 'NOUVELLE-AIPD-900',
    title: 'Portail de prise de rendez-vous en ligne',
    initialRiskLevel: 'MEDIUM',
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<EditDpiaRequest> = {}): EditDpiaRequest => ({
    title: 'Consolidation CRM multi-tenant — v3',
    overallRiskLevel: 'HIGH',
    consultationRequired: true,
    ...over
  });

  // ------------------------------------------------------------------------
  // Règle métier pure (art. 36)
  // ------------------------------------------------------------------------
  describe('obligation de consultation préalable (art. 36)', () => {
    it('ne se déclenche qu\'au-delà d\'un risque résiduel élevé', () => {
      expect(DpiaService.requiresPriorConsultation('LOW')).toBeFalse();
      expect(DpiaService.requiresPriorConsultation('MEDIUM')).toBeFalse();
      expect(DpiaService.requiresPriorConsultation('HIGH')).toBeTrue();
      expect(DpiaService.requiresPriorConsultation('SEVERE')).toBeTrue();
    });
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: DpiaService;
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
      service = TestBed.inject(DpiaService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les analyses pré-chargées et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).length).toBe(3);

      expect(run(service.list('APPROVED')).map(d => d.reference)).toEqual(['TELEMEDICINE-V1']);
      expect(run(service.list('ARCHIVED'))).toEqual([]);
    }));

    it('ne remonte en consultation à mener que les analyses vivantes qui l\'exigent', fakeAsync(() => {
      const rows = run(service.requiringConsultation());

      // dpia-3 n'exige pas de consultation ; les deux autres sont APPROVED / DPO_REVIEW.
      expect(rows.map(d => d.reference).sort())
        .toEqual(['EMPLOYEE-AI-MONITORING', 'TELEMEDICINE-V1']);
    }));

    it('sort une analyse archivée de la liste des consultations à mener', fakeAsync(() => {
      run(service.archive('dpia-1'));

      expect(run(service.requiringConsultation()).map(d => d.reference))
        .toEqual(['EMPLOYEE-AI-MONITORING']);
    }));

    it('résout une analyse par identifiant et par référence', fakeAsync(() => {
      expect(run(service.get('dpia-2')).reference).toBe('EMPLOYEE-AI-MONITORING');
      expect(run(service.getByReference('CRM-CONSOLIDATION-V2')).id).toBe('dpia-3');
    }));

    it('retombe sur la première analyse quand la clé demandée est inconnue', fakeAsync(() => {
      // Repli assumé du mode démo : les écrans restent utilisables sans backend,
      // quel que soit l'identifiant présent dans l'URL.
      expect(run(service.get('dpia-inexistante')).id).toBe('dpia-1');
      expect(run(service.getByReference('REFERENCE-ABSENTE')).id).toBe('dpia-1');
    }));

    // ---- Création ----------------------------------------------------------

    it('crée un brouillon en tête de liste et en déduit l\'obligation de consultation', fakeAsync(() => {
      const created = run(service.create(createReq({ initialRiskLevel: 'SEVERE' })));

      expect(created.status).toBe('DRAFT');
      expect(created.overallRiskLevel).toBe('SEVERE');
      expect(created.consultationRequired).toBeTrue();
      expect(run(service.list())[0].reference).toBe('NOUVELLE-AIPD-900');
    }));

    it('n\'impose pas de consultation pour un risque modéré', fakeAsync(() => {
      expect(run(service.create(createReq())).consultationRequired).toBeFalse();
    }));

    it('normalise en tableau vide les traitements liés non fournis', fakeAsync(() => {
      expect(run(service.create(createReq())).linkedProcessingActivityIds).toEqual([]);

      expect(run(service.create(createReq({
        reference: 'AVEC-LIENS-901', linkedProcessingActivityIds: ['ropa-1', 'ropa-2']
      }))).linkedProcessingActivityIds).toEqual(['ropa-1', 'ropa-2']);
    }));

    // ---- Édition -----------------------------------------------------------

    it('remplace les champs de l\'analyse éditée', fakeAsync(() => {
      const edited = run(service.edit('dpia-3', editReq({
        risksToRightsAndFreedoms: 'Croisement de bases jusqu\'ici cloisonnées.',
        mitigationMeasures: 'Cloisonnement logique par filiale + journalisation des accès.'
      })));

      expect(edited.title).toBe('Consolidation CRM multi-tenant — v3');
      expect(edited.overallRiskLevel).toBe('HIGH');
      expect(edited.consultationRequired).toBeTrue();
      expect(edited.mitigationMeasures).toContain('Cloisonnement logique');
    }));

    it('remet à vide les traitements liés absents de l\'édition', fakeAsync(() => {
      run(service.edit('dpia-1', editReq({ linkedProcessingActivityIds: ['ropa-9'] })));
      expect(run(service.get('dpia-1')).linkedProcessingActivityIds).toEqual(['ropa-9']);

      run(service.edit('dpia-1', editReq()));
      expect(run(service.get('dpia-1')).linkedProcessingActivityIds).toEqual([]);
    }));

    it('édite sans effet de bord quand l\'analyse visée n\'existe pas', fakeAsync(() => {
      const before = run(service.get('dpia-1')).title;

      run(service.edit('dpia-inexistante', editReq()));

      expect(run(service.get('dpia-1')).title).toBe(before);
    }));

    // ---- Cycle de vie ------------------------------------------------------

    it('démarre l\'instruction et retient le responsable désigné', fakeAsync(() => {
      const started = run(service.start('dpia-3', { handledByUserId: 'responsable-aipd' }));

      expect(started.status).toBe('IN_PROGRESS');
      expect(started.handledByUserId).toBe('responsable-aipd');
    }));

    it('renvoie en correction puis resoumet au délégué', fakeAsync(() => {
      run(service.start('dpia-3', { handledByUserId: AUTHOR }));

      expect(run(service.returnToDraft('dpia-3')).status).toBe('DRAFT');
      expect(run(service.submitToDpo('dpia-3')).status).toBe('DPO_REVIEW');
    }));

    it('enregistre l\'avis favorable du délégué et ouvre la période de validité', fakeAsync(() => {
      const approved = run(service.approve('dpia-2', {
        dpoUserId: 'dpo-1', dpoOpinion: 'Avis favorable sous réserve d\'anonymisation.'
      }));

      expect(approved.status).toBe('APPROVED');
      expect(approved.dpoUserId).toBe('dpo-1');
      expect(approved.dpoOpinion).toContain('anonymisation');
      expect(approved.dpoOpinionAt).toBeTruthy();
      expect(approved.effectiveFrom).toBeTruthy();
    }));

    it('enregistre l\'avis défavorable sans ouvrir de période de validité', fakeAsync(() => {
      const rejected = run(service.reject('dpia-2', {
        dpoUserId: 'dpo-1', dpoOpinion: 'Surveillance disproportionnée des salariés.'
      }));

      expect(rejected.status).toBe('REJECTED');
      expect(rejected.dpoOpinionAt).toBeTruthy();
      expect(rejected.effectiveFrom).toBeUndefined();
    }));

    it('clôt la période de validité à l\'archivage', fakeAsync(() => {
      const archived = run(service.archive('dpia-1'));

      expect(archived.status).toBe('ARCHIVED');
      expect(archived.effectiveTo).toBeTruthy();
    }));

    it('laisse le magasin intact quand une transition vise une analyse inconnue', fakeAsync(() => {
      run(service.approve('dpia-inexistante', { dpoUserId: 'dpo-1', dpoOpinion: 'o' }));
      run(service.reject('dpia-inexistante', { dpoUserId: 'dpo-1', dpoOpinion: 'o' }));
      run(service.archive('dpia-inexistante'));

      expect(run(service.list()).map(d => d.status))
        .toEqual(['APPROVED', 'DPO_REVIEW', 'DRAFT']);
    }));

    // ---- Suppression -------------------------------------------------------

    it('supprime une analyse, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('dpia-3'));
      expect(run(service.list()).length).toBe(2);

      run(service.delete('dpia-inexistante'));
      expect(run(service.list()).length).toBe(2);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: DpiaService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(DpiaService);
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

      service.list('DPO_REVIEW').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('DPO_REVIEW');
      filtered.flush([]);
    });

    it('interroge les consultations à mener sur leur propre chemin', () => {
      service.requiringConsultation().subscribe();

      const req = http.expectOne(`${BASE}/requiring-consultation`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('lit une fiche par identifiant et par référence', () => {
      service.get('d-1').subscribe();
      const byId = http.expectOne(`${BASE}/d-1`);
      expect(byId.request.method).toBe('GET');
      byId.flush({} as DpiaView);

      service.getByReference('TELEMEDICINE-V1').subscribe();
      const byRef = http.expectOne(r => r.url === `${BASE}/by-reference`);
      expect(byRef.request.params.get('reference')).toBe('TELEMEDICINE-V1');
      byRef.flush({} as DpiaView);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = createReq();
      service.create(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as DpiaView);

      const edit = editReq();
      service.edit('d-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/d-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as DpiaView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      const opinion = { dpoUserId: 'dpo-1', dpoOpinion: 'o' };
      const transitions: Array<[string, () => void, unknown]> = [
        ['start', () => service.start('d-1', { handledByUserId: AUTHOR }).subscribe(),
          { handledByUserId: AUTHOR }],
        // Les transitions sans corps postent un objet vide plutôt que `null` :
        // un POST sans corps déclenche des 415 sur certains serveurs.
        ['return-to-draft', () => service.returnToDraft('d-1').subscribe(), {}],
        ['submit-to-dpo', () => service.submitToDpo('d-1').subscribe(), {}],
        ['archive', () => service.archive('d-1').subscribe(), {}],
        ['approve', () => service.approve('d-1', opinion).subscribe(), opinion],
        ['reject', () => service.reject('d-1', opinion).subscribe(), opinion]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/d-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as DpiaView);
      });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('d-1').subscribe();

      const req = http.expectOne(`${BASE}/d-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
