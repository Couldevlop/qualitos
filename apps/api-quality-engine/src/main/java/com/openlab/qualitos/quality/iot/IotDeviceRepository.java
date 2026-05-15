package com.openlab.qualitos.quality.iot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IotDeviceRepository extends JpaRepository<IotDevice, UUID> {

    Optional<IotDevice> findByTenantIdAndCode(UUID tenantId, String code);

    Page<IotDevice> findByTenantId(UUID tenantId, Pageable pageable);

    Page<IotDevice> findByTenantIdAndStatus(UUID tenantId, IotDeviceStatus status, Pageable pageable);

    Page<IotDevice> findByTenantIdAndDeviceType(UUID tenantId, IotDeviceType type, Pageable pageable);
}
