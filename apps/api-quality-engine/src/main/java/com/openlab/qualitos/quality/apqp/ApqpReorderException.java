package com.openlab.qualitos.quality.apqp;

/**
 * L'ordre soumis ne décrit pas le cycle.
 *
 * <p>Une réorganisation porte sur le cycle ENTIER : une liste partielle
 * laisserait des phases sans rang, donc hors du V. Plutôt que de deviner où
 * ranger les absentes, on refuse.
 */
public class ApqpReorderException extends RuntimeException {
    public ApqpReorderException() {
        super("The reorder request must list every phase of the cycle exactly once");
    }
}
