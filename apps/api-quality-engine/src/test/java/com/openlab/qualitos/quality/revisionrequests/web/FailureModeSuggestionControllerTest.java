package com.openlab.qualitos.quality.revisionrequests.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.product.domain.Product;
import com.openlab.qualitos.quality.product.domain.ProductLookup;
import com.openlab.qualitos.quality.product.domain.ProductStatus;
import com.openlab.qualitos.quality.risk.FmeaItem;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import com.openlab.qualitos.quality.risk.FmeaProject;
import com.openlab.qualitos.quality.risk.FmeaProjectRepository;
import com.openlab.qualitos.quality.risk.FmeaStatus;
import com.openlab.qualitos.quality.risk.FmeaType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Les suggestions se lisent depuis le PFMEA EN VIGUEUR du produit. Puiser dans un
 * brouillon proposerait à l'opérateur des modes de défaillance que personne n'a
 * encore validés.
 */
@Tag("web")
@WebMvcTest(controllers = FailureModeSuggestionController.class)
@Import(MethodSecurityTestConfig.class)
class FailureModeSuggestionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProductLookup products;
    @MockitoBean FmeaProjectRepository projects;
    @MockitoBean FmeaItemRepository items;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID PROJECT = UUID.randomUUID();
    static final UUID ITEM = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");
    static final String URL = "/api/v1/products/{p}/failure-mode-suggestions";

    @Test
    @WithMockUser(roles = "USER")
    void theActivePfmeaOfTheProductFeedsTheSuggestions() throws Exception {
        givenProduct();
        when(projects.findByTenantIdAndProductId(TENANT, PRODUCT))
                .thenReturn(List.of(activePfmea()));
        when(items.findByProjectIdOrderBySequenceNoAsc(PROJECT)).thenReturn(List.of(item()));

        mockMvc.perform(get(URL, PRODUCT).param("text", "Bavure sur l'alesage constatee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fmeaItemId").value(ITEM.toString()))
                .andExpect(jsonPath("$[0].matchedTerms").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "USER")
    void aProductWithoutAnActivePfmeaSuggestsNothingRatherThanFailing() throws Exception {
        givenProduct();
        FmeaProject draft = activePfmea();
        draft.setStatus(FmeaStatus.DRAFT);
        when(projects.findByTenantIdAndProductId(TENANT, PRODUCT)).thenReturn(List.of(draft));

        mockMvc.perform(get(URL, PRODUCT).param("text", "Bavure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "USER")
    void aDesignFmeaIsNotUsedForFailureModesSeenInProduction() throws Exception {
        givenProduct();
        FmeaProject design = activePfmea();
        design.setType(FmeaType.DESIGN_FMEA);
        when(projects.findByTenantIdAndProductId(TENANT, PRODUCT)).thenReturn(List.of(design));

        mockMvc.perform(get(URL, PRODUCT).param("text", "Bavure alesage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "USER")
    void anUnknownProductAnswers404() throws Exception {
        when(products.findById(PRODUCT)).thenReturn(Optional.empty());

        mockMvc.perform(get(URL, PRODUCT).param("text", "Bavure"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void anEmptyTextIsRefusedBeforeAnyLookup() throws Exception {
        mockMvc.perform(get(URL, PRODUCT).param("text", "   "))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithAnonymousUser
    void anAnonymousVisitorGetsNoSuggestion() throws Exception {
        mockMvc.perform(get(URL, PRODUCT).param("text", "Bavure"))
                .andExpect(status().is4xxClientError());
    }

    private void givenProduct() {
        when(products.findById(PRODUCT)).thenReturn(Optional.of(
                Product.rehydrate(PRODUCT, TENANT, "REF-4471", "Support", null, null,
                        ProductStatus.ACTIVE, null, null, null, USER, NOW, NOW)));
    }

    private FmeaProject activePfmea() {
        FmeaProject project = new FmeaProject();
        project.setId(PROJECT);
        project.setTenantId(TENANT);
        project.setProductId(PRODUCT);
        project.setCode("PF-4471");
        project.setName("Assemblage");
        project.setType(FmeaType.PROCESS_FMEA);
        project.setStatus(FmeaStatus.ACTIVE);
        project.setCreatedBy(USER);
        project.setCreatedAt(NOW);
        project.setUpdatedAt(NOW);
        return project;
    }

    private FmeaItem item() {
        FmeaItem item = new FmeaItem();
        item.setId(ITEM);
        item.setTenantId(TENANT);
        item.setProjectId(PROJECT);
        item.setFailureMode("Bavure sur alésage");
        item.setFailureEffect("Montage impossible");
        return item;
    }
}
