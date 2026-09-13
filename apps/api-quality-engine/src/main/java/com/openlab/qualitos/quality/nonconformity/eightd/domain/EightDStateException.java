package com.openlab.qualitos.quality.nonconformity.eightd.domain;

/**
 * Le geste demandé ne convient pas à l'état du rapport ou de la non-conformité :
 * émettre avant la clôture, modifier un rapport scellé, télécharger un document
 * qui n'a pas encore été émis. Rendu en 409.
 */
public class EightDStateException extends RuntimeException {

    public EightDStateException(String message) {
        super(message);
    }
}
