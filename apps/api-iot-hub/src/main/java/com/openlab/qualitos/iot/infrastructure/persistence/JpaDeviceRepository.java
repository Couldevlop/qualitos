package com.openlab.qualitos.iot.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Hexagonal output adapter for {@link DeviceRepository}. */
@Component
public class JpaDeviceRepository implements DeviceRepository {

  private static final TypeReference<Map<String, Object>> TWIN_TYPE = new TypeReference<>() {};

  private final DeviceJpaRepository jpa;
  private final ObjectMapper objectMapper;

  public JpaDeviceRepository(DeviceJpaRepository jpa, ObjectMapper objectMapper) {
    this.jpa = jpa;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public Device save(Device d) {
    DeviceEntity e = toEntity(d);
    DeviceEntity saved = jpa.save(e);
    return toDomain(saved);
  }

  @Override
  public Optional<Device> findById(UUID tenantId, UUID id) {
    return jpa.findByTenantIdAndId(tenantId, id).map(this::toDomain);
  }

  @Override
  public Optional<Device> findByCode(UUID tenantId, String code) {
    return jpa.findByTenantIdAndCode(tenantId, code).map(this::toDomain);
  }

  @Override
  public Optional<Device> findUniqueByCode(String code) {
    List<DeviceEntity> matches = jpa.findByCode(code);
    // Fail-closed on cross-tenant ambiguity: never guess the tenant.
    if (matches.size() != 1) {
      return Optional.empty();
    }
    return Optional.of(toDomain(matches.get(0)));
  }

  @Override
  public List<Device> findAllByTenant(UUID tenantId) {
    return jpa.findAllByTenantId(tenantId).stream().map(this::toDomain).toList();
  }

  @Override
  @Transactional
  public void touchLastSeen(UUID tenantId, UUID id, Instant when) {
    jpa.touchLastSeen(tenantId, id, when);
  }

  @Override
  public long countByTenant(UUID tenantId) {
    return jpa.countByTenantId(tenantId);
  }

  @Override
  @Transactional
  public void updateTwin(UUID tenantId, UUID deviceId, Map<String, Object> twin) {
    // Tenant-scoped (OWASP A01) : le WHERE filtre par tenant + id. UPDATE en masse
    // (pas de load-modify-save) pour éviter le contrôle « expected 1 row » qui levait
    // une StaleStateException empoisonnant la session lors d'ingestions multi-métriques.
    jpa.updateTwinJson(tenantId, deviceId, writeTwin(twin));
  }

  // ---- mappers --------------------------------------------------------

  private DeviceEntity toEntity(Device d) {
    DeviceEntity e = new DeviceEntity();
    e.setId(d.id());
    e.setTenantId(d.tenantId());
    e.setCode(d.code());
    e.setName(d.name());
    e.setType(d.type());
    e.setProtocol(d.protocol());
    e.setEnterprise(d.enterprise());
    e.setSite(d.site());
    e.setArea(d.area());
    e.setWorkCenter(d.workCenter());
    e.setEquipment(d.equipment());
    e.setCertFingerprintSha256(d.certFingerprintSha256());
    e.setTwinJson(writeTwin(d.twin()));
    e.setProvisionedAt(d.provisionedAt());
    e.setLastSeenAt(d.lastSeenAt());
    return e;
  }

  private Device toDomain(DeviceEntity e) {
    return new Device(
        e.getId(), e.getTenantId(), e.getCode(), e.getName(),
        e.getType(), e.getProtocol(),
        e.getEnterprise(), e.getSite(), e.getArea(), e.getWorkCenter(), e.getEquipment(),
        e.getCertFingerprintSha256(),
        readTwin(e.getTwinJson()),  // round-trip JSON réel (§9.6 Device Shadow)
        e.getProvisionedAt(), e.getLastSeenAt());
  }

  /** Sérialise le twin en JSON (null si vide, pour ne pas stocker « {} »). */
  private String writeTwin(Map<String, Object> twin) {
    if (twin == null || twin.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(twin);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new IllegalStateException("twin serialization failed", ex);
    }
  }

  /** Désérialise le twin JSON en carte (vide si null/illisible — défensif). */
  private Map<String, Object> readTwin(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, TWIN_TYPE);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      return Map.of();
    }
  }
}
