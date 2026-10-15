import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { DIRECTORY_CACHE_MAX, DIRECTORY_CACHE_TTL_MS, DirectoryService, normalizeSearch } from './directory.service';
import { DirectoryPage, DirectoryUser } from './directory.types';

describe('DirectoryService', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/directory/users`;
  const alice: DirectoryUser = { id: 'a1', accountId: 'kc-a1', displayName: 'Alice Durand', email: 'alice@acme.com', active: true };
  const page = (content: DirectoryUser[]): DirectoryPage =>
    ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 });

  let service: DirectoryService;
  let http: HttpTestingController;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(DirectoryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    environment.useMockApi = prevMock;
    jasmine.clock().uninstall();
  });

  it('cherche avec le terme rogné, la page et la taille', () => {
    let res: DirectoryPage | undefined;
    service.search('  ali  ').subscribe(p => res = p);
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('q')).toBe('ali');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush(page([alice]));
    expect(res?.content).toEqual([alice]);
  });

  it('sans terme, n’envoie pas de paramètre q', () => {
    service.search('').subscribe();
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.has('q')).toBeFalse();
    req.flush(page([]));
  });

  it('respecte les bornes du serveur plutôt que de provoquer un 400', () => {
    service.search('x'.repeat(150), 500, -2).subscribe();
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('q')?.length).toBe(100);
    expect(req.request.params.get('size')).toBe('50');
    expect(req.request.params.get('page')).toBe('0');
    req.flush(page([]));
  });

  it('get(id) ne rappelle pas le serveur tant que le nom est frais', () => {
    const vus: string[] = [];
    service.get('a1').subscribe(u => vus.push(u.displayName));
    service.get('a1').subscribe(u => vus.push(u.displayName));
    http.expectOne(`${endpoint}/a1`).flush(alice);
    service.get('a1').subscribe(u => vus.push(u.displayName));
    expect(vus).toEqual(['Alice Durand', 'Alice Durand', 'Alice Durand']);
  });

  it('get(id) relit le serveur une fois le délai écoulé', () => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date(2026, 0, 1));
    let lectures = 0;
    service.get('a1').subscribe(() => lectures++);
    http.expectOne(`${endpoint}/a1`).flush(alice);
    jasmine.clock().tick(DIRECTORY_CACHE_TTL_MS - 1);
    service.get('a1').subscribe(() => lectures++);
    http.expectNone(`${endpoint}/a1`);
    jasmine.clock().tick(2);
    service.get('a1').subscribe(() => lectures++);
    http.expectOne(`${endpoint}/a1`).flush(alice);
    expect(lectures).toBe(3);
  });

  it('un échec ne reste pas en cache', () => {
    let erreur: unknown;
    service.get('zz').subscribe({ error: e => erreur = e });
    http.expectOne(`${endpoint}/zz`).flush('absent', { status: 404, statusText: 'Not Found' });
    expect(erreur).toBeTruthy();
    service.get('zz').subscribe();
    http.expectOne(`${endpoint}/zz`).flush(alice);
  });

  it('les résultats d’une recherche alimentent le cache des noms', () => {
    service.search('ali').subscribe();
    http.expectOne(r => r.url === endpoint).flush(page([alice]));
    let nom: string | undefined;
    service.get('a1').subscribe(u => nom = u.displayName);
    http.expectNone(`${endpoint}/a1`);
    expect(nom).toBe('Alice Durand');
  });

  it('getByAccount lit /accounts/{accountId} et garde le nom', () => {
    const noms: string[] = [];
    service.getByAccount('kc-a1').subscribe(u => noms.push(u.displayName));
    service.getByAccount('kc-a1').subscribe(u => noms.push(u.displayName));
    http.expectOne(`${environment.apiBaseUrl}/api/v1/directory/accounts/kc-a1`).flush(alice);
    service.getByAccount('kc-a1').subscribe(u => noms.push(u.displayName));
    expect(noms).toEqual(['Alice Durand', 'Alice Durand', 'Alice Durand']);
  });

  it('getByAccount : un 404 ne reste pas en cache, et le compte est encodé', () => {
    const accounts = `${environment.apiBaseUrl}/api/v1/directory/accounts`;
    let status: number | undefined;
    service.getByAccount('a/b').subscribe({ error: e => status = e.status });
    http.expectOne(`${accounts}/a%2Fb`).flush('absent', { status: 404, statusText: 'Not Found' });
    expect(status).toBe(404);
    service.getByAccount('a/b').subscribe();
    http.expectOne(`${accounts}/a%2Fb`).flush(alice);
  });

  it('une recherche alimente aussi le cache par compte', () => {
    service.search('ali').subscribe();
    http.expectOne(r => r.url === endpoint).flush(page([alice]));
    let compte: string | undefined;
    service.getByAccount('kc-a1').subscribe(u => compte = u.accountId);
    http.expectNone(`${environment.apiBaseUrl}/api/v1/directory/accounts/kc-a1`);
    expect(compte).toBe('kc-a1');
  });

  it('l’identifiant est encodé dans le chemin', () => {
    let status: number | undefined;
    service.get('a/b').subscribe({ error: e => status = e.status });
    http.expectOne(`${endpoint}/a%2Fb`).flush('x', { status: 400, statusText: 'Bad Request' });
    expect(status).toBe(400);
  });

  it('le cache est borné et se vide sur demande', () => {
    const many = Array.from({ length: DIRECTORY_CACHE_MAX + 5 }, (_, i) =>
      ({ id: `u${i}`, accountId: `kc-u${i}`, displayName: `U${i}`, email: `u${i}@acme.com`, active: true }));
    service.search('u').subscribe();
    http.expectOne(r => r.url === endpoint).flush(page(many));
    // La plus ancienne a été oubliée, la plus récente est gardée.
    const noms: string[] = [];
    service.get('u0').subscribe(u => noms.push(u.displayName));
    http.expectOne(`${endpoint}/u0`).flush(many[0]);
    service.get(`u${DIRECTORY_CACHE_MAX + 4}`).subscribe(u => noms.push(u.displayName));
    http.expectNone(`${endpoint}/u${DIRECTORY_CACHE_MAX + 4}`);

    service.clearCache();
    service.get(`u${DIRECTORY_CACHE_MAX + 4}`).subscribe(u => noms.push(u.displayName));
    http.expectOne(`${endpoint}/u${DIRECTORY_CACHE_MAX + 4}`).flush(many[DIRECTORY_CACHE_MAX + 4]);
    expect(noms).toEqual(['U0', `U${DIRECTORY_CACHE_MAX + 4}`, `U${DIRECTORY_CACHE_MAX + 4}`]);
  });

  it('une valeur absente vaut un terme vide', () => {
    service.search(null as unknown as string).subscribe();
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.has('q')).toBeFalse();
    req.flush(page([]));
  });

  it('normalise comme le serveur : sans accents, minuscules, espaces resserrés', () => {
    expect(normalizeSearch('  Élodie   MÜLLER ')).toBe('elodie muller');
  });

  describe('en démonstration sans serveur', () => {

    beforeEach(() => environment.useMockApi = true);

    it('cherche dans des personnes fictives, accents ignorés', () => {
      let res: DirectoryPage | undefined;
      service.search('elodie').subscribe(p => res = p);
      expect(res?.content.map(u => u.displayName)).toEqual(['Élodie Martin']);
    });

    it('pagine les personnes fictives', () => {
      let res: DirectoryPage | undefined;
      service.search('', 2, 1).subscribe(p => res = p);
      expect(res?.content.length).toBe(2);
      expect(res?.totalElements).toBe(4);
      expect(res?.totalPages).toBe(2);
    });

    it('la première personne fictive est l’utilisateur fictif d’AuthService', () => {
      let nom: string | undefined;
      let erreur: { status: number } | undefined;
      service.getByAccount('00000000-0000-0000-0000-000000000001').subscribe(u => nom = u.displayName);
      service.getByAccount('inconnu').subscribe({ error: e => erreur = e });
      expect(nom).toBe('Camille Bernard');
      expect(erreur?.status).toBe(404);
    });

    it('get(id) rend la personne fictive ou une erreur 404', () => {
      let nom: string | undefined;
      let erreur: { status: number } | undefined;
      service.get('00000000-0000-4000-8000-000000000003').subscribe(u => nom = u.displayName);
      service.get('inconnu').subscribe({ error: e => erreur = e });
      expect(nom).toBe('Karim Haddad');
      expect(erreur?.status).toBe(404);
    });
  });
});
