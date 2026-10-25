package com.openlab.qualitos.quality.airouting.infrastructure;

import com.openlab.qualitos.quality.airouting.domain.AiProvider;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicy;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.aigateway.AiGatewayException;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiPolicyInfrastructureTest {

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMIN = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void le_catalogue_lit_le_defaut_et_la_configuration_de_chaque_fournisseur() {
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        when(gateway.providers()).thenReturn(Map.<String, Object>of(
                "default_provider", "mistral",
                "providers", List.<Object>of(
                        Map.of("name", "ollama", "configured", true),
                        Map.of("name", "mistral", "configured", false),
                        Map.of("name", "openai", "configured", true),
                        Map.of("name", "anthropic"),
                        "pas-un-objet")));

        var snapshot = new AiGatewayProviderCatalog(gateway).describe().orElseThrow();

        assertThat(snapshot.platformDefault()).contains(AiProvider.MISTRAL);
        assertThat(snapshot.configured()).containsOnly(
                Map.entry(AiProvider.OLLAMA, true), Map.entry(AiProvider.MISTRAL, false));
    }

    @Test
    void le_catalogue_ignore_un_defaut_inconnu_ou_absent() {
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        when(gateway.providers()).thenReturn(Map.<String, Object>of("default_provider", "openai"));
        assertThat(new AiGatewayProviderCatalog(gateway).describe().orElseThrow().platformDefault()).isEmpty();

        when(gateway.providers()).thenReturn(Map.<String, Object>of());
        assertThat(new AiGatewayProviderCatalog(gateway).describe().orElseThrow().configured()).isEmpty();
    }

    @Test
    void le_catalogue_dit_quand_l_ai_service_ne_repond_pas() {
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        when(gateway.providers()).thenThrow(new AiGatewayException("down"));

        assertThat(new AiGatewayProviderCatalog(gateway).describe()).isEmpty();
    }

    @Test
    void le_journal_garde_le_fournisseur_l_ancien_et_l_indication_on_premise() {
        AuditEventService events = mock(AuditEventService.class);
        AuditLogAiPolicyPublisher publisher = new AuditLogAiPolicyPublisher(events);

        publisher.pinned(TENANT, AiProvider.OLLAMA, Optional.of(AiProvider.MISTRAL), ADMIN);
        publisher.pinned(TENANT, AiProvider.MISTRAL, Optional.empty(), ADMIN);
        publisher.released(TENANT, AiProvider.MISTRAL, ADMIN);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> captor =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(events, org.mockito.Mockito.times(3)).recordForTenant(eq(TENANT), captor.capture());
        List<AuditEventDto.RecordEventRequest> recorded = captor.getAllValues();
        assertThat(recorded.get(0).action()).isEqualTo("ai.policy.pinned");
        assertThat(recorded.get(0).payloadJson())
                .isEqualTo("{\"provider\":\"ollama\",\"onPremise\":true,\"previous\":\"mistral\"}");
        assertThat(recorded.get(1).payloadJson()).contains("\"previous\":null");
        assertThat(recorded.get(2).action()).isEqualTo("ai.policy.released");
        assertThat(recorded.get(2).payloadJson()).isEqualTo("{\"previous\":\"mistral\"}");
        assertThat(recorded).allSatisfy(r -> {
            assertThat(r.actorUserId()).isEqualTo(ADMIN);
            assertThat(r.resourceId()).isEqualTo(TENANT);
        });
    }

    @Test
    void l_adaptateur_jpa_enregistre_relit_et_retire() {
        TenantAiPolicyJpaRepository jpa = mock(TenantAiPolicyJpaRepository.class);
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TenantAiPolicyRepositoryAdapter adapter = new TenantAiPolicyRepositoryAdapter(jpa);
        Instant now = Instant.parse("2026-10-25T10:00:00Z");

        TenantAiPolicy saved = adapter.save(new TenantAiPolicy(TENANT, AiProvider.OLLAMA, now, ADMIN));

        assertThat(saved.getProvider()).isEqualTo(AiProvider.OLLAMA);
        assertThat(saved.getUpdatedAt()).isEqualTo(now);
        ArgumentCaptor<TenantAiPolicyJpaEntity> row = ArgumentCaptor.forClass(TenantAiPolicyJpaEntity.class);
        verify(jpa).save(row.capture());
        assertThat(row.getValue().getProvider()).isEqualTo("ollama");
        assertThat(row.getValue().getUpdatedBy()).isEqualTo(ADMIN);

        when(jpa.findById(TENANT)).thenReturn(Optional.of(row.getValue()));
        assertThat(adapter.findByTenantId(TENANT)).map(TenantAiPolicy::getTenantId).contains(TENANT);

        adapter.deleteByTenantId(TENANT);
        verify(jpa).deleteById(TENANT);
    }

    @Test
    void le_contexte_refuse_une_requete_sans_tenant() {
        assertThatThrownBy(() -> new JwtAiRoutingContext().requireTenantId())
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void le_contexte_lit_le_tenant_du_jeton() {
        TenantContext.setTenantId(TENANT.toString());

        assertThat(new JwtAiRoutingContext().requireTenantId()).isEqualTo(TENANT);
    }
}
