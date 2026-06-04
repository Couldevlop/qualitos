package com.openlab.qualitos.quality.notifications.infrastructure;

import com.openlab.qualitos.quality.notifications.application.NotificationEventPublisher;
import com.openlab.qualitos.quality.notifications.application.NotificationService;
import com.openlab.qualitos.quality.notifications.application.TenantProvider;
import com.openlab.qualitos.quality.notifications.application.UserProvider;
import com.openlab.qualitos.quality.notifications.domain.NotificationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class NotificationsBeanConfiguration {

    @Bean
    public NotificationService notificationService(
            NotificationRepository repo,
            @Qualifier("notificationsTenantContextProvider") TenantProvider tenantProvider,
            UserProvider userProvider,
            NotificationEventPublisher events,
            Clock clock) {
        return new NotificationService(repo, tenantProvider, userProvider, events, clock);
    }
}
