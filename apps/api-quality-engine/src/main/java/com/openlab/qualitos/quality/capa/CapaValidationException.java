package com.openlab.qualitos.quality.capa;

/**
 * Entrée refusée sur un dossier ou une action CAPA (400).
 *
 * <p>Distincte de {@link CapaStateException} (409) : celle-ci dit « la valeur
 * envoyée n'est pas admissible », l'autre dit « le dossier n'est pas dans un
 * état qui permet ce geste ». Les confondre reviendrait à demander à
 * l'utilisateur de changer d'état alors qu'il doit corriger sa saisie.
 *
 * <p>Elle existe parce que la mise à jour d'une action est un PATCH : le record
 * n'y est pas validé par Jakarta (un champ absent doit rester intouché), donc
 * les bornes du libellé se tiennent ici.
 */
public class CapaValidationException extends RuntimeException {

    public CapaValidationException(String message) {
        super(message);
    }
}
