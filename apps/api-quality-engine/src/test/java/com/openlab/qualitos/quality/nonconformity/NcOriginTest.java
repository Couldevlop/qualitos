package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Origine d'une non-conformité : interne ou externe.
 *
 * <p>Une NC détectée par l'organisation elle-même (autocontrôle, audit interne)
 * et une NC signalée du dehors (client, fournisseur, autorité, organisme
 * certificateur) ne se traitent pas de la même façon : ni les délais, ni
 * l'obligation de réponse, ni les destinataires. Les distinguer permet de leur
 * consacrer deux entrées de navigation — et surtout de compter séparément ce qui
 * remonte du terrain et ce qui remonte de l'extérieur.
 *
 * <p>Choix assumé du défaut : {@code INTERNAL}. C'est le cas majoritaire, et
 * classer d'office en « externe » une NC dont personne n'a précisé l'origine
 * gonflerait à tort l'indicateur qui compte le plus vis-à-vis des clients.
 */
@ExtendWith(MockitoExtension.class)
class NcOriginTest {

    @Mock NonConformityRepository repo;
    @Mock CapaCaseRepository capaRepo;
    @Mock org.springframework.context.ApplicationEventPublisher events;
    @InjectMocks NcService service;

    private static final UUID TENANT = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    private NcDto.CreateRequest request(NcOrigin origin) {
        return new NcDto.CreateRequest(
                "Joint défectueux", "détail", NcCategory.PRODUCT, NcSeverity.MAJOR,
                Instant.now(), "Atelier 3", null, null, null, null, origin, null, null);
    }

    @Test
    @DisplayName("une origine non précisée vaut « interne »")
    void defaultsToInternal() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        NcDto.Response created = service.create(request(null));

        assertThat(created.origin()).isEqualTo(NcOrigin.INTERNAL);
    }

    @Test
    @DisplayName("l'origine demandée est conservée")
    void keepsTheRequestedOrigin() {
        when(repo.countByTenantIdAndReferenceStartingWith(eq(TENANT), anyString())).thenReturn(0L);
        when(repo.existsByTenantIdAndReference(eq(TENANT), anyString())).thenReturn(false);
        when(repo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<NonConformity> saved = ArgumentCaptor.forClass(NonConformity.class);
        service.create(request(NcOrigin.EXTERNAL));

        verify(repo).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getOrigin()).isEqualTo(NcOrigin.EXTERNAL);
    }

    @Test
    @DisplayName("la liste se filtre sur l'origine")
    void filtersByOrigin() {
        Pageable page = PageRequest.of(0, 10);
        NonConformity externe = new NonConformity();
        externe.setId(UUID.randomUUID());
        externe.setTenantId(TENANT);
        externe.setReference("NC-2026-0001");
        externe.setTitle("Réclamation client");
        externe.setCategory(NcCategory.PRODUCT);
        externe.setSeverity(NcSeverity.MAJOR);
        externe.setStatus(NcStatus.OPEN);
        externe.setOrigin(NcOrigin.EXTERNAL);
        externe.setDetectedAt(Instant.now());
        when(repo.findAll(any(Specification.class), eq(page)))
                .thenReturn(new PageImpl<>(List.of(externe)));

        var result = service.findAll(null, null, null, NcOrigin.EXTERNAL, null, page);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).origin()).isEqualTo(NcOrigin.EXTERNAL);
    }

    @Test
    @DisplayName("sans filtre d'origine, les deux origines remontent")
    void withoutOriginFilterBothAreListed() {
        Pageable page = PageRequest.of(0, 10);
        when(repo.findAll(any(Specification.class), eq(page)))
                .thenReturn(new PageImpl<>(List.of()));

        service.findAll(null, null, null, null, null, page);

        // Le filtre est bâti dans tous les cas : c'est la spécification qui décide
        // d'ignorer un critère nul, et non une cascade de branches par combinaison.
        verify(repo).findAll(any(Specification.class), eq(page));
    }
}
