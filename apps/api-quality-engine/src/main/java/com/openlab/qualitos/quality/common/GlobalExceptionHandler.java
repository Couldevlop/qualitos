package com.openlab.qualitos.quality.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import java.util.Set;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Les refus de la PLATEFORME, et eux seuls.
 *
 * <p>Ce fichier portait 225 gestionnaires et importait 205 classes d exception : le
 * socle dependait de soixante-six modules metier, alors que soixante-dix-huit modules
 * dependent de lui. Chaque module dit maintenant ses propres refus, dans son propre
 * conseil.
 *
 * <p>Ce qui reste ici ne vient d aucun module : le client absent du jeton, le second
 * facteur exige, le module desactive, la licence en lecture seule, les refus que Spring
 * et Jakarta levent avant tout code metier, le stockage objet injoignable -- et le filet
 * de securite.
 *
 * <p>{@code @Order(LOWEST_PRECEDENCE)} : le filet {@code Exception} accepte tout. Ce
 * conseil doit donc etre consulte EN DERNIER, apres les conseils de module, sans quoi il
 * repondrait 500 a la place de chacun d eux.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {




    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MissingTenantContextException.class)
    public ProblemDetail handleMissingTenant(MissingTenantContextException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/missing-tenant"));
        problem.setTitle("Missing Tenant Context");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    /** 409 : l'objet a changé entre la lecture et l'écriture (deux décisions au même instant). */
    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(org.springframework.dao.OptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Cet élément vient d'être modifié par quelqu'un d'autre : rechargez puis recommencez.");
        problem.setType(URI.create("https://qualitos.io/errors/concurrent-update"));
        problem.setTitle("Concurrent Update");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUpload(MaxUploadSizeExceededException ex) {
        // Limite multipart Spring dépassée (1er rempart, avant lecture des octets).
        // Le libellé est NEUTRE : ce rempart est commun à tous les téléversements.
        // Il annonçait « Non-Conformity Photo Too Large » sur le dépôt d'une preuve
        // CAPA — constaté en préproduction — ce qui envoie chercher le défaut dans
        // le mauvais module.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the maximum allowed size");
        problem.setType(URI.create("https://qualitos.io/errors/upload-too-large"));
        problem.setTitle("Uploaded File Too Large");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    /**
     * Le stockage objet est configuré mais ne répond pas comme attendu : bucket
     * absent, identifiants refusés, service injoignable.
     *
     * <p>Sans ce gestionnaire, ces pannes tombaient dans l'attrape-tout et
     * l'API annonçait « erreur interne » — un diagnostic qui désigne
     * l'application alors que la coupure est ailleurs. Constaté en
     * préproduction : dépôt de photo de NC et de preuve CAPA en 500 identiques,
     * pendant que le stockage était bien joignable mais son bucket jamais créé.
     *
     * <p>503, comme un stockage désactivé : pour l'utilisateur, c'est la même
     * chose — les pièces jointes sont indisponibles — et l'écran sait déjà le
     * dire. Le détail de l'erreur reste au journal, pas dans la réponse : il
     * nomme des ressources internes.
     */
    @ExceptionHandler(SdkException.class)
    public ProblemDetail handleObjectStorageFailure(SdkException ex) {
        log.error("Stockage objet en échec : {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Object storage is unavailable");
        problem.setType(URI.create("https://qualitos.io/errors/storage-unavailable"));
        problem.setTitle("Binary Storage Unavailable");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        // Validation des @RequestParam / @PathVariable via @Min/@Max/@Pattern :
        // Spring lève ConstraintViolationException qui n'a pas de handler natif.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/validation-failed"));
        problem.setTitle("Constraint Violation");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex) {
        // Malformed JSON or unparseable request body (e.g. enum value not accepted).
        // Renvoyer 400 Bad Request, conformément RFC 7807 §3.1.
        Throwable root = ex.getMostSpecificCause();
        String detail = root != null ? root.getMessage() : "Malformed request body";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://qualitos.io/errors/malformed-request"));
        problem.setTitle("Malformed Request Body");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // Binding @RequestParam/@PathVariable error (e.g. enum value not accepted).
        String name = ex.getName();
        String value = ex.getValue() == null ? "null" : ex.getValue().toString();
        String detail = "Invalid value '" + value + "' for parameter '" + name + "'";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://qualitos.io/errors/parameter-type-mismatch"));
        problem.setTitle("Parameter Type Mismatch");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingPart(MissingServletRequestPartException ex) {
        // Part multipart requise absente (ex. champ 'file' manquant) → 400 (et non 500).
        String detail = "Missing required multipart part '" + ex.getRequestPartName() + "'";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://qualitos.io/errors/missing-multipart-part"));
        problem.setTitle("Missing Required Multipart Part");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex) {
        // Paramètre @RequestParam requis absent → 400 Bad Request (et non 500).
        String detail = "Missing required parameter '" + ex.getParameterName() + "'";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://qualitos.io/errors/missing-parameter"));
        problem.setTitle("Missing Required Parameter");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    /**
     * Mauvaise méthode HTTP sur une route qui existe → 405, et non 500.
     *
     * <p>Constaté en peuplant une instance de démonstration : un POST sur
     * {@code /audits/plans/{id}/start}, qui est un PATCH, répondait « Internal
     * Server Error ». Le serveur n'avait rien de cassé — il refusait, et disait le
     * contraire. Un client qui lit 500 réessaie ou alerte ; sur 405 il corrige sa
     * requête, et l'en-tête {@code Allow} lui dit comment.
     *
     * <p>Même famille que le paramètre requis absent, traité juste au-dessus : une
     * exception de Spring MVC laissée sans gestionnaire tombe dans le fourre-tout
     * des 500.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        Set<HttpMethod> permises = ex.getSupportedHttpMethods();
        String detail = "Method " + ex.getMethod() + " is not supported on this endpoint"
                + (permises == null || permises.isEmpty() ? ""
                   : " (allowed: " + permises.stream().map(HttpMethod::name)
                           .collect(Collectors.joining(", ")) + ")");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED, detail);
        problem.setType(URI.create("https://qualitos.io/errors/method-not-allowed"));
        problem.setTitle("Method Not Allowed");
        problem.setProperty("timestamp", Instant.now());

        // L'en-tête `Allow` est exigé par la RFC sur un 405, et c'est lui qui dit au
        // client quoi faire au lieu de le laisser deviner.
        HttpHeaders entetes = new HttpHeaders();
        if (permises != null && !permises.isEmpty()) {
            entetes.setAllow(permises);
        }
        return new ResponseEntity<>(problem, entetes, HttpStatus.METHOD_NOT_ALLOWED);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setType(URI.create("https://qualitos.io/errors/validation-failed"));
        problem.setTitle("Validation Failed");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    /**
     * 403 et non 401 : la session est valide, c'est la force de l'authentification
     * qui ne l'est pas. Un 401 déclencherait une reconnexion silencieuse qui
     * reproduirait le même jeton, sans jamais demander le second facteur.
     */
    @ExceptionHandler(StepUpRequiredException.class)
    public ProblemDetail handleStepUpRequired(StepUpRequiredException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/step-up-required"));
        problem.setTitle("Second Factor Required");
        problem.setProperty("action", ex.getAction());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        // BUG #3 — Une violation de contrainte d'intégrité (unicité, FK, NOT NULL) qui
        // n'a pas été traduite en exception métier dédiée est un CONFLIT de données, pas
        // une erreur serveur : 409 (et non 500). On NE divulgue PAS le message SQL brut
        // (nom de contrainte, colonnes, valeurs) — OWASP A09. Le détail technique reste
        // dans les logs serveur.
        log.warn("Data integrity violation mapped to 409", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The request conflicts with the current state of the resource "
                        + "(duplicate or constraint violation)");
        problem.setType(URI.create("https://qualitos.io/errors/data-conflict"));
        problem.setTitle("Data Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    @ExceptionHandler(org.springframework.data.core.PropertyReferenceException.class)
    public ProblemDetail handlePropertyReference(
            org.springframework.data.core.PropertyReferenceException ex) {
        // BUG #24 — Un ?sort=champInconnu (ou tout tri sur une propriété absente) fait
        // remonter PropertyReferenceException depuis Spring Data jusqu'au handler
        // générique → 500 sur TOUS les endpoints paginés. C'est une erreur du CLIENT
        // (paramètre de tri invalide) : 400. On expose le nom de propriété fautif
        // (déjà fourni par le client, pas une info interne sensible) pour aider au debug.
        log.debug("Invalid sort/property reference mapped to 400: {}", ex.getPropertyName());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Unknown sort or filter property: '" + ex.getPropertyName() + "'");
        problem.setType(URI.create("https://qualitos.io/errors/invalid-sort-property"));
        problem.setTitle("Invalid Sort Property");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex) {
        // Chemin inexistant → 404 (et non 500). Évite de masquer une 404 en
        // erreur serveur et ne divulgue aucune information interne (OWASP A05/A09).
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "Resource not found");
        problem.setType(URI.create("https://qualitos.io/errors/resource-not-found"));
        problem.setTitle("Resource Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    @ExceptionHandler({
            org.springframework.security.access.AccessDeniedException.class,
            org.springframework.security.authorization.AuthorizationDeniedException.class})
    public ProblemDetail handleAccessDenied(RuntimeException ex) {
        // H1/C1 — Un refus d'autorisation (@PreAuthorize ou règle URL propagée jusqu'au
        // handler) doit être un 403, pas un 500. Sans ce handler, le catch-all
        // Exception → 500 masquerait le refus (OWASP A01/A09). Aucun détail interne exposé.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Access denied");
        problem.setType(URI.create("https://qualitos.io/errors/access-denied"));
        problem.setTitle("Access Denied");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }


    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(URI.create("https://qualitos.io/errors/internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
