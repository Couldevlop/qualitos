import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ModuleActivation, ModuleCatalogEntry, TenantModuleSummary } from './admin.types';
import { TenantModulesService } from './tenant-modules.service';

/**
 * `/api/v1/tenant-modules` (15 routes) n'avait aucun consommateur : la promesse
 * « modulable, désactivation à chaud » n'était pilotable qu'en appelant l'API à la main.
 */
describe('TenantModulesService', () => {
  let service: TenantModulesService;
  let http: HttpTestingController;

  const base = `${environment.apiBaseUrl}/api/v1/tenant-modules`;

  const entry = (code: string, over: Partial<ModuleCatalogEntry> = {}): ModuleCatalogEntry => ({
    code, name: code.toUpperCase(), category: 'Qualité', minimumTier: 'STANDARD',
    dependencies: [], coreModule: false, ...over
  });

  const activation = (code: string, over: Partial<ModuleActivation> = {}): ModuleActivation => ({
    id: 'a-' + code, tenantId: 't1', moduleCode: code, status: 'ACTIVE', enabled: true,
    billingTier: 'STANDARD', configurationJson: null, trialEndsAt: null, expiresAt: null,
    activatedAt: null, activatedBy: null, statusChangedAt: null, lastChangedBy: null,
    updatedAt: null, ...over
  });

  const summary = (activations: ModuleActivation[]): TenantModuleSummary => ({
    tenantId: 't1', tenantTier: 'PRO', totalActivations: activations.length,
    enabledCount: activations.filter(a => a.enabled).length,
    trialCount: 0, activeCount: activations.length, suspendedCount: 0,
    expiredCount: 0, disabledCount: 0, activations
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(TenantModulesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('joint le catalogue aux activations du tenant', (done) => {
    service.overview().subscribe(({ rows, summary: s }) => {
      expect(rows.length).toBe(2);
      expect(s.tenantTier).toBe('PRO');
      const pdca = rows.find(r => r.entry.code === 'pdca');
      expect(pdca?.activation?.status).toBe('ACTIVE');
      done();
    });

    http.expectOne(`${base}/catalog`).flush([entry('pdca'), entry('iot')]);
    http.expectOne(`${base}/summary`).flush(summary([activation('pdca')]));
  });

  it('conserve les modules jamais activés, sinon on ne pourrait pas les activer', (done) => {
    service.overview().subscribe(({ rows }) => {
      const iot = rows.find(r => r.entry.code === 'iot');
      expect(iot).toBeDefined();
      expect(iot?.activation).toBeNull();
      done();
    });

    http.expectOne(`${base}/catalog`).flush([entry('iot')]);
    http.expectOne(`${base}/summary`).flush(summary([]));
  });

  it('trie par catégorie puis par nom pour un regroupement lisible', (done) => {
    service.overview().subscribe(({ rows }) => {
      expect(rows.map(r => r.entry.code)).toEqual(['b', 'a', 'c']);
      done();
    });

    http.expectOne(`${base}/catalog`).flush([
      entry('c', { category: 'Z', name: 'C' }),
      entry('a', { category: 'A', name: 'ZZ' }),
      entry('b', { category: 'A', name: 'AA' })
    ]);
    http.expectOne(`${base}/summary`).flush(summary([]));
  });

  it('active un module', (done) => {
    service.activate('iot').subscribe(a => {
      expect(a.moduleCode).toBe('iot');
      done();
    });
    const req = http.expectOne(`${base}/activations`);
    expect(req.request.body).toEqual({ moduleCode: 'iot', expiresAt: null });
    req.flush(activation('iot'));
  });

  it('démarre un essai avec sa date de fin', (done) => {
    service.startTrial('iot', '2026-09-01T00:00:00Z').subscribe(() => done());
    const req = http.expectOne(`${base}/activations/trial`);
    expect(req.request.body).toEqual({ moduleCode: 'iot', trialEndsAt: '2026-09-01T00:00:00Z' });
    req.flush(activation('iot', { status: 'TRIAL' }));
  });

  it('convertit un essai en activation', (done) => {
    service.convertTrial('a-iot').subscribe(() => done());
    http.expectOne(`${base}/activations/a-iot/convert`).flush(activation('iot'));
  });

  it('suspend, reprend et désactive une activation', (done) => {
    service.suspend('a1').subscribe();
    http.expectOne(`${base}/activations/a1/suspend`).flush(activation('x', { status: 'SUSPENDED' }));

    service.resume('a1').subscribe();
    http.expectOne(`${base}/activations/a1/resume`).flush(activation('x'));

    service.disable('a1').subscribe(() => done());
    http.expectOne(`${base}/activations/a1/disable`).flush(activation('x', { status: 'DISABLED' }));
  });

  it('change le palier de facturation', (done) => {
    service.changeTier('a1', 'ENTERPRISE').subscribe(() => done());
    const req = http.expectOne(`${base}/activations/a1/tier`);
    expect(req.request.body).toEqual({ newTier: 'ENTERPRISE' });
    req.flush(activation('x', { billingTier: 'ENTERPRISE' }));
  });
});
