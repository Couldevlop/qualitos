package com.openlab.qualitos.quality.kpi;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KpiServiceTest {

    @Mock KpiDefinitionRepository defRepo;
    @Mock KpiMeasurementRepository measureRepo;
    @InjectMocks KpiService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID KPI = UUID.randomUUID();
    static final UUID MEAS = UUID.randomUUID();

    @BeforeEach
    void setup() { TenantContext.setTenantId(TENANT.toString()); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    // ----- Definition CRUD -----

    @Test
    void create_defaultsFrequencyMonthly() {
        when(defRepo.findByTenantIdAndCode(TENANT, "first-pass-yield")).thenReturn(Optional.empty());
        when(defRepo.save(any())).thenAnswer(inv -> {
            KpiDefinition d = inv.getArgument(0);
            d.setId(KPI); d.setCreatedAt(Instant.now()); d.setUpdatedAt(Instant.now());
            return d;
        });
        KpiDto.KpiResponse out = service.create(new KpiDto.CreateKpiRequest(
                "first-pass-yield", "First Pass Yield", null, "quality", "%",
                KpiDirection.HIGHER_IS_BETTER, null,
                new BigDecimal("95"), new BigDecimal("90"), new BigDecimal("80"),
                null, null, USER));
        assertThat(out.frequency()).isEqualTo(KpiFrequency.MONTHLY);
        assertThat(out.status()).isEqualTo(KpiStatus.DRAFT);
        assertThat(out.targetValue()).isEqualByComparingTo("95");
    }

    @Test
    void create_duplicateCode_throws() {
        when(defRepo.findByTenantIdAndCode(TENANT, "dup")).thenReturn(Optional.of(kpi(KpiStatus.DRAFT)));
        assertThatThrownBy(() -> service.create(new KpiDto.CreateKpiRequest(
                "dup", "x", null, null, null, KpiDirection.HIGHER_IS_BETTER,
                null, null, null, null, null, null, USER)))
                .isInstanceOf(KpiStateException.class);
    }

    @Test
    void create_noTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.create(new KpiDto.CreateKpiRequest(
                "x", "n", null, null, null, KpiDirection.HIGHER_IS_BETTER,
                null, null, null, null, null, null, USER)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void get_crossTenant_appearsNotFound() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        d.setTenantId(UUID.randomUUID());
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.get(KPI))
                .isInstanceOf(KpiNotFoundException.class);
    }

    @Test
    void update_archived_rejected() {
        KpiDefinition d = kpi(KpiStatus.ARCHIVED);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.update(KPI, new KpiDto.UpdateKpiRequest(
                "x", null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(KpiStateException.class);
    }

    @Test
    void update_appliesPatches() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        when(defRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.update(KPI, new KpiDto.UpdateKpiRequest(
                "Renamed", "desc", "ops", "%", KpiFrequency.WEEKLY,
                new BigDecimal("96"), new BigDecimal("88"), new BigDecimal("70"),
                "manufacturing", USER));
        assertThat(d.getName()).isEqualTo("Renamed");
        assertThat(d.getFrequency()).isEqualTo(KpiFrequency.WEEKLY);
        assertThat(d.getTargetValue()).isEqualByComparingTo("96");
    }

    @Test
    void delete_active_rejected() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.delete(KPI))
                .isInstanceOf(KpiStateException.class);
    }

    @Test
    void delete_draft_cascadesMeasurements() {
        KpiDefinition d = kpi(KpiStatus.DRAFT);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        service.delete(KPI);
        verify(measureRepo).deleteByKpiId(KPI);
        verify(defRepo).delete(d);
    }

    @Test
    void list_filterPaths() {
        when(defRepo.findByTenantIdAndStatus(eq(TENANT), eq(KpiStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(kpi(KpiStatus.ACTIVE))));
        assertThat(service.list(KpiStatus.ACTIVE, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(defRepo.findByTenantIdAndCategory(eq(TENANT), eq("quality"), any()))
                .thenReturn(new PageImpl<>(List.of(kpi(KpiStatus.ACTIVE))));
        assertThat(service.list(null, "quality", PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(defRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(kpi(KpiStatus.ACTIVE))));
        assertThat(service.list(null, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    // ----- Lifecycle -----

    @Test
    void activate_fromDraft_ok() {
        KpiDefinition d = kpi(KpiStatus.DRAFT);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        when(defRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.activate(KPI).status()).isEqualTo(KpiStatus.ACTIVE);
    }

    @Test
    void activate_idempotent() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        service.activate(KPI);
        verify(defRepo, never()).save(any());
    }

    @Test
    void activate_archived_rejected() {
        KpiDefinition d = kpi(KpiStatus.ARCHIVED);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.activate(KPI))
                .isInstanceOf(KpiStateException.class);
    }

    @Test
    void reopen_fromActive_ok() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        when(defRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.reopen(KPI).status()).isEqualTo(KpiStatus.DRAFT);
    }

    @Test
    void reopen_fromDraft_rejected() {
        KpiDefinition d = kpi(KpiStatus.DRAFT);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.reopen(KPI))
                .isInstanceOf(KpiStateException.class);
    }

    @Test
    void archive_terminal() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        when(defRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.archive(KPI).status()).isEqualTo(KpiStatus.ARCHIVED);
    }

    @Test
    void archive_alreadyArchived_rejected() {
        KpiDefinition d = kpi(KpiStatus.ARCHIVED);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.archive(KPI))
                .isInstanceOf(KpiStateException.class);
    }

    // ----- Measurements -----

    @Test
    void record_inactiveKpi_rejected() {
        KpiDefinition d = kpi(KpiStatus.DRAFT);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.record(KPI, new KpiDto.RecordMeasurementRequest(
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-30T23:59:59Z"),
                new BigDecimal("92"), null, null, USER, null)))
                .isInstanceOf(KpiStateException.class);
    }

    @Test
    void record_periodEndBeforeStart_rejected() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.record(KPI, new KpiDto.RecordMeasurementRequest(
                Instant.parse("2026-04-30T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                new BigDecimal("92"), null, null, USER, null)))
                .isInstanceOf(KpiStateException.class)
                .hasMessageContaining("periodStart");
    }

    @Test
    void record_newPeriod_persistsWithDerivedHealth() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        when(measureRepo.findByKpiIdAndPeriodStart(eq(KPI), any()))
                .thenReturn(Optional.empty());
        when(measureRepo.save(any())).thenAnswer(inv -> {
            KpiMeasurement m = inv.getArgument(0);
            m.setId(MEAS); m.setCreatedAt(Instant.now()); return m;
        });
        KpiDto.MeasurementResponse out = service.record(KPI, new KpiDto.RecordMeasurementRequest(
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-30T23:59:59Z"),
                new BigDecimal("96"), "%", MeasurementSource.MANUAL, USER, "Q2 W1"));
        assertThat(out.health()).isEqualTo(KpiHealth.OK);
        assertThat(out.value()).isEqualByComparingTo("96");
        assertThat(out.unit()).isEqualTo("%");
    }

    @Test
    void record_existingPeriod_updatesIdempotent() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        KpiMeasurement existing = new KpiMeasurement();
        existing.setId(MEAS); existing.setTenantId(TENANT); existing.setKpiId(KPI);
        existing.setPeriodStart(Instant.parse("2026-04-01T00:00:00Z"));
        existing.setPeriodEnd(Instant.parse("2026-04-30T23:59:59Z"));
        existing.setValue(new BigDecimal("80"));
        existing.setCreatedAt(Instant.now());
        when(measureRepo.findByKpiIdAndPeriodStart(eq(KPI), eq(existing.getPeriodStart())))
                .thenReturn(Optional.of(existing));
        when(measureRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        KpiDto.MeasurementResponse out = service.record(KPI, new KpiDto.RecordMeasurementRequest(
                existing.getPeriodStart(), existing.getPeriodEnd(),
                new BigDecimal("92"), null, MeasurementSource.COMPUTED, USER, null));
        assertThat(existing.getValue()).isEqualByComparingTo("92");
        assertThat(out.health()).isEqualTo(KpiHealth.WARNING);
        assertThat(out.source()).isEqualTo(MeasurementSource.COMPUTED);
    }

    @Test
    void record_defaultsUnitFromDefinition() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        d.setUnit("ppm");
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        when(measureRepo.findByKpiIdAndPeriodStart(any(), any())).thenReturn(Optional.empty());
        when(measureRepo.save(any())).thenAnswer(inv -> {
            KpiMeasurement m = inv.getArgument(0);
            m.setId(MEAS); m.setCreatedAt(Instant.now()); return m;
        });
        KpiDto.MeasurementResponse out = service.record(KPI, new KpiDto.RecordMeasurementRequest(
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-30T23:59:59Z"),
                new BigDecimal("90"), null, null, USER, null));
        assertThat(out.unit()).isEqualTo("ppm");
    }

    @Test
    void deleteMeasurement_archived_rejected() {
        KpiDefinition d = kpi(KpiStatus.ARCHIVED);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.deleteMeasurement(KPI, MEAS))
                .isInstanceOf(KpiStateException.class);
    }

    @Test
    void deleteMeasurement_crossKpi_appearsNotFound() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        KpiMeasurement m = new KpiMeasurement();
        m.setId(MEAS); m.setKpiId(UUID.randomUUID()); m.setTenantId(TENANT);
        when(measureRepo.findById(MEAS)).thenReturn(Optional.of(m));
        assertThatThrownBy(() -> service.deleteMeasurement(KPI, MEAS))
                .isInstanceOf(KpiMeasurementNotFoundException.class);
    }

    @Test
    void deleteMeasurement_happyPath() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        KpiMeasurement m = new KpiMeasurement();
        m.setId(MEAS); m.setKpiId(KPI); m.setTenantId(TENANT);
        when(measureRepo.findById(MEAS)).thenReturn(Optional.of(m));
        service.deleteMeasurement(KPI, MEAS);
        verify(measureRepo).delete(m);
    }

    @Test
    void listMeasurements_paginated() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        when(measureRepo.findByKpiIdOrderByPeriodStartDesc(eq(KPI), any()))
                .thenReturn(new PageImpl<>(List.of(measurement("92"))));
        assertThat(service.listMeasurements(KPI, PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    // ----- Status / Trend -----

    @Test
    void currentStatus_noMeasurement_unknown() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        when(measureRepo.findTop24ByKpiIdOrderByPeriodStartDesc(KPI)).thenReturn(List.of());
        KpiDto.KpiCurrentStatus out = service.currentStatus(KPI);
        assertThat(out.health()).isEqualTo(KpiHealth.UNKNOWN);
        assertThat(out.latestValue()).isNull();
    }

    @Test
    void currentStatus_withMeasurement_computesHealth() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        when(measureRepo.findTop24ByKpiIdOrderByPeriodStartDesc(KPI))
                .thenReturn(List.of(measurement("96")));
        KpiDto.KpiCurrentStatus out = service.currentStatus(KPI);
        assertThat(out.health()).isEqualTo(KpiHealth.OK);
        assertThat(out.latestValue()).isEqualByComparingTo("96");
    }

    @Test
    void trend_returnsChronologicalOrder() {
        KpiDefinition d = kpi(KpiStatus.ACTIVE);
        when(defRepo.findById(KPI)).thenReturn(Optional.of(d));
        // Repo retourne DESC ; le service doit renvoyer ASC.
        KpiMeasurement m1 = measurementAt("96", "2026-04-01T00:00:00Z");
        KpiMeasurement m2 = measurementAt("92", "2026-03-01T00:00:00Z");
        KpiMeasurement m3 = measurementAt("85", "2026-02-01T00:00:00Z");
        when(measureRepo.findTop24ByKpiIdOrderByPeriodStartDesc(KPI))
                .thenReturn(List.of(m1, m2, m3));
        KpiDto.KpiTrend t = service.trend(KPI);
        assertThat(t.sampleCount()).isEqualTo(3);
        assertThat(t.points().get(0).periodStart()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
        assertThat(t.points().get(2).periodStart()).isEqualTo(Instant.parse("2026-04-01T00:00:00Z"));
        // Health calculé pour chaque point : 85 < target 95 mais ≥ warning 90 → WARNING
        assertThat(t.points().get(2).health()).isEqualTo(KpiHealth.OK);
        assertThat(t.points().get(1).health()).isEqualTo(KpiHealth.WARNING);
    }

    // ----- helpers -----

    private KpiDefinition kpi(KpiStatus status) {
        KpiDefinition d = new KpiDefinition();
        d.setId(KPI); d.setTenantId(TENANT);
        d.setCode("k"); d.setName("KPI");
        d.setDirection(KpiDirection.HIGHER_IS_BETTER);
        d.setFrequency(KpiFrequency.MONTHLY);
        d.setStatus(status);
        d.setTargetValue(new BigDecimal("95"));
        d.setThresholdWarning(new BigDecimal("90"));
        d.setThresholdCritical(new BigDecimal("80"));
        d.setCreatedBy(USER);
        d.setCreatedAt(Instant.now()); d.setUpdatedAt(Instant.now());
        return d;
    }

    private KpiMeasurement measurement(String value) {
        return measurementAt(value, "2026-04-01T00:00:00Z");
    }

    private KpiMeasurement measurementAt(String value, String start) {
        KpiMeasurement m = new KpiMeasurement();
        m.setId(UUID.randomUUID());
        m.setTenantId(TENANT); m.setKpiId(KPI);
        m.setPeriodStart(Instant.parse(start));
        m.setPeriodEnd(Instant.parse(start).plusSeconds(86400 * 30L));
        m.setValue(new BigDecimal(value));
        m.setSource(MeasurementSource.MANUAL);
        m.setCreatedAt(Instant.now());
        return m;
    }
}
