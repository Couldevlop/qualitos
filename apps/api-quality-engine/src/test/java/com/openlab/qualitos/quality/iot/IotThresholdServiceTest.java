package com.openlab.qualitos.quality.iot;

import com.openlab.qualitos.quality.capa.CapaCriticity;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IotThresholdServiceTest {

    @Mock IotThresholdRepository repo;
    @Mock IotDeviceRepository deviceRepo;
    @InjectMocks IotThresholdService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID DEV = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();

    @BeforeEach
    void setup() { TenantContext.setTenantId(TENANT.toString()); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void create_tenantWide_persists() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IotDto.ThresholdResponse out = service.create(new IotDto.ThresholdRequest(
                null, "temperature", null, 8.0, CapaCriticity.HIGH, OWNER, null));

        assertThat(out.tenantId()).isEqualTo(TENANT);
        assertThat(out.deviceId()).isNull();
        assertThat(out.maxValue()).isEqualTo(8.0);
        assertThat(out.enabled()).isTrue(); // null → activé par défaut
        verifyNoInteractions(deviceRepo); // pas de seuil ciblé device
    }

    @Test
    void create_deviceScoped_validatesTenant() {
        IotDevice d = new IotDevice();
        d.setId(DEV);
        d.setTenantId(TENANT);
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(d));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IotDto.ThresholdResponse out = service.create(new IotDto.ThresholdRequest(
                DEV, "temperature", 2.0, 8.0, CapaCriticity.MEDIUM, OWNER, true));

        assertThat(out.deviceId()).isEqualTo(DEV);
    }

    @Test
    void create_deviceOfOtherTenant_rejected() {
        IotDevice d = new IotDevice();
        d.setId(DEV);
        d.setTenantId(UUID.randomUUID());
        when(deviceRepo.findById(DEV)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.create(new IotDto.ThresholdRequest(
                DEV, "temperature", null, 8.0, CapaCriticity.HIGH, OWNER, null)))
                .isInstanceOf(IotDeviceNotFoundException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void list_filtersByTenant() {
        when(repo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(sample())));
        assertThat(service.list(PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    @Test
    void get_unknown_throws() {
        when(repo.findByIdAndTenantId(any(), eq(TENANT))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(UUID.randomUUID()))
                .isInstanceOf(IotThresholdNotFoundException.class);
    }

    @Test
    void update_mutatesFields() {
        IotThreshold t = sample();
        when(repo.findByIdAndTenantId(eq(t.getId()), eq(TENANT))).thenReturn(Optional.of(t));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IotDto.ThresholdResponse out = service.update(t.getId(), new IotDto.ThresholdRequest(
                null, "pressure", 1.0, 5.0, CapaCriticity.CRITICAL, OWNER, false));

        assertThat(out.metric()).isEqualTo("pressure");
        assertThat(out.minValue()).isEqualTo(1.0);
        assertThat(out.capaCriticity()).isEqualTo(CapaCriticity.CRITICAL);
        assertThat(out.enabled()).isFalse();
    }

    @Test
    void delete_unknown_throws() {
        when(repo.findByIdAndTenantId(any(), eq(TENANT))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
                .isInstanceOf(IotThresholdNotFoundException.class);
    }

    private IotThreshold sample() {
        IotThreshold t = new IotThreshold();
        t.setId(UUID.randomUUID());
        t.setTenantId(TENANT);
        t.setMetric("temperature");
        t.setMaxValue(8.0);
        t.setCapaCriticity(CapaCriticity.HIGH);
        t.setCapaOwnerId(OWNER);
        t.setEnabled(true);
        return t;
    }
}
