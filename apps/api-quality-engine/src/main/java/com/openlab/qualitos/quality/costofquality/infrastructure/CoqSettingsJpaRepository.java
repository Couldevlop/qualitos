package com.openlab.qualitos.quality.costofquality.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CoqSettingsJpaRepository extends JpaRepository<CoqSettingsJpaEntity, UUID> {
}
