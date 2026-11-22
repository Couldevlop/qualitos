package com.openlab.qualitos.quality.derogation.web;

import com.openlab.qualitos.quality.common.ProblemDetails;
import com.openlab.qualitos.quality.derogation.application.DerogationMailException;
import com.openlab.qualitos.quality.derogation.domain.DerogationConflictException;
import com.openlab.qualitos.quality.derogation.domain.DerogationForbiddenException;
import com.openlab.qualitos.quality.derogation.domain.DerogationNotFoundException;
import com.openlab.qualitos.quality.derogation.domain.DerogationStateException;
import com.openlab.qualitos.quality.derogation.domain.DerogationValidationException;
import com.openlab.qualitos.quality.derogation.infrastructure.CoreDirectoryAdapter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module de dérogation, dits par le module lui-même.
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} : le socle garde un filet
 * {@code @ExceptionHandler(Exception.class)} qui accepte tout ; sans cette
 * priorité, il répondrait 500 à la place de ce module.
 *
 * <p>Les statuts sont choisis, pas improvisés :
 * <ul>
 *   <li><b>404</b> pour l'introuvable, y compris un dossier hors de portée —
 *       répondre 403 révélerait son existence, et donc qu'un client donné a
 *       demandé une dérogation (ADR 0081) ;</li>
 *   <li><b>409</b> pour un état qui ne permet pas le geste, et pour deux gestes
 *       qui se croisent : dans les deux cas l'écran doit recharger, non
 *       corriger sa saisie ;</li>
 *   <li><b>422</b> pour une fiche incomplète : la liste des champs manquants
 *       voyage dans le détail, parce que c'est elle qui permet de corriger sans
 *       deviner ;</li>
 *   <li><b>502</b> quand le courriel n'est pas parti : la faute n'est pas à
 *       l'appelant, et le dossier reste renvoyable.</li>
 * </ul>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class DerogationExceptionHandler {

    @ExceptionHandler(DerogationNotFoundException.class)
    public ProblemDetail introuvable(DerogationNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "derogation-not-found", "Derogation Not Found");
    }

    @ExceptionHandler(DerogationStateException.class)
    public ProblemDetail etat(DerogationStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "derogation-state", "Derogation State Conflict");
    }

    @ExceptionHandler(DerogationConflictException.class)
    public ProblemDetail concurrence(DerogationConflictException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "derogation-conflict", "Derogation Changed Concurrently");
    }

    /**
     * La fiche incomplète, avec la LISTE des champs qui manquent.
     *
     * <p>Le message porte déjà la phrase de la maquette ; le détail porte la
     * liste, pour que l'écran puisse marquer chaque champ plutôt que d'afficher
     * une phrase à relire.
     */
    @ExceptionHandler(DerogationValidationException.class)
    public ProblemDetail validation(DerogationValidationException ex) {
        ProblemDetail detail = ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(), "derogation-invalid", "Derogation Incomplete");
        if (!ex.getMissingFields().isEmpty()) {
            detail.setProperty("missingFields", ex.getMissingFields());
        }
        return detail;
    }

    @ExceptionHandler(DerogationForbiddenException.class)
    public ProblemDetail interdit(DerogationForbiddenException ex) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, ex.getMessage(),
                "derogation-forbidden", "Derogation Gesture Not Yours");
    }

    @ExceptionHandler(DerogationMailException.class)
    public ProblemDetail courriel(DerogationMailException ex) {
        return ProblemDetails.of(HttpStatus.BAD_GATEWAY, ex.getMessage(),
                "derogation-mail-failed", "Derogation Mail Not Sent");
    }

    @ExceptionHandler(CoreDirectoryAdapter.DirectoryUnavailableException.class)
    public ProblemDetail annuaire(CoreDirectoryAdapter.DirectoryUnavailableException ex) {
        return ProblemDetails.of(HttpStatus.BAD_GATEWAY, ex.getMessage(),
                "derogation-directory-unavailable", "Directory Unavailable");
    }
}
