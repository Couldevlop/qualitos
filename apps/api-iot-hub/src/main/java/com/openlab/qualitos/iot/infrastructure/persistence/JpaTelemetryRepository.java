package com.openlab.qualitos.iot.infrastructure.persistence;

import com.openlab.qualitos.iot.domain.model.RollupBucket;
import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.model.TelemetryRollup;
import com.openlab.qualitos.iot.domain.port.TelemetryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class JpaTelemetryRepository implements TelemetryRepository {

  private final TelemetryJpaRepository jpa;

  @PersistenceContext
  private EntityManager entityManager;

  public JpaTelemetryRepository(TelemetryJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  @Transactional
  public TelemetryPoint save(TelemetryPoint p) {
    return toDomain(jpa.save(toEntity(p)));
  }

  @Override
  @Transactional
  public List<TelemetryPoint> saveAll(List<TelemetryPoint> points) {
    List<TelemetryEntity> entities = new ArrayList<>(points.size());
    for (TelemetryPoint p : points) entities.add(toEntity(p));
    List<TelemetryEntity> saved = jpa.saveAll(entities);
    List<TelemetryPoint> out = new ArrayList<>(saved.size());
    for (TelemetryEntity e : saved) out.add(toDomain(e));
    return out;
  }

  @Override
  public List<TelemetryPoint> findByDevice(UUID tenantId, UUID deviceId, Instant from, Instant to, int limit) {
    return jpa.findRange(tenantId, deviceId, from, to,
            PageRequest.of(0, Math.min(Math.max(limit, 1), 1000)))
        .stream().map(this::toDomain).toList();
  }

  @Override
  public long countByTenant(UUID tenantId) {
    return jpa.countByTenantId(tenantId);
  }

  /**
   * Agrégation par tranche temporelle en SQL standard portable (PostgreSQL simple ET
   * H2 en test) : {@code date_trunc(unit, recorded_at)} + AVG/MIN/MAX/COUNT, filtré par
   * tenant + device + metric (tenant-scopé, OWASP A01).
   *
   * <p>L'unité {@code date_trunc} ne peut pas être un paramètre lié → elle est injectée
   * depuis {@link RollupBucket#sqlUnit()}, une valeur littérale issue de l'enum (allow-list,
   * jamais du wire) : pas de surface d'injection (OWASP A03). Tous les autres champs sont
   * des paramètres liés.
   */
  @Override
  public List<TelemetryRollup> rollupByDevice(
      UUID tenantId, UUID deviceId, String metric, RollupBucket bucket, int limit) {
    String unit = bucket.sqlUnit(); // littéral sûr issu de l'enum
    String sql = """
        SELECT date_trunc('%s', t.recorded_at) AS bucket,
               AVG(t.value_double) AS avg_v,
               MIN(t.value_double) AS min_v,
               MAX(t.value_double) AS max_v,
               COUNT(t.value_double) AS cnt
          FROM iot_telemetry t
         WHERE t.tenant_id = :tenantId
           AND t.device_id = :deviceId
           AND t.metric = :metric
           AND t.value_double IS NOT NULL
         GROUP BY date_trunc('%s', t.recorded_at)
         ORDER BY bucket DESC
        """.formatted(unit, unit);

    Query query = entityManager.createNativeQuery(sql);
    query.setParameter("tenantId", tenantId);
    query.setParameter("deviceId", deviceId);
    query.setParameter("metric", metric);
    query.setMaxResults(Math.min(Math.max(limit, 1), 1000));

    @SuppressWarnings("unchecked")
    List<Object[]> rows = query.getResultList();
    List<TelemetryRollup> out = new ArrayList<>(rows.size());
    for (Object[] row : rows) {
      Instant bucketStart = toInstant(row[0]);
      double avg = toDouble(row[1]);
      double min = toDouble(row[2]);
      double max = toDouble(row[3]);
      long count = toLong(row[4]);
      out.add(new TelemetryRollup(bucketStart, metric, avg, min, max, count));
    }
    return out;
  }

  // ---- conversions JDBC tolérantes (PostgreSQL vs H2 renvoient des types distincts) ----

  private static Instant toInstant(Object value) {
    if (value instanceof Instant i) return i;
    if (value instanceof OffsetDateTime odt) return odt.toInstant();
    if (value instanceof java.sql.Timestamp ts) return ts.toInstant();
    if (value instanceof java.time.LocalDateTime ldt) return ldt.toInstant(java.time.ZoneOffset.UTC);
    throw new IllegalStateException("Unexpected bucket type: "
        + (value == null ? "null" : value.getClass()));
  }

  private static double toDouble(Object value) {
    return value == null ? 0.0 : ((Number) value).doubleValue();
  }

  private static long toLong(Object value) {
    return value == null ? 0L : ((Number) value).longValue();
  }

  private TelemetryEntity toEntity(TelemetryPoint p) {
    TelemetryEntity e = new TelemetryEntity();
    e.setId(p.id() == null ? UUID.randomUUID() : p.id());
    e.setTenantId(p.tenantId());
    e.setDeviceId(p.deviceId());
    e.setMetric(p.metric());
    e.setValue(p.value());
    e.setUnit(p.unit());
    e.setRecordedAt(p.recordedAt());
    return e;
  }

  private TelemetryPoint toDomain(TelemetryEntity e) {
    return new TelemetryPoint(
        e.getId(), e.getTenantId(), e.getDeviceId(),
        e.getMetric(), e.getValue(), e.getUnit(), e.getRecordedAt());
  }
}
