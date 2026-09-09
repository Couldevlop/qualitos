package com.openlab.qualitos.quality.ideas.infrastructure;

import com.openlab.qualitos.quality.circle.CircleProposal;
import com.openlab.qualitos.quality.circle.ProposalStatus;
import com.openlab.qualitos.quality.circle.QualityCircle;
import com.openlab.qualitos.quality.ideas.domain.Idea;
import com.openlab.qualitos.quality.ideas.domain.IdeaStatus;

/** Traduit la ligne héritée du module cercle en idée, et réciproquement. */
public final class IdeaMapper {

    private IdeaMapper() {}

    public static Idea toDomain(CircleProposal p) {
        return Idea.rehydrate(
                p.getId(),
                p.getTenantId(),
                p.getCircle() == null ? null : p.getCircle().getId(),
                p.getTitle(),
                p.getDescription(),
                toIdeaStatus(p.getStatus()),
                p.getProposedBy(),
                p.getProposedByName(),
                p.getValidatedBy(),
                p.getValidatedAt(),
                p.getImplementedAt(),
                p.getMeasuredAt(),
                p.getImpactNote(),
                p.getRejectionReason(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    /**
     * @param existing la ligne déjà en base, ou {@code null} pour une création.
     *                 On la réutilise plutôt que d'en construire une neuve :
     *                 remplacer l'instance ferait perdre la version JPA et la
     *                 réunion d'origine.
     * @param circle   le cercle, s'il y en a un.
     */
    public static CircleProposal toEntity(Idea idea, CircleProposal existing, QualityCircle circle) {
        CircleProposal p = existing != null ? existing : new CircleProposal();
        p.setTenantId(idea.getTenantId());
        if (circle != null) {
            p.setCircle(circle);
        }
        p.setTitle(idea.getTitle());
        p.setDescription(idea.getDescription());
        p.setStatus(toProposalStatus(idea.getStatus()));
        p.setProposedBy(idea.getProposedBy());
        p.setProposedByName(idea.getProposedByName());
        p.setValidatedBy(idea.getValidatedBy());
        p.setValidatedAt(idea.getValidatedAt());
        p.setImplementedAt(idea.getImplementedAt());
        p.setMeasuredAt(idea.getMeasuredAt());
        p.setImpactNote(idea.getImpactNote());
        p.setRejectionReason(idea.getRejectionReason());
        return p;
    }

    public static IdeaStatus toIdeaStatus(ProposalStatus s) {
        return IdeaStatus.valueOf(s.name());
    }

    public static ProposalStatus toProposalStatus(IdeaStatus s) {
        return ProposalStatus.valueOf(s.name());
    }
}
