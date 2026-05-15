package com.openlab.qualitos.quality.iot;

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
class IotDeviceServiceTest {

    @Mock IotDeviceRepository repo;
    @InjectMocks IotDeviceService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID DEV = UUID.randomUUID();

    @BeforeEach
    void setup() { TenantContext.setTenantId(TENANT.toString()); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void create_persistsWithTenantAndProvisionedStatus() {
        when(repo.findByTenantIdAndCode(TENANT, "machine-A"))
                .thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> {
            IotDevice d = inv.getArgument(0);
            d.setId(DEV);
            d.setCreatedAt(Instant.now());
            d.setUpdatedAt(Instant.now());
            return d;
        });

        IotDto.DeviceResponse out = service.create(new IotDto.CreateDeviceRequest(
                "machine-A", "Press A", IotDeviceType.PLC, IotProtocol.OPC_UA,
                "site|area|line", null, null, USER));

        assertThat(out.tenantId()).isEqualTo(TENANT);
        assertThat(out.status()).isEqualTo(IotDeviceStatus.PROVISIONED);
        assertThat(out.code()).isEqualTo("machine-A");
    }

    @Test
    void create_duplicateCode_throws() {
        when(repo.findByTenantIdAndCode(TENANT, "dup")).thenReturn(Optional.of(device()));
        assertThatThrownBy(() -> service.create(new IotDto.CreateDeviceRequest(
                "dup", "n", IotDeviceType.PLC, IotProtocol.MQTT, null, null, null, USER)))
                .isInstanceOf(IotDeviceStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void create_noTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.create(new IotDto.CreateDeviceRequest(
                "x", "n", IotDeviceType.PLC, IotProtocol.MQTT, null, null, null, USER)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void list_byStatus_filtered() {
        when(repo.findByTenantIdAndStatus(eq(TENANT), eq(IotDeviceStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(device())));
        assertThat(service.list(IotDeviceStatus.ACTIVE, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    @Test
    void list_byType_filtered() {
        when(repo.findByTenantIdAndDeviceType(eq(TENANT), eq(IotDeviceType.PLC), any()))
                .thenReturn(new PageImpl<>(List.of(device())));
        assertThat(service.list(null, IotDeviceType.PLC, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    @Test
    void list_noFilter_allTenantDevices() {
        when(repo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(device())));
        assertThat(service.list(null, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    @Test
    void get_otherTenant_appearsNotFound() {
        IotDevice d = device();
        d.setTenantId(UUID.randomUUID());
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.get(DEV))
                .isInstanceOf(IotDeviceNotFoundException.class);
    }

    @Test
    void update_appliesPatches() {
        IotDevice d = device();
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(DEV, new IotDto.UpdateDeviceRequest(
                "Renamed", IotDeviceType.SENSOR_TEMPERATURE, IotProtocol.MQTT,
                "new-loc", "desc", "{}"));

        assertThat(d.getName()).isEqualTo("Renamed");
        assertThat(d.getDeviceType()).isEqualTo(IotDeviceType.SENSOR_TEMPERATURE);
        assertThat(d.getProtocol()).isEqualTo(IotProtocol.MQTT);
        assertThat(d.getLocation()).isEqualTo("new-loc");
        assertThat(d.getDescription()).isEqualTo("desc");
        assertThat(d.getMetadataJson()).isEqualTo("{}");
    }

    @Test
    void delete_provisioned_allowed() {
        IotDevice d = device();
        d.setTelemetryCount(0L);
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        service.delete(DEV);
        verify(repo).delete(d);
    }

    @Test
    void delete_withTelemetry_blockedUnlessDecommissioned() {
        IotDevice d = device();
        d.setStatus(IotDeviceStatus.ACTIVE);
        d.setTelemetryCount(42L);
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.delete(DEV))
                .isInstanceOf(IotDeviceStateException.class)
                .hasMessageContaining("decommission first");
    }

    @Test
    void delete_decommissionedWithTelemetry_allowed() {
        IotDevice d = device();
        d.setStatus(IotDeviceStatus.DECOMMISSIONED);
        d.setTelemetryCount(42L);
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        service.delete(DEV);
        verify(repo).delete(d);
    }

    @Test
    void activate_fromProvisioned_ok() {
        IotDevice d = device();
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        IotDto.DeviceResponse out = service.activate(DEV);
        assertThat(out.status()).isEqualTo(IotDeviceStatus.ACTIVE);
    }

    @Test
    void activate_fromSuspended_ok() {
        IotDevice d = device();
        d.setStatus(IotDeviceStatus.SUSPENDED);
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.activate(DEV).status()).isEqualTo(IotDeviceStatus.ACTIVE);
    }

    @Test
    void activate_fromDecommissioned_rejected() {
        IotDevice d = device();
        d.setStatus(IotDeviceStatus.DECOMMISSIONED);
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.activate(DEV))
                .isInstanceOf(IotDeviceStateException.class);
    }

    @Test
    void suspend_fromActive_ok() {
        IotDevice d = device();
        d.setStatus(IotDeviceStatus.ACTIVE);
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.suspend(DEV).status()).isEqualTo(IotDeviceStatus.SUSPENDED);
    }

    @Test
    void suspend_fromProvisioned_rejected() {
        IotDevice d = device(); // status PROVISIONED par default
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.suspend(DEV))
                .isInstanceOf(IotDeviceStateException.class);
    }

    @Test
    void decommission_fromActive_ok() {
        IotDevice d = device();
        d.setStatus(IotDeviceStatus.ACTIVE);
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.decommission(DEV).status()).isEqualTo(IotDeviceStatus.DECOMMISSIONED);
    }

    @Test
    void decommission_alreadyDecommissioned_rejected() {
        IotDevice d = device();
        d.setStatus(IotDeviceStatus.DECOMMISSIONED);
        when(repo.findById(DEV)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.decommission(DEV))
                .isInstanceOf(IotDeviceStateException.class);
    }

    private IotDevice device() {
        IotDevice d = new IotDevice();
        d.setId(DEV);
        d.setTenantId(TENANT);
        d.setCode("dev-1");
        d.setName("Device 1");
        d.setDeviceType(IotDeviceType.PLC);
        d.setProtocol(IotProtocol.OPC_UA);
        d.setStatus(IotDeviceStatus.PROVISIONED);
        d.setCreatedBy(USER);
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return d;
    }
}
