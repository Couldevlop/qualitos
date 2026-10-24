package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dmaic_resolution_settings")
@Getter
@Setter
@NoArgsConstructor
public class ResolutionSettingsJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false, unique = true)
    private UUID tenantId;

    /** Les questions en JSON, dans l'ordre où la fiche les pose. */
    @Column(nullable = false, columnDefinition = "text")
    private String questions;

    @Column(name = "failure_types", nullable = false, columnDefinition = "text")
    private String failureTypes;

    @Column(nullable = false, columnDefinition = "text")
    private String severities;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
