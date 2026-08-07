import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { FiveWhysService } from './five-whys.service';
import { FiveWhysAnalysis, FiveWhysStep } from './five-whys.types';

/**
 * Analyses des 5 Pourquoi (§3.5).
 *
 * <p>Le service n'a aucune règle de méthode à lui : elles sont tenues par le
 * serveur. Ce qu'il doit garantir, c'est le contrat — la bonne route, le bon
 * verbe, et surtout aucun `tenantId` transmis par le client (§18.2 #2) : le
 * tenant vient du jeton, l'accepter d'ailleurs ouvrirait la porte d'à côté.
 */
describe('FiveWhysService', () => {

  const BASE = `${environment.apiBaseUrl}/api/v1/five-whys`;

  let service: FiveWhysService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(FiveWhysService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pagine la liste, avec des valeurs par défaut utilisables', () => {
    service.list().subscribe();

    const req = http.expectOne(r => r.url === BASE);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('honore la page et la taille demandées', () => {
    service.list(3, 50).subscribe();

    const req = http.expectOne(r => r.url === BASE);
    expect(req.request.params.get('page')).toBe('3');
    expect(req.request.params.get('size')).toBe('50');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 3, size: 50 });
  });

  it('demande les analyses d\'une non-conformité par le paramètre ncId', () => {
    // Même chemin que la liste : c'est le paramètre qui change l'intention.
    service.listForNc('nc-9').subscribe();

    const req = http.expectOne(r => r.url === BASE);
    expect(req.request.params.get('ncId')).toBe('nc-9');
    expect(req.request.params.has('page')).toBeFalse();
    req.flush([]);
  });

  it('lit, crée et supprime une analyse sur sa propre ressource', () => {
    service.get('a-1').subscribe();
    const get = http.expectOne(`${BASE}/a-1`);
    expect(get.request.method).toBe('GET');
    get.flush({} as FiveWhysAnalysis);

    service.create({ ncId: 'nc-1', problem: 'Arrêt de ligne récurrent' }).subscribe();
    const post = http.expectOne(BASE);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ ncId: 'nc-1', problem: 'Arrêt de ligne récurrent' });
    post.flush({} as FiveWhysAnalysis);

    service.delete('a-1').subscribe();
    const del = http.expectOne(`${BASE}/a-1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('corrige l\'énoncé du problème en PATCH', () => {
    service.updateProblem('a-1', 'Énoncé corrigé').subscribe();

    const req = http.expectOne(`${BASE}/a-1/problem`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ problem: 'Énoncé corrigé' });
    req.flush({} as FiveWhysAnalysis);
  });

  it('ajoute un pourquoi sous la ressource de l\'analyse', () => {
    service.addStep('a-1', 'Le convoyeur a dérivé').subscribe();

    const req = http.expectOne(`${BASE}/a-1/steps`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ answer: 'Le convoyeur a dérivé' });
    req.flush({} as FiveWhysStep);
  });

  it('modifie ou retire un pourquoi par son propre identifiant', () => {
    // Le maillon est adressé directement : l'analyse est déduite côté serveur,
    // qui vérifie au passage que le maillon est bien le dernier.
    service.updateStep('s-2', 'Formulation corrigée').subscribe();
    const patch = http.expectOne(`${BASE}/steps/s-2`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ answer: 'Formulation corrigée' });
    patch.flush({} as FiveWhysStep);

    service.deleteStep('s-2').subscribe();
    const del = http.expectOne(`${BASE}/steps/s-2`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('conclut la cause racine en PUT — la conclusion remplace, elle ne s\'empile pas', () => {
    service.setRootCause('a-1', 'Presse mal réglée').subscribe();

    const req = http.expectOne(`${BASE}/a-1/root-cause`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ rootCause: 'Presse mal réglée' });
    req.flush({} as FiveWhysAnalysis);
  });

  it('ne transmet jamais de tenant : il vient du jeton (§18.2 #2)', () => {
    service.list().subscribe();
    const list = http.expectOne(r => r.url === BASE);
    expect(list.request.params.has('tenantId')).toBeFalse();
    list.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });

    service.create({ ncId: 'nc-1' }).subscribe();
    const create = http.expectOne(BASE);
    expect(Object.keys(create.request.body as object)).not.toContain('tenantId');
    create.flush({} as FiveWhysAnalysis);
  });
});
