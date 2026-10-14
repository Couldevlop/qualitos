package com.openlab.qualitos.quality.change;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MocSettingsRowRepository extends JpaRepository<MocSettingsRow, UUID> {
}
