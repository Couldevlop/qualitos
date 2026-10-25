package com.openlab.qualitos.quality.airouting.application;

import com.openlab.qualitos.quality.airouting.InMemoryTenantAiPolicyRepository;
import com.openlab.qualitos.quality.airouting.domain.AiProvider;
import com.openlab.qualitos.quality.airouting.domain.AiProviderNotConfiguredException;
import com.openlab.qualitos.quality.airouting.domain.InvalidAiProviderException;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'administrateur choisit où partent les prompts de SON tenant ; l'écran dit ce qui
 * répond vraiment ; chaque changement laisse une trace.
 */
class AiPolicyAdminServiceTest {

    private static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ADMIN = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");

    private final InMemoryTenantAiPolicyRepository repository = new InMemoryTenantAiPolicyRepository();
    private final List<String> journal = new ArrayList<>();
    private Optional<AiProviderCatalog.Snapshot> catalog;
    private UUID currentTenant;
    private AiPolicyAdminService service;

    @BeforeEach
    void setUp() {
        currentTenant = TENANT;
        catalog = Optional.of(new AiProviderCatalog.Snapshot(Optional.of(AiProvider.MISTRAL), Map.of(
                AiProvider.OLLAMA, true, AiProvider.MISTRAL, true, AiProvider.ANTHROPIC, false)));
        AiRoutingContext context = new AiRoutingContext() {
            @Override public UUID requireTenantId() { return currentTenant; }
            @Override public UUID requireActorId() { return ADMIN; }
        };
        AiPolicyAuditPublisher audit = new AiPolicyAuditPublisher() {
            @Override
            public void pinned(UUID tenantId, AiProvider provider, Optional<AiProvider> previous, UUID actor) {
                journal.add("pinned " + tenantId + " " + provider.wireName() + " <- "
                        + previous.map(AiProvider::wireName).orElse("défaut") + " by " + actor);
            }

            @Override
            public void released(UUID tenantId, AiProvider previous, UUID actor) {
                journal.add("released " + tenantId + " " + previous.wireName() + " by " + actor);
            }
        };
        service = new AiPolicyAdminService(repository, () -> catalog, context, audit,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void sans_politique_le_tenant_suit_le_defaut_de_la_plateforme() {
        AiPolicyDto.View v = service.view();

        assertThat(v.pinned()).isFalse();
        assertThat(v.provider()).isNull();
        assertThat(v.effectiveProvider()).isEqualTo("mistral");
        assertThat(v.platformDefault()).isEqualTo("mistral");
        assertThat(v.catalogAvailable()).isTrue();
        assertThat(v.updatedAt()).isNull();
        assertThat(v.updatedBy()).isNull();
        assertThat(v.providers()).containsExactly(
                new AiPolicyDto.ProviderOption("ollama", true, true),
                new AiPolicyDto.ProviderOption("anthropic", false, false),
                new AiPolicyDto.ProviderOption("mistral", false, true));
    }

    @Test
    void epingler_sur_ollama_garde_les_prompts_chez_le_client_et_le_journalise() {
        AiPolicyDto.View v = service.pin(new AiPolicyDto.PinCommand("ollama"));

        assertThat(v.pinned()).isTrue();
        assertThat(v.provider()).isEqualTo("ollama");
        assertThat(v.effectiveProvider()).isEqualTo("ollama");
        assertThat(v.updatedAt()).isEqualTo(NOW);
        assertThat(v.updatedBy()).isEqualTo(ADMIN);
        assertThat(repository.findByTenantId(TENANT)).map(TenantAiPolicy::getProvider).contains(AiProvider.OLLAMA);
        assertThat(journal).containsExactly("pinned " + TENANT + " ollama <- défaut by " + ADMIN);
    }

    @Test
    void changer_de_fournisseur_journalise_l_ancien_et_le_nouveau() {
        service.pin(new AiPolicyDto.PinCommand("ollama"));
        service.pin(new AiPolicyDto.PinCommand("mistral"));

        assertThat(journal).last().isEqualTo("pinned " + TENANT + " mistral <- ollama by " + ADMIN);
    }

    @Test
    void reconfirmer_le_meme_fournisseur_ne_pollue_pas_le_journal() {
        service.pin(new AiPolicyDto.PinCommand("ollama"));
        service.pin(new AiPolicyDto.PinCommand("OLLAMA"));

        assertThat(journal).hasSize(1);
    }

    @Test
    void refuse_un_fournisseur_que_l_ai_service_declare_non_configure() {
        assertThatThrownBy(() -> service.pin(new AiPolicyDto.PinCommand("anthropic")))
                .isInstanceOf(AiProviderNotConfiguredException.class);

        assertThat(repository.size()).isZero();
        assertThat(journal).isEmpty();
    }

    @Test
    void refuse_un_fournisseur_hors_liste_avant_tout_effet() {
        assertThatThrownBy(() -> service.pin(new AiPolicyDto.PinCommand("openai")))
                .isInstanceOf(InvalidAiProviderException.class);

        assertThat(repository.size()).isZero();
    }

    @Test
    void ai_service_injoignable_l_ecran_le_dit_sans_inventer_de_disponibilite() {
        catalog = Optional.empty();

        AiPolicyDto.View v = service.view();

        assertThat(v.catalogAvailable()).isFalse();
        assertThat(v.platformDefault()).isNull();
        assertThat(v.effectiveProvider()).isNull();
        assertThat(v.providers()).allSatisfy(o -> assertThat(o.configured()).isNull());
    }

    @Test
    void ai_service_injoignable_l_epinglage_reste_possible() {
        catalog = Optional.empty();

        AiPolicyDto.View v = service.pin(new AiPolicyDto.PinCommand("ollama"));

        assertThat(v.effectiveProvider()).isEqualTo("ollama");
        assertThat(repository.size()).isEqualTo(1);
    }

    @Test
    void un_fournisseur_absent_du_catalogue_n_est_pas_refuse() {
        catalog = Optional.of(new AiProviderCatalog.Snapshot(Optional.empty(), Map.of(AiProvider.OLLAMA, true)));

        service.pin(new AiPolicyDto.PinCommand("mistral"));

        assertThat(repository.findByTenantId(TENANT)).isPresent();
    }

    @Test
    void revenir_au_defaut_retire_la_politique_et_le_journalise() {
        service.pin(new AiPolicyDto.PinCommand("ollama"));

        AiPolicyDto.View v = service.release();

        assertThat(v.pinned()).isFalse();
        assertThat(v.effectiveProvider()).isEqualTo("mistral");
        assertThat(repository.size()).isZero();
        assertThat(journal).last().isEqualTo("released " + TENANT + " ollama by " + ADMIN);
    }

    @Test
    void revenir_au_defaut_sans_politique_ne_journalise_rien() {
        service.release();

        assertThat(journal).isEmpty();
    }

    @Test
    void un_tenant_ne_voit_ni_ne_touche_la_politique_d_un_autre() {
        service.pin(new AiPolicyDto.PinCommand("ollama"));

        currentTenant = OTHER;
        assertThat(service.view().pinned()).isFalse();
        service.release();

        assertThat(repository.findByTenantId(TENANT)).isPresent();
    }

    @Test
    void le_resolveur_rend_la_politique_du_tenant_et_rien_pour_les_autres() {
        service.pin(new AiPolicyDto.PinCommand("ollama"));
        TenantAiPolicyResolver resolver = new TenantAiPolicyResolver(repository);

        assertThat(resolver.providerFor(TENANT)).contains(AiProvider.OLLAMA);
        assertThat(resolver.providerFor(OTHER)).isEmpty();
        assertThatThrownBy(() -> resolver.providerFor(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void un_instantane_sans_donnees_ne_refuse_rien() {
        AiProviderCatalog.Snapshot s = new AiProviderCatalog.Snapshot(null, null);

        assertThat(s.platformDefault()).isEmpty();
        assertThat(s.refuses(AiProvider.MISTRAL)).isFalse();
    }
}
