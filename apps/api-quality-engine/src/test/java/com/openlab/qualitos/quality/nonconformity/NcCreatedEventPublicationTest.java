package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.common.TenantContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * La création d'une non-conformité annonce le fait ; le moteur de propositions de
 * révision l'écoute après commit.
 *
 * <p>Le point vérifié ici n'est pas cosmétique : l'événement doit porter le tenant
 * de l'ENTITÉ, pas celui du contexte d'exécution. Le consommateur tourne plus tard,
 * hors de la requête, et lire le contexte ambiant à ce moment-là donnerait le
 * mauvais tenant — ou aucun.
 */
class NcCreatedEventPublicationTest {

    static final UUID CONTEXT_TENANT = UUID.randomUUID();
    static final UUID ENTITY_TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID FMEA_ITEM = UUID.randomUUID();
    static final UUID REPORTER = UUID.randomUUID();
    static final Instant DETECTED = Instant.parse("2026-08-19T06:00:00Z");

    NonConformityRepository repo;
    ApplicationEventPublisher events;
    NcService service;

    @BeforeEach
    void setUp() {
        repo = mock(NonConformityRepository.class);
        events = mock(ApplicationEventPublisher.class);
        service = new NcService(repo, mock(CapaCaseRepository.class), events);
        TenantContext.setTenantId(CONTEXT_TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void creatingANonConformityAnnouncesItExactlyOnce() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(CONTEXT_TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(CONTEXT_TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> {
            NonConformity nc = inv.getArgument(0);
            nc.setId(UUID.randomUUID());
            return nc;
        });

        service.create(createRequest());

        verify(events, times(1)).publishEvent(any(NcCreatedEvent.class));
    }

    @Test
    void theEventCarriesTheTenantOfTheEntityNotTheOneOfTheAmbientContext() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(CONTEXT_TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(CONTEXT_TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> {
            NonConformity nc = inv.getArgument(0);
            nc.setId(UUID.randomUUID());
            // On fait volontairement diverger le tenant persisté de celui du
            // contexte : si l'émetteur lisait le contexte, le test le verrait.
            nc.setTenantId(ENTITY_TENANT);
            return nc;
        });

        service.create(createRequest());

        ArgumentCaptor<NcCreatedEvent> captor = ArgumentCaptor.forClass(NcCreatedEvent.class);
        verify(events).publishEvent(captor.capture());
        NcCreatedEvent event = captor.getValue();
        assertThat(event.tenantId()).isEqualTo(ENTITY_TENANT);
        assertThat(event.productId()).isEqualTo(PRODUCT);
        assertThat(event.fmeaItemId()).isEqualTo(FMEA_ITEM);
        assertThat(event.title()).isEqualTo("Bavure sur alésage");
        assertThat(event.detectedAt()).isEqualTo(DETECTED);
    }

    @Test
    void aNonConformityWithoutAProductIsAnnouncedAllTheSame() {
        // Le filtre appartient au consommateur : un émetteur qui trierait pour lui
        // empêcherait d'y brancher autre chose plus tard.
        when(repo.countByTenantIdAndReferenceStartingWith(eq(CONTEXT_TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(CONTEXT_TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> {
            NonConformity nc = inv.getArgument(0);
            nc.setId(UUID.randomUUID());
            return nc;
        });

        service.create(new NcDto.CreateRequest("Retard de livraison", null, NcCategory.PROCESS,
                NcSeverity.MINOR, DETECTED, null, null, null, null, REPORTER, null, null, null));

        ArgumentCaptor<NcCreatedEvent> captor = ArgumentCaptor.forClass(NcCreatedEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().productId()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void theProductFilterIsAppliedInTheQueryAlongsideTheTenant() {
        // Le filtre doit vivre dans la requête. Écrémer une page déjà découpée
        // rendrait des pages trouées et un total faux.
        when(repo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(Page.empty());

        service.findAll(null, null, null, null, PRODUCT, PageRequest.of(0, 10));

        ArgumentCaptor<Specification<NonConformity>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(repo).findAll(captor.capture(), any(PageRequest.class));

        Root<NonConformity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> tenantPath = mock(Path.class);
        Path<Object> productPath = mock(Path.class);
        when(root.get("tenantId")).thenReturn(tenantPath);
        when(root.get("productId")).thenReturn(productPath);
        when(cb.equal(any(), any(Object.class))).thenReturn(mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        captor.getValue().toPredicate(root, query, cb);

        verify(cb).equal(tenantPath, CONTEXT_TENANT);
        verify(cb).equal(productPath, PRODUCT);
    }

    private NcDto.CreateRequest createRequest() {
        return new NcDto.CreateRequest("Bavure sur alésage", "constatée au poste 20",
                NcCategory.PRODUCT, NcSeverity.MAJOR, DETECTED, "Atelier 3", null, null, null,
                REPORTER, null, PRODUCT, FMEA_ITEM);
    }
}
