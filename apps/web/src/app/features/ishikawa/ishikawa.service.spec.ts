import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { IshikawaService } from './ishikawa.service';
import {
  ConvertedPdcaCycle,
  CreateIshikawaCauseRequest,
  CreateIshikawaDiagramRequest,
  IshikawaCauseResponse,
  IshikawaDiagramResponse,
  SuggestedCause
} from './ishikawa.types';

/**
 * Diagrammes d'Ishikawa (§3.5).
 *
 * Le service porte deux implémentations du même contrat : un magasin en mémoire
 * (démo sans backend) et les appels HTTP réels. Deux points portent la valeur du
 * module : la conversion d'un diagramme — ou d'une cause-racine précise — en
 * cycle PDCA, qui matérialise le référentiel commun du §3.6, et les suggestions
 * de causes par l'IA, réparties sur les catégories du 6M. Les deux modes sont
 * testés.
 */
describe('IshikawaService', () => {

  const BASE = `${environment.apiBaseUrl}/api/v1/ishikawa/diagrams`;

  const diagramReq = (
    over: Partial<CreateIshikawaDiagramRequest> = {}
  ): CreateIshikawaDiagramRequest => ({
    problemStatement: 'Retards de livraison sur la ligne de conditionnement',
    mode: 'SIX_M',
    ownerId: 'demo-user',
    ...over
  });

  const causeReq = (over: Partial<CreateIshikawaCauseRequest> = {}): CreateIshikawaCauseRequest => ({
    category: 'METHODS',
    label: 'Procédure de changement de format non standardisée',
    ...over
  });

  // ------------------------------------------------------------------------
  // Magasin en mémoire
  // ------------------------------------------------------------------------
  describe('en mode démo (magasin en mémoire)', () => {
    let service: IshikawaService;
    let http: HttpTestingController;
    let prevMock: boolean;

    /** Les réponses simulées sont différées (`delay`) : on déroule le temps virtuel. */
    function run<T>(source: Observable<T>): T {
      let value: T | undefined;
      source.subscribe(v => (value = v));
      tick(1000);
      return value as T;
    }

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = true;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(IshikawaService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      // Le mode démo ne doit émettre AUCUNE requête réseau.
      http.verify();
    });

    // ---- Lectures ----------------------------------------------------------

    it('liste les diagrammes pré-chargés et sait les filtrer par statut', fakeAsync(() => {
      expect(run(service.listDiagrams()).totalElements).toBe(2);

      expect(run(service.listDiagrams(0, 50, 'VALIDATED')).content.map(d => d.id)).toEqual(['ish-1']);
      expect(run(service.listDiagrams(0, 50, 'DRAFT')).content).toEqual([]);
    }));

    it('résout un diagramme par identifiant, avec repli si inconnu', fakeAsync(() => {
      expect(run(service.getDiagram('ish-2')).mode).toBe('SEVEN_M');
      // Repli assumé du mode démo : les écrans restent utilisables sans backend.
      expect(run(service.getDiagram('ish-inexistant')).id).toBe('ish-1');
    }));

    // ---- Création et édition -------------------------------------------------

    it('crée un diagramme en brouillon, sans cause, en tête de liste', fakeAsync(() => {
      const created = run(service.createDiagram(diagramReq()));

      expect(created.status).toBe('DRAFT');
      expect(created.causes).toEqual([]);
      expect(run(service.listDiagrams()).content[0].problemStatement)
        .toContain('Retards de livraison');
    }));

    it('ne met à jour que les champs transmis', fakeAsync(() => {
      const updated = run(service.updateDiagram('ish-2', { status: 'VALIDATED' }));

      expect(updated.status).toBe('VALIDATED');
      // L'énoncé du problème n'était pas transmis : il ne doit pas être effacé.
      expect(updated.problemStatement).toContain('Annulations chirurgie');
    }));

    it('met à jour sans effet de bord quand le diagramme visé n\'existe pas', fakeAsync(() => {
      const before = run(service.getDiagram('ish-1')).problemStatement;

      run(service.updateDiagram('ish-inexistant', { problemStatement: 'usurpé' }));

      expect(run(service.getDiagram('ish-1')).problemStatement).toBe(before);
    }));

    it('supprime un diagramme, et ignore une suppression inconnue', fakeAsync(() => {
      run(service.deleteDiagram('ish-2'));
      expect(run(service.listDiagrams()).totalElements).toBe(1);

      run(service.deleteDiagram('ish-inexistant'));
      expect(run(service.listDiagrams()).totalElements).toBe(1);
    }));

    // ---- Causes ---------------------------------------------------------------

    it('rattache une cause au diagramme, avec sa catégorie', fakeAsync(() => {
      const cause = run(service.addCause('ish-2', causeReq({ rootCauseScore: 0.7 })));

      expect(cause.diagramId).toBe('ish-2');
      expect(cause.category).toBe('METHODS');
      expect(cause.rootCauseScore).toBe(0.7);
      expect(run(service.getDiagram('ish-2')).causes.map(c => c.id)).toEqual([cause.id]);
    }));

    it('accepte une sous-cause rattachée à une cause parente', fakeAsync(() => {
      // Les 5 pourquoi se construisent en profondeur : sans parent, l'arbre des
      // causes s'aplatit et la cause racine devient indiscernable.
      const cause = run(service.addCause('ish-1', causeReq({ parentId: 'c1' })));

      expect(cause.parentId).toBe('c1');
    }));

    it('rend la cause créée même quand le diagramme visé n\'existe pas', fakeAsync(() => {
      const cause = run(service.addCause('ish-inexistant', causeReq()));

      expect(cause.label).toContain('changement de format');
    }));

    // ---- Suggestions IA --------------------------------------------------------

    it('propose des causes réparties sur plusieurs catégories du 6M', fakeAsync(() => {
      const suggestions = run(service.suggestCauses('ish-1'));

      expect(suggestions.length).toBeGreaterThan(0);
      // Des suggestions toutes rangées dans la même branche n'aideraient pas :
      // l'intérêt de l'Ishikawa est justement de balayer les familles de causes.
      const categories = new Set(suggestions.map(s => s.category));
      expect(categories.size).toBeGreaterThan(1);
      expect(suggestions.every(s => !!s.label)).toBeTrue();
    }));

    // ---- Conversion en PDCA ------------------------------------------------------

    it('convertit le diagramme en cycle PDCA au stade Plan', fakeAsync(() => {
      const cycle = run(service.convertToPdca('ish-1'));

      // §3.6 — le référentiel est commun : une cause identifiée ici devient un
      // cycle d'amélioration là-bas, sans ressaisie.
      expect(cycle.id).toBeTruthy();
      expect(cycle.status).toBe('PLAN');
    }));

    it('convertit aussi une cause-racine ciblée', fakeAsync(() => {
      expect(run(service.convertToPdca('ish-1', 'c1')).status).toBe('PLAN');
    }));
  });

  // ------------------------------------------------------------------------
  // Appels HTTP réels
  // ------------------------------------------------------------------------
  describe('en mode connecté (HTTP)', () => {
    let service: IshikawaService;
    let http: HttpTestingController;
    let prevMock: boolean;

    beforeEach(() => {
      prevMock = environment.useMockApi;
      environment.useMockApi = false;
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
      });
      service = TestBed.inject(IshikawaService);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
      environment.useMockApi = prevMock;
      http.verify();
    });

    it('pagine la liste et n\'ajoute le statut que s\'il est fourni', () => {
      service.listDiagrams().subscribe();
      const plain = http.expectOne(r => r.url === BASE);
      expect(plain.request.params.get('page')).toBe('0');
      expect(plain.request.params.get('size')).toBe('50');
      expect(plain.request.params.has('status')).toBeFalse();
      plain.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });

      service.listDiagrams(2, 20, 'IN_REVIEW').subscribe();
      const filtered = http.expectOne(r => r.url === BASE);
      expect(filtered.request.params.get('page')).toBe('2');
      expect(filtered.request.params.get('status')).toBe('IN_REVIEW');
      filtered.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 });
    });

    it('crée en POST, lit en GET, met à jour en PATCH et supprime en DELETE', () => {
      const body = diagramReq();
      service.createDiagram(body).subscribe();
      const post = http.expectOne(BASE);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(body);
      post.flush({} as IshikawaDiagramResponse);

      service.getDiagram('d-1').subscribe();
      const get = http.expectOne(`${BASE}/d-1`);
      expect(get.request.method).toBe('GET');
      get.flush({} as IshikawaDiagramResponse);

      service.updateDiagram('d-1', { status: 'VALIDATED' }).subscribe();
      const patch = http.expectOne(`${BASE}/d-1`);
      expect(patch.request.method).toBe('PATCH');
      expect(patch.request.body).toEqual({ status: 'VALIDATED' });
      patch.flush({} as IshikawaDiagramResponse);

      service.deleteDiagram('d-1').subscribe();
      const del = http.expectOne(`${BASE}/d-1`);
      expect(del.request.method).toBe('DELETE');
      del.flush(null);
    });

    it('poste une cause sous la ressource du diagramme', () => {
      const body = causeReq();

      service.addCause('d-1', body).subscribe();

      const req = http.expectOne(`${BASE}/d-1/causes`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      req.flush({} as IshikawaCauseResponse);
    });

    it('demande les suggestions de causes en POST', () => {
      service.suggestCauses('d-1').subscribe();

      const req = http.expectOne(`${BASE}/d-1/suggest-causes`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush([] as SuggestedCause[]);
    });

    it('convertit en PDCA sans paramètre, ou en ciblant une cause', () => {
      service.convertToPdca('d-1').subscribe();
      const whole = http.expectOne(r => r.url === `${BASE}/d-1/convert-to-pdca`);
      expect(whole.request.method).toBe('POST');
      expect(whole.request.params.has('causeId')).toBeFalse();
      whole.flush({} as ConvertedPdcaCycle);

      service.convertToPdca('d-1', 'c-9').subscribe();
      const targeted = http.expectOne(r => r.url === `${BASE}/d-1/convert-to-pdca`);
      expect(targeted.request.params.get('causeId')).toBe('c-9');
      targeted.flush({} as ConvertedPdcaCycle);
    });
  });
});
