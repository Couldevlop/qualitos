package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditActorProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Acteur courant depuis le {@code sub} du JWT ({@link CurrentUser}). Garantit
 * que l'auteur de l'audit blanc est l'identité authentifiée (OWASP A01, §18.2 #5).
 */
@Component("mockAuditCurrentUserActorProvider")
public class MockAuditCurrentUserActorProvider implements MockAuditActorProvider {

    @Override
    public UUID requireActorId() {
        return CurrentUser.requireUserId();
    }
}
