package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.capa.CapaTransition;
import com.openlab.qualitos.quality.capa.CapaTransitionEvent;
import com.openlab.qualitos.quality.nonconformity.NcCreatedEvent;
import com.openlab.qualitos.quality.revisionrequests.application.CapaRevisionTrigger;
import com.openlab.qualitos.quality.revisionrequests.application.NcRevisionTrigger;
import com.openlab.qualitos.quality.revisionrequests.application.RevisionRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Branche le moteur de propositions sur les faits du système.
 *
 * <p>APRÈS COMMIT, jamais avant : avant le commit, la non-conformité que le
 * moteur relit pourrait disparaître d'un rollback, et il proposerait une révision
 * pour un défaut qui n'a jamais été enregistré.
 *
 * <p>Une proposition de révision est un confort ; la non-conformité est le fait.
 * Si le moteur tombe, la NC reste enregistrée : c'est la seule hiérarchie
 * acceptable entre les deux.
 */
@Component
public class RevisionRequestEventListeners {

    private static final Logger log = LoggerFactory.getLogger(RevisionRequestEventListeners.class);

    private final NcRevisionTrigger ncTrigger;
    private final CapaRevisionTrigger capaTrigger;
    private final RevisionRequestService service;

    public RevisionRequestEventListeners(NcRevisionTrigger ncTrigger,
                                         CapaRevisionTrigger capaTrigger,
                                         RevisionRequestService service) {
        this.ncTrigger = ncTrigger;
        this.capaTrigger = capaTrigger;
        this.service = service;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNcCreated(NcCreatedEvent event) {
        safely("NC " + event.ncId(), () -> service.record(ncTrigger.propose(event)));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCapaTransition(CapaTransitionEvent event) {
        if (event.transition() != CapaTransition.CLOSED) return;
        safely("CAPA " + event.payload().get("id"), () -> service.record(capaTrigger.propose(event)));
    }

    private void safely(String subject, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            // Une proposition manquée se rattrape à la prochaine NC. Une NC perdue,
            // non : elle a été constatée au poste et ne sera pas resaisie.
            log.warn("Proposition de révision abandonnée pour {} : {}", subject, ex.toString());
        }
    }
}
