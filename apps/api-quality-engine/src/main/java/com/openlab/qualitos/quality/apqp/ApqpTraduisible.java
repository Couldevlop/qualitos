package com.openlab.qualitos.quality.apqp;

import java.time.Instant;

/**
 * Ce qu'une ligne du cycle doit dire pour qu'on sache s'il faut la traduire.
 *
 * <p>Une phase et un livrable n'ont rien d'autre en commun, et cette interface ne
 * cherche pas à leur en donner : elle expose les deux dates qui répondent à la
 * seule question posée — cette ligne a-t-elle été retouchée depuis l'amorçage ?
 * Si oui, son texte appartient au client et ne se traduit plus.
 */
interface ApqpTraduisible {

    Instant getCreatedAt();

    Instant getUpdatedAt();
}
