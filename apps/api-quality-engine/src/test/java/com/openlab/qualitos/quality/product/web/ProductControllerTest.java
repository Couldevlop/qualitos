package com.openlab.qualitos.quality.product.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.product.application.ProductDto;
import com.openlab.qualitos.quality.product.application.ProductService;
import com.openlab.qualitos.quality.product.domain.ProductCodeConflictException;
import com.openlab.qualitos.quality.product.domain.ProductNotFoundException;
import com.openlab.qualitos.quality.product.domain.ProductStateException;
import com.openlab.qualitos.quality.product.domain.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Le tenant vient du JWT, jamais du corps ({@link #aTenantIdSlippedIntoTheBodyIsIgnored()}),
 * un produit d'un autre tenant répond 404 (jamais 403), et l'écriture est réservée
 * aux rôles qui pilotent le système qualité (OWASP A01).
 */
@Tag("web")
@WebMvcTest(controllers = ProductController.class)
@Import(MethodSecurityTestConfig.class)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProductService service;
    ObjectMapper om;

    static final UUID ID = UUID.randomUUID();
    static final UUID COMPONENT_ID = UUID.randomUUID();
    static final UUID OPERATION_ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");
    static final ProductDto.View VIEW = new ProductDto.View(
            ID, "REF-4471", "Support moteur", null, null,
            ProductStatus.DRAFT, null, null, null, NOW, NOW);
    static final ProductDto.ComponentView COMPONENT_VIEW =
            new ProductDto.ComponentView(COMPONENT_ID, 10, "CMP-1", "Vis", BigDecimal.TEN, "PCE", null);
    static final ProductDto.OperationView OPERATION_VIEW =
            new ProductDto.OperationView(OPERATION_ID, 10, "OP-10", "Perçage", "Poste 1");

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ---------- cas exigés par le plan (verbatim) ----------

    @Test
    @WithMockUser(roles = "USER")
    void aSimpleUserCannotCreateAProduct() throws Exception {
        mockMvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"REF-4471\",\"designation\":\"Support moteur\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void aQualityManagerCanCreateAProduct() throws Exception {
        when(service.create(any())).thenReturn(VIEW);

        mockMvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"REF-4471\",\"designation\":\"Support moteur\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("REF-4471"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void aTenantIdSlippedIntoTheBodyIsIgnored() throws Exception {
        when(service.create(any())).thenReturn(VIEW);

        mockMvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"REF-4471\",\"designation\":\"Support\","
                               + "\"tenantId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated());
        // La commande n'a pas de champ tenantId : Jackson l'ignore, le service prend
        // le tenant du contexte. Le test verrouille l'absence du champ.
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void anInvalidCodeIsRefusedBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"réf 4471/A\",\"designation\":\"Support\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    // ---------- lecture : ouverte à tout utilisateur authentifié ----------

    @Test @WithMockUser(roles = "USER")
    void list_200() throws Exception {
        when(service.list()).thenReturn(List.of(VIEW));
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "USER")
    void get_200() throws Exception {
        when(service.get(ID)).thenReturn(VIEW);
        mockMvc.perform(get("/api/v1/products/{id}", ID)).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "USER")
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new ProductNotFoundException(ID));
        mockMvc.perform(get("/api/v1/products/{id}", ID)).andExpect(status().isNotFound());
    }

    @Test @WithAnonymousUser
    void list_anonymous_401or403() throws Exception {
        mockMvc.perform(get("/api/v1/products")).andExpect(status().is4xxClientError());
    }

    // ---------- écriture : produit ----------

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void create_codeConflict_409() throws Exception {
        when(service.create(any())).thenThrow(new ProductCodeConflictException("REF-4471"));
        mockMvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"REF-4471\",\"designation\":\"Support\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void update_200() throws Exception {
        when(service.update(eq(ID), any())).thenReturn(VIEW);
        mockMvc.perform(put("/api/v1/products/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"designation\":\"Nouveau nom\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "USER")
    void update_forbiddenForASimpleUser() throws Exception {
        mockMvc.perform(put("/api/v1/products/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"designation\":\"Nouveau nom\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void update_invalidState_409() throws Exception {
        when(service.update(eq(ID), any()))
                .thenThrow(new ProductStateException("An obsolete product cannot be modified"));
        mockMvc.perform(put("/api/v1/products/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"designation\":\"Nouveau nom\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser(roles = "USER")
    void delete_forbiddenForASimpleUser() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void activate_200() throws Exception {
        when(service.activate(ID)).thenReturn(VIEW);
        mockMvc.perform(post("/api/v1/products/{id}/activate", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "USER")
    void activate_forbiddenForASimpleUser() throws Exception {
        mockMvc.perform(post("/api/v1/products/{id}/activate", ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void obsolete_200() throws Exception {
        when(service.markObsolete(ID)).thenReturn(VIEW);
        mockMvc.perform(post("/api/v1/products/{id}/obsolete", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "USER")
    void obsolete_forbiddenForASimpleUser() throws Exception {
        mockMvc.perform(post("/api/v1/products/{id}/obsolete", ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ---------- bornes de longueur (§ describe : famille/indice/donneur d'ordre/site) ----------

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void create_familyTooLong_400() throws Exception {
        String body = om.writeValueAsString(new ProductWebDto.CreateRequest(
                "REF-1", "Support", "F".repeat(121), null, null, null, null));
        mockMvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void create_revisionIndexTooLong_400() throws Exception {
        String body = om.writeValueAsString(new ProductWebDto.CreateRequest(
                "REF-1", "Support", null, "R".repeat(17), null, null, null));
        mockMvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void create_customerLabelTooLong_400() throws Exception {
        String body = om.writeValueAsString(new ProductWebDto.CreateRequest(
                "REF-1", "Support", null, null, "C".repeat(251), null, null));
        mockMvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void create_siteLabelTooLong_400() throws Exception {
        String body = om.writeValueAsString(new ProductWebDto.CreateRequest(
                "REF-1", "Support", null, null, null, "S".repeat(251), null));
        mockMvc.perform(post("/api/v1/products").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void update_designationBlank_400() throws Exception {
        mockMvc.perform(put("/api/v1/products/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"designation\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    // ---------- composants : la chaîne d'appartenance passe par le produit ----------

    @Test @WithMockUser(roles = "USER")
    void components_200() throws Exception {
        when(service.components(ID)).thenReturn(List.of(COMPONENT_VIEW));
        mockMvc.perform(get("/api/v1/products/{id}/components", ID)).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void addComponent_201() throws Exception {
        when(service.addComponent(eq(ID), any())).thenReturn(COMPONENT_VIEW);
        mockMvc.perform(post("/api/v1/products/{id}/components", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNo\":10,\"reference\":\"CMP-1\",\"label\":\"Vis\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("CMP-1"));
    }

    @Test @WithMockUser(roles = "USER")
    void addComponent_forbiddenForASimpleUser() throws Exception {
        mockMvc.perform(post("/api/v1/products/{id}/components", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNo\":10,\"reference\":\"CMP-1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void addComponent_negativeSequenceNo_400() throws Exception {
        mockMvc.perform(post("/api/v1/products/{id}/components", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNo\":-1,\"reference\":\"CMP-1\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void addComponent_labelTooLong_400() throws Exception {
        String body = om.writeValueAsString(new ProductWebDto.ComponentRequest(
                10, "CMP-1", "L".repeat(251), null, null, null));
        mockMvc.perform(post("/api/v1/products/{id}/components", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void addComponent_unitTooLong_400() throws Exception {
        String body = om.writeValueAsString(new ProductWebDto.ComponentRequest(
                10, "CMP-1", "Vis", null, "U".repeat(25), null));
        mockMvc.perform(post("/api/v1/products/{id}/components", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void updateComponent_200() throws Exception {
        when(service.updateComponent(eq(ID), eq(COMPONENT_ID), any())).thenReturn(COMPONENT_VIEW);
        mockMvc.perform(put("/api/v1/products/{id}/components/{componentId}", ID, COMPONENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNo\":10,\"reference\":\"CMP-1\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void updateComponent_notFound_404() throws Exception {
        when(service.updateComponent(eq(ID), eq(COMPONENT_ID), any()))
                .thenThrow(new ProductNotFoundException(COMPONENT_ID));
        mockMvc.perform(put("/api/v1/products/{id}/components/{componentId}", ID, COMPONENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNo\":10,\"reference\":\"CMP-1\"}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void deleteComponent_204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}/components/{componentId}", ID, COMPONENT_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser(roles = "USER")
    void deleteComponent_forbiddenForASimpleUser() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}/components/{componentId}", ID, COMPONENT_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ---------- opérations : même chaîne d'appartenance ----------

    @Test @WithMockUser(roles = "USER")
    void operations_200() throws Exception {
        when(service.operations(ID)).thenReturn(List.of(OPERATION_VIEW));
        mockMvc.perform(get("/api/v1/products/{id}/operations", ID)).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void addOperation_201() throws Exception {
        when(service.addOperation(eq(ID), any())).thenReturn(OPERATION_VIEW);
        mockMvc.perform(post("/api/v1/products/{id}/operations", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNo\":10,\"code\":\"OP-10\",\"label\":\"Perçage\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OP-10"));
    }

    @Test @WithMockUser(roles = "USER")
    void addOperation_forbiddenForASimpleUser() throws Exception {
        mockMvc.perform(post("/api/v1/products/{id}/operations", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNo\":10,\"code\":\"OP-10\",\"label\":\"Perçage\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void addOperation_negativeSequenceNo_400() throws Exception {
        mockMvc.perform(post("/api/v1/products/{id}/operations", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNo\":-1,\"code\":\"OP-10\",\"label\":\"Perçage\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void updateOperation_200() throws Exception {
        when(service.updateOperation(eq(ID), eq(OPERATION_ID), any())).thenReturn(OPERATION_VIEW);
        mockMvc.perform(put("/api/v1/products/{id}/operations/{operationId}", ID, OPERATION_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNo\":10,\"code\":\"OP-10\",\"label\":\"Perçage\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void updateOperation_notFound_404() throws Exception {
        when(service.updateOperation(eq(ID), eq(OPERATION_ID), any()))
                .thenThrow(new ProductNotFoundException(OPERATION_ID));
        mockMvc.perform(put("/api/v1/products/{id}/operations/{operationId}", ID, OPERATION_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sequenceNo\":10,\"code\":\"OP-10\",\"label\":\"Perçage\"}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void deleteOperation_204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}/operations/{operationId}", ID, OPERATION_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser(roles = "USER")
    void deleteOperation_forbiddenForASimpleUser() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}/operations/{operationId}", ID, OPERATION_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
