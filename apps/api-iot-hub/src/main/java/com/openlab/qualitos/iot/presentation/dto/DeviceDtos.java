package com.openlab.qualitos.iot.presentation.dto;

import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.model.DeviceType;
import com.openlab.qualitos.iot.domain.model.Protocol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class DeviceDtos {

  private DeviceDtos() {}

  /** Request body for device registration. tenantId IS NOT in the body (CLAUDE.md §18.2). */
  public record RegisterDeviceRequest(
      @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9_.-]{1,100}") String code,
      @NotBlank @Size(max = 200) String name,
      @NotNull DeviceType type,
      @NotNull Protocol protocol,
      @Size(max = 100) String enterprise,
      @Size(max = 100) String site,
      @Size(max = 100) String area,
      @Size(max = 100) String workCenter,
      @Size(max = 100) String equipment,
      @Pattern(regexp = "[A-Fa-f0-9]{0,64}") String certFingerprintSha256,
      Map<String, Object> twin
  ) {}

  /** Response payload. */
  public record DeviceResponse(
      UUID id,
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
      Instant provisionedAt,
      Instant lastSeenAt
  ) {
    public static DeviceResponse from(Device d) {
      return new DeviceResponse(
          d.id(), d.tenantId(), d.code(), d.name(), d.type(), d.protocol(),
          d.enterprise(), d.site(), d.area(), d.workCenter(), d.equipment(),
          d.certFingerprintSha256(), d.provisionedAt(), d.lastSeenAt());
    }
  }
}
