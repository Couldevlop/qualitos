package com.openlab.qualitos.quality.controlplan.infrastructure;

import com.openlab.qualitos.quality.controlplan.application.TenantProvider;
import com.openlab.qualitos.quality.controlplan.domain.FmeaItemLookup;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import com.openlab.qualitos.quality.risk.FmeaProjectRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Remonte de la ligne de PFMEA au produit qu'elle couvre, en deux sauts :
 * ligne → projet → produit.
 *
 * <p>Le contrôle de tenant est fait ici, sur la ligne comme sur le projet : les
 * dépôts Spring Data du module {@code risk} exposent des lectures par identifiant
 * qui ne le font pas, et s'en remettre à l'appelant serait exactement la faille
 * qu'on cherche à fermer.
 */
@Component
public class FmeaItemLookupAdapter implements FmeaItemLookup {

    private final FmeaItemRepository items;
    private final FmeaProjectRepository projects;
    private final TenantProvider tenants;

    public FmeaItemLookupAdapter(FmeaItemRepository items, FmeaProjectRepository projects,
                                 @Qualifier("controlPlanTenantContextProvider") TenantProvider tenants) {
        this.items = items;
        this.projects = projects;
        this.tenants = tenants;
    }

    @Override
    public Optional<UUID> productCoveredBy(UUID fmeaItemId) {
        UUID tenant = tenants.requireTenantId();
        return items.findById(fmeaItemId)
                .filter(item -> tenant.equals(item.getTenantId()))
                .flatMap(item -> projects.findById(item.getProjectId()))
                .filter(project -> tenant.equals(project.getTenantId()))
                .map(project -> project.getProductId());
    }
}
