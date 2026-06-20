package com.openlab.qualitos.quality.standards.auditblanc.application;

import java.util.UUID;

/**
 * Port : acteur courant (sujet du JWT, jamais du body — OWASP A01, §18.2 #5).
 * Standards Hub §8.4 onglet 7.
 */
public interface MockAuditActorProvider {

    UUID requireActorId();
}
