package com.openlab.qualitos.quality.authz.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.common.CurrentUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/** Câblage des droits par client : un service sans Spring, des adaptateurs. */
@Configuration
public class AuthzBeanConfiguration {

    @Bean
    public AuthorizationService authorizationService(AuthzRoleRepository roles,
                                                     AuthzMemberRoleRepository memberRoles,
                                                     AuditEventService auditEvents,
                                                     Clock clock) {
        return new AuthorizationService(
                new AuthzAdapters.Roles(roles, memberRoles, clock),
                new AuthzAdapters.Members(memberRoles, clock),
                new AuthzAdapters.JwtContext(),
                new AuthzAdapters.Audit(auditEvents),
                clock);
    }

    /**
     * La portée des registres : l'action « voir tout » du module, sinon ce qui
     * concerne l'utilisateur du jeton. Sans utilisateur identifié (compte de
     * service, clé d'API) et sans « voir tout », la portée ne désigne personne :
     * rien n'est rendu plutôt que tout.
     */
    @Bean
    public RecordScope recordScope(AuthorizationService authorization) {
        return viewAll -> authorization.has(viewAll)
                ? Optional.empty()
                : Optional.of(CurrentUser.userId().orElse(NOBODY));
    }

    /** L'identifiant que personne ne porte. */
    static final UUID NOBODY = new UUID(0L, 0L);
}
