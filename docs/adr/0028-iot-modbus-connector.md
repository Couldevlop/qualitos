# ADR 0028 — Connecteur d'ingestion Modbus TCP/RTU (lecture structurée par la passerelle Edge)

- **Statut** : Accepté
- **Date** : 2026-06-16
- **Owners** : @Couldevlop
- **Phase** : P3 (Module IoT/Edge Connectivity, CLAUDE.md §9.4)

## Contexte

CLAUDE.md §9.4 liste **Modbus TCP / RTU** (équipements legacy, PLC, capteurs industriels)
parmi les protocoles V1. Les connecteurs MQTT, OPC-UA, HL7 FHIR et LoRaWAN étaient déjà
livrés ; Modbus restait STUB (`docs/AUDIT-stub-vs-reel.md`).

Modbus est un protocole **bas niveau, sans sémantique** : des registres 16 bits (holding,
input, coils) lus en blocs, dont l'interprétation (entier/float 32 bits sur deux registres,
endianness mot/octet, échelle) dépend entièrement de la cartographie de l'équipement. Deux
tensions :

1. **Où placer le décodage du fil** — déchiffrer des registres bruts impose une lib Modbus
   et une carte de registres par équipement ; le faire dans le hub couplerait le core à
   chaque modèle de PLC.
2. **Résolution tenant (§18.2 #2)** — comme pour MQTT/LoRaWAN, une lecture arrive d'une
   passerelle en machine-to-machine, **sans JWT par device** : le tenant ne peut venir ni
   d'un claim par lecture, ni (interdit) de la charge utile.

## Décision

Connecteur **entrant en webhook**, calqué sur le couple LoRaWAN/MQTT, en Clean Architecture
hexagonale dans `apps/api-iot-hub` (`infrastructure/modbus/` + `presentation/rest/`).

### Décodage du fil délégué à la passerelle Edge

Le décodage bas niveau (registres bruts → grandeurs physiques) est **délégué à la passerelle
Edge** (§9.5), qui connaît la carte de registres de chaque équipement. QualitOS reçoit une
**lecture déjà structurée en JSON** ; le core reste agnostique du modèle de PLC (un nouvel
équipement = une carte côté gateway, zéro code core — cf. §9.10).

### Format de lecture retenu

```json
{
  "deviceCode": "PLC-ATELIER-3",
  "readings": [
    { "register": 40001, "metric": "pressure",    "value": 4.2,  "unit": "bar"  },
    { "register": 40002, "metric": "temperature", "value": 55.0, "unit": "degC" }
  ],
  "time": "2026-06-16T08:30:00Z"
}
```

- Chaque entrée de `readings` portant un **registre entier** et une **valeur numérique
  finie** → une mesure `{metric, value, unit?, recordedAt?}`. Registre absent/non entier,
  valeur non numérique / non finie / absente, ou entrée non-objet sont **ignorés et comptés**.
- `metric` est utilisé tel quel si fourni, sinon **dérivé du numéro de registre**
  (`register_40001`) pour rester exploitable sans cartographie côté hub.
- `time` accepte ISO-8601 (offset toléré) **ou epoch millisecondes** (fréquent côté gateways
  industrielles) ; absent/invalide → horodatage serveur en aval.

### Résolution tenant — chemin MQTT (et **non** FHIR)

Le device est résolu par son code via `DeviceRepository.findUniqueByCode(String)` (lookup
tenant-agnostique) ; le tenant qui fait foi est `Device.tenantId()`. Toute **collision
cross-tenant** du code → `Optional.empty()` → **rejet fail-closed** (on ne devine jamais le
tenant — anti-IDOR A01). Aucun tenant n'est jamais lu de la charge utile, même si elle en
contient un (ignoré).

### Composants

- `ModbusReadingDecoder` — décodeur pur (Jackson `JsonNode`), ne lève jamais ; renvoie
  `DecodeResult(deviceCode, measurements, droppedReadings)`.
- `ModbusReadingHandler` — handler pur ; résout le device, ingère via `IngestTelemetryUseCase`,
  renvoie `Outcome(ingested, dropped, issues)` (issues sans PII, sanitizées CRLF/longueur —
  A09). Borne dure `maxReadings` (OWASP A04, anti-DoS). Une mesure en échec n'interrompt pas
  la lecture.
- `ModbusProperties` (`qualitos.iot.modbus.*`) — `enabled` défaut **false**,
  `max-readings` défaut 100.
- `ModbusConnectorConfig` — beans gardés par le flag ; rien n'est chargé sans opt-in.
- `ModbusController` — `POST /api/v1/iot/modbus`, `@PreAuthorize` sur
  `DEVICE/GATEWAY/TENANT_ADMIN/QUALITY_MANAGER` (mêmes rôles que l'ingestion télémétrie),
  gating `@ConditionalOnProperty` → 404 si désactivé. 202 si ≥1 mesure ingérée, sinon 422.

`Protocol.MODBUS_TCP` préexistait dans l'enum allow-list — aucun ajout requis.

## Justification

- **Calquer MQTT** (et non FHIR) est imposé par l'absence de JWT par device : FHIR scope la
  résolution au tenant du JWT (`findByCode(tenant, code)`), inapplicable ici.
- **Lecture structurée** plutôt que registres bruts : Modbus n'a aucune sémantique embarquée ;
  décoder le fil dans le hub coûterait une lib Modbus + une carte de registres par équipement,
  couplant le core. La passerelle Edge tient déjà cette carte.
- **Désactivé par défaut** : aucune surface exposée sans opt-in (cohérent MQTT/OPC-UA/LoRaWAN,
  A05).

## Conséquences

- ✅ Cas d'usage industriel §9.9 (capteurs pression/température PLC → télémétrie → seuils →
  CAPA via ADR 0016) fonctionnel end-to-end, 100 % testable sous H2.
- ✅ Multi-tenant fail-closed garanti, aucun tenant depuis le réseau.
- ⚠ Le décodage de registres bruts (endianness, float 32 bits, échelle, coils/holding) reste
  **hors périmètre** : délégué à la passerelle Edge. Sparkplug B / DICOM restent STUB.
- ⚠ Pas de vérification de signature webhook (HMAC) à ce stade : à assurer côté gateway
  (mTLS) ou en en-tête signé si exposition directe.

## Tests d'invariant

- `ModbusReadingDecoderTest` (15 cas) — parsing registres, métrique dérivée, unités,
  horodatage ISO/offset/epoch-ms, robustesse registres/valeurs/entrées pourris.
- `ModbusReadingHandlerTest` (12 cas) — ingestion, tenant du device, **collision cross-tenant
  fail-closed**, device inconnu, payload pourri, troncature, échec aval, sanitization A09.
- `ModbusControllerIntegrationTest` (5 cas, @SpringBootTest H2) — 401 sans JWT, 202/422,
  cross-tenant fail-closed.
- `ModbusConnectorDisabledTest` — beans absents sans opt-in (404).
- `ModbusPropertiesTest` — défauts sûrs (off, max 100).
- `CleanArchitectureTest` (ArchUnit) — couches respectées.
- Gate JaCoCo module (85 % ligne / 75 % branche) : `verify` vert.

## Références

- CLAUDE.md §9.4 (protocoles), §9.5 (Edge Gateway), §9.10 (adaptation domaine),
  §18.2 #2 (tenant), §11.1 (OWASP).
- ADR 0001 (tenant via JWT), ADR 0007 (connecteurs SPI), ADR 0016 (seuil IoT → CAPA),
  ADR 0026 (connecteur LoRaWAN — patron calqué).
- Patrons calqués : connecteur LoRaWAN (`infrastructure/lorawan/`), handler MQTT
  (`infrastructure/mqtt/`).
