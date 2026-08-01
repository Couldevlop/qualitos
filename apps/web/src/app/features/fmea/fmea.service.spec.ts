import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { FmeaService } from './fmea.service';
import {
  CreateFmeaItemRequest,
  CreateFmeaProjectRequest,
  FmeaItemResponse,
  FmeaProjectResponse
} from './fmea.types';

/**
 * Analyse des modes de défaillance (AMDEC / FMEA, §4.5).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Le magasin rejoue ce qui fait la
 * valeur de l'AMDEC : l'indice de priorité de risque (IPR = gravité × occurrence
 * × détection) est TOUJOURS recalculé par le service, jamais reçu de l'appelant,
 * et la criticité se juge contre le seuil du projet — un même IPR est critique
 * sur un projet et pas sur un autre. Les deux modes sont testés.
 */
describe('FmeaService', () => {

  const AUTHOR = 'demo-user';
  const BASE = `${environment.apiBaseUrl}/api/v1/fmea`;

  const projectReq = (over: Partial<CreateFmeaProjectRequest> = {}): CreateFmeaProjectRequest => ({
    code: 'PFMEA-NEW-900',
    name: 'FMEA processus — ligne de conditionnement',
    type: 'PROCESS_FMEA',
    createdBy: AUTHOR,
    ...over
  });

  const itemReq = (over: Partial<CreateFmeaItemRequest> = {}): CreateFmeaItemRequest => ({
    function: 'Dosage produit',
    failureMode: 'Surdosage',
    severity: 5,
    occurrence: 4,
    detection: 3,
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: FmeaService;
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
      service = TestBed.inject(FmeaService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Projets : lectures --------------------------------------------------

    it('liste les projets pré-chargés', fakeAsync(() => {
      const page = run(service.list());

      expect(page.totalElements).toBe(3);
      expect(page.content.map(p => p.code))
        .toEqual(['PFMEA-ASM-A', 'DFMEA-PRD-V3', 'BOWTIE-CYBER-001']);
    }));

    it('filtre par statut et par type, y compris conjointement', fakeAsync(() => {
      expect(run(service.list(0, 50, 'DRAFT')).content.map(p => p.code)).toEqual(['DFMEA-PRD-V3']);
      expect(run(service.list(0, 50, undefined, 'BOW_TIE')).content.map(p => p.code))
        .toEqual(['BOWTIE-CYBER-001']);

      expect(run(service.list(0, 50, 'ACTIVE', 'PROCESS_FMEA')).content.map(p => p.code))
        .toEqual(['PFMEA-ASM-A']);
      expect(run(service.list(0, 50, 'DRAFT', 'BOW_TIE')).content).toEqual([]);
    }));

    it('résout un projet par identifiant, avec repli sur le premier si inconnu', fakeAsync(() => {
      expect(run(service.get('fmea-3')).code).toBe('BOWTIE-CYBER-001');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.get('fmea-inexistant')).id).toBe('fmea-1');
    }));

    // ---- Projets : écritures -------------------------------------------------

    it('crée un projet en brouillon, en tête de liste et sans lignes', fakeAsync(() => {
      const created = run(service.create(projectReq()));

      expect(created.status).toBe('DRAFT');
      expect(created.revision).toBe(1);
      expect(run(service.list()).content[0].code).toBe('PFMEA-NEW-900');
      expect(run(service.listItems(created.id)).content).toEqual([]);
    }));

    it('applique le seuil de criticité par défaut quand il n\'est pas fourni', fakeAsync(() => {
      expect(run(service.create(projectReq())).criticalRpnThreshold).toBe(100);
      expect(run(service.create(projectReq({
        code: 'PFMEA-NEW-901', criticalRpnThreshold: 60
      }))).criticalRpnThreshold).toBe(60);
    }));

    it('met à jour le projet et ignore une mise à jour inconnue', fakeAsync(() => {
      const updated = run(service.update('fmea-2', {
        name: 'FMEA conception — capteur V4', criticalRpnThreshold: 90
      }));
      expect(updated.name).toBe('FMEA conception — capteur V4');
      expect(updated.criticalRpnThreshold).toBe(90);

      const before = run(service.get('fmea-1')).name;
      run(service.update('fmea-inexistant', { name: 'x' }));
      expect(run(service.get('fmea-1')).name).toBe(before);
    }));

    it('parcourt le cycle de vie du projet', fakeAsync(() => {
      expect(run(service.activate('fmea-2')).status).toBe('ACTIVE');
      expect(run(service.reopen('fmea-2')).status).toBe('DRAFT');
      expect(run(service.archive('fmea-2')).status).toBe('ARCHIVED');
    }));

    it('laisse le magasin intact quand une transition vise un projet inconnu', fakeAsync(() => {
      run(service.activate('fmea-inexistant'));

      expect(run(service.list()).content.map(p => p.status))
        .toEqual(['ACTIVE', 'DRAFT', 'ACTIVE']);
    }));

    it('supprime le projet et ses lignes', fakeAsync(() => {
      run(service.delete('fmea-1'));

      expect(run(service.list()).totalElements).toBe(2);
      expect(run(service.listItems('fmea-1')).content).toEqual([]);

      // Une suppression inconnue ne retire rien.
      run(service.delete('fmea-inexistant'));
      expect(run(service.list()).totalElements).toBe(2);
    }));

    // ---- Lignes : calcul de l'IPR --------------------------------------------

    it('liste les lignes d\'un projet, et rend une page vide pour un projet sans ligne', fakeAsync(() => {
      expect(run(service.listItems('fmea-1')).content.map(i => i.rpn)).toEqual([160, 84]);
      expect(run(service.listItems('fmea-2')).content).toEqual([]);
      expect(run(service.listItems('projet-inconnu')).content).toEqual([]);
    }));

    it('calcule l\'IPR à partir des trois cotations, sans le recevoir de l\'appelant', fakeAsync(() => {
      const item = run(service.addItem('fmea-2', itemReq({ severity: 5, occurrence: 4, detection: 3 })));

      expect(item.rpn).toBe(60);
      expect(item.sequenceNo).toBe(1);
    }));

    it('numérote les lignes dans l\'ordre d\'ajout', fakeAsync(() => {
      run(service.addItem('fmea-2', itemReq()));
      const second = run(service.addItem('fmea-2', itemReq({ failureMode: 'Sous-dosage' })));

      expect(second.sequenceNo).toBe(2);
      expect(run(service.listItems('fmea-2')).totalElements).toBe(2);
    }));

    it('juge la criticité contre le seuil DU PROJET, pas contre une constante', fakeAsync(() => {
      // Même cotation, IPR 84 : sous le seuil de fmea-1 (100), au-dessus de celui
      // de fmea-3 (80). C'est le projet qui définit ce qui est critique.
      const cotation = itemReq({ severity: 7, occurrence: 3, detection: 4 });

      expect(run(service.addItem('fmea-1', cotation)).critical).toBeFalse();
      expect(run(service.addItem('fmea-3', cotation)).critical).toBeTrue();
    }));

    it('traite le seuil comme une borne atteinte, pas seulement dépassée', fakeAsync(() => {
      // IPR exactement égal au seuil de fmea-1 (100) : critique.
      expect(run(service.addItem('fmea-1', itemReq({
        severity: 10, occurrence: 10, detection: 1
      }))).critical).toBeTrue();
    }));

    it('retient l\'IPR résiduel quand les trois cotations d\'après action sont fournies', fakeAsync(() => {
      const item = run(service.addItem('fmea-2', itemReq({
        resultingSeverity: 5, resultingOccurrence: 2, resultingDetection: 2
      })));

      expect(item.rpnAfter).toBe(20);
    }));

    it('ne calcule aucun IPR résiduel tant que l\'action n\'est pas cotée', fakeAsync(() => {
      expect(run(service.addItem('fmea-2', itemReq())).rpnAfter).toBeUndefined();

      // Cotation partielle : le produit serait faussement nul, on n'affiche rien.
      expect(run(service.addItem('fmea-2', itemReq({
        resultingSeverity: 4, resultingOccurrence: 2
      }))).rpnAfter).toBeUndefined();
    }));

    // ---- Lignes : mise à jour ------------------------------------------------

    it('recalcule l\'IPR et la criticité à chaque mise à jour de cotation', fakeAsync(() => {
      const updated = run(service.updateItem('fmea-1', 'fmi-2', { occurrence: 6 }));

      expect(updated.rpn).toBe(7 * 6 * 4);
      expect(updated.critical).toBeTrue();
    }));

    it('fait retomber la criticité quand l\'action réduit le risque', fakeAsync(() => {
      const updated = run(service.updateItem('fmea-1', 'fmi-1', { occurrence: 1, detection: 2 }));

      expect(updated.rpn).toBe(16);
      expect(updated.critical).toBeFalse();
    }));

    it('recalcule aussi l\'IPR résiduel quand les trois cotations d\'après action existent', fakeAsync(() => {
      const updated = run(service.updateItem('fmea-1', 'fmi-1', {
        resultingSeverity: 8, resultingOccurrence: 1, resultingDetection: 2
      }));

      expect(updated.rpnAfter).toBe(16);
    }));

    it('supprime une ligne, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.deleteItem('fmea-1', 'fmi-2'));
      expect(run(service.listItems('fmea-1')).totalElements).toBe(1);

      run(service.deleteItem('fmea-1', 'fmi-inexistante'));
      run(service.deleteItem('projet-inconnu', 'fmi-1'));
      expect(run(service.listItems('fmea-1')).totalElements).toBe(1);
    }));

    // ---- Statistiques --------------------------------------------------------

    it('agrège le nombre de lignes, les critiques, l\'IPR maximal et moyen', fakeAsync(() => {
      const stats = run(service.statistics('fmea-1'));

      expect(stats.totalItems).toBe(2);
      expect(stats.criticalItems).toBe(1);
      expect(stats.maxRpn).toBe(160);
      expect(stats.averageRpn).toBe((160 + 84) / 2);
      expect(stats.criticalRpnThreshold).toBe(100);
    }));

    it('ne divise pas par zéro sur un projet sans ligne', fakeAsync(() => {
      const stats = run(service.statistics('fmea-2'));

      expect(stats.totalItems).toBe(0);
      expect(stats.averageRpn).toBe(0);
      expect(stats.maxRpn).toBe(0);
    }));

    it('retombe sur le seuil par défaut pour un projet inconnu', fakeAsync(() => {
      const stats = run(service.statistics('projet-inconnu'));

      expect(stats.projectId).toBe('projet-inconnu');
      expect(stats.totalItems).toBe(0);
      expect(stats.criticalRpnThreshold).toBe(100);
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: FmeaService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(FmeaService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('pagine la liste des projets et n\'ajoute les filtres que s\'ils sont fournis', () => {
      service.list().subscribe();
      const plain = http.expectOne(r => r.url === `${BASE}/projects`);
      expect(plain.request.params.get('page')).toBe('0');
      expect(plain.request.params.get('size')).toBe('50');
      expect(plain.request.params.has('status')).toBeFalse();
      expect(plain.request.params.has('type')).toBeFalse();
      plain.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      service.list(2, 10, 'ACTIVE', 'BOW_TIE').subscribe();
      const filtered = http.expectOne(r => r.url === `${BASE}/projects`);
      expect(filtered.request.params.get('page')).toBe('2');
      expect(filtered.request.params.get('size')).toBe('10');
      expect(filtered.request.params.get('status')).toBe('ACTIVE');
      expect(filtered.request.params.get('type')).toBe('BOW_TIE');
      filtered.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });
    });

    it('lit un projet et ses statistiques', () => {
      service.get('p-1').subscribe();
      const project = http.expectOne(`${BASE}/projects/p-1`);
      expect(project.request.method).toBe('GET');
      project.flush({} as FmeaProjectResponse);

      service.statistics('p-1').subscribe();
      const stats = http.expectOne(`${BASE}/projects/p-1/statistics`);
      expect(stats.request.method).toBe('GET');
      stats.flush({
        projectId: 'p-1', totalItems: 0, criticalItems: 0,
        maxRpn: 0, averageRpn: 0, criticalRpnThreshold: 100
      });
    });

    it('crée en POST et met à jour en PATCH — mise à jour partielle', () => {
      const body = projectReq();
      service.create(body).subscribe();
      const post = http.expectOne(`${BASE}/projects`);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as FmeaProjectResponse);

      service.update('p-1', { name: 'n' }).subscribe();
      const patch = http.expectOne(`${BASE}/projects/p-1`);
      expect(patch.request.method).toBe('PATCH');
      expect(patch.request.body).toEqual({ name: 'n' });
      patch.flush({} as FmeaProjectResponse);
    });

    it('poste chaque transition sur son propre sous-chemin, sans corps', () => {
      const calls: Array<[string, () => void]> = [
        ['activate', () => service.activate('p-1').subscribe()],
        ['reopen', () => service.reopen('p-1').subscribe()],
        ['archive', () => service.archive('p-1').subscribe()]
      ];

      calls.forEach(([path, call]) => {
        call();
        const req = http.expectOne(`${BASE}/projects/p-1/${path}`);
        expect(req.request.method).withContext(path).toBe('POST');
        expect(req.request.body).withContext(path).toEqual({});
        req.flush({} as FmeaProjectResponse);
      });
    });

    it('gère les lignes sous la ressource du projet', () => {
      service.listItems('p-1').subscribe();
      const list = http.expectOne(r => r.url === `${BASE}/projects/p-1/items`);
      expect(list.request.params.get('page')).toBe('0');
      expect(list.request.params.get('size')).toBe('100');
      list.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      const body = itemReq();
      service.addItem('p-1', body).subscribe();
      const add = http.expectOne(`${BASE}/projects/p-1/items`);
      expect(add.request.method).toBe('POST');
      expect(add.request.body).toEqual(body);
      add.flush({} as FmeaItemResponse);

      service.updateItem('p-1', 'i-1', { severity: 9 }).subscribe();
      const patch = http.expectOne(`${BASE}/projects/p-1/items/i-1`);
      expect(patch.request.method).toBe('PATCH');
      expect(patch.request.body).toEqual({ severity: 9 });
      patch.flush({} as FmeaItemResponse);

      service.deleteItem('p-1', 'i-1').subscribe();
      const del = http.expectOne(`${BASE}/projects/p-1/items/i-1`);
      expect(del.request.method).toBe('DELETE');
      del.flush(null);
    });

    it('supprime un projet en DELETE sur la ressource', () => {
      service.delete('p-1').subscribe();

      const req = http.expectOne(`${BASE}/projects/p-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
