package com.openlab.qualitos.quality.aigateway;

import com.openlab.qualitos.quality.ai.guard.AiGuardProperties;
import com.openlab.qualitos.quality.ai.guard.TokenBucketAiGuard;
import com.openlab.qualitos.quality.airouting.InMemoryTenantAiPolicyRepository;
import com.openlab.qualitos.quality.airouting.application.TenantAiPolicyResolver;
import com.openlab.qualitos.quality.airouting.domain.AiProvider;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicy;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicyRepository;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La souveraineté se joue ici : chaque appel LLM part avec le fournisseur auquel le
 * tenant est épinglé, et rien d'autre que les appels LLM n'en porte.
 */
class AiGatewayProviderRoutingTest {

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private HttpServer server;
    private int port;
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private final InMemoryTenantAiPolicyRepository policies = new InMemoryTenantAiPolicyRepository();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String path = exchange.getRequestURI().getPath();
            String reply = path.endsWith("/providers")
                    ? "{\"default_provider\":\"mistral\",\"providers\":[{\"name\":\"ollama\",\"configured\":true}]}"
                    : "{\"text\":\"ok\",\"provider\":\"ollama\",\"questions\":[]}";
            byte[] b = reply.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, b.length);
            exchange.getResponseBody().write(b);
            exchange.close();
        });
        server.start();
        port = server.getAddress().getPort();
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        TenantContext.clear();
    }

    private AiGatewayClient client(TenantAiPolicyResolver resolver) {
        return new AiGatewayClient("http://localhost:" + port, 2000, 5000, "dev-claims",
                new TokenBucketAiGuard(new AiGuardProperties()),
                new AiServiceTokenProvider("", "", "", "", 30, 1000, 1000), resolver);
    }

    private void pin(UUID tenant, AiProvider provider) {
        policies.save(new TenantAiPolicy(tenant, provider, Instant.now(), UUID.randomUUID()));
    }

    @Test
    void un_tenant_epingle_sur_ollama_envoie_tous_ses_appels_llm_a_ollama() {
        pin(TENANT, AiProvider.OLLAMA);
        AiGatewayClient c = client(new TenantAiPolicyResolver(policies));

        c.complete("système", "rapport d'audit", 50);
        c.askNlq("combien de NC en mars ?", 10);
        c.mockAudit(Map.of("standard_code", "iso-9001"), 1);

        assertThat(bodies).hasSize(3).allSatisfy(b -> assertThat(b).contains("\"provider\":\"ollama\""));
    }

    @Test
    void la_politique_du_tenant_prime_sur_un_fournisseur_pose_par_l_appelant() {
        pin(TENANT, AiProvider.OLLAMA);

        client(new TenantAiPolicyResolver(policies))
                .mockAudit(Map.of("standard_code", "iso-9001", "provider", "anthropic"), 1);

        assertThat(bodies.get(0)).contains("\"provider\":\"ollama\"").doesNotContain("anthropic");
    }

    @Test
    void sans_politique_aucun_fournisseur_n_est_impose() {
        pin(OTHER, AiProvider.OLLAMA);
        AiGatewayClient c = client(new TenantAiPolicyResolver(policies));

        c.complete("s", "u", 10);
        c.askNlq("q", 10);

        assertThat(bodies).allSatisfy(b -> assertThat(b).doesNotContain("provider"));
    }

    @Test
    void les_calculs_purs_ne_portent_jamais_de_fournisseur() {
        pin(TENANT, AiProvider.OLLAMA);

        client(new TenantAiPolicyResolver(policies)).detectSpc(List.of(1.0, 2.0, 3.0), null, null);

        assertThat(bodies.get(0)).doesNotContain("provider");
    }

    @Test
    void politique_illisible_l_appel_ne_part_pas() {
        TenantAiPolicyRepository broken = new InMemoryTenantAiPolicyRepository() {
            @Override
            public Optional<TenantAiPolicy> findByTenantId(UUID tenantId) {
                throw new IllegalStateException("base indisponible");
            }
        };
        AiGatewayClient c = client(new TenantAiPolicyResolver(broken));

        assertThatThrownBy(() -> c.complete("s", "u", 10)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> c.askNlq("q", 10)).isInstanceOf(IllegalStateException.class);
        assertThat(bodies).isEmpty();
    }

    @Test
    void la_liste_des_fournisseurs_est_relayee_sans_routage() {
        pin(TENANT, AiProvider.OLLAMA);

        Map<String, Object> r = client(new TenantAiPolicyResolver(policies)).providers();

        assertThat(r).containsEntry("default_provider", "mistral");
    }

    @Test
    void la_liste_des_fournisseurs_exige_un_tenant() {
        TenantContext.clear();

        assertThatThrownBy(() -> client(InMemoryTenantAiPolicyRepository.noPolicy()).providers())
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void ai_service_injoignable_la_liste_echoue_proprement() {
        server.stop(0);

        assertThatThrownBy(() -> client(InMemoryTenantAiPolicyRepository.noPolicy()).providers())
                .isInstanceOf(AiGatewayException.class);
    }
}
