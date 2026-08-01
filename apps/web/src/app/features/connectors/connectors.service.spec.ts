import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ConnectorsService } from './connectors.service';
import {
  CommConnection, ConnectorPage, EhrConnection, ErpConnection
} from './connectors.types';

/**
 * Les trois contrôleurs de connecteurs (`/api/v1/erp`, `/api/v1/ehr`, `/api/v1/comm`)
 * n'avaient aucun consommateur. Les tests verrouillent surtout deux invariants :
 * les URL réellement exposées par les contrôleurs, et le plafond de pagination du
 * serveur (100), qui rabote silencieusement au-delà.
 */
describe('ConnectorsService', () => {
  let service: ConnectorsService;
  let http: HttpTestingController;

  const erpBase = `${environment.apiBaseUrl}/api/v1/erp/connections`;
  const ehrBase = `${environment.apiBaseUrl}/api/v1/ehr/connections`;
  const commBase = `${environment.apiBaseUrl}/api/v1/comm/connections`;

  const erp = (over: Partial<ErpConnection> = {}): ErpConnection => ({
    id: 'e-1', tenantId: 't-1', name: 'SAP prod', provider: 'SAP',
    baseUrl: 'https://erp.example/odata', username: 'svc', externalScope: null,
    status: 'ACTIVE', consecutiveFailures: 0, lastSyncAt: null, lastSuccessAt: null,
    createdBy: 'u-1', createdAt: null, updatedAt: null, ...over
  });

  const ehr = (over: Partial<EhrConnection> = {}): EhrConnection => ({
    id: 'h-1', tenantId: 't-1', name: 'CHU', provider: 'FHIR_R5',
    fhirBaseUrl: 'https://fhir.example/R5', authMode: 'BASIC', username: 'svc',
    resourceCategory: null, status: 'ACTIVE', consecutiveFailures: 0,
    lastSyncAt: null, lastSuccessAt: null, createdBy: 'u-1', createdAt: null, updatedAt: null, ...over
  });

  const comm = (over: Partial<CommConnection> = {}): CommConnection => ({
    id: 'c-1', tenantId: 't-1', name: 'Alertes', provider: 'SLACK', channel: '#qualite',
    status: 'ACTIVE', consecutiveFailures: 0, lastNotifiedAt: null, lastSuccessAt: null,
    createdBy: 'u-1', createdAt: null, updatedAt: null, ...over
  });

  const pageOf = <T>(content: T[]): ConnectorPage<T> => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: 20
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ConnectorsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ---- Pagination ------------------------------------------------------------

  it('transmet page et taille aux trois listes', () => {
    service.listErp(2, 50).subscribe();
    const erpReq = http.expectOne(r => r.url === erpBase);
    expect(erpReq.request.params.get('page')).toBe('2');
    expect(erpReq.request.params.get('size')).toBe('50');
    erpReq.flush(pageOf([erp()]));

    service.listEhr(1, 10).subscribe();
    const ehrReq = http.expectOne(r => r.url === ehrBase);
    expect(ehrReq.request.params.get('page')).toBe('1');
    ehrReq.flush(pageOf([ehr()]));

    service.listComm(0, 20).subscribe();
    const commReq = http.expectOne(r => r.url === commBase);
    expect(commReq.request.params.get('size')).toBe('20');
    commReq.flush(pageOf([comm()]));
  });

  it('borne la taille au plafond serveur : au-delà, la page serait rabotée sans erreur', () => {
    service.listErp(0, 500).subscribe();
    const req = http.expectOne(r => r.url === erpBase);
    expect(req.request.params.get('size')).toBe('100');
    req.flush(pageOf([]));
  });

  it('refuse un index négatif et une taille nulle plutôt que de laisser le serveur trancher', () => {
    service.listComm(-3, 0).subscribe();
    const req = http.expectOne(r => r.url === commBase);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('1');
    req.flush(pageOf([]));
  });

  // ---- ERP -------------------------------------------------------------------

  it('crée une connexion ERP sur la route du contrôleur', (done) => {
    service.createErp({
      name: 'SAP prod', provider: 'SAP', baseUrl: 'https://erp.example/odata',
      secret: 'topsecret', createdBy: 'u-1'
    }).subscribe(c => { expect(c.id).toBe('e-1'); done(); });

    const req = http.expectOne(erpBase);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.secret).toBe('topsecret');
    req.flush(erp());
  });

  it('met à jour une connexion ERP en PATCH', (done) => {
    service.updateErp('e-1', { status: 'DISABLED' }).subscribe(() => done());
    const req = http.expectOne(`${erpBase}/e-1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'DISABLED' });
    req.flush(erp({ status: 'DISABLED' }));
  });

  it('supprime une connexion ERP', (done) => {
    service.deleteErp('e-1').subscribe(() => done());
    const req = http.expectOne(`${erpBase}/e-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('lance une synchronisation ERP et remonte le rapport', (done) => {
    service.syncErp('e-1').subscribe(report => {
      expect(report.suppliersImported).toBe(4);
      expect(report.errorMessage).toBeNull();
      done();
    });
    const req = http.expectOne(`${erpBase}/e-1/sync`);
    expect(req.request.method).toBe('POST');
    req.flush({
      connectionId: 'e-1', suppliersImported: 4, suppliersIgnored: 1,
      kpisImported: 12, kpisIgnored: 0, ranAt: '2026-07-01T08:00:00Z', errorMessage: null
    });
  });

  it('remonte un rapport ERP en échec sans le transformer en erreur HTTP', (done) => {
    service.syncErp('e-1').subscribe(report => {
      expect(report.errorMessage).toBe('Secret decryption failed');
      done();
    });
    http.expectOne(`${erpBase}/e-1/sync`).flush({
      connectionId: 'e-1', suppliersImported: 0, suppliersIgnored: 0,
      kpisImported: 0, kpisIgnored: 0, ranAt: '2026-07-01T08:00:00Z',
      errorMessage: 'Secret decryption failed'
    });
  });

  // ---- EHR -------------------------------------------------------------------

  it('crée une connexion FHIR avec son mode d\'authentification', (done) => {
    service.createEhr({
      name: 'CHU', provider: 'FHIR_R5', fhirBaseUrl: 'https://fhir.example/R5',
      authMode: 'BEARER', secret: 'jeton-porteur', createdBy: 'u-1'
    }).subscribe(() => done());

    const req = http.expectOne(ehrBase);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.authMode).toBe('BEARER');
    req.flush(ehr({ authMode: 'BEARER' }));
  });

  it('met à jour et supprime une connexion FHIR', (done) => {
    service.updateEhr('h-1', { name: 'CHU Nord' }).subscribe(c => expect(c.name).toBe('CHU Nord'));
    const patch = http.expectOne(`${ehrBase}/h-1`);
    expect(patch.request.method).toBe('PATCH');
    patch.flush(ehr({ name: 'CHU Nord' }));

    service.deleteEhr('h-1').subscribe(() => done());
    const del = http.expectOne(`${ehrBase}/h-1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('lance une synchronisation FHIR et remonte ses compteurs', (done) => {
    service.syncEhr('h-1').subscribe(report => {
      expect(report.created).toBe(3);
      expect(report.skipped).toBe(9);
      done();
    });
    const req = http.expectOne(`${ehrBase}/h-1/sync`);
    expect(req.request.method).toBe('POST');
    req.flush({
      connectionId: 'h-1', totalFetched: 12, created: 3, skipped: 9, errors: 0,
      ranAt: '2026-07-01T08:00:00Z', errorMessage: null
    });
  });

  // ---- Communication ---------------------------------------------------------

  it('crée une destination de notification', (done) => {
    service.createComm({
      name: 'Alertes', provider: 'SLACK',
      webhookUrl: 'https://hooks.slack.com/services/abc', channel: '#qualite',
      createdBy: 'u-1'
    }).subscribe(() => done());

    const req = http.expectOne(commBase);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.webhookUrl).toBe('https://hooks.slack.com/services/abc');
    req.flush(comm());
  });

  it('met à jour et supprime une destination', (done) => {
    service.updateComm('c-1', { channel: '#hse' }).subscribe(c => expect(c.channel).toBe('#hse'));
    const patch = http.expectOne(`${commBase}/c-1`);
    expect(patch.request.method).toBe('PATCH');
    patch.flush(comm({ channel: '#hse' }));

    service.deleteComm('c-1').subscribe(() => done());
    const del = http.expectOne(`${commBase}/c-1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('envoie un test de destination et remonte l\'échec applicatif tel quel', (done) => {
    service.testComm('c-1').subscribe(result => {
      expect(result.success).toBeFalse();
      expect(result.errorMessage).toBe('HTTP 404');
      done();
    });
    const req = http.expectOne(`${commBase}/c-1/test`);
    expect(req.request.method).toBe('POST');
    req.flush({ connectionId: 'c-1', success: false, errorMessage: 'HTTP 404' });
  });

  it('ne renvoie jamais de secret dans les réponses de liste', (done) => {
    service.listComm(0, 20).subscribe(page => {
      // Le contrat serveur exclut l'URL du webhook : l'écran n'a donc rien à masquer.
      expect(Object.keys(page.content[0])).not.toContain('webhookUrl');
      done();
    });
    http.expectOne(r => r.url === commBase).flush(pageOf([comm()]));
  });
});
