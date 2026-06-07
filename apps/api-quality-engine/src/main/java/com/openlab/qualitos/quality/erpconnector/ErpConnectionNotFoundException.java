package com.openlab.qualitos.quality.erpconnector;

import com.openlab.qualitos.quality.itsm.ItsmConnectionNotFoundException;

import java.util.UUID;

/**
 * Connexion ERP introuvable (ou appartenant à un autre tenant — on ne leak pas
 * l'existence cross-tenant).
 *
 * <p>Étend {@link ItsmConnectionNotFoundException} pour réutiliser SON mapping HTTP 404
 * déjà déclaré dans {@code GlobalExceptionHandler} (interdit d'édition) : un
 * {@code @ResponseStatus} serait sinon supplanté par le catch-all
 * {@code @ExceptionHandler(Exception.class)} → 500. C'est la consigne « réutilise
 * l'existant » du brief. Le message est surchargé pour rester explicite côté ERP.
 */
public class ErpConnectionNotFoundException extends ItsmConnectionNotFoundException {

    private final String message;

    public ErpConnectionNotFoundException(UUID id) {
        super(id);
        this.message = "ERP connection not found: " + id;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
