package com.openlab.qualitos.quality.visiongateway;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module visiongateway, dits par le module lui-meme.
 *
 * <p>Ces gestionnaires vivaient dans {@code common/GlobalExceptionHandler}, qui
 * devait importer l exception de chaque module pour savoir quel statut lui
 * repondre. Le socle dependait ainsi de soixante-six modules, et tout module
 * neuf devait MODIFIER un fichier partage pour que ses refus sortent avec le bon
 * statut.
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
public class VisionExceptionHandler {


    // --- Passerelle Vision 5S (ai-vision-5s, §3.2) ---

    
    @ExceptionHandler(VisionUnavailableException.class)
    public ProblemDetail handleVisionUnavailable(VisionUnavailableException ex) {
        return ProblemDetails.of(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(),
                "vision-unavailable", "Vision Service Unavailable");
    }

    @ExceptionHandler(VisionGatewayException.class)
    public ProblemDetail handleVisionGateway(VisionGatewayException ex) {
        return ProblemDetails.of(HttpStatus.BAD_GATEWAY, ex.getMessage(),
                "vision-gateway-error", "Vision Gateway Error");
    }

    @ExceptionHandler(VisionImageValidationException.class)
    public ProblemDetail handleVisionImageValidation(VisionImageValidationException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "vision-image-invalid", "Invalid Vision Image");
    }

    @ExceptionHandler(VisionImageTooLargeException.class)
    public ProblemDetail handleVisionImageTooLarge(VisionImageTooLargeException ex) {
        return ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(),
                "vision-image-too-large", "Vision Image Too Large");
    }
}
