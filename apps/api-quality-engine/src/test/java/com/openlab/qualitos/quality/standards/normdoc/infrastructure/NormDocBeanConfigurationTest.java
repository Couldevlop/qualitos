package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.standards.normdoc.application.NormDocActorProvider;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocEventPublisher;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocService;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocStandardLookup;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerator;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NormDocBeanConfigurationTest {

    @Mock NormDocRepository repo;
    @Mock NormDocGenerator generator;
    @Mock NormDocStandardLookup standards;
    @Mock NormDocTenantProvider tenantProvider;
    @Mock NormDocActorProvider actorProvider;
    @Mock NormDocEventPublisher events;

    @Test
    void wiresService() {
        NormDocService service = new NormDocBeanConfiguration().normDocService(
                repo, generator, standards, tenantProvider, actorProvider, events,
                Clock.systemUTC());
        assertThat(service).isNotNull();
    }
}
