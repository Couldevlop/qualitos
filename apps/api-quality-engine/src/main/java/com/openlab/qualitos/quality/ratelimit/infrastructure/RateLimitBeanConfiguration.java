package com.openlab.qualitos.quality.ratelimit.infrastructure;

import com.openlab.qualitos.quality.ratelimit.application.RateLimitService;
import com.openlab.qualitos.quality.ratelimit.application.TenantProvider;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitCounter;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicyRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RateLimitBeanConfiguration {

    @Bean
    public RateLimitService rateLimitService(RateLimitPolicyRepository policies,
                                             RateLimitCounter counter,
                                             TenantProvider tenantProvider,
                                             Clock clock) {
        return new RateLimitService(policies, counter, tenantProvider, clock);
    }
}
