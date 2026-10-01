package com.openlab.qualitos.quality.riskregister.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les ports de persistance du registre.
 *
 * <p>Chaque lecture prend le tenant en argument : l'isolation est dans la
 * signature, pas dans un appel qu'on pourrait oublier.
 *
 * <p>Aucune suppression de fiche : un risque ou une opportunité se clôt ou
 * s'écarte, il ne disparaît pas. La fiche compte comme preuve d'une exigence
 * normative, et une preuve effacée ne se présente pas à l'auditeur.
 */
public final class RegisterRepositories {

    private RegisterRepositories() {}

    public interface Risks {
        Risk save(Risk risk);

        Optional<Risk> findByIdAndTenant(UUID id, UUID tenantId);

        List<Risk> findByTenant(UUID tenantId);

        long countByTenant(UUID tenantId);

        boolean referenceTaken(UUID tenantId, String reference);

        /** Les risques déjà issus de cet objet : l'écran les montre plutôt que d'en créer un doublon. */
        List<Risk> findBySource(UUID tenantId, RegisterOrigin origin, UUID sourceId);
    }

    public interface Opportunities {
        Opportunity save(Opportunity opportunity);

        Optional<Opportunity> findByIdAndTenant(UUID id, UUID tenantId);

        List<Opportunity> findByTenant(UUID tenantId);

        long countByTenant(UUID tenantId);

        boolean referenceTaken(UUID tenantId, String reference);
    }

    public interface Actions {
        OpportunityAction save(OpportunityAction action);

        Optional<OpportunityAction> findByIdAndTenant(UUID id, UUID tenantId);

        List<OpportunityAction> findByOpportunity(UUID tenantId, UUID opportunityId);

        /** Le plus grand numéro déjà attribué dans ce client, 0 s'il n'y en a aucun. */
        int maxNumber(UUID tenantId);

        void delete(OpportunityAction action);
    }

    public interface Events {
        RegisterEvent save(RegisterEvent event);

        /** Du plus récent au plus ancien : le suivi se lit par le haut. */
        List<RegisterEvent> findByItem(UUID tenantId, RegisterItemKind kind, UUID itemId);
    }
}
