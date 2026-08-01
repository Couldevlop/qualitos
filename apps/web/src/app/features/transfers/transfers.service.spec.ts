import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { TransfersService, requiresDerogationJustification } from './transfers.service';
import { CreateTransferRequest, EditTransferRequest, TransferView } from './transfers.types';

/**
 * Transferts de données hors Union européenne (RGPD chapitre V).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * DRAFT → ACTIVE ⇄ SUSPENDED → TERMINATED, et la règle qui distingue les
 * mécanismes : seule la dérogation de l'article 49 — voie d'exception, non
 * systématisable — exige une justification écrite. Les deux modes sont testés.
 */
describe('TransfersService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/gdpr/cross-border-transfers`;

  const createReq = (over: Partial<CreateTransferRequest> = {}): CreateTransferRequest => ({
    reference: 'SUPPORT-N3-CANADA',
    recipientName: 'HelpDesk North Inc.',
    mechanism: 'ADEQUACY_DECISION',
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<EditTransferRequest> = {}): EditTransferRequest => ({
    recipientName: 'HelpDesk North Inc. — entité canadienne',
    mechanism: 'STANDARD_CONTRACTUAL_CLAUSES',
    ...over
  });

  // ------------------------------------------------------------------------
  // Règle métier pure (chapitre V)
  // ------------------------------------------------------------------------
  describe('exigence de justification', () => {
    it('ne vise que la dérogation de l\'article 49', () => {
      // Les autres mécanismes reposent sur un cadre général — décision
      // d'adéquation, clauses types, règles d'entreprise contraignantes — qui
      // porte lui-même la garantie. L'article 49 est une voie d'exception : elle
      // se justifie au cas par cas.
      expect(requiresDerogationJustification('DEROGATION_ART49')).toBeTrue();
      expect(requiresDerogationJustification('ADEQUACY_DECISION')).toBeFalse();
      expect(requiresDerogationJustification('STANDARD_CONTRACTUAL_CLAUSES')).toBeFalse();
      expect(requiresDerogationJustification('BINDING_CORPORATE_RULES')).toBeFalse();
      expect(requiresDerogationJustification('CODE_OF_CONDUCT')).toBeFalse();
    });
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: TransfersService;
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
      service = TestBed.inject(TransfersService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les transferts pré-chargés et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).length).toBe(3);

      expect(run(service.list('DRAFT')).map(t => t.reference)).toEqual(['URGENT-MEDIC-EVAC-INDIA']);
      expect(run(service.list('TERMINATED'))).toEqual([]);
    }));

    it('résout un transfert par identifiant, avec repli si inconnu', fakeAsync(() => {
      expect(run(service.get('cbt-2')).reference).toBe('BACKUP-UK-OFFSITE');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('cbt-inexistant')).id).toBe('cbt-1');
    }));

    // ---- Création ----------------------------------------------------------

    it('crée un brouillon en tête de registre', fakeAsync(() => {
      const created = run(service.create(createReq()));

      expect(created.status).toBe('DRAFT');
      expect(created.effectiveFrom).toBeUndefined();
      expect(run(service.list())[0].reference).toBe('SUPPORT-N3-CANADA');
    }));

    it('normalise en tableaux vides les listes non fournies', fakeAsync(() => {
      const created = run(service.create(createReq()));

      expect(created.destinationCountries).toEqual([]);
      expect(created.dataCategories).toEqual([]);
      expect(created.linkedProcessingActivityIds).toEqual([]);
      expect(created.linkedProcessorAgreementIds).toEqual([]);
    }));

    it('retient la justification quand le transfert relève de l\'article 49', fakeAsync(() => {
      const created = run(service.create(createReq({
        reference: 'EVAC-URGENCE',
        mechanism: 'DEROGATION_ART49',
        derogationJustification: 'Art. 49.1.f — sauvegarde des intérêts vitaux de la personne.',
        destinationCountries: ['IN']
      })));

      expect(requiresDerogationJustification(created.mechanism)).toBeTrue();
      expect(created.derogationJustification).toContain('49.1.f');
    }));

    it('rattache le transfert au registre et aux contrats de sous-traitance', fakeAsync(() => {
      // Le chapitre V ne se lit pas isolément : un transfert doit se raccrocher
      // au traitement qu'il sert et au contrat qui l'encadre.
      const created = run(service.create(createReq({
        reference: 'CRM-LIE',
        linkedProcessingActivityIds: ['ropa-2'],
        linkedProcessorAgreementIds: ['pa-1']
      })));

      expect(created.linkedProcessingActivityIds).toEqual(['ropa-2']);
      expect(created.linkedProcessorAgreementIds).toEqual(['pa-1']);
    }));

    // ---- Édition -----------------------------------------------------------

    it('remplace les champs du transfert édité', fakeAsync(() => {
      const edited = run(service.edit('cbt-3', editReq({
        safeguardsDescription: 'Clauses types module 2, annexes complétées.'
      })));

      expect(edited.recipientName).toContain('entité canadienne');
      expect(edited.mechanism).toBe('STANDARD_CONTRACTUAL_CLAUSES');
      expect(edited.safeguardsDescription).toContain('module 2');
    }));

    it('remet à vide les listes absentes de l\'édition', fakeAsync(() => {
      run(service.edit('cbt-1', editReq({ destinationCountries: ['US'], dataCategories: ['contact'] })));
      expect(run(service.get('cbt-1')).destinationCountries).toEqual(['US']);

      run(service.edit('cbt-1', editReq()));
      const after = run(service.get('cbt-1'));
      expect(after.destinationCountries).toEqual([]);
      expect(after.dataCategories).toEqual([]);
      expect(after.linkedProcessingActivityIds).toEqual([]);
      expect(after.linkedProcessorAgreementIds).toEqual([]);
    }));

    it('édite sans effet de bord quand le transfert visé n\'existe pas', fakeAsync(() => {
      const before = run(service.get('cbt-1')).recipientName;

      run(service.edit('cbt-inexistant', editReq()));

      expect(run(service.get('cbt-1')).recipientName).toBe(before);
    }));

    // ---- Cycle de vie ------------------------------------------------------

    it('ouvre la période d\'effet à la première activation', fakeAsync(() => {
      const activated = run(service.activate('cbt-3'));

      expect(activated.status).toBe('ACTIVE');
      expect(activated.effectiveFrom).toBeTruthy();
    }));

    it('conserve la date d\'effet initiale lors d\'une reprise après suspension', fakeAsync(() => {
      const first = run(service.activate('cbt-3')).effectiveFrom;
      run(service.suspend('cbt-3', { reason: 'Audit du destinataire en cours.' }));

      const resumed = run(service.activate('cbt-3'));

      // Le transfert n'a pas recommencé : sa date d'origine reste opposable.
      expect(resumed.effectiveFrom).toBe(first!);
    }));

    it('suspend le transfert en conservant le motif', fakeAsync(() => {
      const suspended = run(service.suspend('cbt-1', {
        reason: 'Invalidation du cadre par la CJUE.'
      }));

      expect(suspended.status).toBe('SUSPENDED');
      expect(suspended.suspendReason).toContain('CJUE');
    }));

    it('met fin au transfert et clôt sa période d\'effet', fakeAsync(() => {
      const terminated = run(service.terminate('cbt-1', {
        reason: 'Changement de prestataire.'
      }));

      expect(terminated.status).toBe('TERMINATED');
      expect(terminated.terminationReason).toContain('Changement de prestataire');
      expect(terminated.effectiveTo).toBeTruthy();
    }));

    it('laisse le magasin intact quand une transition vise un transfert inconnu', fakeAsync(() => {
      run(service.activate('cbt-inexistant'));
      run(service.suspend('cbt-inexistant', { reason: 'r' }));
      run(service.terminate('cbt-inexistant', { reason: 'r' }));

      expect(run(service.list()).map(t => t.status)).toEqual(['ACTIVE', 'ACTIVE', 'DRAFT']);
    }));

    // ---- Suppression -------------------------------------------------------

    it('supprime un transfert, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('cbt-3'));
      expect(run(service.list()).length).toBe(2);

      run(service.delete('cbt-inexistant'));
      expect(run(service.list()).length).toBe(2);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: TransfersService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(TransfersService);
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

      service.list('SUSPENDED').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('status')).toBe('SUSPENDED');
      filtered.flush([]);
    });

    it('lit une fiche par identifiant', () => {
      service.get('t-1').subscribe();

      const req = http.expectOne(`${BASE}/t-1`);
      expect(req.request.method).toBe('GET');
      req.flush({} as TransferView);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = createReq();
      service.create(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as TransferView);

      const edit = editReq();
      service.edit('t-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/t-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as TransferView);
    });

    it('poste chaque transition sur son propre sous-chemin', () => {
      // L'activation n'a pas de corps métier : objet vide plutôt que `null`, un
      // POST sans corps déclenchant des 415 sur certains serveurs.
      const transitions: Array<[string, () => void, unknown]> = [
        ['activate', () => service.activate('t-1').subscribe(), {}],
        ['suspend', () => service.suspend('t-1', { reason: 'r' }).subscribe(), { reason: 'r' }],
        ['terminate', () => service.terminate('t-1', { reason: 'r' }).subscribe(), { reason: 'r' }]
      ];

      transitions.forEach(([path, call, body]) => {
        call();
        const req = http.expectOne(`${BASE}/t-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual(body);
        req.flush({} as TransferView);
      });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('t-1').subscribe();

      const req = http.expectOne(`${BASE}/t-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
