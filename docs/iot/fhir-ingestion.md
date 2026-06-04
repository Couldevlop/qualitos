# Ingestion HL7 FHIR R5 — api-iot-hub

> CLAUDE.md §9.4 (Santé) · threat model : `docs/security/threat-model-iot.md`
> (section « Extension 2026-06 »).

## Ce que ça fait

Un moteur d'intégration santé (Mirth, Rhapsody, interface EHR/LIS) pousse des
`Observation` FHIR R5 — unitaires ou en `Bundle` — vers QualitOS. Chaque mesure
d'équipement (frigo pharmacie, autoclave, sonde de température, automate) devient
un point de télémétrie : les seuils configurés déclenchent automatiquement une
**non-conformité** côté quality-engine (chaîne §9.9 : excursion T° → NC → quarantaine).

```
EHR / Mirth ──POST /api/v1/iot/fhir──▶ api-iot-hub ──seuils──▶ NC quality-engine
              (Observation | Bundle)      │
                                          └─▶ TimescaleDB (télémétrie)
```

## Contrat d'API

`POST /api/v1/iot/fhir` — `Content-Type: application/fhir+json` (ou `application/json`)

- **Auth** : OAuth2 `client_credentials`, token JWT portant le claim `tenant_id`.
  Rôles acceptés : `DEVICE`, `GATEWAY`, `TENANT_ADMIN`, `QUALITY_MANAGER`.
- **Corps** : une ressource `Observation` ou un `Bundle` (les entrées non-Observation
  sont ignorées sans erreur — un Bundle d'intégration peut contenir Patient, Device…).

### Mapping Observation → télémétrie

| Champ télémétrie | Source FHIR (ordre de priorité) |
|---|---|
| `deviceCode` | `device.identifier.value` → `device.reference` (`Device/<code>`) → `device.display` |
| `metric` | `code.coding[0].code` (LOINC/SNOMED) → `code.text` |
| `value` | `valueQuantity.value` (numérique requis) |
| `unit` | `valueQuantity.unit` → `valueQuantity.code` (UCUM) |
| `recordedAt` | `effectiveDateTime` → `issued` (ISO-8601, offsets tolérés ; absent = heure serveur) |

Le device doit être **provisionné au préalable** dans le registry
(`POST /api/v1/iot/devices`, protocole `HL7_FHIR`) — code device unique par tenant.

### Réponses

| Statut | Sens |
|---|---|
| `202 Accepted` | ≥ 1 observation ingérée — corps `{ingested, dropped, issues[]}` |
| `422 Unprocessable Entity` | tout rejeté (device inconnu, valueQuantity manquant…) — motifs dans `issues[]` |
| `401 / 403` | token absent / rôle insuffisant |

### Exemple

```bash
curl -X POST https://<hub>/api/v1/iot/fhir \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Observation", "status": "final",
    "device": {"identifier": {"value": "FRIDGE-PH-01"}},
    "code": {"coding": [{"system": "http://loinc.org", "code": "8310-5"}]},
    "valueQuantity": {"value": 7.4, "unit": "Cel"},
    "effectiveDateTime": "2026-06-04T10:15:30Z"
  }'
# → 202 {"ingested":1,"dropped":0,"issues":[]}
```

## Garanties de sécurité

- **Tenant** : exclusivement le claim JWT (§18.2-2) — tout `tenant_id` du payload est
  ignoré ; la résolution device est tenant-scopée (un code d'un autre tenant = 422).
- **PII** : les champs patient (`subject`, `performer`…) ne sont ni lus, ni stockés,
  ni journalisés — seule la mesure physique est extraite.
- **Anti-DoS** : Bundles bornés à `qualitos.iot.fhir.max-bundle-entries` (200) ;
  troncature signalée dans `issues[]`.
- **Kill-switch** : `qualitos.iot.fhir.enabled=false` (env `IOT_FHIR_ENABLED`)
  retire l'endpoint ET le handler du contexte Spring.

## Architecture (Clean Architecture, vérifiée ArchUnit)

| Couche | Classe | Rôle |
|---|---|---|
| infrastructure | `FhirObservationMapper` | mapping FHIR→télémétrie, pur, sans Spring |
| infrastructure | `FhirIngestionHandler` | orchestration + résolution device, ne lève jamais |
| infrastructure | `FhirConnectorConfig` / `FhirProperties` | câblage conditionnel |
| presentation | `FhirIngestionController` | délégation HTTP uniquement |

Le use case (`IngestTelemetryUseCase`) et le domaine sont inchangés — même chemin
que MQTT/OPC-UA (seuils, NC, touchLastSeen).
