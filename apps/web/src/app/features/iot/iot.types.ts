import { SpringPage } from '../pdca/pdca.types';

/**
 * Parc IoT et télémétrie (§9).
 * Contrat de `/api/v1/iot` tel qu'exposé par `IotController` de api-quality-engine
 * (IotDto, IotDeviceStatus, IotDeviceType, IotProtocol).
 *
 * Le tenant n'apparaît jamais en entrée : il est dérivé du JWT côté serveur
 * (règle §18.2 #2). Il figure en sortie parce que le serveur le renvoie.
 */

/** Cycle de vie d'un équipement. `DECOMMISSIONED` est terminal côté serveur. */
export type IotDeviceStatus = 'PROVISIONED' | 'ACTIVE' | 'SUSPENDED' | 'DECOMMISSIONED';

/** Catégories d'équipements supportées en V1 (§9.2). */
export type IotDeviceType =
  | 'PLC'
  | 'SENSOR_TEMPERATURE'
  | 'SENSOR_VIBRATION'
  | 'SENSOR_PRESSURE'
  | 'SENSOR_HUMIDITY'
  | 'SENSOR_GENERIC'
  | 'CAMERA'
  | 'BIOMED'
  | 'AGRO_STATION'
  | 'BUILDING_BMS'
  | 'GATEWAY'
  | 'UNKNOWN';

/** Protocoles d'ingestion supportés (§9.4). `MANUAL` = relevé REST direct. */
export type IotProtocol =
  | 'OPC_UA'
  | 'MQTT'
  | 'SPARKPLUG_B'
  | 'MODBUS_TCP'
  | 'HL7_FHIR'
  | 'HL7_V2'
  | 'DICOM'
  | 'LORAWAN'
  | 'MANUAL';

/** Criticité de la CAPA ouverte quand un seuil est franchi (§9.9). */
export type CapaCriticity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

/** Ordre d'affichage des types et protocoles dans les listes déroulantes. */
export const DEVICE_TYPES: IotDeviceType[] = [
  'SENSOR_TEMPERATURE', 'SENSOR_VIBRATION', 'SENSOR_PRESSURE', 'SENSOR_HUMIDITY',
  'SENSOR_GENERIC', 'PLC', 'CAMERA', 'BIOMED', 'AGRO_STATION', 'BUILDING_BMS',
  'GATEWAY', 'UNKNOWN'
];

export const PROTOCOLS: IotProtocol[] = [
  'MQTT', 'OPC_UA', 'SPARKPLUG_B', 'MODBUS_TCP', 'LORAWAN',
  'HL7_FHIR', 'HL7_V2', 'DICOM', 'MANUAL'
];

// ----- Équipement -----

export interface DeviceResponse {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  deviceType: IotDeviceType;
  protocol: IotProtocol;
  status: IotDeviceStatus;
  location: string | null;
  description: string | null;
  metadataJson: string | null;
  /** Horodatage d'ingestion de la dernière mesure. `null` = aucun signal reçu. */
  lastSeenAt: string | null;
  telemetryCount: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export type DevicePage = SpringPage<DeviceResponse>;

export interface CreateDeviceRequest {
  code: string;
  name: string;
  deviceType: IotDeviceType;
  protocol: IotProtocol;
  location?: string;
  description?: string;
  metadataJson?: string;
  /** UUID de l'utilisateur courant : exigé (@NotNull) par le serveur. */
  createdBy: string;
}

/**
 * PATCH partiel : le serveur n'applique que les champs non `null`. Un champ omis
 * conserve donc sa valeur — cette route ne permet pas de vider un champ existant.
 * Le `code` n'y figure pas : il est immuable après création (unicité par tenant).
 */
export interface UpdateDeviceRequest {
  name?: string;
  deviceType?: IotDeviceType;
  protocol?: IotProtocol;
  location?: string;
  description?: string;
  metadataJson?: string;
}

// ----- Télémétrie -----

export interface TelemetryResponse {
  id: string;
  tenantId: string;
  deviceId: string;
  metric: string;
  /** `null` quand la mesure est textuelle (état, code défaut…). */
  valueNumeric: number | null;
  valueText: string | null;
  unit: string | null;
  source: IotProtocol;
  recordedAt: string;
  ingestedAt: string;
}

export type TelemetryPage = SpringPage<TelemetryResponse>;

export interface TelemetryIngestRequest {
  metric: string;
  valueNumeric?: number;
  valueText?: string;
  unit?: string;
  /** Si omis, le serveur horodate à la réception. */
  recordedAt?: string;
  /** Si omis, le serveur enregistre `MANUAL`. */
  source?: IotProtocol;
}

// ----- Seuils de surveillance (§9.7, §9.9) -----

export interface ThresholdResponse {
  id: string;
  tenantId: string;
  /** `null` = seuil applicable à tous les équipements du tenant pour la métrique. */
  deviceId: string | null;
  metric: string;
  minValue: number | null;
  maxValue: number | null;
  capaCriticity: CapaCriticity;
  capaOwnerId: string;
  enabled: boolean;
  fmeaItemId: string | null;
  openPdcaCycle: boolean;
  createdAt: string;
}

export type ThresholdPage = SpringPage<ThresholdResponse>;

/**
 * Le serveur valide qu'au moins une borne est fournie et que `minValue <= maxValue`.
 * `PATCH` remplace intégralement le seuil : tous les champs sont renvoyés, pas
 * seulement ceux modifiés.
 */
export interface ThresholdRequest {
  deviceId: string | null;
  metric: string;
  minValue: number | null;
  maxValue: number | null;
  capaCriticity: CapaCriticity;
  capaOwnerId: string;
  enabled: boolean;
  fmeaItemId: string | null;
  openPdcaCycle: boolean;
}

// ----- Vues composées (côté écran, pas côté serveur) -----

/**
 * Santé d'un équipement, dérivée de la fraîcheur du dernier signal.
 *
 * `INACTIVE` couvre tout ce qui n'est pas `ACTIVE` : le serveur refuse la
 * télémétrie de ces équipements, la notion de fraîcheur n'y a donc aucun sens.
 */
export type DeviceHealth = 'LIVE' | 'AGING' | 'SILENT' | 'NEVER_SEEN' | 'INACTIVE';

export interface DeviceRow {
  device: DeviceResponse;
  health: DeviceHealth;
  /** Âge du dernier signal en millisecondes ; `null` si aucun signal reçu. */
  ageMs: number | null;
}

/**
 * Comptage de santé sur les équipements ACTIFS.
 *
 * `truncated` signale que le tenant compte plus d'équipements actifs que la page
 * maximale autorisée par le serveur : les compteurs ne portent alors que sur
 * l'échantillon rapatrié, et l'écran le dit explicitement plutôt que d'afficher
 * un total faux.
 */
export interface FleetHealth {
  scanned: number;
  total: number;
  truncated: boolean;
  live: number;
  aging: number;
  silent: number;
  neverSeen: number;
}

export interface DevicesOverview {
  page: DevicePage;
  rows: DeviceRow[];
  fleet: FleetHealth;
}

export interface DeviceDetail {
  device: DeviceResponse;
  health: DeviceHealth;
  ageMs: number | null;
  /** Les 100 dernières mesures (toutes métriques), les plus récentes d'abord. */
  telemetry: TelemetryResponse[];
  /** Nombre total de mesures côté serveur pour cet équipement. */
  telemetryTotal: number;
  /** Métriques distinctes présentes dans les mesures rapatriées. */
  metrics: string[];
  /** Seuils applicables : ceux de l'équipement + ceux portant sur tout le tenant. */
  thresholds: ThresholdResponse[];
}

/** Fenêtre d'analyse de la courbe de télémétrie. */
export type TelemetryWindow = 'RECENT' | 'H24' | 'D7' | 'D30';
