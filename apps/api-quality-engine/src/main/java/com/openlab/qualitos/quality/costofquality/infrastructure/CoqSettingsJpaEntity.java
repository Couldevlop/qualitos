package com.openlab.qualitos.quality.costofquality.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Un réglage par client : la clé primaire est le tenant lui-même. */
@Entity
@Table(name = "coq_settings")
@Getter
@Setter
@NoArgsConstructor
public class CoqSettingsJpaEntity {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
