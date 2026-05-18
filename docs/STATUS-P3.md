# Statut P3 — IoT + Industry Packs + Vision 5S

> Mise à jour : 2026-05-17. Cf. CLAUDE.md §5, §9, §11.4, §17.

## Livré (sprint 1)

### Industry Packs framework
- `libs/industry-commons/` — SPI Java en architecture hexagonale (domain / application / infrastructure / presentation), `IndustryPackProvider`, `IndustryPack` (value object), `IndustryPackYamlLoader` avec SnakeYAML `SafeConstructor` (rejet `!!java/` — OWASP YAML safety), fingerprint SHA-256.
- `libs/industry-packs/packs/manufacturing.yaml` — pack référence (IATF 16949, SPC/FMEA/OEE/TRS, 10+ Ishikawa templates, 5+ Poka-Yoke, parcours formation Quality Manager / Operator / Process Engineer).

### api-iot-hub (Spring Boot 3.5 + Java 21, port 8083)
- Device Registry `/api/v1/iot/devices` (ISA-95 hierarchy, X.509 cert fingerprint, digital twin shadow JSONB).
- Telemetry ingestion `/api/v1/iot/telemetry` (single + batch) → TimescaleDB hypertable.
- Stream rule engine stub : breach → Spring `ApplicationEvent` + Feign vers api-quality-engine (auto-création NC).
- Distroless image, RFC 7807, OpenAPI 3.1.

### ai-vision-5s (FastAPI Python 3.13, distroless, port 8090)
- `POST /v1/vision/5s/analyze` — multipart image, détections déterministes (stub SHA-256-of-bytes).
- `POST /v1/vision/5s/score` — breakdown Seiri/Seiton/Seiso/Seiketsu/Shitsuke.
- JWT via Keycloak JWKS, libmagic MIME check, cap 10 MB, EXIF strip.
- Tests pytest avec layering hexagonal (domain / application / infrastructure / presentation).

### Documentation
- ADR `0004-iot-hub-architecture.md` (Clean Architecture hexagonale).
- ADR `0005-industry-packs-spi.md` (loader YAML safe).
- `docs/security/cbom.md` (Crypto Bill of Materials).
- `docs/security/threat-model-iot.md` (STRIDE).
- `docs/security/owasp-checklist-p3.md` (Top 10 + ML).

## En cours / non livré sprint 1 (renvoyé en sprint 2)

- **Crypto-agility lib** (X25519+ML-KEM-768 + Ed25519+ML-DSA-65 via Bouncy Castle FIPS) : ADR + emplacement créé, implémentation différée à sprint 2.
- **YOLOv8 réel** : sprint 1 = stub déterministe ; sprint 2 = wiring `ultralytics` + modèles fine-tunés.
- **MQTT/EMQX broker** : entrée docker-compose ajoutée ; consommateur côté api-iot-hub sprint 2.
- **Edge Gateway K3s** : reporté P3 sprint 3+.
- **ArchUnit dependency tests** : tests basiques présents, durcissement sprint 2.
- **JaCoCo gate à 85 %** sur industry-commons : à finaliser quand le SPI sera couvert par tests E2E.

## Conflits avec P4 / P5

- **P4** consomme `libs/industry-packs/packs/*.yaml` (6 packs sectoriels ajoutés par P4). Le contrat YAML défini ici est respecté.
- **P5** n'a pas d'overlap (ai-service, dashboards, marketplace).
- **Ports docker-compose** : 8083 (iot-hub), 8090 (vision), 1883/18083 (emqx). Aucun conflit avec P4 (8084 connectors) ni P5 (8085 ai-service, 6333 qdrant, 11434 ollama).

## Démo curl

```bash
# Industry Packs
curl -H "Authorization: Bearer $TOK" http://localhost:8181/api/v1/industry-packs
curl -X POST -H "Authorization: Bearer $TOK" -H "Content-Type: application/json" \
  -d '{"packId":"manufacturing","version":"1.0.0"}' \
  http://localhost:8181/api/v1/industry-packs/activate

# IoT Hub
curl -X POST -H "Authorization: Bearer $TOK" -H "Content-Type: application/json" \
  -d '{"deviceId":"line-3-press-01","site":"site-paris","area":"area-extrusion","workCenter":"wc-3","equipment":"press-01","certFingerprint":"sha256:ab12..."}' \
  http://localhost:8083/api/v1/iot/devices

curl -X POST -H "Authorization: Bearer $TOK" -H "Content-Type: application/json" \
  -d '{"deviceId":"line-3-press-01","metric":"pressure","value":102.3,"unit":"bar","ts":"2026-05-17T13:00:00Z"}' \
  http://localhost:8083/api/v1/iot/telemetry

# Vision 5S
curl -X POST -H "Authorization: Bearer $TOK" -F "image=@./audit-photo.jpg" \
  http://localhost:8090/v1/vision/5s/analyze
```
