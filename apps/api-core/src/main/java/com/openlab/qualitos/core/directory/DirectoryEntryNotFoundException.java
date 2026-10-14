package com.openlab.qualitos.core.directory;

/**
 * Personne absente de l'annuaire du client du jeton.
 *
 * <p>Même réponse que l'identifiant n'existe nulle part ou qu'il appartienne à
 * un autre client : l'annuaire ne doit pas servir d'oracle d'existence.
 */
public class DirectoryEntryNotFoundException extends RuntimeException {

    public DirectoryEntryNotFoundException() {
        super("Directory entry not found");
    }
}
