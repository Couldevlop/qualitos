package com.openlab.qualitos.iot.infrastructure.persistence;

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

  private final DeviceJpaRepository jpa;

  public JpaDeviceRepository(DeviceJpaRepository jpa) {
    this.jpa = jpa;
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
    e.setTwinJson(d.twin().isEmpty() ? null : d.twin().toString());
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
        Map.of(), // twin re-hydrated empty in V1 — JSON parsing is left for P4 (StandardsHub also stores JSON)
        e.getProvisionedAt(), e.getLastSeenAt());
  }
}
