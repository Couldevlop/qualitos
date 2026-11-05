package com.openlab.qualitos.quality.aigateway;

/**
 * La passerelle IA a <em>répondu</em>, et elle refuse la requête : texte trop long,
 * invite suspectée d'injection, champ hors bornes.
 *
 * <p>Distincte de {@link AiGatewayException}, qui dit que la passerelle n'a pas
 * répondu. La différence n'est pas cosmétique : un refus n'est pas une panne, donc
 * il ne compte pas dans le disjoncteur du tenant (OWASP LLM04). Les confondre
 * permettait à un utilisateur de fermer tout le chemin IA de son organisation en
 * répétant une requête que la passerelle refuse — par exemple un document plus long
 * que le plafond — puisque cinq refus consécutifs ouvraient le disjoncteur pour
 * trente secondes, réarmables indéfiniment.
 */
public class AiRequestRejectedException extends RuntimeException {

    public AiRequestRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
