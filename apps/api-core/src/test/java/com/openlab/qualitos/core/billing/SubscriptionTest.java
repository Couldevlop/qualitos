package com.openlab.qualitos.core.billing;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'invariant que porte l'entité elle-même, indépendamment du service :
 * <b>une résiliation ne se réécrit pas</b>.
 *
 * <p>{@link SubscriptionService} refuse déjà la seconde résiliation, plus tôt
 * — avant l'appel au moteur, qu'il faut éviter de déranger pour rien. Le
 * garde-fou est ici aussi parce que le service n'est pas le seul appelant
 * concevable (un ordonnanceur d'échéances, une reprise de données), et qu'une
 * date de résiliation écrasée est irrécupérable : c'est elle qui décide de la
 * dernière période facturée, donc du dernier montant réclamé au client.
 */
class SubscriptionTest {

    static final UUID ACTEUR = UUID.randomUUID();
    static final Instant PREMIERE = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    void unAbonnementNeufEstVivant() {
        assertThat(abonnement().isLive()).isTrue();
    }

    @Test
    void resilierHorodateEtNommeSonAuteur() {
        Subscription subscription = abonnement();

        subscription.cancel(ACTEUR, PREMIERE);

        assertThat(subscription.isLive()).isFalse();
        assertThat(subscription.getCancelledAt()).isEqualTo(PREMIERE);
        assertThat(subscription.getCancelledBy()).isEqualTo(ACTEUR);
    }

    @Test
    void uneSecondeResiliationNEcrasePasLaPremiere() {
        Subscription subscription = abonnement();
        subscription.cancel(ACTEUR, PREMIERE);
        UUID autreActeur = UUID.randomUUID();

        assertThatThrownBy(() -> subscription.cancel(autreActeur,
                Instant.parse("2026-09-01T00:00:00Z")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deja resilie");

        assertThat(subscription.getCancelledAt()).isEqualTo(PREMIERE);
        assertThat(subscription.getCancelledBy()).isEqualTo(ACTEUR);
    }

    private static Subscription abonnement() {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .moduleCode("controlplan")
                .billingTier(BillingTier.STANDARD)
                .period(BillingPeriod.MONTHLY)
                .amountCents(9900)
                .currency("EUR")
                .startedOn(LocalDate.of(2026, 3, 15))
                .nextRenewal(LocalDate.of(2026, 4, 15))
                .createdAt(Instant.parse("2026-03-15T10:00:00Z"))
                .createdBy(ACTEUR)
                .build();
    }
}
