import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PaService } from './pa.service';
import { CreatePaRequest, EditPaRequest, PaView } from './pa.types';

/**
 * Contrats de sous-traitance (RGPD art. 28).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue le cycle
 * DRAFT → ACTIVE → TERMINATED, et surtout la péremption : un contrat actif dont
 * l'échéance est passée bascule de lui-même à la lecture. Un DPA périmé qui
 * continuerait d'apparaître comme actif, c'est un traitement sans base
 * contractuelle qui passe inaperçu. Les deux modes sont testés.
 */
describe('PaService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/gdpr/processor-agreements`;
  const DAY = 86400000;
  const inDays = (d: number) => new Date(Date.now() + d * DAY).toISOString();

  const createReq = (over: Partial<CreatePaRequest> = {}): CreatePaRequest => ({
    reference: 'DPA-CRM-SAAS',
    processorName: 'CRMCloud SAS',
    servicesDescription: 'Hébergement du CRM commercial.',
    breachNotificationCommitmentHours: 48,
    auditRights: true,
    createdByUserId: AUTHOR,
    ...over
  });

  const editReq = (over: Partial<EditPaRequest> = {}): EditPaRequest => ({
    processorName: 'CRMCloud SAS — entité française',
    servicesDescription: 'Hébergement du CRM commercial, périmètre France.',
    breachNotificationCommitmentHours: 24,
    auditRights: true,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: PaService;
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
      service = TestBed.inject(PaService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les contrats pré-chargés et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.list()).length).toBe(3);

      expect(run(service.list('ACTIVE')).map(p => p.reference)).toEqual(['DPA-AWS-EU']);
      expect(run(service.list('EXPIRED')).map(p => p.reference)).toEqual(['DPA-EMAIL-MARKETING']);
      expect(run(service.list('TERMINATED'))).toEqual([]);
    }));

    it('résout un contrat par identifiant et par référence', fakeAsync(() => {
      expect(run(service.get('pa-2')).reference).toBe('DPA-PAYROLL-FR');
      expect(run(service.getByReference('DPA-AWS-EU')).id).toBe('pa-1');
    }));

    it('retombe sur le premier contrat quand la clé est inconnue', fakeAsync(() => {
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('pa-inexistant')).id).toBe('pa-1');
      expect(run(service.getByReference('DPA-ABSENTE')).id).toBe('pa-1');
    }));

    // ---- Péremption automatique ---------------------------------------------

    it('fait basculer d\'elle-même une échéance dépassée à la lecture', fakeAsync(() => {
      const created = run(service.create(createReq({ expirationDate: inDays(-1) })));
      run(service.activate(created.id));

      // La bascule se fait à la lecture : aucun balayage manuel n'est requis
      // pour qu'un contrat périmé cesse d'être présenté comme actif.
      expect(run(service.get(created.id)).status).toBe('EXPIRED');
    }));

    it('laisse actif un contrat encore valide', fakeAsync(() => {
      expect(run(service.get('pa-1')).status).toBe('ACTIVE');
    }));

    it('ne périme pas un contrat sans échéance', fakeAsync(() => {
      const created = run(service.create(createReq()));
      run(service.activate(created.id));

      // Un contrat à durée indéterminée reste actif : l'absence d'échéance
      // n'est pas une échéance dépassée.
      expect(run(service.get(created.id)).status).toBe('ACTIVE');
    }));

    // ---- Balayage des échéances ----------------------------------------------

    it('bascule les contrats échus et rend leur nombre', fakeAsync(() => {
      const premier = run(service.create(createReq({
        reference: 'DPA-A', expirationDate: inDays(-2)
      })));
      const second = run(service.create(createReq({
        reference: 'DPA-B', expirationDate: inDays(-3)
      })));
      run(service.activate(premier.id));
      run(service.activate(second.id));

      expect(run(service.expireDue()).expired).toBe(2);
      expect(run(service.list('EXPIRED')).length).toBe(3);
    }));

    it('ne compte pas deux fois un contrat déjà périmé', fakeAsync(() => {
      const created = run(service.create(createReq({ expirationDate: inDays(-1) })));
      run(service.activate(created.id));

      expect(run(service.expireDue()).expired).toBe(1);
      expect(run(service.expireDue()).expired).toBe(0);
    }));

    it('borne le nombre de contrats traités par balayage', fakeAsync(() => {
      const premier = run(service.create(createReq({
        reference: 'DPA-A', expirationDate: inDays(-2)
      })));
      const second = run(service.create(createReq({
        reference: 'DPA-B', expirationDate: inDays(-3)
      })));
      run(service.activate(premier.id));
      run(service.activate(second.id));

      expect(run(service.expireDue(1)).expired).toBe(1);
    }));

    it('ignore les contrats non actifs lors du balayage', fakeAsync(() => {
      // Un brouillon dont l'échéance est passée n'a jamais produit d'effet :
      // le faire basculer en EXPIRED laisserait croire à un contrat rompu.
      run(service.create(createReq({ expirationDate: inDays(-1) })));

      expect(run(service.expireDue()).expired).toBe(0);
    }));

    // ---- Création -------------------------------------------------------------

    it('crée un brouillon en tête de liste', fakeAsync(() => {
      const created = run(service.create(createReq()));

      expect(created.status).toBe('DRAFT');
      expect(run(service.list())[0].reference).toBe('DPA-CRM-SAAS');
    }));

    it('normalise en tableaux vides les listes non fournies', fakeAsync(() => {
      const created = run(service.create(createReq()));

      expect(created.subProcessorCategories).toEqual([]);
      expect(created.linkedProcessingActivityIds).toEqual([]);
      expect(created.thirdCountryTransfers).toEqual([]);
    }));

    it('retient les sous-traitants ultérieurs et les transferts hors UE', fakeAsync(() => {
      const created = run(service.create(createReq({
        subProcessorCategories: ['CDN', 'Support niveau 3'],
        thirdCountryTransfers: ['US'],
        transferSafeguards: 'Clauses contractuelles types 2021/914 + mesures supplémentaires.'
      })));

      expect(created.subProcessorCategories).toEqual(['CDN', 'Support niveau 3']);
      expect(created.thirdCountryTransfers).toEqual(['US']);
      expect(created.transferSafeguards).toContain('2021/914');
    }));

    // ---- Édition ---------------------------------------------------------------

    it('remplace les champs du contrat édité', fakeAsync(() => {
      const edited = run(service.edit('pa-2', editReq({
        securityMeasures: 'Chiffrement au repos et en transit.'
      })));

      expect(edited.processorName).toContain('entité française');
      expect(edited.breachNotificationCommitmentHours).toBe(24);
      expect(edited.securityMeasures).toContain('Chiffrement');
    }));

    it('remet à vide les listes absentes de l\'édition', fakeAsync(() => {
      run(service.edit('pa-1', editReq({ subProcessorCategories: ['CDN'] })));
      expect(run(service.get('pa-1')).subProcessorCategories).toEqual(['CDN']);

      run(service.edit('pa-1', editReq()));
      const after = run(service.get('pa-1'));
      expect(after.subProcessorCategories).toEqual([]);
      expect(after.thirdCountryTransfers).toEqual([]);
    }));

    it('édite sans effet de bord quand le contrat visé n\'existe pas', fakeAsync(() => {
      const before = run(service.get('pa-1')).processorName;

      run(service.edit('pa-inexistant', editReq()));

      expect(run(service.get('pa-1')).processorName).toBe(before);
    }));

    // ---- Cycle de vie -----------------------------------------------------------

    it('active le contrat et lui donne une date d\'effet à défaut', fakeAsync(() => {
      const created = run(service.create(createReq()));

      const activated = run(service.activate(created.id));

      expect(activated.status).toBe('ACTIVE');
      expect(activated.effectiveFrom).toBeTruthy();
    }));

    it('conserve la date d\'effet négociée quand elle est renseignée', fakeAsync(() => {
      const created = run(service.create(createReq({ effectiveFrom: '2026-01-15' })));

      expect(run(service.activate(created.id)).effectiveFrom).toBe('2026-01-15');
    }));

    it('résilie le contrat en conservant le motif', fakeAsync(() => {
      const terminated = run(service.terminate('pa-1', {
        reason: 'Changement de prestataire d\'hébergement.'
      }));

      expect(terminated.status).toBe('TERMINATED');
      expect(terminated.terminationReason).toContain('Changement de prestataire');
    }));

    it('laisse le magasin intact quand une transition vise un contrat inconnu', fakeAsync(() => {
      run(service.activate('pa-inexistant'));
      run(service.terminate('pa-inexistant', { reason: 'r' }));

      expect(run(service.list()).map(p => p.status))
        .toEqual(['ACTIVE', 'DRAFT', 'EXPIRED']);
    }));

    // ---- Suppression --------------------------------------------------------------

    it('supprime un contrat, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('pa-2'));
      expect(run(service.list()).length).toBe(2);

      run(service.delete('pa-inexistant'));
      expect(run(service.list()).length).toBe(2);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: PaService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(PaService);
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
      service.get('p-1').subscribe();
      const byId = http.expectOne(`${BASE}/p-1`);
      expect(byId.request.method).toBe('GET');
      byId.flush({} as PaView);

      service.getByReference('DPA-AWS-EU').subscribe();
      const byRef = http.expectOne(r => r.url === `${BASE}/by-reference`);
      expect(byRef.request.params.get('reference')).toBe('DPA-AWS-EU');
      byRef.flush({} as PaView);
    });

    it('crée en POST sur la collection et édite en PUT sur la ressource', () => {
      const body = createReq();
      service.create(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as PaView);

      const edit = editReq();
      service.edit('p-1', edit).subscribe();
      const put = http.expectOne(`${BASE}/p-1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(edit);
      put.flush({} as PaView);
    });

    it('poste l\'activation sans corps et la résiliation avec son motif', () => {
      service.activate('p-1').subscribe();
      const activate = http.expectOne(`${BASE}/p-1/activate`);
      expect(activate.request.method).toBe('POST');
      expect(activate.request.body).toEqual({});
      activate.flush({} as PaView);

      service.terminate('p-1', { reason: 'r' }).subscribe();
      const terminate = http.expectOne(`${BASE}/p-1/terminate`);
      expect(terminate.request.method).toBe('POST');
      expect(terminate.request.body).toEqual({ reason: 'r' });
      terminate.flush({} as PaView);
    });

    it('déclenche le balayage des échéances avec sa borne', () => {
      service.expireDue().subscribe();
      const byDefault = http.expectOne(r => r.url === `${BASE}/expire-due`);
      expect(byDefault.request.method).toBe('POST');
      expect(byDefault.request.params.get('limit')).toBe('200');
      byDefault.flush({ expired: 0 });

      service.expireDue(25).subscribe();
      const bounded = http.expectOne(r => r.url === `${BASE}/expire-due`);
      expect(bounded.request.params.get('limit')).toBe('25');
      bounded.flush({ expired: 0 });
    });

    it('supprime en DELETE sur la ressource', () => {
      service.delete('p-1').subscribe();

      const req = http.expectOne(`${BASE}/p-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
