package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaActionType;
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
 * <p>L'ouverture passe par {@link CapaService#createCase}, pas par le dépôt :
 * le dossier naît avec son journal de cycle de vie et ses notifications, comme
 * s'il avait été ouvert depuis l'écran CAPA, de la criticité du niveau brut du
 * risque. Sa nature — corrective ou préventive — est choisie à l'ouverture.
 *
 * <p>Le dossier naît avec SON action, de même nature, confiée au responsable
 * désigné et portant la même échéance : depuis un risque, le dossier EST
 * l'action. Les deux écritures partagent la transaction du contrôleur — un
 * dossier sans action ne survit pas à l'échec de la seconde.
 */
public class CapaRiskGateway implements RiskCapaGateway {

    private final CapaService capaService;
    private final CapaCaseRepository capaCases;

    public CapaRiskGateway(CapaService capaService, CapaCaseRepository capaCases) {
        this.capaService = capaService;
        this.capaCases = capaCases;
    }

    @Override
    public LinkedCapa open(Risk risk, String title, String description, RiskCapaKind kind, String assignee,
                           LocalDate dueDate, UUID ownerId) {
        CapaDto.CaseResponse dossier = capaService.createCase(new CapaDto.CreateCaseRequest(
                title, description, typeDossier(kind), criticite(risk), CapaSourceType.RISK,
                risk.getReference(), ownerId, null, dueDate));
        CapaDto.ActionResponse action = capaService.addAction(dossier.id(), new CapaDto.ActionRequest(
                title, description, null, typeAction(kind), null, assignee, null, dueDate));
        return new LinkedCapa(dossier.id(), dossier.title(), dossier.dueDate(), dossier.status().name(),
                kind, action.assigneeName());
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

    static CapaActionType typeAction(RiskCapaKind kind) {
        return kind == RiskCapaKind.CORRECTIVE ? CapaActionType.CORRECTIVE : CapaActionType.PREVENTIVE;
    }

    /** Les dossiers ouverts avant le choix de nature étaient tous préventifs. */
    static RiskCapaKind nature(CapaType type) {
        return type == CapaType.CORRECTIVE ? RiskCapaKind.CORRECTIVE : RiskCapaKind.PREVENTIVE;
    }

    /** Le responsable de la première action ; aucun pour un dossier ouvert avant qu'on le désigne. */
    static String responsable(CapaCase c) {
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
