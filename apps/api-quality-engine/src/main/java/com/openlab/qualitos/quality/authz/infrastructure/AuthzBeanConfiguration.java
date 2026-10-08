package com.openlab.qualitos.quality.authz.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

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
}
