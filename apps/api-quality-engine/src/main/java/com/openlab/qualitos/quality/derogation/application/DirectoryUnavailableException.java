package com.openlab.qualitos.quality.derogation.application;

/**
 * L'annuaire des comptes n'a pas répondu.
 *
 * <p>À distinguer d'un compte absent, que {@link DirectoryPort} rend par un
 * {@code Optional} vide : ici on ne sait pas, et répondre « ce manager n'existe
 * pas » serait un mensonge qui ferait corriger une saisie correcte.
 *
 * <p>Déclarée dans {@code application}, aux côtés du port qu'elle accompagne, et
 * non dans l'adaptateur qui la lève : la couche web doit pouvoir lui donner un
 * statut sans dépendre de l'infrastructure (§18.3, la dépendance va vers
 * l'intérieur).
 */
public class DirectoryUnavailableException extends RuntimeException {

    public DirectoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
