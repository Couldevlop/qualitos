package com.openlab.qualitos.quality.privacynotices.infrastructure;

import com.openlab.qualitos.quality.privacynotices.application.PrivacyNoticeEventPublisher;
import com.openlab.qualitos.quality.privacynotices.application.PrivacyNoticeService;
import com.openlab.qualitos.quality.privacynotices.application.TenantProvider;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PrivacyNoticesBeanConfiguration {

    @Bean
    public PrivacyNoticeService privacyNoticeService(
            PrivacyNoticeRepository repo,
            @Qualifier("privacyNoticesTenantContextProvider") TenantProvider tenantProvider,
            PrivacyNoticeEventPublisher events,
            Clock clock) {
        return new PrivacyNoticeService(repo, tenantProvider, events, clock);
    }
}
