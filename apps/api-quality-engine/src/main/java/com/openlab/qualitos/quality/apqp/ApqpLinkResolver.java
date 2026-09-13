package com.openlab.qualitos.quality.apqp;

import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.controlplan.infrastructure.ControlPlanJpaRepository;
import com.openlab.qualitos.quality.pdca.PdcaCycleRepository;
import com.openlab.qualitos.quality.risk.FmeaProjectRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Vérifie qu'un renvoi de livrable désigne un enregistrement RÉEL du client.
 *
 * <p>Sans cette vérification, un livrable de genre {@code MODULE_LINK}
 * accepterait n'importe quel identifiant : l'écran afficherait un lien, le clic
 * tomberait sur une page vide, et rien ne dirait pourquoi. Un lien mort est pire
 * qu'une absence de lien — il affirme qu'une preuve existe.
 *
 * <p>Le filtre de client fait partie de la vérification : renvoyer vers l'AMDEC
 * d'un autre tenant reviendrait à lui emprunter sa preuve.
 */
@Component
public class ApqpLinkResolver {

    private final FmeaProjectRepository fmeas;
    private final ControlPlanJpaRepository controlPlans;
    private final PdcaCycleRepository pdcaCycles;
    private final CapaCaseRepository capaCases;

    public ApqpLinkResolver(FmeaProjectRepository fmeas,
                            ControlPlanJpaRepository controlPlans,
                            PdcaCycleRepository pdcaCycles,
                            CapaCaseRepository capaCases) {
        this.fmeas = fmeas;
        this.controlPlans = controlPlans;
        this.pdcaCycles = pdcaCycles;
        this.capaCases = capaCases;
    }

    /**
     * @throws ApqpDeliverableValidationException si le client n'a pas cet
     *         enregistrement — qu'il n'existe pas, ou qu'il appartienne à un autre
     */
    public void verifier(ApqpLinkedKind kind, UUID id, UUID tenantId) {
        boolean existe = switch (kind) {
            case FMEA -> fmeas.existsByIdAndTenantId(id, tenantId);
            case CONTROL_PLAN -> controlPlans.findByIdAndTenantId(id, tenantId).isPresent();
            case PDCA -> pdcaCycles.findByIdAndTenantId(id, tenantId).isPresent();
            case CAPA -> capaCases.findByIdAndTenantId(id, tenantId).isPresent();
        };
        if (!existe) {
            throw new ApqpDeliverableValidationException(
                    "No " + kind + " record " + id + " in this tenant");
        }
    }
}
