package com.openlab.qualitos.quality.standards.normdoc.application;

import java.util.UUID;

/**
 * Port : acteur courant (sujet du JWT). Sert à attribuer les transitions
 * (soumission, approbation) à l'identité authentifiée — l'approbateur est le
 * sujet du JWT, jamais un identifiant du body (OWASP A01, §18.2 #5).
 */
public interface NormDocActorProvider {
    UUID requireActorId();
}
