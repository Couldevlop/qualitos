package com.openlab.qualitos.quality.ehrconnector;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Échec d'un appel sortant vers le serveur FHIR (réseau, HTTP non-2xx, parsing).
 *
 * <p>Mappée en 502 Bad Gateway via {@link ResponseStatus} si elle remonte jusqu'au
 * contrôleur. En pratique, {@link EhrConnectorService#sync} l'attrape et la
 * convertit en {@link EhrDto.SyncReport} avec {@code errorMessage} renseigné, sans
 * jamais exposer le secret ni de PII.</p>
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class EhrSyncException extends RuntimeException {
    public EhrSyncException(String msg) { super(msg); }
    public EhrSyncException(String msg, Throwable cause) { super(msg, cause); }
}
