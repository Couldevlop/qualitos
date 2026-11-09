import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { catalogLabel, clearCatalogOverrides } from './catalog-overrides';
import { CatalogService } from './catalog.service';
import { Catalog, CatalogEntry, CatalogSummary } from './catalog.types';

/**
 * Les listes déroulantes du client : ce que les écrans de saisie en reçoivent,
 * et ce que l'écran de paramétrage y écrit.
 */
describe('CatalogService', () => {
  let service: CatalogService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/v1/settings/catalogs`;

  const CATALOGUE = (o: Partial<Catalog> = {}): Catalog => ({
    catalogue: 'fives-zone',
    mode: 'OPEN',
    defaults: false,
    entries: [
      { code: 'BLOC-A', label: 'Bloc opératoire A', active: true },
      { code: 'ANCIEN', label: 'Ancienne aile', active: false }
    ],
    ...o
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(CatalogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists the catalogues open to tenant settings', () => {
    let resume: CatalogSummary[] = [];
    service.summaries().subscribe(r => (resume = r));

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([{ catalogue: 'fives-zone', mode: 'OPEN', defaults: true, entryCount: 8 }]);

    expect(resume.length).toBe(1);
    expect(resume[0].entryCount).toBe(8);
  });

  it('reads one catalogue, and never sends the tenant itself', () => {
    let lu: Catalog | undefined;
    service.catalog('audit-type').subscribe(c => (lu = c));

    const req = httpMock.expectOne(`${base}/audit-type`);
    expect(req.request.method).toBe('GET');
    expect(req.request.urlWithParams).not.toContain('tenant');
    req.flush(CATALOGUE({ catalogue: 'audit-type', mode: 'RENAME' }));

    expect(lu?.mode).toBe('RENAME');
  });

  it('entries keeps only what is in service, in the tenant order', () => {
    let entrees: CatalogEntry[] | null = null;
    service.entries('fives-zone').subscribe(e => (entrees = e));
    httpMock.expectOne(`${base}/fives-zone`).flush(CATALOGUE());

    expect(entrees!.length).toBe(1);
    expect(entrees![0].code).toBe('BLOC-A');
  });

  /**
   * Le cas le plus important : sans réglage, l'écran garde SES libellés, traduits
   * en six langues. Rendre le défaut français serait une régression pour tout
   * utilisateur qui ne lit pas le français.
   */
  it('returns null when the tenant has set nothing, so screens keep their own labels', () => {
    let entrees: CatalogEntry[] | null | undefined;
    let libelles: Record<string, string> | null | undefined;
    service.entries('audit-type').subscribe(e => (entrees = e));
    service.labels('audit-type').subscribe(l => (libelles = l));

    httpMock.expectOne(`${base}/audit-type`).flush(
      CATALOGUE({ catalogue: 'audit-type', mode: 'RENAME', defaults: true }));

    expect(entrees).toBeNull();
    expect(libelles).toBeNull();
  });

  it('labels indexes the tenant words by code', () => {
    let libelles: Record<string, string> | null | undefined;
    service.labels('fives-zone').subscribe(l => (libelles = l));
    httpMock.expectOne(`${base}/fives-zone`).flush(CATALOGUE());

    expect(libelles).toEqual({ 'BLOC-A': 'Bloc opératoire A' });
  });

  /** Un formulaire doit rester utilisable quand le paramétrage est injoignable. */
  it('falls back to the screen labels when the catalogue cannot be read', () => {
    let entrees: CatalogEntry[] | null | undefined;
    service.entries('fives-zone').subscribe(e => (entrees = e));
    httpMock.expectOne(`${base}/fives-zone`).flush('nope', { status: 503, statusText: 'down' });

    expect(entrees).toBeNull();
  });

  it('reads a catalogue once for several screens, and again after it is set', () => {
    service.entries('fives-zone').subscribe();
    service.entries('fives-zone').subscribe();
    httpMock.expectOne(`${base}/fives-zone`).flush(CATALOGUE());
    // Un second abonné ne relance pas l'appel : la liste ne change qu'au paramétrage.
    service.entries('fives-zone').subscribe();
    httpMock.expectNone(`${base}/fives-zone`);

    service.replace('fives-zone', [{ code: 'BLOC-B', label: 'Bloc B' }]).subscribe();
    httpMock.expectOne(`${base}/fives-zone`).flush(CATALOGUE({
      entries: [{ code: 'BLOC-B', label: 'Bloc B', active: true }]
    }));
    // Enregistrer relit aussi le réglage tenu en mémoire pour les libellés
    // synchrones des écrans (ADR 0111).
    httpMock.expectOne(`${base}/custom`).flush({});

    let entrees: CatalogEntry[] | null = null;
    service.entries('fives-zone').subscribe(e => (entrees = e));
    httpMock.expectOne(`${base}/fives-zone`).flush(CATALOGUE({
      entries: [{ code: 'BLOC-B', label: 'Bloc B', active: true }]
    }));
    expect(entrees![0].code).toBe('BLOC-B');
  });

  it('replace PUTs the entries in order, and reset goes back to the platform', () => {
    service.replace('fives-zone', [
      { code: 'B', label: 'Bloc B' }, { code: 'A', label: 'Bloc A', active: false }
    ]).subscribe();

    const put = httpMock.expectOne(`${base}/fives-zone`);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ entries: [
      { code: 'B', label: 'Bloc B' }, { code: 'A', label: 'Bloc A', active: false }
    ] });
    put.flush(CATALOGUE());

    httpMock.expectOne(`${base}/custom`).flush({});

    service.reset('fives-zone').subscribe();
    const post = httpMock.expectOne(`${base}/fives-zone/reset`);
    expect(post.request.method).toBe('POST');
    post.flush(CATALOGUE({ defaults: true }));
    httpMock.expectOne(`${base}/custom`).flush({});
  });

  it('invalidate clears one catalogue, or all of them', () => {
    service.entries('fives-zone').subscribe();
    httpMock.expectOne(`${base}/fives-zone`).flush(CATALOGUE());
    service.entries('audit-type').subscribe();
    httpMock.expectOne(`${base}/audit-type`).flush(CATALOGUE({ catalogue: 'audit-type' }));

    service.invalidate('fives-zone');
    service.entries('fives-zone').subscribe();
    httpMock.expectOne(`${base}/fives-zone`).flush(CATALOGUE());
    service.entries('audit-type').subscribe();
    httpMock.expectNone(`${base}/audit-type`);

    service.invalidate();
    service.entries('audit-type').subscribe();
    httpMock.expectOne(`${base}/audit-type`).flush(CATALOGUE({ catalogue: 'audit-type' }));
  });

  describe('preload', () => {

    afterEach(() => clearCatalogOverrides());

    it('charge en un seul appel les listes reglees, pour les libelles synchrones', () => {
      let fini = false;
      service.preload().subscribe(() => (fini = true));
      httpMock.expectOne(`${base}/custom`).flush({
        'audit-type': [{ code: 'INTERNAL', label: 'Revue interne', active: true }]
      });
      expect(fini).toBeTrue();
      expect(catalogLabel('audit-type', 'INTERNAL')).toBe('Revue interne');
    });

    it("n'est pas une panne quand le serveur refuse : les ecrans gardent leurs libelles", () => {
      let fini = false;
      service.preload().subscribe(() => (fini = true));
      httpMock.expectOne(`${base}/custom`).flush('non', { status: 500, statusText: 'Server Error' });
      expect(fini).toBeTrue();
      expect(catalogLabel('audit-type', 'INTERNAL')).toBeNull();
    });

    it('oublie tout a la deconnexion', () => {
      service.preload().subscribe();
      httpMock.expectOne(`${base}/custom`).flush({
        'audit-type': [{ code: 'INTERNAL', label: 'Revue interne', active: true }]
      });
      service.forget();
      expect(catalogLabel('audit-type', 'INTERNAL')).toBeNull();
    });

    it('relit le reglage apres un enregistrement : la nouvelle liste vaut tout de suite', () => {
      service.replace('audit-type', [{ code: 'INTERNAL', label: 'Revue interne' }]).subscribe();
      httpMock.expectOne({ method: 'PUT', url: `${base}/audit-type` })
        .flush(CATALOGUE({ catalogue: 'audit-type', mode: 'RENAME' }));
      httpMock.expectOne(`${base}/custom`).flush({
        'audit-type': [{ code: 'INTERNAL', label: 'Revue interne', active: true }]
      });
      expect(catalogLabel('audit-type', 'INTERNAL')).toBe('Revue interne');
    });

    it('relit le reglage apres un retablissement', () => {
      service.preload().subscribe();
      httpMock.expectOne(`${base}/custom`).flush({
        'audit-type': [{ code: 'INTERNAL', label: 'Revue interne', active: true }]
      });

      service.reset('audit-type').subscribe();
      httpMock.expectOne({ method: 'POST', url: `${base}/audit-type/reset` })
        .flush(CATALOGUE({ catalogue: 'audit-type', defaults: true }));
      httpMock.expectOne(`${base}/custom`).flush({});
      expect(catalogLabel('audit-type', 'INTERNAL')).toBeNull();
    });
  });
});
