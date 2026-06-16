package com.openlab.qualitos.iot.presentation.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.openlab.qualitos.iot.infrastructure.sparkplug.SparkplugIngestionHandler;
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

/**
 * Ingestion <b>Sparkplug B</b> (CLAUDE.md §9.4) : une passerelle Edge (Tahu / EMQX
 * Sparkplug codec) pousse un payload NDATA/DDATA <em>déjà décodé en JSON</em> ; chaque
 * métrique devient un point de télémétrie QualitOS (capteur vibration cobot, sonde T°…),
 * avec seuils → NC automatique.
 *
 * <p>Désactivé par défaut ({@code qualitos.iot.sparkplug.enabled=false}) : sans le flag,
 * ni le handler ni ce contrôleur n'existent → l'endpoint répond 404 (OWASP A05, surface
 * nulle), même garantie que le connecteur MQTT.
 *
 * <h2>Sécurité (§18.2 règle 2)</h2>
 * Sparkplug B circule sur MQTT : le payload identifie un <em>équipement</em>, jamais un
 * tenant. Le tenant est résolu par le handler depuis le registre device
 * ({@code findUniqueByCode}, fail-closed) — JAMAIS lu du payload. L'endpoint reste
 * deny-by-default ({@code @PreAuthorize}) : la passerelle s'authentifie en OAuth2
 * client_credentials avec un rôle DEVICE/GATEWAY.
 *
 * <p>Sémantique : 202 si au moins une métrique est ingérée, 422 si tout est rejeté
 * (le détail par métrique est dans {@code issues}).
 */
@RestController
@RequestMapping("/api/v1/iot/sparkplug")
@ConditionalOnProperty(
    prefix = "qualitos.iot.sparkplug", name = "enabled", havingValue = "true")
public class SparkplugIngestionController {

  private final SparkplugIngestionHandler handler;

  public SparkplugIngestionController(SparkplugIngestionHandler handler) {
    this.handler = handler;
  }

  /** Bilan d'ingestion renvoyé à la passerelle. */
  public record IngestSummary(int ingested, int dropped, List<String> issues) {}

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('DEVICE','GATEWAY','TENANT_ADMIN','QUALITY_MANAGER')")
  public ResponseEntity<IngestSummary> ingest(@RequestBody JsonNode body) {
    // Pas de TenantContext ici : Sparkplug B (sur MQTT) n'identifie pas de tenant ;
    // le handler le résout depuis le registre device (fail-closed). §18.2 règle 2.
    SparkplugIngestionHandler.Outcome outcome = handler.handle(body);

    HttpStatus status =
        outcome.ingested() > 0 ? HttpStatus.ACCEPTED : HttpStatus.UNPROCESSABLE_ENTITY;
    return ResponseEntity.status(status)
        .body(new IngestSummary(outcome.ingested(), outcome.dropped(), outcome.issues()));
  }
}
