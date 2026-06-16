package com.openlab.qualitos.iot.domain.model;

/**
 * Granularité d'un rollup de télémétrie (CLAUDE.md §9.3, §6.4).
 *
 * <p>Allow-list stricte : seules ces tranches sont acceptées par l'endpoint de rollup,
 * ce qui évite toute injection d'unité {@code date_trunc} non maîtrisée (OWASP A03 — la
 * valeur littérale passée au SQL ne vient jamais directement de l'utilisateur).
 */
public enum RollupBucket {
  HOUR("hour"),
  DAY("day"),
  MINUTE("minute");

  private final String sqlUnit;

  RollupBucket(String sqlUnit) {
    this.sqlUnit = sqlUnit;
  }

  /** Unité littérale sûre pour {@code date_trunc(unit, ts)} — issue de l'enum, jamais du wire. */
  public String sqlUnit() {
    return sqlUnit;
  }

  /** Parse tolérant (insensible à la casse) ; valeur par défaut {@link #HOUR} si null/vide. */
  public static RollupBucket fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return HOUR;
    }
    return switch (raw.trim().toLowerCase()) {
      case "hour", "hourly", "1h" -> HOUR;
      case "day", "daily", "1d" -> DAY;
      case "minute", "min", "1m" -> MINUTE;
      default -> throw new IllegalArgumentException("unsupported bucket: " + raw);
    };
  }
}
