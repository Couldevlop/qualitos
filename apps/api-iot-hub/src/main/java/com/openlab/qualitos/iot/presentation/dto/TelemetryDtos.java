package com.openlab.qualitos.iot.presentation.dto;

import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.model.TelemetryRollup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TelemetryDtos {
  private TelemetryDtos() {}

  public record IngestRequest(
      @NotNull UUID deviceId,
      @NotBlank @Size(max = 100) String metric,
      Double value,
      @Size(max = 32) String unit,
      Instant recordedAt
  ) {}

  public record BatchIngestRequest(
      @NotNull UUID deviceId,
      @NotEmpty @Size(max = 1000) @Valid List<PointEntry> points
  ) {}

  public record PointEntry(
      @NotBlank @Size(max = 100) String metric,
      Double value,
      @Size(max = 32) String unit,
      Instant recordedAt
  ) {}

  public record TelemetryResponse(
      UUID id,
      UUID deviceId,
      String metric,
      Double value,
      String unit,
      Instant recordedAt
  ) {
    public static TelemetryResponse from(TelemetryPoint p) {
      return new TelemetryResponse(
          p.id(), p.deviceId(), p.metric(), p.value(), p.unit(), p.recordedAt());
    }
  }

  /** Un bucket agrégé (avg/min/max/count) d'une métrique sur une tranche temporelle (§9.3). */
  public record RollupBucketResponse(
      Instant bucketStart,
      String metric,
      double avg,
      double min,
      double max,
      long count
  ) {
    public static RollupBucketResponse from(TelemetryRollup r) {
      return new RollupBucketResponse(
          r.bucketStart(), r.metric(), r.avg(), r.min(), r.max(), r.count());
    }
  }
}
