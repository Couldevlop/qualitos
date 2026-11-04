package com.openlab.qualitos.quality.nonconformity;

/**
 * Une saisie de non-conformité que le domaine refuse : sous-catégorie inconnue,
 * catalogue incohérent.
 *
 * <p>Distincte de {@link NcStateException}, qui dit « cette transition n'est pas
 * possible dans l'état actuel » (409). Ici la demande elle-même est mal formée :
 * 400.
 */
public class NcValidationException extends RuntimeException {

    public NcValidationException(String message) {
        super(message);
    }
}
