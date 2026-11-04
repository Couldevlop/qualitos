package com.openlab.qualitos.quality.nonconformity.subcategory;

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

/**
 * Le catalogue de sous-catégories propre à un client. Une ligne par client, ou
 * aucune : sans ligne, le client travaille avec le catalogue de la plateforme.
 */
@Entity
@Table(name = "nc_subcategory_settings")
@Getter
@Setter
@NoArgsConstructor
public class NcSubcategorySettingsJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false, unique = true)
    private UUID tenantId;

    /** Le catalogue en JSON, dans l'ordre d'affichage. */
    @Column(nullable = false, columnDefinition = "text")
    private String catalogue;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
