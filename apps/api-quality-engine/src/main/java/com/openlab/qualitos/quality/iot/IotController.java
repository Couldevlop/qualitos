package com.openlab.qualitos.quality.iot;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/iot")
public class IotController {

    private final IotDeviceService deviceService;
    private final TelemetryIngestionService telemetryService;
    private final IotThresholdService thresholdService;

    public IotController(IotDeviceService deviceService, TelemetryIngestionService telemetryService,
                         IotThresholdService thresholdService) {
        this.deviceService = deviceService;
        this.telemetryService = telemetryService;
        this.thresholdService = thresholdService;
    }

    // ---- Devices ----

    @GetMapping("/devices")
    public Page<IotDto.DeviceResponse> list(
            @RequestParam(required = false) IotDeviceStatus status,
            @RequestParam(required = false) IotDeviceType type,
            @PageableDefault(size = 50) Pageable pageable) {
        return deviceService.list(status, type, pageable);
    }

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public IotDto.DeviceResponse create(@Valid @RequestBody IotDto.CreateDeviceRequest req) {
        return deviceService.create(req);
    }

    @GetMapping("/devices/{id}")
    public IotDto.DeviceResponse get(@PathVariable UUID id) { return deviceService.get(id); }

    @PatchMapping("/devices/{id}")
    public IotDto.DeviceResponse update(@PathVariable UUID id,
                                        @Valid @RequestBody IotDto.UpdateDeviceRequest req) {
        return deviceService.update(id, req);
    }

    @DeleteMapping("/devices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { deviceService.delete(id); }

    @PostMapping("/devices/{id}/activate")
    public IotDto.DeviceResponse activate(@PathVariable UUID id) { return deviceService.activate(id); }

    @PostMapping("/devices/{id}/suspend")
    public IotDto.DeviceResponse suspend(@PathVariable UUID id) { return deviceService.suspend(id); }

    @PostMapping("/devices/{id}/decommission")
    public IotDto.DeviceResponse decommission(@PathVariable UUID id) {
        return deviceService.decommission(id);
    }

    // ---- Telemetry ----

    @PostMapping("/devices/{id}/telemetry")
    @ResponseStatus(HttpStatus.CREATED)
    public IotDto.TelemetryResponse ingest(@PathVariable UUID id,
                                           @Valid @RequestBody IotDto.TelemetryIngestRequest req) {
        return telemetryService.ingest(id, req);
    }

    @GetMapping("/devices/{id}/telemetry")
    public Page<IotDto.TelemetryResponse> recent(
            @PathVariable UUID id,
            @PageableDefault(size = 100) Pageable pageable) {
        return telemetryService.recent(id, pageable);
    }

    @GetMapping("/devices/{id}/telemetry/range")
    public Page<IotDto.TelemetryResponse> range(
            @PathVariable UUID id,
            @RequestParam String metric,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 500) Pageable pageable) {
        return telemetryService.range(id, metric, from, to, pageable);
    }

    // ---- Thresholds (§9.7, §9.9) : dépassement → CAPA auto ----

    @GetMapping("/thresholds")
    public Page<IotDto.ThresholdResponse> listThresholds(@PageableDefault(size = 50) Pageable pageable) {
        return thresholdService.list(pageable);
    }

    @PostMapping("/thresholds")
    @ResponseStatus(HttpStatus.CREATED)
    public IotDto.ThresholdResponse createThreshold(@Valid @RequestBody IotDto.ThresholdRequest req) {
        return thresholdService.create(req);
    }

    @GetMapping("/thresholds/{id}")
    public IotDto.ThresholdResponse getThreshold(@PathVariable UUID id) {
        return thresholdService.get(id);
    }

    @PatchMapping("/thresholds/{id}")
    public IotDto.ThresholdResponse updateThreshold(@PathVariable UUID id,
                                                    @Valid @RequestBody IotDto.ThresholdRequest req) {
        return thresholdService.update(id, req);
    }

    @DeleteMapping("/thresholds/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteThreshold(@PathVariable UUID id) {
        thresholdService.delete(id);
    }
}
