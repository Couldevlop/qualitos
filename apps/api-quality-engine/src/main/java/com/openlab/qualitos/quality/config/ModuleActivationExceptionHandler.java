package com.openlab.qualitos.quality.config;

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
 * Les refus du module config, dits par le module lui-meme.
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
public class ModuleActivationExceptionHandler {


    /**
     * Module non souscrit (§10.4) — 403, mais d'un TYPE distinct du refus de droits.
     *
     * <p>Les deux se règlent à des endroits opposés : un manque de droits se voit
     * avec l'administrateur du tenant, un module non souscrit avec l'éditeur. Un
     * type unique enverrait l'utilisateur frapper à la mauvaise porte — le même
     * travers que 401 rendu comme 403.
     *
     * <p>Le code du module figure dans la réponse : c'est le nom d'une ligne du
     * catalogue public, pas une information interne, et le taire obligerait à
     * deviner lequel des vingt modules manque.
     */
    @ExceptionHandler(com.openlab.qualitos.quality.config.ModuleNotEnabledException.class)
    public ProblemDetail handleModuleNotEnabled(
            com.openlab.qualitos.quality.config.ModuleNotEnabledException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Module not enabled for this tenant: " + ex.getModuleCode());
        problem.setType(URI.create("https://qualitos.io/errors/module-not-enabled"));
        problem.setTitle("Module Not Enabled");
        problem.setProperty("moduleCode", ex.getModuleCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
