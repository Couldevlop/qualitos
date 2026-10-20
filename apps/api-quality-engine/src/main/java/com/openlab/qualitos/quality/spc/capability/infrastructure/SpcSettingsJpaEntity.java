package com.openlab.qualitos.quality.spc.capability.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Un réglage par client : la clé primaire est le tenant lui-même. */
@Entity
@Table(name = "spc_settings")
@Getter
@Setter
@NoArgsConstructor
public class SpcSettingsJpaEntity {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "capable_threshold", nullable = false, precision = 4, scale = 2)
    private BigDecimal capableThreshold;

    @Column(name = "watch_threshold", nullable = false, precision = 4, scale = 2)
    private BigDecimal watchThreshold;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
