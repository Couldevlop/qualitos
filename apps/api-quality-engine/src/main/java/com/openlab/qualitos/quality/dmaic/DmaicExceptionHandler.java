package com.openlab.qualitos.quality.dmaic;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus du module dmaic, dits par le module lui-meme.
 *
 * <p>Ces gestionnaires vivaient dans {@code common/GlobalExceptionHandler}, qui
 * devait importer l exception de chaque module pour savoir quel statut lui
 * repondre. Le socle dependait ainsi de soixante-six modules, et tout module
 * neuf devait MODIFIER un fichier partage pour que ses refus sortent avec le bon
 * statut (ADR 0128).
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} : Spring consulte les conseils dans
 * l ordre et retient le PREMIER qui possede un gestionnaire applicable. Le socle
 * garde un filet {@code @ExceptionHandler(Exception.class)} qui accepte tout ;
 * sans cette priorite, il repondrait 500 a la place de ce module.
 *
 * <p>La portee reste GLOBALE, sans {@code assignableTypes} : une exception de ce
 * module peut etre levee pendant une requete adressee a un autre module -- un
 * ecart qui ouvre un dossier CAPA, par exemple -- et elle doit garder son statut.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class DmaicExceptionHandler {

    @ExceptionHandler(DmaicProjectNotFoundException.class)
    public ProblemDetail handleDmaicProjectNotFound(DmaicProjectNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "dmaic-project-not-found", "DMAIC Project Not Found");
    }

    @ExceptionHandler(ProcessMeasureNotFoundException.class)
    public ProblemDetail handleProcessMeasureNotFound(ProcessMeasureNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "process-measure-not-found", "Process Measure Not Found");
    }

    @ExceptionHandler(PokaYokeDeviceNotFoundException.class)
    public ProblemDetail handlePokaYokeDeviceNotFound(PokaYokeDeviceNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "pokayoke-device-not-found", "Poka-Yoke Device Not Found");
    }

    @ExceptionHandler(PokaYokeAssignmentNotFoundException.class)
    public ProblemDetail handlePokaYokeAssignmentNotFound(PokaYokeAssignmentNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "pokayoke-assignment-not-found", "Poka-Yoke Assignment Not Found");
    }


    /**
     * Un refus de passage de phase ou de clôture : l'utilisateur doit savoir ce
     * qui manque.
     *
     * <p>{@code userMessage} et {@code reasons} sont le canal <b>déclaré</b> des
     * messages destinés à l'utilisateur. L'écran n'affiche jamais le {@code
     * detail} d'un ProblemDetail quelconque — il pourrait porter un nom de
     * classe, une contrainte JPA ou une trace. Il affiche ces deux propriétés,
     * et seulement elles, parce que leur contenu est rédigé pour être lu.
     *
     * <p>Déclaré avant {@link #handleDmaicState} : Spring choisit le gestionnaire
     * du type le plus spécifique, mais l'ordre de lecture doit montrer que
     * celui-ci prime, sans quoi la sous-classe paraîtrait sans effet.
     */
    @ExceptionHandler(DmaicGateException.class)
    public ProblemDetail handleDmaicGate(DmaicGateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/dmaic-gate"));
        problem.setTitle("DMAIC Gate Not Passed");
        problem.setProperty("userMessage", ex.getMessage());
        problem.setProperty("reasons", ex.reasons());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DmaicStateException.class)
    public ProblemDetail handleDmaicState(DmaicStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "dmaic-invalid-state", "Invalid DMAIC State");
    }
}
