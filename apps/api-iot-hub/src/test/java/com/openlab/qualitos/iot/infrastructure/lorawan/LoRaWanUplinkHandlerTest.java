package com.openlab.qualitos.iot.infrastructure.lorawan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.model.DeviceType;
import com.openlab.qualitos.iot.domain.model.Protocol;
import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import com.openlab.qualitos.iot.domain.port.NonConformancePublisher;
import com.openlab.qualitos.iot.domain.port.TelemetryRepository;
import com.openlab.qualitos.iot.domain.service.StreamRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Handler d'uplinks LoRaWAN : résolution tenant via le registre device (chemin MQTT),
 * fail-closed sur collision cross-tenant, robustesse — tests unitaires purs.
 *
 * <p>{@link IngestTelemetryUseCase} est {@code final} (non mockable) : comme
 * {@code MqttTelemetryMessageHandlerTest} et {@code FhirIngestionHandlerTest}, on pilote
 * un use case RÉEL adossé à des stubs in-memory, ce qui vérifie aussi le tenant
 * effectivement persisté.
 */
class LoRaWanUplinkHandlerTest {

  private static final UUID DEVICE_TENANT = UUID.randomUUID();
  private static final UUID OTHER_TENANT = UUID.randomUUID();

  private final ObjectMapper json = new ObjectMapper();

  private InMemoryDeviceRepo deviceRepo;
  private RecordingTelemetryRepo telemetryRepo;
  private LoRaWanUplinkHandler handler;

  @BeforeEach
  void setUp() {
    deviceRepo = new InMemoryDeviceRepo();
    telemetryRepo = new RecordingTelemetryRepo();
    StreamRuleEngine engine = new StreamRuleEngine((t, d, m) -> List.of());
    NonConformancePublisher publisher = e -> { };
    IngestTelemetryUseCase ingestUseCase =
        new IngestTelemetryUseCase(deviceRepo, telemetryRepo, engine, publisher);
    handler = new LoRaWanUplinkHandler(
        deviceRepo, ingestUseCase, new LoRaWanUplinkDecoder(), "deviceName", 50);
  }

  private JsonNode parse(String s) throws Exception {
    return json.readTree(s);
  }

  private Device register(UUID tenantId, String code) {
    Device d = new Device(UUID.randomUUID(), tenantId, code, "Capteur sol",
        DeviceType.AGRO_STATION, Protocol.LORAWAN,
        null, null, null, null, null, null, Map.of(), Instant.now(), null);
    deviceRepo.save(d);
    return d;
  }

  // ---- ingestion nominale -----------------------------------------------------

  @Test
  @DisplayName("Uplink valide + device connu → ingestion avec le tenant du registre device")
  void validUplinkIsIngestedWithDeviceTenant() throws Exception {
    Device d = register(DEVICE_TENANT, "SOL-01");

    var outcome = handler.handle(parse("""
        {"deviceName":"SOL-01",
         "decoded":{"temperature":4.2,"humidity":55},
         "units":{"temperature":"degC","humidity":"%RH"},
         "time":"2026-06-16T08:30:00Z"}"""));

    assertThat(outcome.ingested()).isEqualTo(2);
    assertThat(outcome.dropped()).isZero();
    assertThat(telemetryRepo.saved).hasSize(2);
    assertThat(telemetryRepo.saved).allSatisfy(p -> {
      assertThat(p.tenantId()).isEqualTo(DEVICE_TENANT);
      assertThat(p.deviceId()).isEqualTo(d.id());
      assertThat(p.recordedAt()).isEqualTo(Instant.parse("2026-06-16T08:30:00Z"));
    });
    assertThat(telemetryRepo.saved)
        .anySatisfy(p -> {
          assertThat(p.metric()).isEqualTo("temperature");
          assertThat(p.value()).isEqualTo(4.2);
          assertThat(p.unit()).isEqualTo("degC");
        });
  }

  @Test
  @DisplayName("Aucun tenant n'est lu de la charge utile (un tenantId injecté est ignoré)")
  void tenantIsNeverReadFromPayload() throws Exception {
    register(DEVICE_TENANT, "SOL-01");
    UUID attacker = UUID.randomUUID();

    var outcome = handler.handle(parse("""
        {"deviceName":"SOL-01","tenantId":"%s","decoded":{"t":1}}""".formatted(attacker)));

    assertThat(outcome.ingested()).isEqualTo(1);
    assertThat(telemetryRepo.saved).singleElement()
        .satisfies(p -> assertThat(p.tenantId()).isEqualTo(DEVICE_TENANT).isNotEqualTo(attacker));
  }

  // ---- sécurité tenant --------------------------------------------------------

  @Test
  @DisplayName("Device inconnu → rejet fail-closed, rien ingéré")
  void unknownDeviceIsDropped() throws Exception {
    var outcome = handler.handle(parse("""
        {"deviceName":"GHOST","decoded":{"t":1}}"""));

    assertThat(outcome.ingested()).isZero();
    assertThat(outcome.dropped()).isEqualTo(1);
    assertThat(outcome.issues()).anySatisfy(i -> assertThat(i).contains("unknown/ambiguous device"));
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  @DisplayName("Collision cross-tenant du code → fail-closed (anti-IDOR A01)")
  void crossTenantCollisionFailsClosed() throws Exception {
    register(DEVICE_TENANT, "DUP");
    register(OTHER_TENANT, "DUP");

    var outcome = handler.handle(parse("""
        {"deviceName":"DUP","decoded":{"t":1,"h":2}}"""));

    assertThat(outcome.ingested()).isZero();
    assertThat(outcome.dropped()).isEqualTo(2); // les 2 mesures rejetées
    assertThat(outcome.issues()).anySatisfy(i -> assertThat(i).contains("unknown/ambiguous device"));
    assertThat(telemetryRepo.saved).isEmpty();
  }

  // ---- robustesse / payloads pourris ------------------------------------------

  @Test
  @DisplayName("Payload null ou non-objet → 1 rejet, jamais d'exception")
  void nullOrNonObjectPayload() throws Exception {
    assertThat(handler.handle(null).dropped()).isEqualTo(1);
    assertThat(handler.handle(parse("[1,2,3]")).dropped()).isEqualTo(1);
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  @DisplayName("Pas de code device dans l'uplink → rejet motivé")
  void missingDeviceCodeIsDropped() throws Exception {
    var outcome = handler.handle(parse("""
        {"decoded":{"t":1}}"""));

    assertThat(outcome.ingested()).isZero();
    assertThat(outcome.dropped()).isEqualTo(1);
    assertThat(outcome.issues()).anySatisfy(i -> assertThat(i).contains("no device code"));
  }

  @Test
  @DisplayName("Device connu mais aucune mesure numérique → rejet, champs non numériques comptés")
  void noNumericMeasurement() throws Exception {
    register(DEVICE_TENANT, "SOL-01");

    var outcome = handler.handle(parse("""
        {"deviceName":"SOL-01","decoded":{"label":"x","active":true}}"""));

    assertThat(outcome.ingested()).isZero();
    assertThat(outcome.dropped()).isEqualTo(1);
    assertThat(outcome.issues())
        .anySatisfy(i -> assertThat(i).contains("no numeric measurement"))
        .anySatisfy(i -> assertThat(i).contains("non-numeric field"));
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  @DisplayName("Mesures numériques + champs pourris : numériques ingérées, pourris signalés")
  void mixedNumericAndJunk() throws Exception {
    register(DEVICE_TENANT, "SOL-01");

    var outcome = handler.handle(parse("""
        {"deviceName":"SOL-01","decoded":{"temperature":21.0,"label":"zoneA","flag":false}}"""));

    assertThat(outcome.ingested()).isEqualTo(1);
    assertThat(outcome.issues()).anySatisfy(i -> assertThat(i).contains("non-numeric field"));
    assertThat(telemetryRepo.saved).singleElement()
        .satisfies(p -> assertThat(p.metric()).isEqualTo("temperature"));
  }

  @Test
  @DisplayName("Uplink au-delà de la borne → troncature signalée (OWASP A04, anti-DoS)")
  void uplinkIsTruncatedAtMaxMeasurements() throws Exception {
    register(DEVICE_TENANT, "SOL-01");
    StreamRuleEngine engine = new StreamRuleEngine((t, d, m) -> List.of());
    IngestTelemetryUseCase useCase =
        new IngestTelemetryUseCase(deviceRepo, telemetryRepo, engine, e -> { });
    handler = new LoRaWanUplinkHandler(
        deviceRepo, useCase, new LoRaWanUplinkDecoder(), "deviceName", 1);

    var outcome = handler.handle(parse("""
        {"deviceName":"SOL-01","decoded":{"a":1,"b":2,"c":3}}"""));

    assertThat(outcome.ingested()).isEqualTo(1);
    assertThat(outcome.dropped()).isEqualTo(2);
    assertThat(outcome.issues()).anySatisfy(i -> assertThat(i).contains("truncated"));
    assertThat(telemetryRepo.saved).hasSize(1);
  }

  @Test
  @DisplayName("Une mesure en échec runtime n'interrompt pas le reste de l'uplink")
  void failingMeasurementDoesNotAbortUplink() throws Exception {
    register(DEVICE_TENANT, "SOL-01");
    telemetryRepo.throwOnSaveOnce = true; // la 1re sauvegarde lève, la 2e passe

    var outcome = handler.handle(parse("""
        {"deviceName":"SOL-01","decoded":{"a":1,"b":2}}"""));

    assertThat(outcome.ingested()).isEqualTo(1);
    assertThat(outcome.dropped()).isEqualTo(1);
    assertThat(outcome.issues()).anySatisfy(i -> assertThat(i).contains("ingestion failed"));
    assertThat(telemetryRepo.saved).hasSize(1);
  }

  // ---- hygiène des messages (A09) ---------------------------------------------

  @Test
  @DisplayName("Le code device cité dans les issues est neutralisé (pas de CRLF, tronqué)")
  void issuesAreSanitized() throws Exception {
    String hostile = "EVIL\\r\\nFAKE-LOG-" + "x".repeat(100);

    var outcome = handler.handle(parse("""
        {"deviceName":"%s","decoded":{"t":1}}""".formatted(hostile)));

    assertThat(outcome.dropped()).isEqualTo(1);
    String issue = outcome.issues().stream()
        .filter(i -> i.contains("unknown/ambiguous device")).findFirst().orElseThrow();
    assertThat(issue).doesNotContain("\r").doesNotContain("\n");
    assertThat(issue.length()).isLessThan(150);
    assertThat(telemetryRepo.saved).isEmpty();
  }

  // ---- stubs in-memory (mêmes conventions que MQTT / FHIR) ---------------------

  static class InMemoryDeviceRepo implements DeviceRepository {
    final Map<UUID, Device> byId = new LinkedHashMap<>();

    @Override public Device save(Device d) { byId.put(d.id(), d); return d; }
    @Override public Optional<Device> findById(UUID t, UUID i) {
      Device d = byId.get(i);
      return d != null && d.tenantId().equals(t) ? Optional.of(d) : Optional.empty();
    }
    @Override public Optional<Device> findByCode(UUID t, String c) {
      return byId.values().stream()
          .filter(d -> d.tenantId().equals(t) && d.code().equals(c)).findFirst();
    }
    @Override public Optional<Device> findUniqueByCode(String c) {
      var matches = byId.values().stream().filter(d -> d.code().equals(c)).toList();
      return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }
    @Override public List<Device> findAllByTenant(UUID t) {
      return byId.values().stream().filter(d -> d.tenantId().equals(t)).toList();
    }
    @Override public void touchLastSeen(UUID t, UUID id, Instant when) { }
    @Override public long countByTenant(UUID t) { return findAllByTenant(t).size(); }
    @Override public void updateTwin(UUID t, UUID id, Map<String, Object> twin) {
      Device d = byId.get(id);
      if (d != null && d.tenantId().equals(t)) {
        byId.put(id, new Device(d.id(), d.tenantId(), d.code(), d.name(), d.type(), d.protocol(),
            d.enterprise(), d.site(), d.area(), d.workCenter(), d.equipment(),
            d.certFingerprintSha256(), twin, d.provisionedAt(), d.lastSeenAt()));
      }
    }
  }

  static class RecordingTelemetryRepo implements TelemetryRepository {
    final List<TelemetryPoint> saved = new ArrayList<>();
    boolean throwOnSaveOnce = false;

    @Override public TelemetryPoint save(TelemetryPoint p) {
      if (throwOnSaveOnce) {
        throwOnSaveOnce = false;
        throw new RuntimeException("downstream boom");
      }
      saved.add(p);
      return p;
    }
    @Override public List<TelemetryPoint> saveAll(List<TelemetryPoint> pts) {
      saved.addAll(pts);
      return pts;
    }
    @Override public List<TelemetryPoint> findByDevice(UUID t, UUID d, Instant from, Instant to, int lim) {
      return saved;
    }
    @Override public long countByTenant(UUID t) { return saved.size(); }
  }
}
