package com.openlab.qualitos.iot.presentation.rest;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.application.usecase.RegisterDeviceUseCase;
import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import com.openlab.qualitos.iot.domain.service.DeviceShadow;
import com.openlab.qualitos.iot.infrastructure.config.TenantContext;
import com.openlab.qualitos.iot.presentation.dto.DeviceDtos;
import com.openlab.qualitos.iot.presentation.dto.TelemetryDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Device Registry + Telemetry REST API. OWASP A01 — every endpoint deny-by-default
 * via {@link PreAuthorize}; tenantId always derived from JWT via {@link TenantContext}.
 */
@RestController
@RequestMapping("/api/v1/iot")
public class DeviceController {

  private final RegisterDeviceUseCase registerUseCase;
  private final IngestTelemetryUseCase ingestUseCase;
  private final DeviceRepository deviceRepository;

  public DeviceController(
      RegisterDeviceUseCase registerUseCase,
      IngestTelemetryUseCase ingestUseCase,
      DeviceRepository deviceRepository) {
    this.registerUseCase = registerUseCase;
    this.ingestUseCase = ingestUseCase;
    this.deviceRepository = deviceRepository;
  }

  @PostMapping("/devices")
  @PreAuthorize("hasAnyRole('TENANT_ADMIN','QUALITY_MANAGER','SUPER_ADMIN')")
  public ResponseEntity<DeviceDtos.DeviceResponse> register(
      @Valid @RequestBody DeviceDtos.RegisterDeviceRequest req) {
    UUID tenantId = TenantContext.requireTenantId();
    Device d = registerUseCase.register(
        tenantId, req.code(), req.name(), req.type(), req.protocol(),
        req.enterprise(), req.site(), req.area(), req.workCenter(), req.equipment(),
        req.certFingerprintSha256(), req.twin());
    return ResponseEntity.created(URI.create("/api/v1/iot/devices/" + d.id()))
        .body(DeviceDtos.DeviceResponse.from(d));
  }

  @GetMapping("/devices")
  @PreAuthorize("isAuthenticated()")
  public List<DeviceDtos.DeviceResponse> list() {
    UUID tenantId = TenantContext.requireTenantId();
    return deviceRepository.findAllByTenant(tenantId).stream()
        .map(DeviceDtos.DeviceResponse::from)
        .toList();
  }

  @GetMapping("/devices/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<DeviceDtos.DeviceResponse> get(@PathVariable UUID id) {
    UUID tenantId = TenantContext.requireTenantId();
    return deviceRepository.findById(tenantId, id)
        .map(DeviceDtos.DeviceResponse::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /** Device Shadow / Digital Twin (§9.6) : état reported + desired de l'équipement. */
  @GetMapping("/devices/{id}/shadow")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> shadow(@PathVariable UUID id) {
    UUID tenantId = TenantContext.requireTenantId();
    return deviceRepository.findById(tenantId, id)
        .map(Device::twin)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /** Met à jour la face « desired » (consigne) du twin. Tenant dérivé du JWT (A01). */
  @PatchMapping("/devices/{id}/shadow/desired")
  @PreAuthorize("hasAnyRole('TENANT_ADMIN','QUALITY_MANAGER','SUPER_ADMIN')")
  public ResponseEntity<Map<String, Object>> setDesired(
      @PathVariable UUID id, @RequestBody Map<String, Object> desired) {
    UUID tenantId = TenantContext.requireTenantId();
    return deviceRepository.findById(tenantId, id)
        .map(device -> {
          Map<String, Object> twin = DeviceShadow.setDesired(device.twin(), desired);
          deviceRepository.updateTwin(tenantId, id, twin);
          return ResponseEntity.ok(twin);
        })
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping("/telemetry")
  @PreAuthorize("hasAnyRole('DEVICE','GATEWAY','TENANT_ADMIN','QUALITY_MANAGER')")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public TelemetryDtos.TelemetryResponse ingest(@Valid @RequestBody TelemetryDtos.IngestRequest req) {
    UUID tenantId = TenantContext.requireTenantId();
    TelemetryPoint saved = ingestUseCase.ingest(
        tenantId, req.deviceId(), req.metric(), req.value(), req.unit(), req.recordedAt());
    return TelemetryDtos.TelemetryResponse.from(saved);
  }

  @PostMapping("/telemetry/batch")
  @PreAuthorize("hasAnyRole('DEVICE','GATEWAY','TENANT_ADMIN','QUALITY_MANAGER')")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public List<TelemetryDtos.TelemetryResponse> ingestBatch(
      @Valid @RequestBody TelemetryDtos.BatchIngestRequest req) {
    UUID tenantId = TenantContext.requireTenantId();
    java.time.Instant now = java.time.Instant.now();
    var points = req.points().stream()
        .map(e -> new TelemetryPoint(
            null, tenantId, req.deviceId(),
            e.metric(), e.value(), e.unit(),
            e.recordedAt() == null ? now : e.recordedAt()))
        .toList();
    return ingestUseCase.ingestBatch(tenantId, req.deviceId(), points).stream()
        .map(TelemetryDtos.TelemetryResponse::from).toList();
  }
}
