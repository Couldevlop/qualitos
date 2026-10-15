package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.riskregister.application.RiskCapaGateway;
import com.openlab.qualitos.quality.riskregister.domain.Risk;
import com.openlab.qualitos.quality.riskregister.domain.RiskCapaKind;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Le pont vers le module CAPA.
 *
 * <p>L'ouverture passe par {@link CapaService#openCase}, pas par le dépôt :
 * le dossier naît avec son journal de cycle de vie et ses notifications, comme
 * s'il avait été ouvert depuis l'écran CAPA, de la criticité du niveau brut du
 * risque. Sa nature — corrective ou préventive — est choisie à l'ouverture.
 *
 * <p>Le dossier naît avec SON action, en une seule écriture : une CAPA EST son
 * action (ADR 0088). Le responsable choisi dans l'annuaire porte l'action et
 * pilote le dossier.
 */
public class CapaRiskGateway implements RiskCapaGateway {

    private final CapaService capaService;
    private final CapaCaseRepository capaCases;

    public CapaRiskGateway(CapaService capaService, CapaCaseRepository capaCases) {
        this.capaService = capaService;
        this.capaCases = capaCases;
    }

    @Override
    public LinkedCapa open(Risk risk, String title, String description, RiskCapaKind kind, UUID ownerId,
                           String ownerName, LocalDate dueDate, Verification verification) {
        Verification v = verification != null ? verification : Verification.UNDECIDED;
        CapaDto.CaseResponse dossier = capaService.openCase(new CapaDto.OpenCaseRequest(
                title, description, typeDossier(kind), criticite(risk), CapaSourceType.RISK,
                risk.getReference(), ownerId, ownerName, dueDate, v.required(), v.assigneeId(),
                v.assigneeName(), v.instructions()), CapaSourceType.RISK, risk.getReference());
        return new LinkedCapa(dossier.id(), dossier.title(), dossier.dueDate(), dossier.status().name(),
                kind, dossier.ownerName());
    }

    @Override
    public List<LinkedCapa> linkedTo(Risk risk) {
        return capaCases.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                        risk.getTenantId(), CapaSourceType.RISK, risk.getReference()).stream()
                .map(c -> new LinkedCapa(c.getId(), c.getTitle(), c.getDueDate(), c.getStatus().name(),
                        nature(c.getType()), responsable(c)))
                .toList();
    }

    static CapaType typeDossier(RiskCapaKind kind) {
        return kind == RiskCapaKind.CORRECTIVE ? CapaType.CORRECTIVE : CapaType.PREVENTIVE;
    }

    /** Les dossiers ouverts avant le choix de nature étaient tous préventifs. */
    static RiskCapaKind nature(CapaType type) {
        return type == CapaType.CORRECTIVE ? RiskCapaKind.CORRECTIVE : RiskCapaKind.PREVENTIVE;
    }

    /**
     * Le pilote nommé du dossier ; à défaut — dossier antérieur à l'ADR 0088 —
     * le responsable de sa première action ; aucun s'il n'a jamais été désigné.
     */
    static String responsable(CapaCase c) {
        if (c.getOwnerName() != null && !c.getOwnerName().isBlank()) {
            return c.getOwnerName();
        }
        return c.getActions().stream()
                .map(CapaAction::getAssigneeName)
                .filter(n -> n != null && !n.isBlank())
                .findFirst().orElse(null);
    }

    static CapaCriticity criticite(Risk risk) {
        return switch (risk.grossLevel()) {
            case LOW -> CapaCriticity.LOW;
            case MEDIUM -> CapaCriticity.MEDIUM;
            case HIGH -> CapaCriticity.HIGH;
            case CRITICAL -> CapaCriticity.CRITICAL;
        };
    }
}
