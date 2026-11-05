package com.openlab.qualitos.core.tenant;

/**
 * Le logo proposé ne passe pas la politique de téléversement.
 *
 * <p>Elle porte son MOTIF, et pas seulement un message, parce que les trois
 * refus ne sont pas le même échec pour celui qui appelle :
 *
 * <ul>
 *   <li>{@link Reason#TYPE} — le format n'est pas accepté : il faut convertir
 *       le fichier (415) ;</li>
 *   <li>{@link Reason#SIZE} — le fichier est trop lourd : il faut le réduire
 *       (413) ;</li>
 *   <li>{@link Reason#CONTENT} — le fichier n'est pas ce qu'il annonce : il
 *       faut vérifier ce qu'on envoie (400).</li>
 * </ul>
 *
 * <p>Les trois en 400 obligeraient à lire le message pour savoir quoi faire,
 * et un client programmatique ne lit pas les messages.
 */
public class LogoRejectedException extends RuntimeException {

    public enum Reason { TYPE, SIZE, CONTENT }

    private final Reason reason;

    public LogoRejectedException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
