import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, combineLatest, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreateDeviceRequest, DeviceDetail, DeviceHealth, DevicePage, DeviceResponse, DeviceRow,
  DevicesOverview, FleetHealth, IotDeviceStatus, IotDeviceType, TelemetryIngestRequest,
  TelemetryPage, TelemetryResponse, ThresholdPage, ThresholdRequest, ThresholdResponse,
  UpdateDeviceRequest
} from './iot.types';

/**
 * Taille de page maximale acceptée par le serveur
 * (`spring.data.web.pageable.max-page-size: 100` dans application.yml).
 *
 * Au-delà, le serveur RABOTE SILENCIEUSEMENT : demander 500 points ne remonterait
 * jamais que les 100 premiers, sans le moindre signal. Toutes les tailles de page
 * de ce service sont donc bornées ici.
 */
export const MAX_PAGE_SIZE = 100;

/**
 * Parc IoT et télémétrie (§9).
 *
 * L'API `/api/v1/iot` (14 routes de l'engine) n'avait aucun consommateur : l'état de
 * santé du parc — la seule information réellement critique d'une flotte de capteurs —
 * n'était consultable qu'en appelant l'API à la main.
 *
 * Le tenant vient du JWT côté serveur (§18.2 #2) : aucune méthode ne le prend.
 *
 * Périmètre : ce service parle EXCLUSIVEMENT à `IotController` de api-quality-engine,
 * seul service atteignable via `environment.apiBaseUrl`. Le registre du service
 * `api-iot-hub` (port 8083, twin/shadow, rollups) n'est exposé par aucune URL connue
 * du front — on ne fabrique pas une adresse qui n'existe pas.
 */
@Injectable({ providedIn: 'root' })
export class IotService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/iot`;

  constructor(private readonly http: HttpClient) {}

  // ---- Équipements ------------------------------------------------------------

  /**
   * Le serveur n'applique QU'UN seul critère : si `status` est fourni, `type` est
   * ignoré (cf. `IotDeviceService.list`). Les deux ne sont donc jamais envoyés
   * ensemble — l'écran impose l'exclusivité côté filtres.
   */
  listDevices(status: IotDeviceStatus | null, type: IotDeviceType | null,
              page = 0, size = 50): Observable<DevicePage> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', Math.min(size, MAX_PAGE_SIZE));
    if (status) params = params.set('status', status);
    else if (type) params = params.set('type', type);
    return this.http.get<DevicePage>(`${this.endpoint}/devices`, { params });
  }

  getDevice(id: string): Observable<DeviceResponse> {
    return this.http.get<DeviceResponse>(`${this.endpoint}/devices/${id}`);
  }

  createDevice(input: CreateDeviceRequest): Observable<DeviceResponse> {
    return this.http.post<DeviceResponse>(`${this.endpoint}/devices`, input);
  }

  updateDevice(id: string, input: UpdateDeviceRequest): Observable<DeviceResponse> {
    return this.http.patch<DeviceResponse>(`${this.endpoint}/devices/${id}`, input);
  }

  deleteDevice(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/devices/${id}`);
  }

  /** PROVISIONED ou SUSPENDED → ACTIVE. Toute autre transition est refusée (409). */
  activateDevice(id: string): Observable<DeviceResponse> {
    return this.http.post<DeviceResponse>(`${this.endpoint}/devices/${id}/activate`, {});
  }

  /** ACTIVE → SUSPENDED : l'équipement cesse d'être autorisé à émettre. */
  suspendDevice(id: string): Observable<DeviceResponse> {
    return this.http.post<DeviceResponse>(`${this.endpoint}/devices/${id}/suspend`, {});
  }

  /** État terminal : aucune sortie possible côté serveur. */
  decommissionDevice(id: string): Observable<DeviceResponse> {
    return this.http.post<DeviceResponse>(`${this.endpoint}/devices/${id}/decommission`, {});
  }

  // ---- Télémétrie --------------------------------------------------------------

  /** Le serveur refuse (409) toute ingestion sur un équipement non ACTIF. */
  ingestTelemetry(deviceId: string, input: TelemetryIngestRequest): Observable<TelemetryResponse> {
    return this.http.post<TelemetryResponse>(
      `${this.endpoint}/devices/${deviceId}/telemetry`, input);
  }

  /** Mesures les plus récentes d'abord (tri serveur `recordedAt DESC`). */
  recentTelemetry(deviceId: string, page = 0, size = MAX_PAGE_SIZE): Observable<TelemetryPage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', Math.min(size, MAX_PAGE_SIZE));
    return this.http.get<TelemetryPage>(
      `${this.endpoint}/devices/${deviceId}/telemetry`, { params });
  }

  /**
   * Mesures d'une métrique sur une fenêtre, triées par ordre CHRONOLOGIQUE
   * croissant côté serveur. La page 0 renvoie donc le DÉBUT de la fenêtre, pas la
   * fin : l'écran doit annoncer la troncature quand `totalElements` dépasse la page.
   */
  telemetryRange(deviceId: string, metric: string, from: string, to: string,
                 page = 0, size = MAX_PAGE_SIZE): Observable<TelemetryPage> {
    const params = new HttpParams()
      .set('metric', metric)
      .set('from', from)
      .set('to', to)
      .set('page', page)
      .set('size', Math.min(size, MAX_PAGE_SIZE));
    return this.http.get<TelemetryPage>(
      `${this.endpoint}/devices/${deviceId}/telemetry/range`, { params });
  }

  // ---- Seuils de surveillance ---------------------------------------------------

  listThresholds(page = 0, size = MAX_PAGE_SIZE): Observable<ThresholdPage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', Math.min(size, MAX_PAGE_SIZE));
    return this.http.get<ThresholdPage>(`${this.endpoint}/thresholds`, { params });
  }

  createThreshold(input: ThresholdRequest): Observable<ThresholdResponse> {
    return this.http.post<ThresholdResponse>(`${this.endpoint}/thresholds`, input);
  }

  updateThreshold(id: string, input: ThresholdRequest): Observable<ThresholdResponse> {
    return this.http.patch<ThresholdResponse>(`${this.endpoint}/thresholds/${id}`, input);
  }

  deleteThreshold(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/thresholds/${id}`);
  }

  // ---- Vues composées -----------------------------------------------------------

  /**
   * Vue de la liste : la page filtrée pour le tableau, plus un balayage des
   * équipements ACTIFS pour les compteurs de santé.
   *
   * Deux appels sont nécessaires parce qu'aucune route ne renvoie d'agrégat de
   * santé : compter les capteurs muets sur la seule page affichée donnerait un
   * chiffre qui change à chaque pagination. Le balayage est borné à la page
   * maximale du serveur et signale sa propre troncature.
   */
  overview(status: IotDeviceStatus | null, type: IotDeviceType | null,
           page: number, size: number, silenceMs: number): Observable<DevicesOverview> {
    return combineLatest([
      this.listDevices(status, type, page, size),
      this.listDevices('ACTIVE', null, 0, MAX_PAGE_SIZE)
    ]).pipe(
      map(([devicePage, activeScan]) => ({
        page: devicePage,
        rows: sortByHealth(devicePage.content.map(device => toRow(device, silenceMs))),
        fleet: toFleetHealth(activeScan, silenceMs)
      }))
    );
  }

  /**
   * Vue de la fiche : l'équipement, ses dernières mesures et les seuils qui le
   * concernent. `forkJoin` plutôt que des appels séquentiels — les trois routes
   * sont indépendantes et l'écran n'a de sens qu'assemblé.
   */
  detail(deviceId: string, silenceMs: number): Observable<DeviceDetail> {
    return forkJoin({
      device: this.getDevice(deviceId),
      telemetry: this.recentTelemetry(deviceId),
      thresholds: this.listThresholds()
    }).pipe(
      map(({ device, telemetry, thresholds }) => {
        const row = toRow(device, silenceMs);
        return {
          device,
          health: row.health,
          ageMs: row.ageMs,
          telemetry: telemetry.content,
          telemetryTotal: telemetry.totalElements,
          metrics: distinctMetrics(telemetry.content),
          // Un seuil sans `deviceId` porte sur tout le tenant : il s'applique donc
          // aussi à cet équipement et doit être visible depuis sa fiche.
          thresholds: thresholds.content.filter(
            t => t.deviceId === deviceId || t.deviceId === null)
        };
      })
    );
  }
}

/**
 * Santé d'un équipement à partir de la fraîcheur de son dernier signal.
 *
 * Le serveur n'expose aucun indicateur de silence : il n'y a que `lastSeenAt`.
 * La comparaison se fait donc avec l'horloge du poste — un poste déréglé décalera
 * l'appréciation, mais c'est la seule information disponible et un capteur muet
 * doit se voir immédiatement plutôt que pas du tout.
 *
 * `AGING` s'allume à la moitié du seuil de silence : la dérive se voit venir avant
 * que le capteur ne soit déclaré muet.
 */
export function healthOf(device: DeviceResponse, silenceMs: number,
                         now: number = Date.now()): DeviceHealth {
  if (device.status !== 'ACTIVE') return 'INACTIVE';
  if (!device.lastSeenAt) return 'NEVER_SEEN';
  const age = now - Date.parse(device.lastSeenAt);
  if (age > silenceMs) return 'SILENT';
  if (age > silenceMs / 2) return 'AGING';
  return 'LIVE';
}

/** Âge du dernier signal, ou `null` si l'équipement n'a jamais émis. */
export function ageOf(device: DeviceResponse, now: number = Date.now()): number | null {
  if (!device.lastSeenAt) return null;
  return Math.max(0, now - Date.parse(device.lastSeenAt));
}

function toRow(device: DeviceResponse, silenceMs: number): DeviceRow {
  return { device, health: healthOf(device, silenceMs), ageMs: ageOf(device) };
}

/** Un capteur muet passe devant tout le reste : c'est l'anomalie à traiter. */
const HEALTH_RANK: Record<DeviceHealth, number> = {
  SILENT: 0, NEVER_SEEN: 1, AGING: 2, LIVE: 3, INACTIVE: 4
};

export function sortByHealth(rows: DeviceRow[]): DeviceRow[] {
  return [...rows].sort((a, b) =>
    HEALTH_RANK[a.health] - HEALTH_RANK[b.health]
    || a.device.code.localeCompare(b.device.code));
}

function toFleetHealth(scan: DevicePage, silenceMs: number): FleetHealth {
  const healths = scan.content.map(d => healthOf(d, silenceMs));
  const count = (h: DeviceHealth) => healths.filter(x => x === h).length;
  return {
    scanned: scan.content.length,
    total: scan.totalElements,
    truncated: scan.totalElements > scan.content.length,
    live: count('LIVE'),
    aging: count('AGING'),
    silent: count('SILENT'),
    neverSeen: count('NEVER_SEEN')
  };
}

/** Métriques distinctes rencontrées, triées pour un sélecteur stable. */
export function distinctMetrics(events: TelemetryResponse[]): string[] {
  return [...new Set(events.map(e => e.metric))].sort((a, b) => a.localeCompare(b));
}
