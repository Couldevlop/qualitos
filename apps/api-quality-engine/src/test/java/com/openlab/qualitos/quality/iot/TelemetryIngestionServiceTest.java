package com.openlab.qualitos.quality.iot;

import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.pdca.PdcaDto;
import com.openlab.qualitos.quality.pdca.PdcaService;
import com.openlab.qualitos.quality.pdca.PdcaStatus;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class TelemetryIngestionServiceTest {

    @Mock IotDeviceRepository deviceRepo;
    @Mock IotTelemetryEventRepository eventRepo;
    @Mock IotThresholdRepository thresholdRepo;
    @Mock CapaService capaService;
    @Mock CapaCaseRepository capaCaseRepo;
    @Mock PdcaService pdcaService;
    @InjectMocks TelemetryIngestionService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID DEV = UUID.randomUUID();

    @BeforeEach
    void setup() {
        TenantContext.setTenantId(TENANT.toString());
        // Par défaut, aucun seuil configuré → pas de CAPA (surclassé par les tests de dérive).
        lenient().when(thresholdRepo.findApplicable(any(), any(), any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void ingest_active_persistsEventAndIncrementsCounter() {
        IotDevice d = device(IotDeviceStatus.ACTIVE);
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(d));
        when(eventRepo.save(any())).thenAnswer(inv -> {
            IotTelemetryEvent e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setIngestedAt(Instant.now());
            return e;
        });
        when(deviceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IotDto.TelemetryResponse out = service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "temperature", new BigDecimal("23.4"), null, "C", null, null));

        assertThat(out.metric()).isEqualTo("temperature");
        assertThat(out.source()).isEqualTo(IotProtocol.MANUAL);
        assertThat(d.getTelemetryCount()).isOne();
        assertThat(d.getLastSeenAt()).isNotNull();
    }

    @Test
    void ingest_provisioned_rejected() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.PROVISIONED)));
        assertThatThrownBy(() -> service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "x", BigDecimal.ONE, null, null, null, null)))
                .isInstanceOf(IotDeviceStateException.class);
        verifyNoInteractions(eventRepo);
    }

    @Test
    void ingest_suspended_rejected() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.SUSPENDED)));
        assertThatThrownBy(() -> service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "x", BigDecimal.ONE, null, null, null, null)))
                .isInstanceOf(IotDeviceStateException.class);
    }

    @Test
    void ingest_decommissioned_rejected() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.DECOMMISSIONED)));
        assertThatThrownBy(() -> service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "x", BigDecimal.ONE, null, null, null, null)))
                .isInstanceOf(IotDeviceStateException.class);
    }

    @Test
    void ingest_otherTenant_appearsNotFound() {
        IotDevice d = device(IotDeviceStatus.ACTIVE);
        d.setTenantId(UUID.randomUUID());
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "x", BigDecimal.ONE, null, null, null, null)))
                .isInstanceOf(IotDeviceNotFoundException.class);
    }

    @Test
    void ingest_missingDevice_throws() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "x", BigDecimal.ONE, null, null, null, null)))
                .isInstanceOf(IotDeviceNotFoundException.class);
    }

    @Test
    void ingest_neitherNumericNorText_rejected() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.ACTIVE)));
        assertThatThrownBy(() -> service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "x", null, null, null, null, null)))
                .isInstanceOf(IotDeviceStateException.class)
                .hasMessageContaining("valueNumeric/valueText");
    }

    @Test
    void ingest_textOnly_accepted() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.ACTIVE)));
        when(eventRepo.save(any())).thenAnswer(inv -> {
            IotTelemetryEvent e = inv.getArgument(0);
            e.setIngestedAt(Instant.now());
            return e;
        });
        when(deviceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IotDto.TelemetryResponse out = service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "state", null, "RUNNING", null, null, IotProtocol.OPC_UA));
        assertThat(out.valueText()).isEqualTo("RUNNING");
        assertThat(out.source()).isEqualTo(IotProtocol.OPC_UA);
    }

    @Test
    void ingest_capturesRequestedRecordedAt() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.ACTIVE)));
        Instant ts = Instant.parse("2026-03-15T10:00:00Z");
        ArgumentCaptor<IotTelemetryEvent> cap = ArgumentCaptor.forClass(IotTelemetryEvent.class);
        when(eventRepo.save(cap.capture())).thenAnswer(inv -> {
            IotTelemetryEvent e = inv.getArgument(0);
            e.setIngestedAt(Instant.now());
            return e;
        });
        when(deviceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "vibration_rms", new BigDecimal("0.42"), null, "mm/s", ts, IotProtocol.MQTT));

        assertThat(cap.getValue().getRecordedAt()).isEqualTo(ts);
        assertThat(cap.getValue().getSource()).isEqualTo(IotProtocol.MQTT);
    }

    @Test
    void recent_byDevice_returnsPage() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.ACTIVE)));
        when(eventRepo.findByTenantIdAndDeviceIdOrderByRecordedAtDesc(eq(TENANT), eq(DEV), any()))
                .thenReturn(new PageImpl<>(List.of(event())));
        assertThat(service.recent(DEV, PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    @Test
    void recent_otherTenant_appearsNotFound() {
        IotDevice d = device(IotDeviceStatus.ACTIVE);
        d.setTenantId(UUID.randomUUID());
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.recent(DEV, PageRequest.of(0, 10)))
                .isInstanceOf(IotDeviceNotFoundException.class);
    }

    @Test
    void range_filtersByMetricAndWindow() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.ACTIVE)));
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");
        when(eventRepo.findByTenantIdAndDeviceIdAndMetricAndRecordedAtBetweenOrderByRecordedAtAsc(
                eq(TENANT), eq(DEV), eq("temperature"), eq(from), eq(to), any()))
                .thenReturn(new PageImpl<>(List.of(event())));
        assertThat(service.range(DEV, "temperature", from, to, PageRequest.of(0, 100))
                .getTotalElements()).isOne();
    }

    @Test
    void purgeBefore_delegatesToRepo() {
        when(eventRepo.deleteByRecordedAtBefore(any())).thenReturn(17L);
        assertThat(service.purgeBefore(Instant.now())).isEqualTo(17L);
    }

    // ---- Détection de seuil → CAPA (§9.9) ----

    @Test
    void ingest_breachesMax_opensCapa() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.ACTIVE)));
        stubSaves();
        UUID owner = UUID.randomUUID();
        when(thresholdRepo.findApplicable(TENANT, DEV, "temperature"))
                .thenReturn(List.of(threshold(null, 8.0, CapaCriticity.HIGH, owner)));
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);

        service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "temperature", new BigDecimal("12.0"), null, "C", null, null));

        ArgumentCaptor<CapaDto.CreateCaseRequest> cap = ArgumentCaptor.forClass(CapaDto.CreateCaseRequest.class);
        verify(capaService).createCase(cap.capture());
        verify(thresholdRepo).findApplicable(eq(TENANT), eq(DEV), eq("temperature")); // tenant via JWT
        assertThat(cap.getValue().sourceType()).isEqualTo(CapaSourceType.IOT_ALERT);
        assertThat(cap.getValue().type()).isEqualTo(CapaType.CORRECTIVE);
        assertThat(cap.getValue().criticity()).isEqualTo(CapaCriticity.HIGH);
        assertThat(cap.getValue().ownerId()).isEqualTo(owner);
        assertThat(cap.getValue().sourceRef()).isEqualTo("iot:device:" + DEV + ":metric:temperature");
        verify(pdcaService, never()).createCycle(any()); // openPdcaCycle=false par défaut
    }

    @Test
    void ingest_breachWithPdcaEnabled_opensCycleAndReferencesIt() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.ACTIVE)));
        stubSaves();
        IotThreshold t = threshold(null, 8.0, CapaCriticity.HIGH, UUID.randomUUID());
        t.setOpenPdcaCycle(true);
        when(thresholdRepo.findApplicable(TENANT, DEV, "temperature")).thenReturn(List.of(t));
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);
        UUID cycleId = UUID.randomUUID();
        when(pdcaService.createCycle(any())).thenReturn(cycleResp(cycleId));

        service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "temperature", new BigDecimal("12.0"), null, "C", null, null));

        verify(pdcaService).createCycle(any());
        ArgumentCaptor<CapaDto.CreateCaseRequest> cap = ArgumentCaptor.forClass(CapaDto.CreateCaseRequest.class);
        verify(capaService).createCase(cap.capture());
        assertThat(cap.getValue().description()).contains(cycleId.toString());
    }

    private PdcaDto.CycleResponse cycleResp(UUID id) {
        return new PdcaDto.CycleResponse(
                id, TENANT, "t", null, PdcaStatus.PLAN, UUID.randomUUID(),
                Instant.now(), Instant.now(), null, List.of());
    }

    @Test
    void ingest_withinBounds_noCapa() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.ACTIVE)));
        stubSaves();
        when(thresholdRepo.findApplicable(TENANT, DEV, "temperature"))
                .thenReturn(List.of(threshold(null, 8.0, CapaCriticity.HIGH, UUID.randomUUID())));

        service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "temperature", new BigDecimal("5.0"), null, "C", null, null));

        verifyNoInteractions(capaService);
    }

    @Test
    void ingest_breachButActiveCapaExists_noDuplicate() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.ACTIVE)));
        stubSaves();
        when(thresholdRepo.findApplicable(TENANT, DEV, "temperature"))
                .thenReturn(List.of(threshold(null, 8.0, CapaCriticity.HIGH, UUID.randomUUID())));
        when(capaCaseRepo.existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(any(), any(), any(), any()))
                .thenReturn(true); // une CAPA OPEN/IN_PROGRESS couvre déjà l'origine

        service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "temperature", new BigDecimal("12.0"), null, "C", null, null));

        verify(capaService, never()).createCase(any());
    }

    @Test
    void ingest_textOnly_noThresholdEvaluation() {
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(device(IotDeviceStatus.ACTIVE)));
        stubSaves();

        service.ingest(DEV, new IotDto.TelemetryIngestRequest(
                "state", null, "RUNNING", null, null, IotProtocol.OPC_UA));

        verify(thresholdRepo, never()).findApplicable(any(), any(), any());
        verifyNoInteractions(capaService);
    }

    private IotThreshold threshold(Double min, Double max, CapaCriticity crit, UUID owner) {
        IotThreshold t = new IotThreshold();
        t.setTenantId(TENANT);
        t.setMetric("temperature");
        t.setMinValue(min);
        t.setMaxValue(max);
        t.setCapaCriticity(crit);
        t.setCapaOwnerId(owner);
        t.setEnabled(true);
        return t;
    }

    private void stubSaves() {
        when(eventRepo.save(any())).thenAnswer(inv -> {
            IotTelemetryEvent e = inv.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            e.setIngestedAt(Instant.now());
            return e;
        });
        when(deviceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private IotDevice device(IotDeviceStatus status) {
        IotDevice d = new IotDevice();
        d.setId(DEV);
        d.setTenantId(TENANT);
        d.setStatus(status);
        d.setTelemetryCount(0L);
        d.setDeviceType(IotDeviceType.SENSOR_TEMPERATURE);
        d.setProtocol(IotProtocol.MQTT);
        return d;
    }

    private IotTelemetryEvent event() {
        IotTelemetryEvent e = new IotTelemetryEvent();
        e.setId(UUID.randomUUID());
        e.setTenantId(TENANT);
        e.setDeviceId(DEV);
        e.setMetric("temperature");
        e.setValueNumeric(new BigDecimal("21.0"));
        e.setSource(IotProtocol.MQTT);
        e.setRecordedAt(Instant.now());
        e.setIngestedAt(Instant.now());
        return e;
    }
}
