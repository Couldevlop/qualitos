package com.openlab.qualitos.quality.derogation.application;

import java.util.UUID;

/**
 * Le lien qui ouvre un dossier dans QualitOS.
 *
 * <p>Il part dans le courriel du manager — « une notification par mail avec un
 * lien qui le conduit dans QualitOS ». L'adresse publique de l'application
 * dépend du déploiement : le service ne doit pas la connaître, et surtout pas la
 * déduire de la requête en cours, qui peut arriver derrière un relais et porter
 * un en-tête forgé.
 */
public interface DerogationLinkBuilder {

    String linkTo(UUID derogationId);
}
