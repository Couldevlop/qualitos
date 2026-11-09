package com.openlab.qualitos.core.common;

import com.openlab.qualitos.core.billing.ModuleActivationFailedException;
import com.openlab.qualitos.core.billing.SubscriptionNotFoundException;
import com.openlab.qualitos.core.billing.invoice.InvoiceNotFoundException;
import com.openlab.qualitos.core.tenant.TenantAlreadyExistsException;
import com.openlab.qualitos.core.tenant.LogoRejectedException;
import com.openlab.qualitos.core.tenant.TenantNotFoundException;
import com.openlab.qualitos.core.user.UserAlreadyExistsException;
import com.openlab.qualitos.core.user.UserNotFoundException;
import com.openlab.qualitos.core.identity.InvalidRoleException;
import com.openlab.qualitos.core.identity.IdentityProviderException;
import com.openlab.qualitos.core.identity.AccountAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TenantNotFoundException.class)
    public ProblemDetail handleTenantNotFound(TenantNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/tenant-not-found"));
        problem.setTitle("Tenant Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(TenantAlreadyExistsException.class)
    public ProblemDetail handleTenantAlreadyExists(TenantAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/tenant-conflict"));
        problem.setTitle("Tenant Already Exists");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ProblemDetail handleAccountExists(AccountAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/account-conflict"));
        problem.setTitle("Account Already Exists");
        problem.setProperty("field", "email");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ProblemDetail handleInvalidRole(InvalidRoleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/invalid-role"));
        problem.setTitle("Invalid Role");
        problem.setProperty("field", "roles");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * 502 : le fournisseur d'identité a refusé ou ne répond pas. Le détail
     * technique va au journal ; la réponse ne dit que ce que l'appelant peut
     * faire de l'erreur.
     */
    @ExceptionHandler(IdentityProviderException.class)
    public ProblemDetail handleIdentityProvider(IdentityProviderException ex) {
        log.error("identity.provider.failed message={}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/identity-provider"));
        problem.setTitle("Identity Provider Unavailable");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/user-not-found"));
        problem.setTitle("User Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /** 404 identique que la personne n'existe pas ou soit d'un autre client (ADR 0091). */
    @ExceptionHandler(com.openlab.qualitos.core.directory.DirectoryEntryNotFoundException.class)
    public ProblemDetail handleDirectoryEntryNotFound(
            com.openlab.qualitos.core.directory.DirectoryEntryNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/directory-entry-not-found"));
        problem.setTitle("Directory Entry Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /** 404 : la console éditeur n'existe pas dans une installation on-premise (ADR 0082). */
    @ExceptionHandler(com.openlab.qualitos.core.edition.EditionExceptions.NotInThisEdition.class)
    public ProblemDetail handleNotInThisEdition(com.openlab.qualitos.core.edition.EditionExceptions.NotInThisEdition ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/not-in-this-edition"));
        problem.setTitle("Not In This Edition");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /** 403 : l'installation on-premise est en lecture seule faute de licence valable. */
    @ExceptionHandler(com.openlab.qualitos.core.edition.EditionExceptions.LicenseReadOnly.class)
    public ProblemDetail handleLicenseReadOnly(com.openlab.qualitos.core.edition.EditionExceptions.LicenseReadOnly ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/license-read-only"));
        problem.setTitle("License Read Only");
        problem.setProperty("licenseStatus", ex.getStatus().name());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /** 409 : le plafond d'utilisateurs actifs de la licence est atteint. */
    @ExceptionHandler(com.openlab.qualitos.core.edition.EditionExceptions.MemberLimitReached.class)
    public ProblemDetail handleMemberLimit(com.openlab.qualitos.core.edition.EditionExceptions.MemberLimitReached ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/license-member-limit"));
        problem.setTitle("License Member Limit");
        problem.setProperty("limit", ex.getLimit());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/user-conflict"));
        problem.setTitle("User Already Exists");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // ---------- domaine partenaire ----------
    //
    // Les refus du domaine partenaire portent leur raison en clair : « votre
    // cabinet est suspendu » et « votre mission s'est terminee le 12 » appellent
    // deux gestes differents, et un 403 nu les confond.

    @ExceptionHandler({
            com.openlab.qualitos.core.partner.PartnerExceptions.FirmNotFound.class,
            com.openlab.qualitos.core.partner.PartnerExceptions.ConsultantNotFound.class,
            com.openlab.qualitos.core.partner.PartnerExceptions.EngagementNotFound.class
    })
    public ProblemDetail handlePartnerNotFound(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/partner-not-found"));
        problem.setTitle("Partner Resource Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({
            com.openlab.qualitos.core.partner.PartnerExceptions.FirmAlreadyExists.class,
            com.openlab.qualitos.core.partner.PartnerExceptions.ConsultantAlreadyExists.class,
            com.openlab.qualitos.core.partner.PartnerExceptions.EngagementAlreadyLive.class,
            com.openlab.qualitos.core.partner.PartnerExceptions.EngagementNotDecidable.class
    })
    public ProblemDetail handlePartnerConflict(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/partner-conflict"));
        problem.setTitle("Partner Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * L'acces d'un cabinet est refuse : 403, avec la raison.
     *
     * <p>Pas 404 : l'appelant est authentifie et sait deja que ce client
     * existe — le lui cacher ne protegerait rien et l'empecherait de comprendre
     * qu'il doit demander un accord.
     */
    @ExceptionHandler(com.openlab.qualitos.core.partner.PartnerExceptions.AccessNotGranted.class)
    public ProblemDetail handlePartnerAccess(
            com.openlab.qualitos.core.partner.PartnerExceptions.AccessNotGranted ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/partner-access-denied"));
        problem.setTitle("Partner Access Not Granted");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setType(URI.create("https://qualitos.io/errors/validation-failed"));
        problem.setTitle("Validation Failed");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({
            org.springframework.security.access.AccessDeniedException.class,
            org.springframework.security.authorization.AuthorizationDeniedException.class})
    public ProblemDetail handleAccessDenied(RuntimeException ex) {
        // Un refus d'autorisation par @PreAuthorize (sécurité de méthode, comme sur
        // BillingProfileController) n'est PAS intercepté par la chaîne de filtres
        // Spring Security : l'exception surgit à l'intérieur du DispatcherServlet,
        // après que le filtre a déjà laissé passer la requête (l'utilisateur est
        // authentifié, seul son rôle est refusé). Sans ce handler explicite, le
        // catch-all Exception -> 500 ci-dessous la capturait en premier et masquait
        // le refus derrière une erreur serveur générique — un 403 devenait un 500,
        // ce qu'aucun test de rôle bien écrit ne devait laisser passer silencieusement.
        // Même correctif que le moteur de qualité (GlobalExceptionHandler, H1/C1).
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Access denied");
        problem.setType(URI.create("https://qualitos.io/errors/access-denied"));
        problem.setTitle("Access Denied");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MissingTenantContextException.class)
    public ProblemDetail handleMissingTenant(MissingTenantContextException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/missing-tenant"));
        problem.setTitle("Missing Tenant Context");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UnresolvableActorException.class)
    public ProblemDetail handleUnresolvableActor(UnresolvableActorException ex) {
        // 401, pas 500 : un sub absent ou non-UUID (jeton de compte de service,
        // principal non-JWT, claim personnalise) n'est pas une panne serveur —
        // c'est l'identite du principal qui n'est pas exploitable. Sans ce
        // handler, l'IllegalArgumentException de UUID.fromString() tombait dans
        // le catch-all Exception -> 500 ci-dessous, et une action d'administration
        // refusee pour cause d'identite illisible ressemblait a une panne.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/unresolvable-actor"));
        problem.setTitle("Unresolvable Actor");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ProblemDetail handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/subscription-not-found"));
        problem.setTitle("Subscription Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ProblemDetail handleInvoiceNotFound(InvoiceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/invoice-not-found"));
        problem.setTitle("Invoice Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(com.openlab.qualitos.core.platformaudit.PlatformAuditStateException.class)
    public ProblemDetail handlePlatformAuditState(
            com.openlab.qualitos.core.platformaudit.PlatformAuditStateException ex) {
        // 400 et non 500 : une fenêtre de vérification mal bornée est une
        // demande incorrecte, pas une panne de la piste d'audit. Le cas du
        // compteur absent (migration non jouée) passe aussi par ici et mérite
        // une trace — c'est le seul état où la plateforme refuse les actes
        // sensibles faute de pouvoir les consigner.
        log.warn("platform audit trail refused a request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/platform-audit-state"));
        problem.setTitle("Platform Audit Trail");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ModuleActivationFailedException.class)
    public ProblemDetail handleModuleActivationFailed(ModuleActivationFailedException ex) {
        // 502 et non 500 : la panne n'est pas ici. api-core a fait son travail —
        // c'est le moteur de qualité, en aval, qui n'a pas appliqué la décision.
        // La distinction compte pour l'exploitation : un 500 enverrait chercher
        // le défaut dans le mauvais service, et un 503 laisserait croire que la
        // facturation elle-même est indisponible, ce qu'elle n'est pas.
        //
        // L'abonnement N'A PAS été enregistré (voir SubscriptionService) : rien
        // à défaire côté appelant, l'appel peut être rejoué tel quel.
        log.warn("quality engine refused a billing decision: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/module-activation-failed"));
        problem.setTitle("Module Activation Failed");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            ServletRequestBindingException.class})
    public ProblemDetail handleBadRequestBinding(Exception ex) {
        // 400 : un parametre absent ou illisible est une erreur de l'APPELANT.
        //
        // Sans ce handler, une periode de facturation ecrite « septembre » au
        // lieu de « 2026-09 » remontait en 500 « erreur inattendue » : le client
        // de l'API cherchait une panne de serveur pour une faute de frappe, et
        // le message qui disait quoi corriger etait perdu dans le catch-all.
        // Meme famille de defaut que le 403 devenu 500, corrige plus haut : ces
        // exceptions surgissent DANS le DispatcherServlet et n'ont pas de
        // traitement par defaut une fois qu'un @RestControllerAdvice attrape
        // Exception.
        //
        // Le detail est volontairement generique : le message d'origine porte
        // le nom du parametre et la valeur recue, et rendre la valeur recue
        // telle quelle serait un reflet d'entree utilisateur dans la reponse.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request parameter missing or malformed");
        problem.setType(URI.create("https://qualitos.io/errors/bad-request-parameter"));
        problem.setTitle("Bad Request Parameter");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        // 409 : l'état du système s'oppose à l'action (module déjà souscrit,
        // abonnement déjà résilié, module sans tarif au catalogue). La requête
        // est bien formée — la refuser en 400 laisserait croire à une erreur de
        // saisie — et le serveur va bien — la refuser en 500 enverrait chercher
        // une panne inexistante. Rejouer l'appel à l'identique ne changera rien
        // tant que l'état n'aura pas changé, et c'est exactement ce que dit 409.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/conflicting-state"));
        problem.setTitle("Conflicting State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(LogoRejectedException.class)
    public ProblemDetail handleLogoRejected(LogoRejectedException ex) {
        // Trois refus, trois statuts, parce qu'ils demandent trois gestes
        // differents : convertir le fichier, le reduire, ou verifier ce qu'on
        // envoie. Les trois en 400 obligeraient a lire le message pour savoir
        // lequel -- et un client programmatique ne lit pas les messages.
        HttpStatus status = switch (ex.getReason()) {
            case TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case SIZE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case CONTENT -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/logo-rejected"));
        problem.setTitle("Logo Rejected");
        problem.setProperty("reason", ex.getReason().name());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        // Le conteneur a coupe avant que le controleur ne voie le fichier :
        // sans ce handler, un televersement trop gros ressemblait a une panne
        // (500) alors que c'est un refus que l'appelant peut corriger.
        log.warn("upload.too-large limite={}", ex.getMaxUploadSize());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE, "fichier trop volumineux");
        problem.setType(URI.create("https://qualitos.io/errors/logo-rejected"));
        problem.setTitle("Payload Too Large");
        problem.setProperty("reason", "SIZE");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        // 400 : l'argument est refusé par une règle que les annotations de
        // validation ne peuvent pas exprimer, parce qu'elle porte sur DEUX
        // champs à la fois — « une exemption de facturation doit indiquer un
        // motif » (BillingProfileService). Sans ce handler, ce refus tombait
        // dans le catch-all ci-dessous : le client recevait un 500 « erreur
        // inattendue » pour une saisie qu'il pouvait corriger lui-même, et le
        // message expliquant quoi corriger était perdu en route.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/invalid-argument"));
        problem.setTitle("Invalid Argument");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        // Spring MVC lève ses propres exceptions pour des demandes incorrectes
        // — méthode HTTP non supportée (405), type de contenu refusé (415 ou
        // 406), corps illisible (400) — et toutes implémentent ErrorResponse,
        // en portant DÉJÀ le statut qui leur convient. Sans ce test, elles
        // tombaient ici et sortaient en 500 « erreur inattendue » : un
        // intégrateur qui appelait un GET en POST partait chercher une panne de
        // serveur pour une faute d'appel. Le défaut touchait TOUTE surface
        // d'api-core ; il a été vu en écrivant le banc qui vérifie que la piste
        // d'audit n'expose aucune écriture.
        if (ex instanceof org.springframework.web.ErrorResponse cadre) {
            log.warn("rejected request: {}", ex.getMessage());
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.valueOf(cadre.getStatusCode().value()), ex.getMessage());
            problem.setProperty("timestamp", Instant.now());
            return problem;
        }
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(URI.create("https://qualitos.io/errors/internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
