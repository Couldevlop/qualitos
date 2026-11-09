package com.openlab.qualitos.quality.settings.catalog;

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
 * Une entrée du catalogue d'un client : une ligne par valeur, pas un document
 * JSON. Le client qui n'a rien réglé n'a aucune ligne — il travaille alors avec
 * les valeurs de la plateforme ({@link CatalogId#defaults()}).
 *
 * <p>Une ligne par valeur, et non un bloc par liste, pour que l'usage d'un code
 * reste interrogeable en SQL : « qui a renommé quoi », « quelle liste s'écarte
 * du socle », et demain « combien d'écarts classés sous un code retiré ».
 */
@Entity
@Table(name = "tenant_catalog_entries")
@Getter
@Setter
@NoArgsConstructor
public class TenantCatalogEntryRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /** Le nom de la liste ({@link CatalogId#slug()}). */
    @Column(nullable = false, length = 60, updatable = false)
    private String catalogue;

    @Column(nullable = false, length = CatalogEntry.CODE_MAX, updatable = false)
    private String code;

    @Column(nullable = false, length = CatalogEntry.LABEL_MAX)
    private String label;

    /** Rang d'affichage, à partir de 0 : l'ordre est un choix du client. */
    @Column(nullable = false)
    private int position;

    /**
     * Faux pour une valeur retirée des nouvelles saisies. Elle reste en base :
     * effacer un classement passé réécrirait l'histoire.
     */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
