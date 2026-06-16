package com.openlab.qualitos.iot.presentation.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.openlab.qualitos.iot.infrastructure.modbus.ModbusReadingHandler;
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
 * Ingestion de lectures Modbus TCP/RTU (CLAUDE.md §9.4 — PLC / équipements legacy) : une
 * passerelle Edge pousse une lecture déjà structurée (registres décodés) ; chaque mesure
 * devient un point de télémétrie QualitOS (pression, température, débit…), avec seuils → NC
 * auto (ADR 0016).
 *
 * <p>OWASP A01 : endpoint deny-by-default ({@code @PreAuthorize}) — mêmes rôles que
 * l'ingestion télémétrie ({@code DEVICE}/{@code GATEWAY}, plus admin/manager). La passerelle
 * s'authentifie en OAuth2 client_credentials avec un compte de service.
 *
 * <p><b>Résolution tenant (§18.2 #2)</b> : le tenant n'est <em>jamais</em> lu de la charge
 * utile — le device est résolu par son code et son tenant fait foi (cf.
 * {@link ModbusReadingHandler}). Le rôle porté par le token sert l'autorisation, pas
 * l'attribution tenant.
 *
 * <p>Gating : le contrôleur n'existe que si {@code qualitos.iot.modbus.enabled=true}
 * (désactivé par défaut) — sinon la route renvoie 404 et la surface disparaît (A05).
 *
 * <p>Sémantique : 202 si au moins une mesure est ingérée, 422 si tout a été rejeté
 * (le détail par mesure est dans {@code issues}).
 */
@RestController
@RequestMapping("/api/v1/iot/modbus")
@ConditionalOnProperty(prefix = "qualitos.iot.modbus", name = "enabled", havingValue = "true")
public class ModbusController {

  private final ModbusReadingHandler handler;

  public ModbusController(ModbusReadingHandler handler) {
    this.handler = handler;
  }

  /** Bilan d'ingestion renvoyé à la passerelle Edge. */
  public record IngestSummary(int ingested, int dropped, List<String> issues) {}

  @PostMapping
  @PreAuthorize("hasAnyRole('DEVICE','GATEWAY','TENANT_ADMIN','QUALITY_MANAGER')")
  public ResponseEntity<IngestSummary> reading(@RequestBody JsonNode body) {
    ModbusReadingHandler.Outcome outcome = handler.handle(body);

    HttpStatus status = outcome.ingested() > 0 ? HttpStatus.ACCEPTED : HttpStatus.UNPROCESSABLE_ENTITY;
    return ResponseEntity.status(status)
        .body(new IngestSummary(outcome.ingested(), outcome.dropped(), outcome.issues()));
  }
}
