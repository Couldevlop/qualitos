package com.openlab.qualitos.quality.standards;

/**
 * Document GED inéligible comme source d'un référentiel (422).
 *
 * <p>422 et non 400 : la requête est bien formée et le document existe — c'est son
 * ÉTAT qui interdit l'opération, et il peut changer (une procédure approuvée plus
 * tard deviendra une source valide).
 */
public class ProcedureSourceException extends RuntimeException {
    public ProcedureSourceException(String message) { super(message); }
}
