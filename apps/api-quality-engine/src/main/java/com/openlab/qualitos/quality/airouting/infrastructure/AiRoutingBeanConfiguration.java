package com.openlab.qualitos.quality.airouting.infrastructure;

import com.openlab.qualitos.quality.airouting.application.AiPolicyAdminService;
import com.openlab.qualitos.quality.airouting.application.TenantAiPolicyResolver;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicyRepository;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage du routage IA par tenant (ADR 0102). Les classes d'application restent
 * des POJO, testables sans Spring.
 *
 * <p>Le {@code Clock} vient de {@code CommonBeansConfiguration}, unique et
 * {@code @Primary}.
 */
@Configuration
public class AiRoutingBeanConfiguration {

    @Bean
    public TenantAiPolicyRepository tenantAiPolicyRepository(TenantAiPolicyJpaRepository jpa) {
        return new TenantAiPolicyRepositoryAdapter(jpa);
    }

    @Bean
    public TenantAiPolicyResolver tenantAiPolicyResolver(TenantAiPolicyRepository repository) {
        return new TenantAiPolicyResolver(repository);
    }

    @Bean
    public AiPolicyAdminService aiPolicyAdminService(TenantAiPolicyRepository repository,
                                                     AiGatewayClient gateway,
                                                     AuditEventService auditEvents,
                                                     Clock clock) {
        return new AiPolicyAdminService(repository, new AiGatewayProviderCatalog(gateway),
                new JwtAiRoutingContext(), new AuditLogAiPolicyPublisher(auditEvents), clock);
    }
}
