package com.openlab.qualitos.quality.airouting.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiProviderTest {

    @Test
    void lit_le_nom_attendu_par_l_ai_service_sans_tenir_compte_de_la_casse() {
        assertThat(AiProvider.parse(" Ollama ")).isEqualTo(AiProvider.OLLAMA);
        assertThat(AiProvider.parse("MISTRAL")).isEqualTo(AiProvider.MISTRAL);
        assertThat(AiProvider.parse("anthropic").wireName()).isEqualTo("anthropic");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "openai", "http://evil.example", "ollama;drop"})
    void refuse_tout_fournisseur_hors_de_la_liste(String raw) {
        assertThatThrownBy(() -> AiProvider.parse(raw)).isInstanceOf(InvalidAiProviderException.class);
    }

    @Test
    void refuse_l_absence_de_fournisseur() {
        assertThatThrownBy(() -> AiProvider.parse(null)).isInstanceOf(InvalidAiProviderException.class);
    }

    @Test
    void seul_ollama_garde_les_prompts_chez_le_client() {
        assertThat(AiProvider.OLLAMA.isOnPremise()).isTrue();
        assertThat(AiProvider.MISTRAL.isOnPremise()).isFalse();
        assertThat(AiProvider.ANTHROPIC.isOnPremise()).isFalse();
    }

    @Test
    void une_politique_exige_tenant_fournisseur_date_et_auteur() {
        java.util.UUID id = java.util.UUID.randomUUID();
        java.time.Instant now = java.time.Instant.now();
        assertThatThrownBy(() -> new TenantAiPolicy(null, AiProvider.OLLAMA, now, id)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TenantAiPolicy(id, null, now, id)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TenantAiPolicy(id, AiProvider.OLLAMA, null, id)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TenantAiPolicy(id, AiProvider.OLLAMA, now, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void le_refus_de_configuration_nomme_le_fournisseur() {
        AiProviderNotConfiguredException e = new AiProviderNotConfiguredException(AiProvider.MISTRAL);
        assertThat(e.provider()).isEqualTo(AiProvider.MISTRAL);
        assertThat(e.getMessage()).contains("mistral");
    }
}
