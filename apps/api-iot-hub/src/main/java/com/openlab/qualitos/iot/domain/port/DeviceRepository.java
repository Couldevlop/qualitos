package com.openlab.qualitos.iot.domain.port;

import com.openlab.qualitos.iot.domain.model.Device;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Output port — device persistence. Implemented by JPA adapter. */
public interface DeviceRepository {
  Device save(Device device);
  Optional<Device> findById(UUID tenantId, UUID id);
  Optional<Device> findByCode(UUID tenantId, String code);

  /**
   * Tenant-agnostic lookup by device code, used by ingestion paths that do NOT
   * carry a trusted tenant (e.g. the MQTT connector — CLAUDE.md §9.4 / §18.2 rule 2).
   *
   * <p>The resolved {@link Device#tenantId()} becomes the authoritative tenant for
   * the telemetry; the caller must NEVER read a tenant from the wire payload.
   * Because {@code code} is only unique <em>within</em> a tenant, an ambiguous
   * cross-tenant collision MUST yield {@link Optional#empty()} (fail-closed) rather
   * than risk attributing data to the wrong tenant.
   */
  Optional<Device> findUniqueByCode(String code);
  List<Device> findAllByTenant(UUID tenantId);
  void touchLastSeen(UUID tenantId, UUID deviceId, java.time.Instant when);
  long countByTenant(UUID tenantId);

  /**
   * Persiste UNIQUEMENT le twin / Device Shadow d'un équipement (§9.6), sans toucher aux
   * autres champs. Tenant-scoped (OWASP A01 — pas d'IDOR). No-op si l'équipement est absent.
   */
  void updateTwin(UUID tenantId, UUID deviceId, java.util.Map<String, Object> twin);
}
