package com.openlab.qualitos.quality.standards.auditblanc.application;

import java.util.UUID;

/**
 * Port : tenant courant (issu du JWT validé, jamais du body — OWASP A01,
 * CLAUDE.md §18.2 #2). Standards Hub §8.4 onglet 7.
 */
public interface MockAuditTenantProvider {

    UUID requireTenantId();
}
