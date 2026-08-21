package com.openlab.qualitos.quality.risk;

import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.product.domain.Product;
import com.openlab.qualitos.quality.product.domain.ProductLookup;
import com.openlab.qualitos.quality.product.domain.ProductStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Le rattachement d'un FMEA à un produit, et ce qu'il interdit.
 *
 * <p>Mockito est utilisé sans son extension stricte : plusieurs cas montent le
 * service avec un produit qui existe sans jamais le lire, et l'extension stricte
 * transformerait cette mise en scène volontaire en échec.
 */
class FmeaProductLinkTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    @BeforeEach
    void setup() { TenantContext.setTenantId(TENANT.toString()); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void aProductCannotCarryTwoActiveProcessFmeas() {
        FmeaProjectRepository projects = mock(FmeaProjectRepository.class);
        FmeaProject draft = draftForProduct(FmeaType.PROCESS_FMEA);
        when(projects.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(projects.existsByTenantIdAndProductIdAndTypeAndStatus(
                TENANT, PRODUCT, FmeaType.PROCESS_FMEA, FmeaStatus.ACTIVE)).thenReturn(true);

        FmeaService service = newService(projects, productExists());

        assertThatThrownBy(() -> service.activate(draft.getId()))
                .isInstanceOf(FmeaStateException.class)
                .hasMessageContaining("PFMEA");
    }

    @Test
    void aDesignFmeaOnTheSameProductIsNotBlocked() {
        // La contrainte ne vise QUE le PFMEA : un produit porte légitimement en
        // même temps un DFMEA actif et un PFMEA actif.
        FmeaProjectRepository projects = mock(FmeaProjectRepository.class);
        when(projects.existsByTenantIdAndProductIdAndTypeAndStatus(
                TENANT, PRODUCT, FmeaType.DESIGN_FMEA, FmeaStatus.ACTIVE)).thenReturn(false);

        FmeaService service = newService(projects, productExists());

        assertThat(service.canActivate(PRODUCT, FmeaType.DESIGN_FMEA)).isTrue();
    }

    @Test
    void aProjectWithoutAProductIsNeverBlocked() {
        FmeaProjectRepository projects = mock(FmeaProjectRepository.class);

        FmeaService service = newService(projects, productExists());

        assertThat(service.canActivate(null, FmeaType.PROCESS_FMEA)).isTrue();
        verifyNoInteractions(projects);
    }

    @Test
    void activatingAProjectWithoutAProductStillWorks() {
        FmeaProjectRepository projects = mock(FmeaProjectRepository.class);
        FmeaProject draft = draftForProduct(FmeaType.PROCESS_FMEA);
        draft.setProductId(null);
        when(projects.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(projects.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FmeaService service = newService(projects, productExists());

        assertThat(service.activate(draft.getId()).status()).isEqualTo(FmeaStatus.ACTIVE);
    }

    @Test
    void anUnknownProductIsRefusedAtCreation() {
        ProductLookup lookup = mock(ProductLookup.class);
        when(lookup.findById(PRODUCT)).thenReturn(Optional.empty());

        FmeaService service = newService(mock(FmeaProjectRepository.class), lookup);

        assertThatThrownBy(() -> service.attachProduct(UUID.randomUUID(), PRODUCT))
                .isInstanceOf(FmeaStateException.class);
    }

    @Test
    void aKnownProductIsAttachedToTheProject() {
        FmeaProjectRepository projects = mock(FmeaProjectRepository.class);
        FmeaProject draft = draftForProduct(FmeaType.PROCESS_FMEA);
        draft.setProductId(null);
        when(projects.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(projects.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FmeaService service = newService(projects, productExists());

        assertThat(service.attachProduct(draft.getId(), PRODUCT).productId()).isEqualTo(PRODUCT);
    }

    @Test
    void anArchivedProjectCannotBeAttachedToAProduct() {
        FmeaProjectRepository projects = mock(FmeaProjectRepository.class);
        FmeaProject archived = draftForProduct(FmeaType.PROCESS_FMEA);
        archived.setStatus(FmeaStatus.ARCHIVED);
        when(projects.findById(archived.getId())).thenReturn(Optional.of(archived));

        FmeaService service = newService(projects, productExists());

        assertThatThrownBy(() -> service.attachProduct(archived.getId(), PRODUCT))
                .isInstanceOf(FmeaStateException.class);
    }

    @Test
    void aProjectOfAnotherTenantIsNotFoundRatherThanForbidden() {
        FmeaProjectRepository projects = mock(FmeaProjectRepository.class);
        FmeaProject foreign = draftForProduct(FmeaType.PROCESS_FMEA);
        foreign.setTenantId(UUID.randomUUID());
        when(projects.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        FmeaService service = newService(projects, productExists());

        assertThatThrownBy(() -> service.attachProduct(foreign.getId(), PRODUCT))
                .isInstanceOf(FmeaProjectNotFoundException.class);
    }

    @Test
    void creatingAProjectOnAnUnknownProductIsRefused() {
        ProductLookup lookup = mock(ProductLookup.class);
        when(lookup.findById(PRODUCT)).thenReturn(Optional.empty());
        FmeaProjectRepository projects = mock(FmeaProjectRepository.class);
        when(projects.findByTenantIdAndCode(TENANT, "PF-1")).thenReturn(Optional.empty());

        FmeaService service = newService(projects, lookup);

        assertThatThrownBy(() -> service.createProject(new FmeaDto.CreateProjectRequest(
                "PF-1", "Assemblage", null, FmeaType.PROCESS_FMEA, null, null, USER, PRODUCT)))
                .isInstanceOf(FmeaStateException.class);
    }

    @Test
    void theListCanBeNarrowedToOneProduct() {
        FmeaProjectRepository projects = mock(FmeaProjectRepository.class);
        FmeaProject draft = draftForProduct(FmeaType.PROCESS_FMEA);
        when(projects.findByTenantIdAndProductId(eq(TENANT), eq(PRODUCT), any()))
                .thenReturn(new PageImpl<>(List.of(draft)));

        FmeaService service = newService(projects, productExists());

        assertThat(service.listProjects(null, null, PRODUCT, PageRequest.of(0, 20)))
                .singleElement()
                .extracting(FmeaDto.ProjectResponse::productId)
                .isEqualTo(PRODUCT);
    }

    @Test
    void anItemCarriesItsActionPriorityAlongsideItsRpn() {
        FmeaItem item = new FmeaItem();
        item.setSeverity(10);
        item.setOccurrence(4);
        item.setDetection(3);

        item.recomputeRpn();

        assertThat(item.getRpn()).isEqualTo(120);
        assertThat(item.getActionPriority()).isEqualTo(ActionPriority.HIGH);
    }

    // ---------- montage ----------

    private FmeaService newService(FmeaProjectRepository projects, ProductLookup lookup) {
        return new FmeaService(projects, mock(FmeaItemRepository.class), lookup);
    }

    private FmeaProject draftForProduct(FmeaType type) {
        FmeaProject p = new FmeaProject();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT);
        p.setProductId(PRODUCT);
        p.setCode("PF-" + type.name());
        p.setName("Assemblage");
        p.setType(type);
        p.setStatus(FmeaStatus.DRAFT);
        p.setRevision(1);
        p.setCriticalRpnThreshold(100);
        p.setCreatedBy(USER);
        p.setCreatedAt(NOW);
        p.setUpdatedAt(NOW);
        return p;
    }

    private ProductLookup productExists() {
        ProductLookup lookup = mock(ProductLookup.class);
        when(lookup.findById(PRODUCT)).thenReturn(Optional.of(
                Product.rehydrate(PRODUCT, TENANT, "REF-4471", "Support", null, null,
                        ProductStatus.ACTIVE, null, null, null, USER, NOW, NOW)));
        return lookup;
    }
}
