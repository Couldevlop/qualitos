import {
  CapaCriticity, DeviceHealth, IotDeviceStatus, IotDeviceType, IotProtocol
} from './iot.types';

/**
 * Libellés et tonalités partagés par les deux écrans du parc IoT (§9).
 *
 * Centralisés ici parce que la liste et la fiche affichent EXACTEMENT les mêmes
 * badges : dupliquer les tables ferait diverger les tonalités au premier ajout
 * de statut, et un même équipement changerait de couleur selon l'écran.
 */

export type Tone = 'neutral' | 'success' | 'warn' | 'danger';

export function healthLabel(health: DeviceHealth): string {
  return ({
    LIVE: $localize`:@@iot.health.live:En ligne`,
    AGING: $localize`:@@iot.health.aging:Signal vieillissant`,
    SILENT: $localize`:@@iot.health.silent:Muet`,
    NEVER_SEEN: $localize`:@@iot.health.never:Jamais vu`,
    INACTIVE: $localize`:@@iot.health.inactive:Hors surveillance`
  })[health];
}

export function healthTone(health: DeviceHealth): Tone {
  if (health === 'LIVE') return 'success';
  if (health === 'AGING') return 'warn';
  if (health === 'SILENT' || health === 'NEVER_SEEN') return 'danger';
  return 'neutral';
}

export function statusLabel(status: IotDeviceStatus): string {
  return ({
    PROVISIONED: $localize`:@@iot.status.provisioned:Provisionné`,
    ACTIVE: $localize`:@@iot.status.active:En service`,
    SUSPENDED: $localize`:@@iot.status.suspended:Suspendu`,
    DECOMMISSIONED: $localize`:@@iot.status.decommissioned:Décommissionné`
  })[status];
}

export function statusTone(status: IotDeviceStatus): Tone {
  if (status === 'ACTIVE') return 'success';
  if (status === 'SUSPENDED') return 'warn';
  return 'neutral';
}

/** Libellés métier des types d'équipement (§9.2). */
export function typeLabel(type: IotDeviceType): string {
  return ({
    PLC: $localize`:@@iot.type.plc:Automate (PLC)`,
    SENSOR_TEMPERATURE: $localize`:@@iot.type.temperature:Sonde de température`,
    SENSOR_VIBRATION: $localize`:@@iot.type.vibration:Capteur de vibration`,
    SENSOR_PRESSURE: $localize`:@@iot.type.pressure:Capteur de pression`,
    SENSOR_HUMIDITY: $localize`:@@iot.type.humidity:Capteur d'humidité`,
    SENSOR_GENERIC: $localize`:@@iot.type.generic:Capteur générique`,
    CAMERA: $localize`:@@iot.type.camera:Caméra`,
    BIOMED: $localize`:@@iot.type.biomed:Dispositif biomédical`,
    AGRO_STATION: $localize`:@@iot.type.agro:Station agro`,
    BUILDING_BMS: $localize`:@@iot.type.bms:GTB / bâtiment`,
    GATEWAY: $localize`:@@iot.type.gateway:Passerelle Edge`,
    UNKNOWN: $localize`:@@iot.type.unknown:Non renseigné`
  })[type];
}

/**
 * Les protocoles sont des noms propres normalisés (OPC-UA, MQTT, LoRaWAN…) : on ne
 * les traduit pas, on applique seulement la typographie usuelle du secteur. Seul
 * le relevé manuel, qui décrit un mode opératoire et non un standard, est traduit.
 */
export function protocolLabel(protocol: IotProtocol): string {
  if (protocol === 'MANUAL') return $localize`:@@iot.protocol.manual:Relevé manuel`;
  return ({
    OPC_UA: 'OPC-UA',
    MQTT: 'MQTT',
    SPARKPLUG_B: 'Sparkplug B',
    MODBUS_TCP: 'Modbus TCP',
    HL7_FHIR: 'HL7 FHIR',
    HL7_V2: 'HL7 v2',
    DICOM: 'DICOM',
    LORAWAN: 'LoRaWAN',
    MANUAL: 'MANUAL'
  })[protocol];
}

export function criticityLabel(criticity: CapaCriticity): string {
  return ({
    LOW: $localize`:@@iot.criticity.low:Faible`,
    MEDIUM: $localize`:@@iot.criticity.medium:Moyenne`,
    HIGH: $localize`:@@iot.criticity.high:Élevée`,
    CRITICAL: $localize`:@@iot.criticity.critical:Critique`
  })[criticity];
}

export function criticityTone(criticity: CapaCriticity): Tone {
  if (criticity === 'CRITICAL' || criticity === 'HIGH') return 'danger';
  if (criticity === 'MEDIUM') return 'warn';
  return 'neutral';
}

/**
 * Âge d'un signal en durée courte (« 12 min », « 3 h », « 5 j »).
 *
 * Aucune phrase n'est construite par concaténation de mots : l'ordre des termes
 * varie d'une langue à l'autre (arabe, japonais), la durée nue reste juste partout
 * et l'horodatage exact est disponible en infobulle.
 */
export function ageLabel(ageMs: number | null): string {
  if (ageMs === null) return $localize`:@@iot.age.never:Jamais`;
  if (ageMs < 60_000) return $localize`:@@iot.age.now:À l'instant`;
  if (ageMs < 3_600_000) {
    return `${Math.floor(ageMs / 60_000)} ${$localize`:@@iot.unit.minutes:min`}`;
  }
  if (ageMs < 172_800_000) {
    return `${Math.floor(ageMs / 3_600_000)} ${$localize`:@@iot.unit.hours:h`}`;
  }
  return `${Math.floor(ageMs / 86_400_000)} ${$localize`:@@iot.unit.days:j`}`;
}
