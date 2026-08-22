package com.openlab.qualitos.quality.capa.effectiveness.application;

import com.openlab.qualitos.quality.capa.effectiveness.domain.RecurrenceSignature;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Port — les dossiers CAPA clos d'un tenant, avec de quoi reconnaître leur
 * problème d'origine.
 *
 * <p>La signature est résolue par l'adaptateur, au plus près des données : c'est
 * lui qui sait remonter de la CAPA à la non-conformité qui l'a déclenchée.
 */
public interface ClosedCapaPort {

    List<ClosedCapa> findClosed(UUID tenantId);

    /**
     * @param openedAt  ouverture du dossier — début de la fenêtre « avant »
     * @param closedAt  clôture — début de la fenêtre « après »
     * @param signature ce qui définit une récidive ; {@link RecurrenceSignature#NONE}
     *                  quand le dossier ne se rattache à aucune non-conformité
     */
    record ClosedCapa(UUID id, String title, String criticity,
                      Instant openedAt, Instant closedAt,
                      Boolean effectivenessVerified,
                      RecurrenceSignature signature) {
    }
}
