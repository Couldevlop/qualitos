import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { IotService, ageOf, distinctMetrics, healthOf, sortByHealth } from './iot.service';
import {
  DevicePage, DeviceResponse, DeviceRow, TelemetryPage, TelemetryResponse, ThresholdPage,
  ThresholdRequest, ThresholdResponse
} from './iot.types';

/**
 * `/api/v1/iot` (14 routes de l'engine) n'avait aucun consommateur : l'état de santé
 * du parc n'était consultable qu'en appelant l'API à la main.
 */
describe('IotService', () => {
  let service: IotService;
  let http: HttpTestingController;

  const base = `${environment.apiBaseUrl}/api/v1/iot`;
  const HOUR = 3_600_000;

  const device = (id: string, over: Partial<DeviceResponse> = {}): DeviceResponse => ({
    id, tenantId: 't1', code: 'DEV-' + id, name: 'Capteur ' + id,
    deviceType: 'SENSOR_TEMPERATURE', protocol: 'MQTT', status: 'ACTIVE',
    location: 'Atelier A', description: null, metadataJson: null,
    lastSeenAt: '2026-07-31T10:00:00.000Z', telemetryCount: 12, createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const devicePage = (content: DeviceResponse[], total = content.length): DevicePage =>
    ({ content, totalElements: total, totalPages: 1, number: 0, size: 50 });

  const event = (id: string, over: Partial<TelemetryResponse> = {}): TelemetryResponse => ({
    id, tenantId: 't1', deviceId: 'd1', metric: 'temperature', valueNumeric: 4.2,
    valueText: null, unit: '°C', source: 'MQTT',
    recordedAt: '2026-07-31T10:00:00.000Z', ingestedAt: '2026-07-31T10:00:01.000Z', ...over
  });

  const telemetryPage = (content: TelemetryResponse[], total = content.length): TelemetryPage =>
    ({ content, totalElements: total, totalPages: 1, number: 0, size: 100 });

  const threshold = (id: string, over: Partial<ThresholdResponse> = {}): ThresholdResponse => ({
    id, tenantId: 't1', deviceId: 'd1', metric: 'temperature', minValue: 2, maxValue: 8,
    capaCriticity: 'HIGH', capaOwnerId: 'u1', enabled: true, fmeaItemId: null,
    openPdcaCycle: false, createdAt: '2026-01-01T00:00:00Z', ...over
  });

  const thresholdPage = (content: ThresholdResponse[]): ThresholdPage =>
    ({ content, totalElements: content.length, totalPages: 1, number: 0, size: 100 });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(IotService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ---- Routes ----------------------------------------------------------------

  it('liste les équipements sans filtre', (done) => {
    service.listDevices(null, null, 0, 25).subscribe(p => {
      expect(p.content.length).toBe(1);
      done();
    });
    const req = http.expectOne(r => r.url === `${base}/devices`);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('25');
    expect(req.request.params.has('status')).toBeFalse();
    expect(req.request.params.has('type')).toBeFalse();
    req.flush(devicePage([device('d1')]));
  });

  it('n’envoie jamais statut et type ensemble : le serveur ignorerait le second', (done) => {
    service.listDevices('ACTIVE', 'CAMERA').subscribe(() => done());
    const req = http.expectOne(r => r.url === `${base}/devices`);
    expect(req.request.params.get('status')).toBe('ACTIVE');
    expect(req.request.params.has('type')).toBeFalse();
    req.flush(devicePage([]));
  });

  it('envoie le type quand aucun statut n’est demandé', (done) => {
    service.listDevices(null, 'GATEWAY').subscribe(() => done());
    const req = http.expectOne(r => r.url === `${base}/devices`);
    expect(req.request.params.get('type')).toBe('GATEWAY');
    req.flush(devicePage([]));
  });

  it('borne la taille de page au maximum accepté par le serveur', (done) => {
    service.listDevices(null, null, 0, 500).subscribe(() => done());
    const req = http.expectOne(r => r.url === `${base}/devices`);
    expect(req.request.params.get('size')).toBe('100');
    req.flush(devicePage([]));
  });

  it('crée, modifie et supprime un équipement', (done) => {
    service.createDevice({
      code: 'DEV-9', name: 'Sonde', deviceType: 'SENSOR_TEMPERATURE',
      protocol: 'MQTT', createdBy: 'u1'
    }).subscribe();
    const created = http.expectOne(`${base}/devices`);
    expect(created.request.method).toBe('POST');
    created.flush(device('d9'));

    service.updateDevice('d9', { name: 'Sonde froide' }).subscribe();
    const patched = http.expectOne(`${base}/devices/d9`);
    expect(patched.request.method).toBe('PATCH');
    expect(patched.request.body).toEqual({ name: 'Sonde froide' });
    patched.flush(device('d9'));

    service.deleteDevice('d9').subscribe(() => done());
    const removed = http.expectOne(`${base}/devices/d9`);
    expect(removed.request.method).toBe('DELETE');
    removed.flush(null);
  });

  it('pilote les trois transitions d’état', (done) => {
    const seen: string[] = [];

    service.activateDevice('d1').subscribe(d => seen.push(d.status));
    http.expectOne(`${base}/devices/d1/activate`).flush(device('d1'));

    service.suspendDevice('d1').subscribe(d => seen.push(d.status));
    http.expectOne(`${base}/devices/d1/suspend`).flush(device('d1', { status: 'SUSPENDED' }));

    service.decommissionDevice('d1').subscribe(d => {
      seen.push(d.status);
      expect(seen).toEqual(['ACTIVE', 'SUSPENDED', 'DECOMMISSIONED']);
      done();
    });
    http.expectOne(`${base}/devices/d1/decommission`)
      .flush(device('d1', { status: 'DECOMMISSIONED' }));
  });

  it('ingère une mesure sur un équipement', (done) => {
    service.ingestTelemetry('d1', { metric: 'temperature', valueNumeric: 9.1, unit: '°C' })
      .subscribe(t => {
        expect(t.metric).toBe('temperature');
        done();
      });
    const req = http.expectOne(`${base}/devices/d1/telemetry`);
    expect(req.request.body).toEqual({ metric: 'temperature', valueNumeric: 9.1, unit: '°C' });
    req.flush(event('e1'));
  });

  it('lit une fenêtre de mesures avec ses bornes temporelles', (done) => {
    service.telemetryRange('d1', 'temperature',
      '2026-07-30T00:00:00.000Z', '2026-07-31T00:00:00.000Z').subscribe(() => done());
    const req = http.expectOne(r => r.url === `${base}/devices/d1/telemetry/range`);
    expect(req.request.params.get('metric')).toBe('temperature');
    expect(req.request.params.get('from')).toBe('2026-07-30T00:00:00.000Z');
    expect(req.request.params.get('to')).toBe('2026-07-31T00:00:00.000Z');
    expect(req.request.params.get('size')).toBe('100');
    req.flush(telemetryPage([event('e1')]));
  });

  it('gère le cycle de vie d’un seuil', (done) => {
    const payload: ThresholdRequest = {
      deviceId: 'd1', metric: 'temperature', minValue: 2, maxValue: 8,
      capaCriticity: 'HIGH', capaOwnerId: 'u1', enabled: true,
      fmeaItemId: null, openPdcaCycle: true
    };

    service.createThreshold(payload).subscribe();
    const created = http.expectOne(`${base}/thresholds`);
    expect(created.request.body).toEqual(payload);
    created.flush(threshold('t1'));

    service.updateThreshold('t1', payload).subscribe();
    const patched = http.expectOne(`${base}/thresholds/t1`);
    expect(patched.request.method).toBe('PATCH');
    patched.flush(threshold('t1'));

    service.deleteThreshold('t1').subscribe(() => done());
    http.expectOne(`${base}/thresholds/t1`).flush(null);
  });

  // ---- Vues composées ---------------------------------------------------------

  it('remonte les capteurs muets en tête et compte la santé de la flotte', (done) => {
    const now = Date.now();
    const fresh = new Date(now - 60_000).toISOString();
    const old = new Date(now - 5 * HOUR).toISOString();

    service.overview(null, null, 0, 25, HOUR).subscribe(o => {
      expect(o.rows.map(r => r.device.id)).toEqual(['d2', 'd3', 'd1', 'd4']);
      expect(o.rows[0].health).toBe('SILENT');
      expect(o.rows[1].health).toBe('NEVER_SEEN');
      expect(o.rows[3].health).toBe('INACTIVE');
      expect(o.fleet.silent).toBe(1);
      expect(o.fleet.neverSeen).toBe(1);
      expect(o.fleet.live).toBe(1);
      expect(o.fleet.truncated).toBeFalse();
      done();
    });

    const content = [
      device('d1', { code: 'C1', lastSeenAt: fresh }),
      device('d2', { code: 'C2', lastSeenAt: old }),
      device('d3', { code: 'C3', lastSeenAt: null }),
      device('d4', { code: 'C4', status: 'SUSPENDED', lastSeenAt: old })
    ];
    const requests = http.match(r => r.url === `${base}/devices`);
    expect(requests.length).toBe(2);
    requests[0].flush(devicePage(content));
    // Le balayage de santé ne porte que sur les équipements en service.
    requests[1].flush(devicePage(content.slice(0, 3)));
  });

  it('signale un balayage tronqué quand la flotte active dépasse une page', (done) => {
    service.overview(null, null, 0, 25, HOUR).subscribe(o => {
      expect(o.fleet.truncated).toBeTrue();
      expect(o.fleet.scanned).toBe(1);
      expect(o.fleet.total).toBe(420);
      done();
    });
    const requests = http.match(r => r.url === `${base}/devices`);
    requests[0].flush(devicePage([device('d1')]));
    requests[1].flush(devicePage([device('d1')], 420));
  });

  it('assemble la fiche : équipement, mesures et seuils applicables', (done) => {
    service.detail('d1', HOUR).subscribe(d => {
      expect(d.device.id).toBe('d1');
      expect(d.telemetryTotal).toBe(340);
      expect(d.metrics).toEqual(['humidity', 'temperature']);
      // Le seuil d'un AUTRE équipement est écarté, celui du tenant est conservé.
      expect(d.thresholds.map(t => t.id)).toEqual(['t1', 't3']);
      done();
    });

    http.expectOne(`${base}/devices/d1`).flush(device('d1'));
    http.expectOne(r => r.url === `${base}/devices/d1/telemetry`)
      .flush(telemetryPage([
        event('e1', { metric: 'temperature' }),
        event('e2', { metric: 'humidity' }),
        event('e3', { metric: 'temperature' })
      ], 340));
    http.expectOne(r => r.url === `${base}/thresholds`).flush(thresholdPage([
      threshold('t1', { deviceId: 'd1' }),
      threshold('t2', { deviceId: 'd2' }),
      threshold('t3', { deviceId: null })
    ]));
  });

  // ---- Calcul de santé ----------------------------------------------------------

  it('déclare muet un équipement actif silencieux au-delà du seuil', () => {
    const now = Date.parse('2026-07-31T12:00:00.000Z');
    expect(healthOf(device('d1', { lastSeenAt: '2026-07-31T11:55:00.000Z' }), HOUR, now))
      .toBe('LIVE');
    expect(healthOf(device('d1', { lastSeenAt: '2026-07-31T11:15:00.000Z' }), HOUR, now))
      .toBe('AGING');
    expect(healthOf(device('d1', { lastSeenAt: '2026-07-31T09:00:00.000Z' }), HOUR, now))
      .toBe('SILENT');
    expect(healthOf(device('d1', { lastSeenAt: null }), HOUR, now)).toBe('NEVER_SEEN');
  });

  it('ne juge pas la fraîcheur d’un équipement hors service', () => {
    const now = Date.parse('2026-07-31T12:00:00.000Z');
    for (const status of ['PROVISIONED', 'SUSPENDED', 'DECOMMISSIONED'] as const) {
      expect(healthOf(device('d1', { status, lastSeenAt: null }), HOUR, now)).toBe('INACTIVE');
    }
  });

  it('calcule l’âge du dernier signal, jamais négatif', () => {
    const now = Date.parse('2026-07-31T12:00:00.000Z');
    expect(ageOf(device('d1', { lastSeenAt: '2026-07-31T11:00:00.000Z' }), now)).toBe(HOUR);
    expect(ageOf(device('d1', { lastSeenAt: null }), now)).toBeNull();
    // Horloge du poste en retard sur le serveur : on n'affiche pas un âge négatif.
    expect(ageOf(device('d1', { lastSeenAt: '2026-07-31T13:00:00.000Z' }), now)).toBe(0);
  });

  it('trie les lignes par criticité de santé puis par code', () => {
    const rows: DeviceRow[] = [
      { device: device('a', { code: 'Z' }), health: 'LIVE', ageMs: 0 },
      { device: device('b', { code: 'B' }), health: 'SILENT', ageMs: 9 },
      { device: device('c', { code: 'A' }), health: 'SILENT', ageMs: 9 },
      { device: device('d', { code: 'D' }), health: 'INACTIVE', ageMs: null }
    ];
    expect(sortByHealth(rows).map(r => r.device.code)).toEqual(['A', 'B', 'Z', 'D']);
  });

  it('déduplique et trie les métriques rencontrées', () => {
    expect(distinctMetrics([
      event('e1', { metric: 'temperature' }),
      event('e2', { metric: 'humidity' }),
      event('e3', { metric: 'temperature' })
    ])).toEqual(['humidity', 'temperature']);
  });
});
