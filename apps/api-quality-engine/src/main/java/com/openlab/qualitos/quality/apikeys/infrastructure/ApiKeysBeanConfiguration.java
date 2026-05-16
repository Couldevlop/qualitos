package com.openlab.qualitos.quality.apikeys.infrastructure;

import com.openlab.qualitos.quality.apikeys.application.ApiKeyEventPublisher;
import com.openlab.qualitos.quality.apikeys.application.ApiKeyService;
import com.openlab.qualitos.quality.apikeys.application.TenantProvider;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyHasher;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyRepository;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeySecretGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApiKeysBeanConfiguration {

    @Bean
    public ApiKeyService apiKeyService(ApiKeyRepository repo,
                                       ApiKeyHasher hasher,
                                       ApiKeySecretGenerator generator,
                                       TenantProvider tenantProvider,
                                       ApiKeyEventPublisher events,
                                       Clock clock) {
        return new ApiKeyService(repo, hasher, generator, tenantProvider, events, clock);
    }
}
