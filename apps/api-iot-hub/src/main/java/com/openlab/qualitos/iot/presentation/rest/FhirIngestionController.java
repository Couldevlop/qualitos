package com.openlab.qualitos.iot.presentation.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.openlab.qualitos.iot.infrastructure.config.TenantContext;
import com.openlab.qualitos.iot.infrastructure.fhir.FhirIngestionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Ingestion HL7 FHIR R5 (CLAUDE.md §9.4 — Santé) : un moteur d'intégration
 * (Mirth, Rhapsody, EHR…) pousse des {@code Observation} unitaires ou en
 * {@code Bundle} ; chaque mesure d'équipement devient un point de télémétrie
 * QualitOS (frigo pharma, autoclave, sonde T°…), avec seuils → NC automatique.
 *
 * <p>OWASP A01 : endpoint deny-by-default ({@code @PreAuthorize}) ; le tenant vient
 * exclusivement du JWT ({@link TenantContext}) — règle §18.2-2. Le client FHIR
 * s'authentifie en OAuth2 client_credentials avec un token portant le claim
 * {@code tenant_id}.
 *
 * <p>Sémantique de réponse : 202 si au moins une observation est ingérée,
 * 422 si tout a été rejeté (le détail par entrée est dans {@code issues}).
 */
@RestController
@RequestMapping("/api/v1/iot/fhir")
@ConditionalOnProperty(
    prefix = "qualitos.iot.fhir", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FhirIngestionController {

  /** Type MIME normatif FHIR — accepté en plus d'application/json. */
  public static final String APPLICATION_FHIR_JSON = "application/fhir+json";

  private final FhirIngestionHandler handler;

  public FhirIngestionController(FhirIngestionHandler handler) {
    this.handler = handler;
  }

  /** Bilan d'ingestion renvoyé au système intégrateur. */
  public record IngestSummary(int ingested, int dropped, List<String> issues) {}

  @PostMapping(consumes = {APPLICATION_FHIR_JSON, MediaType.APPLICATION_JSON_VALUE})
  @PreAuthorize("hasAnyRole('DEVICE','GATEWAY','TENANT_ADMIN','QUALITY_MANAGER')")
  public ResponseEntity<IngestSummary> ingest(@RequestBody JsonNode body) {
    UUID tenantId = TenantContext.requireTenantId();
    FhirIngestionHandler.Outcome outcome = handler.handle(tenantId, body);

    HttpStatus status = outcome.ingested() > 0 ? HttpStatus.ACCEPTED : HttpStatus.UNPROCESSABLE_ENTITY;
    return ResponseEntity.status(status)
        .body(new IngestSummary(outcome.ingested(), outcome.dropped(), outcome.issues()));
  }
}
