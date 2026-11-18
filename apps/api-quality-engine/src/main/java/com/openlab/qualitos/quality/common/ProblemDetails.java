package com.openlab.qualitos.quality.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;

/**
 * La forme d'un refus, écrite une fois.
 *
 * <p>Les deux cent vingt-cinq gestionnaires d'exception du moteur répétaient les six
 * mêmes lignes — construire le {@link ProblemDetail}, poser le type, le titre,
 * l'horodatage, le rendre — pour ne faire varier que trois valeurs. Cette répétition
 * n'était pas qu'inélégante : elle laissait chaque gestionnaire libre d'oublier
 * l'horodatage ou de forger l'URL autrement, et un client qui lit le champ
 * {@code type} dépend de ce que cette URL soit toujours bâtie pareil.
 *
 * <p>Le préfixe du type vit donc ici, et nulle part ailleurs. Les appelants ne
 * passent que le FRAGMENT qui identifie l'erreur, jamais l'URL entière.
 *
 * <p>Cette classe appartient au socle, et c'est le bon sens de dépendance : les
 * modules la connaissent, elle ne connaît aucun module (ADR 0128).
 */
public final class ProblemDetails {

    /**
     * Préfixe stable des types d'erreur. Il paraît dans le champ {@code type} de
     * chaque réponse d'erreur, et des clients s'en servent pour router leur
     * traitement : le changer est une rupture de contrat, pas un détail de forme.
     */
    private static final String PREFIXE_TYPE = "https://qualitos.io/errors/";

    private ProblemDetails() {
    }

    /**
     * Construit la réponse d'un refus.
     *
     * @param statut le statut HTTP ; c'est lui qui dit la NATURE du refus (404 absent,
     *               409 état incompatible, 422 contenu refusé, 403 droit manquant)
     * @param detail le message lu par un humain. Il ne doit nommer aucune ressource
     *               interne ni aucune donnée d'un autre client.
     * @param type   le fragment identifiant l'erreur, par exemple {@code "nc-not-found"}
     *               — jamais l'URL entière
     * @param titre  le libellé court et stable de la famille d'erreur
     */
    public static ProblemDetail of(HttpStatus statut, String detail, String type, String titre) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statut, detail);
        problem.setType(URI.create(PREFIXE_TYPE + type));
        problem.setTitle(titre);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
