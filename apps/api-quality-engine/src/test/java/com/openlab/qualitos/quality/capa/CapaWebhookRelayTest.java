package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.webhooks.EventType;
import com.openlab.qualitos.quality.webhooks.WebhookService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * EventType déclarait `capa.case.opened`, `capa.case.resolved`,
 * `capa.case.closed` et `capa.effectiveness.verified` — et aucun code ne les
 * publiait. Un tenant pouvait s'y abonner et n'en recevoir jamais un seul : une
 * promesse tenue dans le contrat et nulle part ailleurs.
 */
@ExtendWith(MockitoExtension.class)
class CapaWebhookRelayTest {

    @Mock WebhookService webhooks;

    private static final UUID TENANT = UUID.randomUUID();

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private CapaTransitionEvent event(CapaTransition transition) {
        return new CapaTransitionEvent(TENANT, transition, Map.of("id", "42", "status", "CLOSED"));
    }

    @Test
    @DisplayName("annonce la clôture aux abonnés")
    void publieLaTransition() {
        new CapaWebhookRelay(webhooks).onTransition(event(CapaTransition.CLOSED));

        verify(webhooks).publish(eq(EventType.CAPA_CASE_CLOSED), any());
    }

    @Test
    @DisplayName("ne publie rien pour une transition restée interne")
    void ignoreLesTransitionsInternes() {
        new CapaWebhookRelay(webhooks).onTransition(event(CapaTransition.STARTED));

        verifyNoInteractions(webhooks);
    }

    @Test
    @DisplayName("pose le tenant de l'événement, pas celui qui traînait dans le contexte")
    void reposeLeTenantDeLEvenement() {
        doAnswer(inv -> {
            // C'est PENDANT la publication que le tenant doit être celui du
            // dossier : le relais s'exécute après validation, et se fier au
            // contexte ambiant ferait dépendre une annonce d'un état de passage.
            assertThat(TenantContext.getTenantId()).isEqualTo(TENANT.toString());
            return java.util.List.of();
        }).when(webhooks).publish(any(), any());

        new CapaWebhookRelay(webhooks).onTransition(event(CapaTransition.RESOLVED));

        verify(webhooks).publish(eq(EventType.CAPA_CASE_RESOLVED), any());
    }

    @Test
    @DisplayName("rend le contexte tel qu'il l'a trouvé")
    void restaureLeContextePrecedent() {
        UUID autre = UUID.randomUUID();
        TenantContext.setTenantId(autre.toString());

        new CapaWebhookRelay(webhooks).onTransition(event(CapaTransition.CLOSED));

        assertThat(TenantContext.getTenantId()).isEqualTo(autre.toString());
    }

    @Test
    @DisplayName("laisse le contexte vide s'il l'était")
    void neLaissePasDeTenantResiduel() {
        TenantContext.clear();

        new CapaWebhookRelay(webhooks).onTransition(event(CapaTransition.CLOSED));

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("un abonné injoignable ne remonte pas jusqu'au métier")
    void avaleLEchecDePublication() {
        doThrow(new IllegalStateException("souscription mal formée"))
                .when(webhooks).publish(any(), any());

        // La transition métier est validée : elle ne se rejoue pas. Les échecs de
        // livraison ont déjà leur propre suivi (statut, relances, file de rebut).
        assertThatCode(() -> new CapaWebhookRelay(webhooks).onTransition(event(CapaTransition.CLOSED)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("un échec de publication ne laisse pas le tenant derrière lui")
    void restaureLeContexteMemeApresEchec() {
        doThrow(new IllegalStateException("boum")).when(webhooks).publish(any(), any());

        new CapaWebhookRelay(webhooks).onTransition(event(CapaTransition.CLOSED));

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("la charge utile part telle qu'elle a été constituée")
    void transmetLaChargeUtile() {
        new CapaWebhookRelay(webhooks).onTransition(event(CapaTransition.OPENED));

        verify(webhooks).publish(eq(EventType.CAPA_CASE_OPENED),
                eq(Map.of("id", "42", "status", "CLOSED")));
        verify(webhooks, never()).publish(eq(EventType.CAPA_CASE_CLOSED), any());
    }
}
