package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaActionType;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.revisionrequests.application.CapaActionsPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Expose les actions d'un dossier CAPA sous la forme réduite dont le moteur a
 * besoin. Le dossier est chargé avec son tenant : un identifiant de dossier ne
 * suffit pas à autoriser sa lecture.
 */
@Component
public class CapaActionsAdapter implements CapaActionsPort {

    private final CapaCaseRepository capaCases;

    public CapaActionsAdapter(CapaCaseRepository capaCases) {
        this.capaCases = capaCases;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CapaActionSummary> actionsOf(UUID tenantId, UUID capaId) {
        return capaCases.findByIdAndTenantId(capaId, tenantId)
                .map(capa -> capa.getActions().stream().map(CapaActionsAdapter::summary).toList())
                .orElseGet(List::of);
    }

    private static CapaActionSummary summary(CapaAction action) {
        return new CapaActionSummary(action.getId(), action.getTitle(), action.getDescription(),
                action.getActionType() == CapaActionType.CONTAINMENT);
    }
}
