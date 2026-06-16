package com.openlab.qualitos.iot.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Agrégat (rollup) d'une métrique sur une tranche temporelle (CLAUDE.md §9.3, §6.4).
 *
 * <p>Équivalent applicatif d'un bucket de continuous aggregate TimescaleDB
 * ({@code iot_telemetry_hourly}) : moyenne / min / max / nombre d'échantillons par
 * tranche. Calculé en SQL standard portable (PostgreSQL simple ET H2 en test), ce qui
 * permet d'exposer les rollups sans dépendre de l'extension TimescaleDB.
 *
 * <p>{@code bucketStart} est le début de la tranche (UTC) ; les bornes ouvertes
 * éventuelles (value null) ne sont jamais agrégées (filtrées en amont).
 */
public record TelemetryRollup(
    Instant bucketStart,
    String metric,
    double avg,
    double min,
    double max,
    long count
) {
  public TelemetryRollup {
    Objects.requireNonNull(bucketStart, "bucketStart");
    Objects.requireNonNull(metric, "metric");
  }
}
