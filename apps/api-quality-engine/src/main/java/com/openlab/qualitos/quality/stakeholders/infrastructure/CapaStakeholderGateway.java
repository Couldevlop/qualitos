package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderPorts;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Port vers le module CAPA : ouvrir un dossier depuis une exigence qui n'est
 * pas tenue.
 *
 * <p>L'ouverture passe par {@link CapaService#openCase} et non par le dépôt :
 * le dossier naît ainsi avec son action, son journal et ses notifications,
 * exactement comme depuis n'importe quel autre écran (ADR 0088).
 *
 * <p>La source est {@code OTHER} : une exigence de partie intéressée n'est ni
 * une non-conformité, ni un constat d'audit, ni un risque du registre. La
 * référence, elle, dit précisément de qui et de quelle exigence il s'agit —
 * c'est ce qu'on lira dans le registre CAPA six mois plus tard.
 */
public class CapaStakeholderGateway implements StakeholderPorts.CapaGateway {

    private final CapaService capaService;

    public CapaStakeholderGateway(CapaService capaService) {
        this.capaService = capaService;
    }

    @Override
    public UUID open(String reference, String title, String description, UUID ownerId, String ownerName,
                     LocalDate dueDate) {
        CapaDto.OpenCaseRequest request = new CapaDto.OpenCaseRequest(
                title,
                description,
                // Corrective : on répond à un écart constaté sur une exigence
                // déjà opposable. Un dossier de pur endiguement ne se clôturerait
                // jamais (ISO 9001 §10.2).
                CapaType.CORRECTIVE,
                // La criticité se décide dans la fiche CAPA, qui est l'écran qui
                // la pilote ; on ouvre au cran médian plutôt que d'inventer une
                // cotation que personne n'a prononcée.
                CapaCriticity.MEDIUM,
                null,
                null,
                ownerId,
                ownerName,
                dueDate,
                null,
                null,
                null,
                null);
        return capaService.openCase(request, CapaSourceType.OTHER, reference).id();
    }
}
