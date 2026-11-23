package com.openlab.qualitos.quality.derogation.application;

import java.util.UUID;

/**
 * Le lien qui ouvre un dossier dans QualitOS, sous ses DEUX formes.
 *
 * <p>Elles ne sont pas interchangeables :
 * <ul>
 *   <li>{@link #linkTo(UUID)} rend une adresse <b>absolue</b>, pour le courriel
 *       — « une notification par mail avec un lien qui le conduit dans
 *       QualitOS ». Un chemin relatif dans un courriel ne mene nulle part ;</li>
 *   <li>{@link #pathTo(UUID)} rend un <b>chemin</b>, pour la notification en
 *       application. L'ecran la suit avec {@code router.navigateByUrl} : une
 *       adresse absolue n'y serait pas une navigation mais une route
 *       introuvable, et le clic ne menerait a rien.</li>
 * </ul>
 *
 * <p>L'adresse publique vient de la configuration. Le service ne la connait pas,
 * et surtout ne la deduit pas de la requete en cours, qui peut arriver derriere
 * un relais et porter un en-tete forge.
 */
public interface DerogationLinkBuilder {

    /** Absolu, pour un courriel. */
    String linkTo(UUID derogationId);

    /** Relatif, pour une notification que l'ecran suit lui-meme. */
    String pathTo(UUID derogationId);
}
