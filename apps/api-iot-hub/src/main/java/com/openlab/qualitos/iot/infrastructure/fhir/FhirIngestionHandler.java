package com.openlab.qualitos.iot.infrastructure.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Handler pur d'ingestion FHIR R5 — accepte une {@code Observation} unitaire ou un
 * {@code Bundle} d'Observations et alimente {@link IngestTelemetryUseCase}.
 *
 * <p>Aucun type HTTP/Spring MVC ici : le contrôleur ne fait que déléguer, ce qui rend
 * toute la logique testable en isolation (mêmes conventions que
 * {@code MqttTelemetryMessageHandler}).
 *
 * <h2>Sécurité tenant (§18.2 règle 2 — CRITIQUE)</h2>
 * Contrairement au chemin MQTT (sans identité), l'endpoint FHIR est derrière le JWT :
 * le {@code tenantId} passé ici provient du TenantContext (claim validé), JAMAIS du
 * payload. La résolution d'équipement est <em>tenant-scopée</em>
 * ({@link DeviceRepository#findByCode(UUID, String)}) : un code device d'un autre
 * tenant est invisible (fail-closed, pas d'IDOR — A01).
 *
 * <h2>Robustesse</h2>
 * Ne lève jamais d'exception : chaque entrée invalide est comptée et motivée dans
 * l'{@link Outcome} (sans PII), une entrée pourrie ne bloque jamais le reste du Bundle.
 */
public final class FhirIngestionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(FhirIngestionHandler.class);

  private final DeviceRepository deviceRepository;
  private final IngestTelemetryUseCase ingestUseCase;
  private final FhirObservationMapper mapper;
  private final int maxBundleEntries;

  public FhirIngestionHandler(
      DeviceRepository deviceRepository,
      IngestTelemetryUseCase ingestUseCase,
      FhirObservationMapper mapper,
      int maxBundleEntries) {
    this.deviceRepository = Objects.requireNonNull(deviceRepository, "deviceRepository");
    this.ingestUseCase = Objects.requireNonNull(ingestUseCase, "ingestUseCase");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.maxBundleEntries = maxBundleEntries;
  }

  /** Bilan d'ingestion — renvoyé tel quel au système intégrateur (Mirth, Rhapsody…). */
  public record Outcome(int ingested, int dropped, List<String> issues) {

    public Outcome {
      issues = issues == null ? List.of() : List.copyOf(issues);
    }
  }

  /**
   * Traite une ressource FHIR (Observation ou Bundle). Ne lève jamais d'exception.
   *
   * @param tenantId tenant issu du JWT (TenantContext) — jamais du payload
   * @param root     corps JSON de la requête
   */
  public Outcome handle(UUID tenantId, JsonNode root) {
    Objects.requireNonNull(tenantId, "tenantId");

    if (root == null || !root.isObject()) {
      return new Outcome(0, 1, List.of("payload is not a JSON object"));
    }

    String resourceType = root.path("resourceType").asText("");
    return switch (resourceType) {
      case "Observation" -> processEntries(tenantId, List.of(root), List.of());
      case "Bundle" -> handleBundle(tenantId, root);
      default -> new Outcome(0, 1,
          List.of("unsupported resourceType '" + sanitize(resourceType) + "'"
              + " (expected Observation or Bundle)"));
    };
  }

  // ---------------------------------------------------------------------------

  private Outcome handleBundle(UUID tenantId, JsonNode bundle) {
    JsonNode entries = bundle.get("entry");
    if (entries == null || !entries.isArray() || entries.isEmpty()) {
      return new Outcome(0, 0, List.of("empty Bundle"));
    }

    List<JsonNode> observations = new ArrayList<>();
    List<String> issues = new ArrayList<>();
    int truncated = 0;

    for (int i = 0; i < entries.size(); i++) {
      if (observations.size() >= maxBundleEntries) {
        truncated = entries.size() - i;
        break;
      }
      JsonNode resource = entries.get(i).path("resource");
      // Les Bundles d'intégration contiennent légitimement d'autres ressources
      // (Patient, Device…) : on ne traite que les Observations, sans les compter
      // comme rejets.
      if ("Observation".equals(resource.path("resourceType").asText(""))) {
        observations.add(resource);
      }
    }

    if (truncated > 0) {
      issues.add("bundle truncated: " + truncated + " entr(y/ies) beyond the "
          + maxBundleEntries + "-entry limit were ignored");
    }
    return processEntries(tenantId, observations, issues);
  }

  private Outcome processEntries(UUID tenantId, List<JsonNode> observations, List<String> priorIssues) {
    int ingested = 0;
    int dropped = 0;
    List<String> issues = new ArrayList<>(priorIssues);

    for (int i = 0; i < observations.size(); i++) {
      try {
        FhirObservationMapper.MappingResult result = mapper.map(observations.get(i));
        if (result.isDropped()) {
          dropped++;
          issues.add("observation[" + i + "]: " + result.dropReason());
          continue;
        }

        FhirObservationMapper.MappedObservation obs = result.observation();
        // Résolution TENANT-SCOPÉE : un device d'un autre tenant est introuvable.
        Optional<Device> device = deviceRepository.findByCode(tenantId, obs.deviceCode());
        if (device.isEmpty()) {
          dropped++;
          issues.add("observation[" + i + "]: unknown device code '"
              + sanitize(obs.deviceCode()) + "' for this tenant");
          continue;
        }

        ingestUseCase.ingest(tenantId, device.get().id(),
            obs.metric(), obs.value(), obs.unit(), obs.recordedAt());
        ingested++;

      } catch (RuntimeException ex) {
        // Une entrée en échec ne doit jamais interrompre le Bundle.
        dropped++;
        issues.add("observation[" + i + "]: ingestion failed");
        LOG.warn("FHIR observation[{}] dropped — unexpected error: {}", i, ex.getMessage());
      }
    }

    LOG.debug("FHIR ingestion done — tenant={} ingested={} dropped={}", tenantId, ingested, dropped);
    return new Outcome(ingested, dropped, issues);
  }

  /** Tronque + neutralise les valeurs venues du réseau avant de les citer (A09 log injection). */
  private static String sanitize(String raw) {
    if (raw == null) return "";
    String cleaned = raw.replaceAll("[\\r\\n\\t]", " ");
    return cleaned.length() > 64 ? cleaned.substring(0, 64) + "…" : cleaned;
  }
}
