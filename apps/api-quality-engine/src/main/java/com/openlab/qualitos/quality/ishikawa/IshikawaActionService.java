package com.openlab.qualitos.quality.ishikawa;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Plan d'actions d'un diagramme Ishikawa (§3.5, §3.6).
 *
 * <p>Le tenant vient TOUJOURS du jeton, jamais du corps de la requête : toute
 * lecture comme toute écriture est bornée à l'organisation de l'appelant, et une
 * action appartenant à un autre tenant est introuvable — pas « interdite », mais
 * inexistante de son point de vue.
 */
@Service
public class IshikawaActionService {

    private final IshikawaActionRepository actionRepository;
    private final IshikawaDiagramRepository diagramRepository;

    public IshikawaActionService(IshikawaActionRepository actionRepository,
                                 IshikawaDiagramRepository diagramRepository) {
        this.actionRepository = actionRepository;
        this.diagramRepository = diagramRepository;
    }

    @Transactional(readOnly = true)
    public List<IshikawaDto.ActionResponse> list(UUID diagramId) {
        UUID tenantId = requireTenantId();
        requireDiagram(diagramId, tenantId);
        return actionRepository.findByDiagramIdAndTenantIdOrderByCreatedAtAsc(diagramId, tenantId)
                .stream().map(IshikawaActionService::toResponse).toList();
    }

    @Transactional
    public IshikawaDto.ActionResponse create(UUID diagramId, IshikawaDto.CreateActionRequest request) {
        UUID tenantId = requireTenantId();
        IshikawaDiagram diagram = requireDiagram(diagramId, tenantId);

        IshikawaAction action = new IshikawaAction();
        action.setTenantId(tenantId);
        action.setDiagram(diagram);
        action.setLabel(requireLabel(request.label()));
        action.setResponsible(trimToNull(request.responsible()));
        action.setDecidedOn(request.decidedOn());
        action.setStatus(request.status() == null ? IshikawaActionStatus.TODO : request.status());
        return toResponse(actionRepository.save(action));
    }

    /**
     * Modification champ par champ : l'édition se fait cellule par cellule, un
     * champ absent signifie « inchangé ». Traiter un absent comme un effacement
     * viderait le responsable dès qu'on corrige un libellé.
     */
    @Transactional
    public IshikawaDto.ActionResponse update(UUID id, IshikawaDto.UpdateActionRequest request) {
        UUID tenantId = requireTenantId();
        IshikawaAction action = actionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IshikawaActionNotFoundException(id));

        if (request.label() != null)       action.setLabel(requireLabel(request.label()));
        if (request.responsible() != null) action.setResponsible(trimToNull(request.responsible()));
        if (request.decidedOn() != null)   action.setDecidedOn(request.decidedOn());
        if (request.status() != null)      action.setStatus(request.status());
        return toResponse(actionRepository.save(action));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requireTenantId();
        IshikawaAction action = actionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IshikawaActionNotFoundException(id));
        actionRepository.delete(action);
    }

    // ---- garde-fous -----------------------------------------------------------

    private static String requireLabel(String label) {
        String value = label == null ? "" : label.trim();
        if (value.isEmpty()) {
            // Une cellule vidée par mégarde ne doit pas laisser une ligne muette
            // dans le plan d'actions.
            throw new IshikawaStateException("L'intitulé de l'action est obligatoire");
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private IshikawaDiagram requireDiagram(UUID diagramId, UUID tenantId) {
        return diagramRepository.findByIdAndTenantId(diagramId, tenantId)
                .orElseThrow(() -> new IshikawaDiagramNotFoundException(diagramId));
    }

    private static UUID requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(tenantId);
    }

    private static IshikawaDto.ActionResponse toResponse(IshikawaAction action) {
        return new IshikawaDto.ActionResponse(
                action.getId(),
                action.getDiagram().getId(),
                action.getLabel(),
                action.getResponsible(),
                action.getDecidedOn(),
                action.getStatus(),
                action.getCreatedAt(),
                action.getUpdatedAt());
    }
}
