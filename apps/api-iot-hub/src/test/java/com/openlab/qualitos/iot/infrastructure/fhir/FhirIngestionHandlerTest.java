package com.openlab.qualitos.iot.infrastructure.fhir;

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
 * Handler d'ingestion FHIR : résolution tenant-scopée, robustesse Bundle,
 * fail-closed — tests unitaires purs, sans contexte Spring.
 *
 * <p>{@link IngestTelemetryUseCase} est {@code final} (non mockable) : comme
 * {@code MqttTelemetryMessageHandlerTest}, on pilote un use case RÉEL adossé à
 * des stubs in-memory — ce qui vérifie aussi le tenant effectivement persisté.
 */
class FhirIngestionHandlerTest {

  private static final UUID TENANT = UUID.randomUUID();
  private static final UUID OTHER_TENANT = UUID.randomUUID();

  private final ObjectMapper json = new ObjectMapper();

  private InMemoryDeviceRepo deviceRepo;
  private RecordingTelemetryRepo telemetryRepo;
  private FhirIngestionHandler handler;

  @BeforeEach
  void setUp() {
    deviceRepo = new InMemoryDeviceRepo();
    telemetryRepo = new RecordingTelemetryRepo();
    StreamRuleEngine engine = new StreamRuleEngine((t, d, m) -> List.of());
    NonConformancePublisher publisher = e -> { };
    IngestTelemetryUseCase ingestUseCase =
        new IngestTelemetryUseCase(deviceRepo, telemetryRepo, engine, publisher);
    handler = new FhirIngestionHandler(
        deviceRepo, ingestUseCase, new FhirObservationMapper(), 200);
  }

  private JsonNode parse(String s) throws Exception {
    return json.readTree(s);
  }

  private Device register(UUID tenantId, String code) {
    Device d = new Device(UUID.randomUUID(), tenantId, code, "Frigo",
        DeviceType.BIOMED, Protocol.HL7_FHIR,
        null, null, null, null, null, null, Map.of(), Instant.now(), null);
    deviceRepo.save(d);
    return d;
  }

  private static String observation(String deviceCode, double value) {
    return """
        {"resourceType":"Observation",
         "device":{"reference":"Device/%s"},
         "code":{"coding":[{"code":"temp"}]},
         "valueQuantity":{"value":%s,"unit":"Cel"},
         "effectiveDateTime":"2026-06-04T10:00:00Z"}""".formatted(deviceCode, value);
  }

  // ---- Observation unitaire ---------------------------------------------------

  @Test
  @DisplayName("Observation valide + device connu → ingestion avec le tenant du JWT")
  void singleObservationIsIngested() throws Exception {
    Device d = register(TENANT, "FRIDGE-1");

    var outcome = handler.handle(TENANT, parse(observation("FRIDGE-1", 7.2)));

    assertThat(outcome.ingested()).isEqualTo(1);
    assertThat(outcome.dropped()).isZero();
    assertThat(outcome.issues()).isEmpty();
    assertThat(telemetryRepo.saved).hasSize(1);
    TelemetryPoint p = telemetryRepo.saved.get(0);
    assertThat(p.tenantId()).isEqualTo(TENANT);
    assertThat(p.deviceId()).isEqualTo(d.id());
    assertThat(p.metric()).isEqualTo("temp");
    assertThat(p.value()).isEqualTo(7.2);
    assertThat(p.unit()).isEqualTo("Cel");
    assertThat(p.recordedAt()).isEqualTo(Instant.parse("2026-06-04T10:00:00Z"));
  }

  @Test
  @DisplayName("Device d'un AUTRE tenant → invisible, rejet fail-closed (anti-IDOR A01)")
  void crossTenantDeviceIsDropped() throws Exception {
    register(OTHER_TENANT, "FRIDGE-X");

    var outcome = handler.handle(TENANT, parse(observation("FRIDGE-X", 1)));

    assertThat(outcome.ingested()).isZero();
    assertThat(outcome.dropped()).isEqualTo(1);
    assertThat(outcome.issues()).singleElement().asString().contains("unknown device");
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void observationInvalideEstRejeteeAvecMotif() throws Exception {
    var outcome = handler.handle(TENANT, parse("""
        {"resourceType":"Observation","code":{"text":"t"},"valueQuantity":{"value":1}}"""));

    assertThat(outcome.dropped()).isEqualTo(1);
    assertThat(outcome.issues()).singleElement().asString().contains("device");
    assertThat(telemetryRepo.saved).isEmpty();
  }

  // ---- garde-fous généraux ----------------------------------------------------

  @Test
  void payloadNonObjetEstRejete() {
    var outcome = handler.handle(TENANT, null);
    assertThat(outcome.ingested()).isZero();
    assertThat(outcome.dropped()).isEqualTo(1);
  }

  @Test
  void resourceTypeNonSupporteEstRejete() throws Exception {
    var outcome = handler.handle(TENANT, parse("""
        {"resourceType":"Patient","name":[{"family":"X"}]}"""));

    assertThat(outcome.dropped()).isEqualTo(1);
    assertThat(outcome.issues()).singleElement().asString().contains("unsupported resourceType");
    assertThat(telemetryRepo.saved).isEmpty();
  }

  // ---- Bundle -----------------------------------------------------------------

  @Test
  @DisplayName("Bundle mixte : Observations traitées, autres ressources ignorées sans rejet")
  void bundleMixedResources() throws Exception {
    register(TENANT, "FRIDGE-1");

    var outcome = handler.handle(TENANT, parse("""
        {"resourceType":"Bundle","type":"collection","entry":[
          {"resource":%s},
          {"resource":{"resourceType":"Patient","id":"p1"}},
          {"resource":%s}
        ]}""".formatted(observation("FRIDGE-1", 7.0), observation("FRIDGE-1", 7.5))));

    assertThat(outcome.ingested()).isEqualTo(2);
    assertThat(outcome.dropped()).isZero();
    assertThat(outcome.issues()).isEmpty();
    assertThat(telemetryRepo.saved).hasSize(2);
  }

  @Test
  @DisplayName("Bundle au-delà de la borne → troncature signalée (OWASP A04, anti-DoS)")
  void bundleIsTruncatedAtMaxEntries() throws Exception {
    register(TENANT, "FRIDGE-1");
    StreamRuleEngine engine = new StreamRuleEngine((t, d, m) -> List.of());
    IngestTelemetryUseCase useCase =
        new IngestTelemetryUseCase(deviceRepo, telemetryRepo, engine, e -> { });
    handler = new FhirIngestionHandler(deviceRepo, useCase, new FhirObservationMapper(), 1);

    var outcome = handler.handle(TENANT, parse("""
        {"resourceType":"Bundle","entry":[
          {"resource":%s},
          {"resource":%s}
        ]}""".formatted(observation("FRIDGE-1", 1), observation("FRIDGE-1", 2))));

    assertThat(outcome.ingested()).isEqualTo(1);
    assertThat(outcome.issues()).anySatisfy(i -> assertThat(i).contains("truncated"));
  }

  @Test
  void bundleVideEstSignale() throws Exception {
    var outcome = handler.handle(TENANT, parse("""
        {"resourceType":"Bundle","entry":[]}"""));
    assertThat(outcome.ingested()).isZero();
    assertThat(outcome.issues()).singleElement().asString().contains("empty Bundle");
  }

  @Test
  @DisplayName("Une entrée en échec runtime n'interrompt pas le reste du Bundle")
  void failingEntryDoesNotAbortBundle() throws Exception {
    register(TENANT, "OK");
    // La 1re sauvegarde lève (panne aval simulée), la 2e passe.
    telemetryRepo.throwOnSaveOnce = true;

    var outcome = handler.handle(TENANT, parse("""
        {"resourceType":"Bundle","entry":[
          {"resource":%s},
          {"resource":%s}
        ]}""".formatted(observation("OK", 1), observation("OK", 2))));

    assertThat(outcome.ingested()).isEqualTo(1);
    assertThat(outcome.dropped()).isEqualTo(1);
    assertThat(outcome.issues()).anySatisfy(i -> assertThat(i).contains("ingestion failed"));
    assertThat(telemetryRepo.saved).hasSize(1);
  }

  // ---- hygiène des messages (A09) ---------------------------------------------

  @Test
  @DisplayName("Les codes device cités dans les issues sont neutralisés (pas de CRLF, tronqués)")
  void issuesAreSanitized() throws Exception {
    // \r\n dans le JSON → caractères réels après parsing Jackson.
    String hostile = "EVIL\\r\\nFAKE-LOG-" + "x".repeat(100);

    var outcome = handler.handle(TENANT, parse(observation(hostile, 1)));

    assertThat(outcome.dropped()).isEqualTo(1);
    String issue = outcome.issues().get(0);
    assertThat(issue).doesNotContain("\r").doesNotContain("\n");
    assertThat(issue.length()).isLessThan(150);
    assertThat(telemetryRepo.saved).isEmpty();
  }

  // ---- stubs in-memory (mêmes conventions que MqttTelemetryMessageHandlerTest) -

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
    @Override public void updateTwin(UUID t, UUID id, java.util.Map<String,Object> twin) {
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
