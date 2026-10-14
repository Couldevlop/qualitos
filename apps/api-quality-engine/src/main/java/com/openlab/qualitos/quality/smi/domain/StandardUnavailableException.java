package com.openlab.qualitos.quality.smi.domain;

import java.util.UUID;

/**
 * La norme demandée ne peut pas rejoindre le SMI.
 *
 * <p>Deux raisons, deux réponses : la norme n'est pas une norme de système de
 * management certifiable du catalogue (elle n'a rien à faire dans la matrice
 * des chapitres communs), ou le client l'a déjà adoptée — y compris retirée,
 * auquel cas c'est dans le Standards Hub qu'on la reprend.
 */
public class StandardUnavailableException extends RuntimeException {

    public enum Reason { NOT_A_MANAGEMENT_SYSTEM, ALREADY_ADOPTED }

    private final Reason reason;

    public StandardUnavailableException(UUID standardId, Reason reason) {
        super(reason == Reason.ALREADY_ADOPTED
                ? "Standard " + standardId + " is already adopted by this tenant"
                : "Standard " + standardId + " is not a certifiable management system standard");
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
