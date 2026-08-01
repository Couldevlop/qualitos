import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AiQmsService } from './ai-qms.service';
import {
  AiQmsView,
  ApproveAiQmsRequest,
  DraftAiQmsRequest,
  EditAiQmsRequest
} from './ai-qms.types';

/**
 * Système de management de la qualité des systèmes d'IA (AI Act, art. 17).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * DRAFT → APPROVED → IN_FORCE → SUPERSEDED, la sortie ARCHIVED, et l'invariant
 * qui donne sa valeur probatoire au registre : une seule version en vigueur par
 * référence, la précédente étant remplacée en désignant celle qui lui succède.
 * Les deux modes sont testés.
 */
describe('AiQmsService', () => {

  const AUTHOR = 'demo-user';
  const APPROVER = 'responsable-conformite';
  const BASE = `${environment.apiBaseUrl}/api/v1/ai-act/qms`;

  const draftReq = (over: Partial<DraftAiQmsRequest> = {}): DraftAiQmsRequest => ({
    reference: 'QMS-AI-INDUS',
    version: '2026.1',
    name: 'QMS IA — Détection de défauts en atelier',
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<EditAiQmsRequest> = {}): EditAiQmsRequest => ({
    name: 'QMS IA — V2026.2 révisée',
    ...over
  });

  const approveReq = (over: Partial<ApproveAiQmsRequest> = {}): ApproveAiQmsRequest => ({
    submittedByUserId: AUTHOR,
    approvedByUserId: APPROVER,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: AiQmsService;
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
      service = TestBed.inject(AiQmsService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les systèmes qualité pré-chargés et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).length).toBe(2);

      expect(run(service.list('IN_FORCE')).map(q => q.version)).toEqual(['2026.1']);
      expect(run(service.list('ARCHIVED'))).toEqual([]);
    }));

    it('résout par identifiant et par référence, avec repli si la clé est inconnue', fakeAsync(() => {
      expect(run(service.get('qms-2')).version).toBe('2026.2');
      // Deux versions partagent la référence : la première trouvée fait foi.
      expect(run(service.getByReference('QMS-AI-MEDPHARM')).id).toBe('qms-1');

      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('qms-inexistant')).id).toBe('qms-1');
      expect(run(service.getByReference('QMS-ABSENTE')).id).toBe('qms-1');
    }));

    // ---- Création ----------------------------------------------------------

    it('crée un brouillon en tête de liste, sans approbation ni mise en vigueur', fakeAsync(() => {
      const created = run(service.draft(draftReq()));

      expect(created.status).toBe('DRAFT');
      expect(created.approvedAt).toBeUndefined();
      expect(created.inForceFrom).toBeUndefined();
      expect(run(service.list())[0].reference).toBe('QMS-AI-INDUS');
    }));

    it('retient les neuf sections de l\'article 17 quand elles sont fournies', fakeAsync(() => {
      const created = run(service.draft(draftReq({
        regulatoryComplianceStrategy: 'Veille EU AI Office trimestrielle.',
        designControlDescription: 'Revues de conception aux jalons.',
        qualityControlDescription: 'KPI de dérive de modèle.',
        dataManagementDescription: 'Datasets versionnés, contrôle de biais.',
        riskManagementDescription: 'FMEA IA continu.',
        pmmDescription: 'Plan de surveillance post-marché art. 72.',
        regulatorCommunicationDescription: 'Point d\'entrée unique autorité notifiée.',
        resourceManagementDescription: 'Comité IA mensuel.',
        supplierMonitoringDescription: 'Audits annuels des fournisseurs de modèles.'
      })));

      expect(created.regulatoryComplianceStrategy).toContain('EU AI Office');
      expect(created.pmmDescription).toContain('art. 72');
      expect(created.supplierMonitoringDescription).toContain('Audits annuels');
    }));

    it('normalise en tableau vide les systèmes couverts non fournis', fakeAsync(() => {
      expect(run(service.draft(draftReq())).coveredAiSystemIds).toEqual([]);

      expect(run(service.draft(draftReq({
        version: '2026.9', coveredAiSystemIds: ['sys-1', 'sys-2']
      }))).coveredAiSystemIds).toEqual(['sys-1', 'sys-2']);
    }));

    // ---- Édition -----------------------------------------------------------

    it('remplace les champs du système qualité édité', fakeAsync(() => {
      const edited = run(service.edit('qms-2', editReq({
        riskManagementDescription: 'FMEA IA étendu aux risques fondamentaux.'
      })));

      expect(edited.name).toBe('QMS IA — V2026.2 révisée');
      expect(edited.riskManagementDescription).toContain('risques fondamentaux');
    }));

    it('remet à vide les systèmes couverts absents de l\'édition', fakeAsync(() => {
      run(service.edit('qms-2', editReq({ coveredAiSystemIds: ['sys-9'] })));
      expect(run(service.get('qms-2')).coveredAiSystemIds).toEqual(['sys-9']);

      run(service.edit('qms-2', editReq()));
      expect(run(service.get('qms-2')).coveredAiSystemIds).toEqual([]);
    }));

    it('édite sans effet de bord quand le système visé n\'existe pas', fakeAsync(() => {
      const before = run(service.get('qms-1')).name;

      run(service.edit('qms-inexistant', editReq()));

      expect(run(service.get('qms-1')).name).toBe(before);
    }));

    // ---- Approbation -------------------------------------------------------

    it('trace le soumissionnaire et l\'approbateur, qui sont deux rôles distincts', fakeAsync(() => {
      const approved = run(service.approve('qms-2', approveReq({
        approvalNotes: 'Conforme à l\'article 17, revue annuelle recommandée.'
      })));

      expect(approved.status).toBe('APPROVED');
      // Séparation des rôles : celui qui soumet n'est pas celui qui approuve.
      expect(approved.submittedByUserId).toBe(AUTHOR);
      expect(approved.approvedByUserId).toBe(APPROVER);
      expect(approved.approvalNotes).toContain('article 17');
      expect(approved.approvedAt).toBeTruthy();
    }));

    it('approuve sans effet de bord quand le système visé n\'existe pas', fakeAsync(() => {
      run(service.approve('qms-inexistant', approveReq()));

      expect(run(service.get('qms-2')).status).toBe('DRAFT');
    }));

    // ---- Mise en vigueur : une seule version par référence ------------------

    it('met en vigueur et date l\'entrée en application', fakeAsync(() => {
      const inForce = run(service.putInForce('qms-2'));

      expect(inForce.status).toBe('IN_FORCE');
      expect(inForce.inForceFrom).toBeTruthy();
    }));

    it('remplace la version en vigueur de la même référence, en la désignant', fakeAsync(() => {
      run(service.putInForce('qms-2'));

      const precedente = run(service.get('qms-1'));
      expect(precedente.status).toBe('SUPERSEDED');
      expect(precedente.supersededAt).toBeTruthy();
      // Le lien vers le successeur rend la chaîne des versions vérifiable.
      expect(precedente.supersededByQmsId).toBe('qms-2');
    }));

    it('ne touche pas aux systèmes qualité d\'une autre référence', fakeAsync(() => {
      const autre = run(service.draft(draftReq({ reference: 'QMS-AI-INDUS' })));
      run(service.putInForce(autre.id));

      // Référence distincte : la version en vigueur du groupe reste intacte.
      expect(run(service.get('qms-1')).status).toBe('IN_FORCE');
    }));

    it('met en vigueur sans effet de bord quand le système visé n\'existe pas', fakeAsync(() => {
      run(service.putInForce('qms-inexistant'));

      expect(run(service.get('qms-2')).status).toBe('DRAFT');
    }));

    // ---- Remplacement explicite / archivage ---------------------------------

    it('remplace explicitement une version en désignant celle qui lui succède', fakeAsync(() => {
      const superseded = run(service.supersede('qms-1', { supersededByQmsId: 'qms-2' }));

      expect(superseded.status).toBe('SUPERSEDED');
      expect(superseded.supersededByQmsId).toBe('qms-2');
      expect(superseded.supersededAt).toBeTruthy();
    }));

    it('archive une version avec son motif', fakeAsync(() => {
      const archived = run(service.archive('qms-2', { reason: 'Projet abandonné.' }));

      expect(archived.status).toBe('ARCHIVED');
      expect(archived.archiveReason).toBe('Projet abandonné.');
      expect(archived.archivedAt).toBeTruthy();
    }));

    it('remplace et archive sans effet de bord sur un système inconnu', fakeAsync(() => {
      run(service.supersede('qms-inexistant', { supersededByQmsId: 'qms-2' }));
      run(service.archive('qms-inexistant', { reason: 'r' }));

      expect(run(service.get('qms-1')).status).toBe('IN_FORCE');
      expect(run(service.get('qms-2')).status).toBe('DRAFT');
    }));

    // ---- Suppression --------------------------------------------------------

    it('supprime un système qualité, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('qms-2'));
      expect(run(service.list()).length).toBe(1);

      run(service.delete('qms-inexistant'));
      expect(run(service.list()).length).toBe(1);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: AiQmsService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(AiQmsService);
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

      service.list('IN_FORCE').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('IN_FORCE');
      filtered.flush([]);
    });

    it('lit une fiche par identifiant et par référence', () => {
      service.get('q-1').subscribe();
      const byId = http.expectOne(`${BASE}/q-1`);
      expect(byId.request.method).toBe('GET');
      byId.flush({} as AiQmsView);

      service.getByReference('QMS-AI-MEDPHARM').subscribe();
      const byRef = http.expectOne(r => r.url === `${BASE}/by-reference`);
      expect(byRef.request.params.get('reference')).toBe('QMS-AI-MEDPHARM');
      byRef.flush({} as AiQmsView);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = draftReq();
      service.draft(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as AiQmsView);

      const edit = editReq();
      service.edit('q-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/q-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as AiQmsView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      const approve = approveReq();
      const transitions: Array<[string, () => void, unknown]> = [
        ['approve', () => service.approve('q-1', approve).subscribe(), approve],
        // La mise en vigueur n'a pas de corps métier : objet vide plutôt que
        // `null`, un POST sans corps déclenchant des 415 sur certains serveurs.
        ['put-in-force', () => service.putInForce('q-1').subscribe(), {}],
        ['supersede',
          () => service.supersede('q-1', { supersededByQmsId: 'q-2' }).subscribe(),
          { supersededByQmsId: 'q-2' }],
        ['archive',
          () => service.archive('q-1', { reason: 'r' }).subscribe(),
          { reason: 'r' }]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/q-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as AiQmsView);
      });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('q-1').subscribe();

      const req = http.expectOne(`${BASE}/q-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
