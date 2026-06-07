package com.openlab.qualitos.quality.commconnector;

import com.openlab.qualitos.quality.itsm.ItsmConnectionNotFoundException;

import java.util.UUID;

/**
 * Connexion de communication introuvable (ou hors tenant — on ne leak pas l'existence
 * cross-tenant).
 *
 * <p>Étend {@link ItsmConnectionNotFoundException} (lecture seule, réutilisée) pour
 * hériter du mapping 404 déjà déclaré dans le {@code GlobalExceptionHandler} partagé,
 * que ce module n'a pas le droit d'éditer. On garde un type propre pour pouvoir le
 * catcher spécifiquement si besoin et conserver un message distinct.
 */
public class CommConnectionNotFoundException extends ItsmConnectionNotFoundException {
    public CommConnectionNotFoundException(UUID id) {
        super(id);
    }
}
