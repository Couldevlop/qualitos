package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.nonconformity.NcRiskRevisionsPort;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestRepository;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestStatus;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Repond a la non-conformite depuis le registre des demandes de revision.
 *
 * <p>L'adaptateur est ICI, chez les revisions, et non chez la non-conformite :
 * c'est ce module qui connait les deux cotes, et la dependance garde ainsi le
 * sens qu'elle a deja (revisions -> non-conformite).
 */
public class NcRiskRevisionsAdapter implements NcRiskRevisionsPort {

    private final RevisionRequestRepository requests;

    public NcRiskRevisionsAdapter(RevisionRequestRepository requests) {
        this.requests = requests;
    }

    @Override
    public boolean pfmeaRevisedFor(UUID tenantId, UUID ncId) {
        return acceptee(tenantId, ncId, RevisionTargetType::isPfmea);
    }

    @Override
    public boolean controlPlanRevisedFor(UUID tenantId, UUID ncId) {
        // Le plan de surveillance est ce qui n'est pas le PFMEA : la seule autre
        // cible du registre. Ecrit en complement plutot qu'en liste de valeurs,
        // pour qu'une cible ajoutee demain ne soit pas silencieusement rangee
        // du cote du PFMEA.
        return acceptee(tenantId, ncId, type -> !type.isPfmea());
    }

    private boolean acceptee(UUID tenantId, UUID ncId, Predicate<RevisionTargetType> cible) {
        if (tenantId == null || ncId == null) {
            return false;
        }
        return requests.findByTrigger(tenantId, ncId).stream()
                .filter(r -> r.getStatus() == RevisionRequestStatus.ACCEPTED)
                .map(RevisionRequest::getTargetType)
                .anyMatch(cible);
    }
}
