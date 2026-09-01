package com.openlab.qualitos.quality.tenantmodules.application;

import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivation;

/**
 * Port — chaque transition publie un événement (audit log, bus, etc.).
 * NoOp par défaut côté tests.
 */
public interface ModuleActivationEventPublisher {

    enum Action {
        TRIAL_STARTED, ACTIVATED, SUSPENDED, RESUMED,
        TIER_CHANGED, CONFIGURED, EXPIRED, DISABLED,

        /**
         * Ouverture ou fermeture decidee par l'EDITEUR depuis la surface
         * plateforme ({@code /api/v1/platform/tenants/{tenantId}/modules}),
         * pour le compte d'un client designe — la consequence technique d'une
         * souscription enregistree dans api-core.
         *
         * <p>Deux valeurs distinctes plutot que la reutilisation d'ACTIVATED et
         * DISABLED : dans le journal chaine du client, l'acteur d'une
         * activation plateforme n'est PAS un de ses utilisateurs, c'est
         * l'editeur. Un auditeur peut le deduire de l'identifiant, qui ne
         * figure dans aucun de ses annuaires — mais le deduire suppose qu'il y
         * pense. Nommer l'action le dit.
         *
         * <p>Un SEUL evenement est publie par acte, pas un doublon « metier +
         * plateforme » : dans un journal CHAINE, chaque ligne surnumeraire
         * allonge la chaine a verifier et brouille le decompte des actes
         * reels.
         */
        PLATFORM_ACTIVATED, PLATFORM_DEACTIVATED
    }

    void publish(ModuleActivation activation, Action action);

    final class NoOp implements ModuleActivationEventPublisher {
        @Override public void publish(ModuleActivation a, Action action) { }
    }
}
