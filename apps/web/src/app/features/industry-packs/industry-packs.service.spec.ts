import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { IndustryPacksService } from './industry-packs.service';
import { PacksPage } from './industry-packs.types';

describe('IndustryPacksService', () => {
  let service: IndustryPacksService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/v1/industry-packs`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(IndustryPacksService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list() GETs the catalog with paging params', () => {
    const page: PacksPage = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 50 };
    service.list().subscribe(p => expect(p.totalElements).toBe(0));
    const req = httpMock.expectOne(r => r.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('50');
    req.flush(page);
  });

  it('get() GETs a single pack by code', () => {
    service.get('manufacturing').subscribe(p => expect(p.code).toBe('manufacturing'));
    const req = httpMock.expectOne(`${base}/manufacturing`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: '1', code: 'manufacturing', name: 'Industrie', version: '1.0.0', tags: [] });
  });

  it('activate() POSTs activatedBy and surfaces provisioning counters', () => {
    let captured: { kpisCreated?: number; kpisSkipped?: number; warnings?: string[] } | undefined;
    service.activate('construction', { activatedBy: 'u-1' }).subscribe(r => (captured = r));
    const req = httpMock.expectOne(`${base}/construction/activate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ activatedBy: 'u-1' });
    req.flush({
      id: 'a1', tenantId: 't1', packCode: 'construction', status: 'ACTIVE',
      activatedBy: 'u-1', kpisCreated: 7, kpisSkipped: 2, warnings: ['norm iso-19650 unknown']
    });
    expect(captured?.kpisCreated).toBe(7);
    expect(captured?.kpisSkipped).toBe(2);
    expect(captured?.warnings?.length).toBe(1);
  });

  it('deactivate() DELETEs with deactivatedBy query param', () => {
    service.deactivate('construction', 'u-9').subscribe();
    const req = httpMock.expectOne(r => r.url === `${base}/construction/activate`);
    expect(req.request.method).toBe('DELETE');
    expect(req.request.params.get('deactivatedBy')).toBe('u-9');
    req.flush({ id: 'a1', tenantId: 't1', packCode: 'construction', status: 'DEACTIVATED' });
  });

  it('myActivations() GETs /my', () => {
    service.myActivations().subscribe(list => expect(list.length).toBe(1));
    const req = httpMock.expectOne(`${base}/my`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'a1', tenantId: 't1', packCode: 'manufacturing', status: 'ACTIVE' }]);
  });

  // ---- parseManifest : schéma RICHE -------------------------------------

  it('parseManifest() reads the rich schema (richKpis, ishikawa, poka-yoke, glossary)', () => {
    const json = JSON.stringify({
      sectors: ['construction', 'btp'],
      standards: ['iso-19650', 'iso-9001'],
      richKpis: [{
        kpiId: 'btp_reserves_levees', name: 'Réserves levées', formula: 'a/b*100',
        unit: '%', target: '>= 95', thresholdWarning: '80 - 95', thresholdCritical: '< 80',
        owner: 'directeur_travaux'
      }],
      ishikawaTemplates: [{
        problemArchetype: 'Réserves nombreuses',
        branches: { man: ['Compagnons non qualifiés'], method: ['Autocontrôle non réalisé'] }
      }],
      pokaYokeLibrary: [{ id: 'pk1', name: 'Détection clash BIM', sectorFit: ['construction'] }],
      glossary: { PPSPS: 'Plan particulier de sécurité', BIM: 'Building Information Modeling' },
      connectors: ['bim', 'iot']
    });
    const m = service.parseManifest(json);

    expect(m.rich).toBeTrue();
    expect(m.sectors).toEqual(['construction', 'btp']);
    expect(m.standards).toEqual(['iso-19650', 'iso-9001']);
    expect(m.kpis.length).toBe(1);
    expect(m.kpis[0].thresholdCritical).toBe('< 80');
    expect(m.ishikawaTemplates.length).toBe(1);
    expect(m.ishikawaTemplates[0].branches.length).toBe(2);
    expect(m.ishikawaTemplates[0].branches[0]).toEqual({ label: 'man', causes: ['Compagnons non qualifiés'] });
    expect(m.pokaYoke.length).toBe(1);
    expect(m.glossary.length).toBe(2);
    expect(m.glossary[0]).toEqual({ term: 'PPSPS', definition: 'Plan particulier de sécurité' });
    expect(m.connectors).toEqual(['bim', 'iot']);
  });

  // ---- parseManifest : schéma PLAT --------------------------------------

  it('parseManifest() reads the flat schema (kpis as slugs, glossary map, no rich content)', () => {
    const json = JSON.stringify({
      standards: ['iso-9001', 'iatf-16949'],
      kpis: ['oee', 'first-pass-yield', 'scrap-rate'],
      connectors: ['opc-ua', 'mqtt'],
      glossary: { OEE: 'Overall Equipment Effectiveness' }
    });
    const m = service.parseManifest(json);

    expect(m.rich).toBeFalse();
    expect(m.kpiSlugs).toEqual(['oee', 'first-pass-yield', 'scrap-rate']);
    expect(m.kpis.length).toBe(0);
    expect(m.ishikawaTemplates.length).toBe(0);
    expect(m.pokaYoke.length).toBe(0);
    expect(m.standards).toEqual(['iso-9001', 'iatf-16949']);
    expect(m.glossary.length).toBe(1);
  });

  it('parseManifest() is tolerant of empty / invalid / null input', () => {
    for (const bad of [undefined, null, '', '   ', 'not-json', '[]', '42']) {
      const m = service.parseManifest(bad as string | null | undefined);
      expect(m.rich).toBeFalse();
      expect(m.kpis.length).toBe(0);
      expect(m.glossary.length).toBe(0);
    }
  });
});
