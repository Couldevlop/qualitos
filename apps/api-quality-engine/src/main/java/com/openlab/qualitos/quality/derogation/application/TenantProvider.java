package com.openlab.qualitos.quality.derogation.application;

import java.util.UUID;

/**
 * Le client (tenant) de la requête en cours.
 *
 * <p>Un port, et non un appel statique à {@code TenantContext} : le service ne
 * doit rien savoir du jeton ni de la requête HTTP qui le porte, et une règle
 * métier doit pouvoir se tester sans contexte de sécurité.
 */
public interface TenantProvider {

    UUID requireTenantId();
}
