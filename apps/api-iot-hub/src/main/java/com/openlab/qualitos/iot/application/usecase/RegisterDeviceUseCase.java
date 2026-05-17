package com.openlab.qualitos.iot.application.usecase;

import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.model.DeviceType;
import com.openlab.qualitos.iot.domain.model.Protocol;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Use case — register (provision) a device. tenantId comes from JWT (caller layer). */
public final class RegisterDeviceUseCase {

  private final DeviceRepository repo;

  public RegisterDeviceUseCase(DeviceRepository repo) {
    this.repo = Objects.requireNonNull(repo, "repo");
  }

  public Device register(
      UUID tenantId,
      String code,
      String name,
      DeviceType type,
      Protocol protocol,
      String enterprise,
      String site,
      String area,
      String workCenter,
      String equipment,
      String certFingerprintSha256,
      Map<String, Object> twin) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(protocol, "protocol");

    repo.findByCode(tenantId, code).ifPresent(existing -> {
      throw new DeviceAlreadyExistsException(code);
    });

    Device device = new Device(
        UUID.randomUUID(), tenantId, code, name, type, protocol,
        enterprise, site, area, workCenter, equipment,
        certFingerprintSha256, twin == null ? Map.of() : twin,
        Instant.now(), null);
    return repo.save(device);
  }

  public static class DeviceAlreadyExistsException extends RuntimeException {
    public DeviceAlreadyExistsException(String code) {
      super("Device with code already exists: " + code);
    }
  }
}
