package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.riskregister.application.RiskCapaGateway;
import com.openlab.qualitos.quality.riskregister.domain.Risk;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Le pont vers le module CAPA.
 *
 * <p>L'ouverture passe par {@link CapaService#createCase}, pas par le dépôt :
 * le dossier naît avec son journal de cycle de vie et ses notifications, comme
 * s'il avait été ouvert depuis l'écran CAPA. Le dossier est PRÉVENTIF — l'écart
 * n'est pas survenu —, de la criticité du niveau brut du risque.
 */
public class CapaRiskGateway implements RiskCapaGateway {

    private final CapaService capaService;
    private final CapaCaseRepository capaCases;

    public CapaRiskGateway(CapaService capaService, CapaCaseRepository capaCases) {
        this.capaService = capaService;
        this.capaCases = capaCases;
    }

    @Override
    public LinkedCapa open(Risk risk, String title, String description, LocalDate dueDate, UUID ownerId) {
        CapaDto.CaseResponse dossier = capaService.createCase(new CapaDto.CreateCaseRequest(
                title, description, CapaType.PREVENTIVE, criticite(risk), CapaSourceType.RISK,
                risk.getReference(), ownerId, null, dueDate));
        return new LinkedCapa(dossier.id(), dossier.title(), dossier.dueDate(), dossier.status().name());
    }

    @Override
    public List<LinkedCapa> linkedTo(Risk risk) {
        return capaCases.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                        risk.getTenantId(), CapaSourceType.RISK, risk.getReference()).stream()
                .map(c -> new LinkedCapa(c.getId(), c.getTitle(), c.getDueDate(), c.getStatus().name()))
                .toList();
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
