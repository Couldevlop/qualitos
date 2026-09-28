package com.openlab.qualitos.quality.costofquality.infrastructure;

import com.openlab.qualitos.quality.costofquality.domain.CoqCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "coq_entries")
@Getter
@Setter
@NoArgsConstructor
public class CoqEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "label_id", nullable = false)
    private UUID labelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CoqCategory category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 150)
    private String responsible;

    @Column(name = "imputation_date", nullable = false)
    private LocalDate imputationDate;

    @Column(length = 2000)
    private String comment;

    @Column(name = "part_control", nullable = false)
    private boolean partControl;

    @Column(name = "part_reference", length = 120)
    private String partReference;

    @Column(name = "part_quantity")
    private Integer partQuantity;

    @Column(length = 120)
    private String lot;

    @Column(name = "received_or_made_on")
    private LocalDate receivedOrMadeOn;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
