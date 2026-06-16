package com.openlab.qualitos.iot.presentation.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.openlab.qualitos.iot.infrastructure.lorawan.LoRaWanUplinkHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ingestion d'uplinks LoRaWAN (CLAUDE.md §9.4 — TTN / ChirpStack) : un Network Server
 * pousse en webhook l'uplink déjà décodé ; chaque mesure devient un point de télémétrie
 * QualitOS (capteur sol, station météo, sonde T° longue portée…), avec seuils → NC auto.
 *
 * <p>OWASP A01 : endpoint deny-by-default ({@code @PreAuthorize}) — mêmes rôles que
 * l'ingestion télémétrie ({@code DEVICE}/{@code GATEWAY}, plus admin/manager). Le NS
 * s'authentifie en OAuth2 client_credentials avec un compte de service.
 *
 * <p><b>Résolution tenant (§18.2 #2)</b> : le tenant n'est <em>jamais</em> lu de la charge
 * utile — le device est résolu par son code et son tenant fait foi (cf.
 * {@link LoRaWanUplinkHandler}). Le rôle porté par le token sert l'autorisation, pas
 * l'attribution tenant.
 *
 * <p>Gating : le contrôleur n'existe que si {@code qualitos.iot.lorawan.enabled=true}
 * (désactivé par défaut) — sinon la route renvoie 404 et la surface disparaît (A05).
 *
 * <p>Sémantique : 202 si au moins une mesure est ingérée, 422 si tout a été rejeté
 * (le détail par mesure est dans {@code issues}).
 */
@RestController
@RequestMapping("/api/v1/iot/lorawan")
@ConditionalOnProperty(prefix = "qualitos.iot.lorawan", name = "enabled", havingValue = "true")
public class LoRaWanController {

  private final LoRaWanUplinkHandler handler;

  public LoRaWanController(LoRaWanUplinkHandler handler) {
    this.handler = handler;
  }

  /** Bilan d'ingestion renvoyé au Network Server. */
  public record IngestSummary(int ingested, int dropped, List<String> issues) {}

  @PostMapping("/uplink")
  @PreAuthorize("hasAnyRole('DEVICE','GATEWAY','TENANT_ADMIN','QUALITY_MANAGER')")
  public ResponseEntity<IngestSummary> uplink(@RequestBody JsonNode body) {
    LoRaWanUplinkHandler.Outcome outcome = handler.handle(body);

    HttpStatus status = outcome.ingested() > 0 ? HttpStatus.ACCEPTED : HttpStatus.UNPROCESSABLE_ENTITY;
    return ResponseEntity.status(status)
        .body(new IngestSummary(outcome.ingested(), outcome.dropped(), outcome.issues()));
  }
}
