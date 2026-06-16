package com.openlab.qualitos.iot.domain.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Domain service (pure, no framework) — Device Shadow / Digital Twin merge logic
 * (CLAUDE.md §9.6, modèle Eclipse Ditto « reported / desired »).
 *
 * <p>Le twin est une carte générique {@code Map<String,Object>} structurée en deux
 * faces :
 * <ul>
 *   <li>{@code reported} : dernier état rapporté par l'équipement (par métrique :
 *       {@code {value, unit, at}}), alimenté à chaque télémétrie ;</li>
 *   <li>{@code desired} : état souhaité poussé par la plateforme (consigne).</li>
 * </ul>
 *
 * <p>Toutes les méthodes sont <b>pures</b> : elles renvoient un NOUVEAU twin sans muter
 * l'entrée (immutabilité du modèle de domaine).
 */
public final class DeviceShadow {

  public static final String REPORTED = "reported";
  public static final String DESIRED = "desired";
  public static final String LAST_REPORTED_AT = "lastReportedAt";

  private DeviceShadow() {}

  /**
   * Fusionne une mesure rapportée dans la face {@code reported} du twin.
   *
   * @return un nouveau twin où {@code reported[metric] = {value, unit, at}} et
   *     {@code lastReportedAt = at}.
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> mergeReported(
      Map<String, Object> twin, String metric, Double value, String unit, Instant at) {
    Map<String, Object> next = new LinkedHashMap<>(twin == null ? Map.of() : twin);
    Map<String, Object> reported = new LinkedHashMap<>(
        (Map<String, Object>) next.getOrDefault(REPORTED, Map.of()));

    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("value", value);
    entry.put("unit", unit);
    entry.put("at", at == null ? null : at.toString());
    reported.put(metric, entry);

    next.put(REPORTED, reported);
    next.put(LAST_REPORTED_AT, at == null ? null : at.toString());
    return next;
  }

  /**
   * Remplace la face {@code desired} (consigne) du twin.
   *
   * @return un nouveau twin où {@code desired = desired}.
   */
  public static Map<String, Object> setDesired(
      Map<String, Object> twin, Map<String, Object> desired) {
    Map<String, Object> next = new LinkedHashMap<>(twin == null ? Map.of() : twin);
    next.put(DESIRED, desired == null ? Map.of() : new LinkedHashMap<>(desired));
    return next;
  }
}
