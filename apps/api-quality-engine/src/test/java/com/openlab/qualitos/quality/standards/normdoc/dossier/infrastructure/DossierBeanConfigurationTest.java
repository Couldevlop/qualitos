package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.openlab.qualitos.quality.standards.normdoc.application.NormDocActorProvider;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocStandardLookup;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerator;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocRepository;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierEventPublisher;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierIntegrityPort;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierReuseLookup;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierService;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DocumentationDossier;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument.SectionPlan;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierPlanProvider;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class DossierBeanConfigurationTest {

    @Mock DossierRepository dossiers;
    @Mock DossierPlanProvider planProvider;
    @Mock NormDocGenerator generator;
    @Mock NormDocRepository normDocs;
    @Mock NormDocStandardLookup standards;
    @Mock DossierReuseLookup reuse;
    @Mock DossierIntegrityPort integrity;
    @Mock NormDocTenantProvider tenantProvider;
    @Mock NormDocActorProvider actorProvider;
    @Mock DossierEventPublisher events;

    @Test
    void wiresService() {
        DossierService service = new DossierBeanConfiguration().dossierService(
                dossiers, planProvider, generator, normDocs, standards, reuse, integrity,
                tenantProvider, actorProvider, events, Clock.systemUTC());
        assertThat(service).isNotNull();
    }

    @Test
    void noOpEventPublisher_isInert() {
        DossierDocument doc = DossierDocument.planned("m", NormDocKind.MANUAL, "Manuel",
                List.of(new SectionPlan("s", "Section", List.of("4.1"), "")));
        DocumentationDossier d = DocumentationDossier.start(UUID.randomUUID(), UUID.randomUUID(),
                "iso-9001", "ISO 9001", "ACME", "fr", List.of(doc), UUID.randomUUID(),
                Instant.now());
        assertThatCode(() -> new DossierEventPublisher.NoOp()
                .publish(d, DossierEventPublisher.Action.STARTED)).doesNotThrowAnyException();
    }
}
