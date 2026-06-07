package com.openlab.qualitos.quality.erpconnector;

/**
 * DTO neutre représentant un fournisseur lu depuis un ERP externe.
 * Chaque {@link ErpProviderClient} traduit son format propre (SAP BusinessPartner,
 * Oracle Supplier, Dynamics vendor) vers cette structure avant que le service domaine
 * ne décide quoi en faire (upsert dans le module quality/supplier).
 *
 * @param externalCode code fournisseur côté ERP (idempotence : (tenant, code))
 * @param name         raison sociale
 * @param category     catégorie/typologie brute côté ERP (mappée vers SupplierType)
 * @param countryCode  code pays ISO 3166-1 alpha-2 (optionnel)
 * @param email        email de contact (optionnel)
 */
public record ExternalSupplier(
        String externalCode,
        String name,
        String category,
        String countryCode,
        String email
) {}
