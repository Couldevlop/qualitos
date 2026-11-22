package com.openlab.qualitos.quality.derogation.domain;

import java.util.UUID;

/** Aucun dossier de dérogation sous cet identifiant, pour ce client. */
public class DerogationNotFoundException extends RuntimeException {

    public DerogationNotFoundException(UUID id) {
        super("Dérogation introuvable : " + id);
    }
}
