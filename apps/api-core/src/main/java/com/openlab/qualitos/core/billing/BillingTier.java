package com.openlab.qualitos.core.billing;

/**
 * Le palier commercial d'un tenant : FREE, STANDARD, PRO ou ENTERPRISE.
 *
 * <p><b>Jumelle volontaire.</b> Une énumération strictement identique existe
 * dans le moteur de qualité
 * ({@code com.openlab.qualitos.quality.tenantmodules.domain.BillingTier}),
 * qui l'emploie accessoirement pour décider si un module est activable pour
 * un tenant. {@code api-core} ne peut pas l'importer : {@code api-core} et
 * {@code api-quality-engine} sont deux modules Maven FRÈRES, aucun ne dépend
 * de l'autre — les faire dépendre l'un de l'autre inverserait la couche
 * (le tarif, qui est une affaire de facturation propre à {@code api-core},
 * finirait par dépendre du moteur métier).
 *
 * <p>Créer une bibliothèque partagée pour QUATRE constantes coûterait plus
 * cher (un nouveau module Maven, une release coordonnée dès qu'une valeur
 * change) que la duplication assumée ici. Le palier est un vocabulaire
 * COMMERCIAL — c'est ici, dans la facturation, qu'il vit ; le moteur de
 * qualité ne fait que le consulter pour ses propres besoins d'activation.
 *
 * <p>Les deux copies doivent rester synchronisées à la main : toute évolution
 * de l'une doit être répercutée sur l'autre.
 */
public enum BillingTier { FREE, STANDARD, PRO, ENTERPRISE }
