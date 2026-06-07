package com.openlab.qualitos.quality.ehrconnector;

import com.openlab.qualitos.quality.itsm.ItsmConnectionNotFoundException;

import java.util.UUID;

/**
 * Connexion EHR introuvable (ou hors tenant — on ne distingue pas, pour ne pas
 * leaker l'existence cross-tenant).
 *
 * <p>Étend {@link ItsmConnectionNotFoundException} (même famille « connecteur ») afin
 * de réutiliser le handler 404 déjà déclaré dans {@code GlobalExceptionHandler}
 * (qui possède un {@code @ExceptionHandler(Exception.class)} catch-all : un simple
 * {@code @ResponseStatus} serait masqué). Le GlobalExceptionHandler n'est donc PAS
 * modifié pour ce module.</p>
 */
public class EhrConnectionNotFoundException extends ItsmConnectionNotFoundException {
    public EhrConnectionNotFoundException(UUID id) {
        super(id);
    }
}
