# ADR 0026 — Connecteur d'ingestion LoRaWAN (TTN / ChirpStack)

- **Statut** : Accepté
- **Date** : 2026-06-16
- **Owners** : @Couldevlop
- **Phase** : P3 (Module IoT/Edge Connectivity, CLAUDE.md §9.4)

## Contexte

CLAUDE.md §9.4 liste **LoRaWAN** (capteurs autonomes longue portée — agriculture,
chaîne du froid, smart city) parmi les protocoles V1. Les connecteurs MQTT, OPC-UA et
HL7 FHIR étaient déjà livrés ; LoRaWAN restait STUB (`docs/AUDIT-stub-vs-reel.md`).

Un uplink LoRaWAN ne transite pas par notre API métier : il arrive d'un **Network Server**
(The Things Stack / ChirpStack) en machine-to-machine, **sans JWT par device**. Le NS livre
déjà la charge utile **décodée** par son payload formatter (JS). Deux tensions :

1. **Résolution tenant (§18.2 #2)** — pas d'identité par device, donc le tenant ne peut
   venir ni d'un claim par uplink, ni (interdit) de la charge utile.
2. **Robustesse** — un uplink agrégé contient plusieurs mesures hétérogènes ; une mesure
   pourrie ne doit pas faire échouer le lot.

## Décision

Connecteur **entrant en webhook**, calqué sur le couple FHIR/MQTT, en Clean Architecture
hexagonale dans `apps/api-iot-hub` (`infrastructure/lorawan/` + `presentation/rest/`).

### Format d'uplink retenu (générique TTN/ChirpStack, déjà décodé)

```json
{
  "deviceName": "CAPTEUR-SOL-01",
  "decoded":   { "temperature": 4.2, "humidity": 55 },
  "units":     { "temperature": "degC", "humidity": "%RH" },
  "time":      "2026-06-16T08:30:00Z"
}
```

- Le **champ code device est configurable** (`device-id-field`, défaut `deviceName` ;
  `devEUI` pour ChirpStack) car les NS diffèrent.
- Chaque entrée numérique finie de `decoded` → une mesure `{metric, value, unit?, recordedAt?}`.
  Les valeurs non numériques (texte, booléen, objet, null) sont **ignorées et comptées**.
- `time` ISO-8601 (offset toléré) ; absent/invalide → horodatage serveur en aval.

### Résolution tenant — chemin MQTT (et **non** FHIR)

Le device est résolu par son code via `DeviceRepository.findUniqueByCode(String)` (lookup
tenant-agnostique) ; le tenant qui fait foi est `Device.tenantId()`. Toute **collision
cross-tenant** du code → `Optional.empty()` → **rejet fail-closed** (on ne devine jamais
le tenant — anti-IDOR A01). Aucun tenant n'est jamais lu de la charge utile, même si elle
en contient un (ignoré).

### Composants

- `LoRaWanUplinkDecoder` — décodeur pur (Jackson `JsonNode`, NumPy-free), ne lève jamais ;
  renvoie `DecodeResult(deviceCode, measurements, droppedFields)`.
- `LoRaWanUplinkHandler` — handler pur ; résout le device, ingère via `IngestTelemetryUseCase`,
  renvoie `Outcome(ingested, dropped, issues)` (issues sans PII, sanitizées CRLF/longueur —
  A09). Borne dure `maxMeasurements` (OWASP A04, anti-DoS). Une mesure en échec n'interrompt
  pas l'uplink.
- `LoRaWanProperties` (`qualitos.iot.lorawan.*`) — `enabled` défaut **false**,
  `device-id-field` défaut `deviceName`, `max-measurements` défaut 50.
- `LoRaWanConnectorConfig` — beans gardés par le flag ; rien n'est chargé sans opt-in.
- `LoRaWanController` — `POST /api/v1/iot/lorawan/uplink`, `@PreAuthorize` sur
  `DEVICE/GATEWAY/TENANT_ADMIN/QUALITY_MANAGER` (mêmes rôles que l'ingestion télémétrie),
  gating `@ConditionalOnProperty` → 404 si désactivé. 202 si ≥1 mesure ingérée, sinon 422.

## Justification

- **Calquer MQTT** (et non FHIR) est imposé par l'absence de JWT par device : FHIR scope la
  résolution au tenant du JWT (`findByCode(tenant, code)`), inapplicable ici.
- **Charge utile décodée** plutôt que décodage de frames LoRaWAN bruts : les NS exposent
  nativement le payload formatter ; QualitOS reste agnostique du codec applicatif du capteur
  (un nouveau capteur = un formatter côté NS, zéro code core — cf. §9.10).
- **Désactivé par défaut** : aucune surface exposée sans opt-in (cohérent MQTT/OPC-UA, A05).

## Conséquences

- ✅ Cas d'usage agro §9.9 (capteurs sol/météo longue portée → télémétrie → seuils → CAPA
  via ADR 0016) fonctionnel end-to-end, 100 % testable sous H2.
- ✅ Multi-tenant fail-closed garanti, aucun tenant depuis le réseau.
- ⚠ Le décodage de frames bruts (devAddr/FCnt/MIC, déchiffrement AppSKey) reste **hors
  périmètre** : délégué au Network Server. Sparkplug B / Modbus / DICOM restent STUB.
- ⚠ Pas de vérification de signature webhook NS (HMAC) à ce stade : à ajouter côté gateway
  (mTLS) ou en en-tête signé si exposition directe Internet.

## Tests d'invariant

- `LoRaWanUplinkDecoderTest` (13 cas) — parsing, unités, offset, robustesse champs pourris.
- `LoRaWanUplinkHandlerTest` (11 cas) — ingestion, tenant du device, **collision cross-tenant
  fail-closed**, device inconnu, payload pourri, troncature, échec aval, sanitization A09.
- `LoRaWanControllerIntegrationTest` (5 cas, @SpringBootTest H2) — 401 sans JWT, 202/422,
  cross-tenant fail-closed.
- `LoRaWanConnectorDisabledTest` — beans absents sans opt-in.
- `CleanArchitectureTest` (ArchUnit) — couches respectées.
- Gate JaCoCo module (85 % ligne / 75 % branche) : `verify` vert.

## Références

- CLAUDE.md §9.4 (protocoles), §9.10 (adaptation domaine), §18.2 #2 (tenant), §11.1 (OWASP).
- ADR 0001 (tenant via JWT), ADR 0007 (connecteurs SPI), ADR 0016 (seuil IoT → CAPA).
- Patrons calqués : connecteur FHIR (`infrastructure/fhir/`), handler MQTT (`infrastructure/mqtt/`).
