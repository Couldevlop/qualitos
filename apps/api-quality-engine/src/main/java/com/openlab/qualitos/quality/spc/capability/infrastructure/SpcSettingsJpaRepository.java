package com.openlab.qualitos.quality.spc.capability.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpcSettingsJpaRepository extends JpaRepository<SpcSettingsJpaEntity, UUID> {
}
