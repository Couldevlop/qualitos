package com.openlab.qualitos.quality.commconnector;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Échec d'envoi vers un fournisseur de communication (HTTP non-2xx, réseau, parsing).
 *
 * <p>Mappé en {@code 502 Bad Gateway} : c'est une défaillance d'un service tiers en
 * aval, pas une erreur de la requête cliente. {@code @ResponseStatus} est utilisé car
 * le GlobalExceptionHandler partagé ne doit pas être édité ici.
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class CommSendException extends RuntimeException {
    public CommSendException(String msg) { super(msg); }
    public CommSendException(String msg, Throwable cause) { super(msg, cause); }
}
