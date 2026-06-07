package com.openlab.qualitos.quality.erpconnector;

import com.openlab.qualitos.quality.itsm.ItsmSyncException;

/**
 * Erreur côté provider ERP pendant un fetch (HTTP non-2xx, auth, parsing, réseau).
 *
 * <p>Le {@link ErpConnectorService} l'attrape pendant {@code sync()} et la convertit en
 * SyncReport d'erreur (la sync ne propage donc PAS cette exception au client).
 *
 * <p>Étend {@link ItsmSyncException} pour réutiliser SON mapping HTTP (502) déjà déclaré
 * dans {@code GlobalExceptionHandler} (interdit d'édition), au cas où elle remonterait
 * directement via une couche web. Cohérent avec « provider en aval indisponible ».
 */
public class ErpSyncException extends ItsmSyncException {
    public ErpSyncException(String msg) { super(msg); }
    public ErpSyncException(String msg, Throwable cause) { super(msg, cause); }
}
