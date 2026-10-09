package com.openlab.qualitos.core.identity;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfiguration {

    @Bean
    public IdentityProvider identityProvider(IdentityProperties props, RestClient.Builder builder, Clock clock) {
        return new KeycloakIdentityProvider(props, builder, clock);
    }
}
