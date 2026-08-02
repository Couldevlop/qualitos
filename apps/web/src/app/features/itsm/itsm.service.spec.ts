import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ItsmService } from './itsm.service';
import { ConnectionResponse, CreateConnectionRequest, MappingPage, SyncReport } from './itsm.types';

/**
 * Connecteurs ITSM (§13.3).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Un invariant domine : le SECRET
 * d'une connexion n'est jamais conservé ni restitué côté client (OWASP A02) —
 * le serveur seul le chiffre et le fait tourner. Les deux modes sont testés.
 */
describe('ItsmService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/itsm`;

  const createReq = (over: Partial<CreateConnectionRequest> = {}): CreateConnectionRequest => ({
    name: 'ServiceNow Recette',
    provider: 'SERVICENOW',
    baseUrl: 'https://recette.service-now.com',
    secret: 'un-secret-tres-confidentiel',
    createdBy: AUTHOR,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: ItsmService;
    let http: HttpTestingController;
    let prevMock: boolean;

    /** Les réponses simulées sont différées (`delay`) : on déroule le temps virtuel. */
    function run<T>(source: Observable<T>): T {
      let value: T | undefined;
      source.subscribe(v => (value = v));
      tick(600);
      return value as T;
    }

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(ItsmService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les connexions pré-chargées', fakeAsync(() => {
      const page = run(service.list());

      expect(page.totalElements).toBe(3);
      expect(page.content.map(c => c.provider)).toEqual(['SERVICENOW', 'JIRA_SM', 'JIRA_SM']);
    }));

    it('résout une connexion par identifiant, avec repli si inconnue', fakeAsync(() => {
      expect(run(service.get('itsm-3')).status).toBe('DISABLED_ON_ERRORS');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('itsm-inexistante')).id).toBe('itsm-1');
    }));

    // ---- Secret jamais conservé ---------------------------------------------

    it('ne conserve jamais le secret de la connexion créée', fakeAsync(() => {
      const created = run(service.create(createReq()));

      // Le secret ne doit apparaître dans AUCUN champ restitué : le serveur
      // seul le chiffre et le fait tourner (OWASP A02).
      expect(JSON.stringify(created)).not.toContain('un-secret-tres-confidentiel');
      expect(created.name).toBe('ServiceNow Recette');
    }));

    it('écarte le secret d\'une mise à jour avant de l\'enregistrer localement', fakeAsync(() => {
      const updated = run(service.update('itsm-1', {
        name: 'ServiceNow Production — v2', secret: 'nouveau-secret'
      }));

      expect(updated.name).toBe('ServiceNow Production — v2');
      expect(JSON.stringify(updated)).not.toContain('nouveau-secret');
    }));

    // ---- Création / mise à jour ------------------------------------------------

    it('crée la connexion en tête de liste, avec sa collection de mappages', fakeAsync(() => {
      const created = run(service.create(createReq()));

      expect(run(service.list()).content[0].id).toBe(created.id);
      expect(run(service.listMappings(created.id)).content).toEqual([]);
    }));

    it('met à jour sans effet de bord quand la connexion visée n\'existe pas', fakeAsync(() => {
      const before = run(service.get('itsm-1')).name;

      run(service.update('itsm-inexistante', { name: 'usurpé' }));

      expect(run(service.get('itsm-1')).name).toBe(before);
    }));

    it('supprime la connexion et ses mappages, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.delete('itsm-1'));
      expect(run(service.list()).totalElements).toBe(2);
      expect(run(service.listMappings('itsm-1')).content).toEqual([]);

      run(service.delete('itsm-inexistante'));
      expect(run(service.list()).totalElements).toBe(2);
    }));

    // ---- Synchronisation -------------------------------------------------------

    it('remet le compteur d\'échecs à zéro après une synchronisation réussie', fakeAsync(() => {
      expect(run(service.get('itsm-3')).consecutiveFailures).toBe(5);

      const report = run(service.sync('itsm-3'));

      expect(report.connectionId).toBe('itsm-3');
      expect(report.totalFetched).toBe(report.newImports + report.alreadyKnown);
      const after = run(service.get('itsm-3'));
      // Une synchronisation réussie doit lever le compteur qui a désactivé la
      // connexion, sinon elle resterait bloquée sur un échec passé.
      expect(after.consecutiveFailures).toBe(0);
      expect(after.lastSuccessAt).toBeTruthy();
    }));

    it('rend un rapport même pour une connexion inconnue', fakeAsync(() => {
      const report = run(service.sync('itsm-inexistante'));

      expect(report.connectionId).toBe('itsm-inexistante');
      expect(report.ranAt).toBeTruthy();
    }));

    // ---- Mappages ---------------------------------------------------------------

    it('filtre les mappages par connexion, ou les rend tous', fakeAsync(() => {
      const forOne = run(service.listMappings('itsm-1'));
      const all = run(service.listMappings());

      expect(forOne.totalElements).toBeGreaterThan(0);
      expect(all.totalElements).toBeGreaterThanOrEqual(forOne.totalElements);
      expect(run(service.listMappings('connexion-inconnue')).content).toEqual([]);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: ItsmService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(ItsmService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('pagine la liste des connexions', () => {
      service.list().subscribe();
      const byDefault = http.expectOne(r => r.url === `${BASE}/connections`);
      expect(byDefault.request.params.get('page')).toBe('0');
      expect(byDefault.request.params.get('size')).toBe('20');
      byDefault.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      service.list(2, 50).subscribe();
      const paged = http.expectOne(r => r.url === `${BASE}/connections`);
      expect(paged.request.params.get('page')).toBe('2');
      expect(paged.request.params.get('size')).toBe('50');
      paged.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });
    });

    it('lit une connexion, la crée en POST, la met à jour en PATCH et la supprime', () => {
      service.get('c-1').subscribe();
      const get = http.expectOne(`${BASE}/connections/c-1`);
      expect(get.request.method).toBe('GET');
      get.flush({} as ConnectionResponse);

      const body = createReq();
      service.create(body).subscribe();
      const post = http.expectOne(`${BASE}/connections`);
      expect(post.request.method).toBe('POST');
      // Le secret transite bien vers le SERVEUR — c'est lui qui le chiffre ;
      // ce qui est proscrit, c'est de le conserver côté client.
      expect(post.request.body).toEqual(body);
      post.flush({} as ConnectionResponse);

      service.update('c-1', { name: 'n' }).subscribe();
      const patch = http.expectOne(`${BASE}/connections/c-1`);
      expect(patch.request.method).toBe('PATCH');
      expect(patch.request.body).toEqual({ name: 'n' });
      patch.flush({} as ConnectionResponse);

      service.delete('c-1').subscribe();
      const del = http.expectOne(`${BASE}/connections/c-1`);
      expect(del.request.method).toBe('DELETE');
      del.flush(null);
    });

    it('déclenche la synchronisation en POST sur la connexion', () => {
      service.sync('c-1').subscribe();

      const req = http.expectOne(`${BASE}/connections/c-1/sync`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({} as SyncReport);
    });

    it('n\'ajoute le filtre de connexion aux mappages que s\'il est fourni', () => {
      service.listMappings().subscribe();
      const all = http.expectOne(r => r.url === `${BASE}/mappings`);
      expect(all.request.params.has('connectionId')).toBeFalse();
      all.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 } as MappingPage);

      service.listMappings('c-1', 1, 10).subscribe();
      const filtered = http.expectOne(r => r.url === `${BASE}/mappings`);
      expect(filtered.request.params.get('connectionId')).toBe('c-1');
      expect(filtered.request.params.get('page')).toBe('1');
      expect(filtered.request.params.get('size')).toBe('10');
      filtered.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 } as MappingPage);
    });
  });
});
