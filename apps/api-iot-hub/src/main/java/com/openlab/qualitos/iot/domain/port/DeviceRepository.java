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
  List<Device> findAllByTenant(UUID tenantId);
  void touchLastSeen(UUID tenantId, UUID deviceId, java.time.Instant when);
  long countByTenant(UUID tenantId);
}
