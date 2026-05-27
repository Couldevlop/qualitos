package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.quality.blockchain.application.AnchoringDto;
import com.openlab.qualitos.quality.blockchain.application.AnchoringService;
import com.openlab.qualitos.quality.blockchain.domain.AnchorReadPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Ancrage périodique (ADR 0012 Phase A) : ancre les lots d'événements d'audit
 * non encore ancrés, par tenant. Désactivé en profil "test".
 */
@Component
@Profile("!test")
public class AnchoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnchoringScheduler.class);

    private final AnchorReadPort read;
    private final AnchoringService anchoring;

    public AnchoringScheduler(AnchorReadPort read, AnchoringService anchoring) {
        this.read = read;
        this.anchoring = anchoring;
    }

    @Scheduled(
            initialDelayString = "${qualitos.anchoring.initial-delay-ms:60000}",
            fixedDelayString = "${qualitos.anchoring.fixed-delay-ms:300000}")
    public void anchorAllPendingTenants() {
        List<UUID> tenants = read.tenantsWithUnanchoredEvents();
        if (tenants.isEmpty()) {
            return;
        }
        log.info("[anchor-scheduler] {} tenant(s) avec événements à ancrer", tenants.size());
        for (UUID tenantId : tenants) {
            try {
                AnchoringDto.AnchorBatchResult result =
                        anchoring.anchorBatch(new AnchoringDto.AnchorBatchRequest(tenantId, 0));
                if (result.batchSize() > 0) {
                    log.info("[anchor-scheduler] tenant={} ancré {} événement(s) tx={}",
                            tenantId, result.batchSize(), result.blockchainTxRef());
                }
            } catch (RuntimeException e) {
                // Un tenant en échec ne doit pas bloquer les autres.
                log.error("[anchor-scheduler] échec d'ancrage tenant={}: {}", tenantId, e.getMessage());
            }
        }
    }
}
