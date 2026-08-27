package com.openlab.qualitos.quality.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Ramène un instant à ce que la base de données sait rendre à l'identique.
 *
 * <p>{@link Instant} porte la nanoseconde. La colonne
 * {@code TIMESTAMP WITH TIME ZONE} de PostgreSQL n'en garde que la microseconde,
 * et elle <b>arrondit</b> : {@code .123456789} ressort en {@code .123457}. Un
 * instant écrit puis relu n'est donc pas nécessairement le même objet qu'avant
 * l'écriture.
 *
 * <p>Cela n'a aucune importance tant qu'on se contente d'afficher une date. Cela
 * en a une, décisive, dès qu'un horodatage entre dans le calcul d'une
 * <b>empreinte</b> : l'empreinte est calculée sur l'objet en mémoire, avant
 * l'écriture, et recalculée plus tard sur la ligne relue. Si l'horodatage a
 * changé d'un chiffre entre-temps, l'empreinte ne retombe jamais, et le contrôle
 * censé prouver l'intégrité annonce une falsification qui n'a pas eu lieu.
 *
 * <p>Le journal d'audit chaîné en a fait les frais : la vérification déclarait
 * « Integrity hash mismatch (tamper) » sur la totalité d'un registre intact,
 * premier événement compris. Le scellement des control plans portait la même
 * faille, silencieuse celle-là — le plan approuvé était signé sur une empreinte
 * que personne ne pouvait plus recalculer à partir du document rendu par l'API.
 *
 * <p>D'où cette règle, écrite une fois : <b>tout horodatage qui entre dans une
 * empreinte passe d'abord par ici</b>. On tronque plutôt qu'on n'arrondit — la
 * valeur obtenue n'a plus rien à arrondir, la base la rend donc telle quelle,
 * sans que l'écriture ait à connaître la règle d'arrondi du moteur.
 */
public final class StorableInstant {

    private StorableInstant() {}

    /** L'instant, ramené à la microseconde. {@code null} reste {@code null}. */
    public static Instant micros(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.MICROS);
    }
}
