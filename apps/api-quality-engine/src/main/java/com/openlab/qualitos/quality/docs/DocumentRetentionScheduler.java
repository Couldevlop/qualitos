package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retire chaque jour les fichiers des versions obsolètes arrivés au terme de la
 * conservation réglée par chaque client (ADR 0094). Un client qui n'a rien réglé
 * garde tout : la purge ne se décide jamais par défaut.
 *
 * <p>Même forme que les autres tâches : mince, inactive en profil « test »,
 * {@code fixedDelay} pour qu'un passage lent ne chevauche pas le suivant. Un
 * passage traite un lot par client ; le reste attend le passage suivant.
 */
@Component
@Profile("!test")
public class DocumentRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DocumentRetentionScheduler.class);

    private final DocumentSettingsRowRepository settings;
    private final DocumentFileService files;

    public DocumentRetentionScheduler(DocumentSettingsRowRepository settings, DocumentFileService files) {
        this.settings = settings;
        this.files = files;
    }

    @Scheduled(
            initialDelayString = "${qualitos.documents.retention.initial-delay-ms:600000}",
            fixedDelayString = "${qualitos.documents.retention.fixed-delay-ms:86400000}")
    public void run() {
        int total = 0;
        try {
            for (DocumentSettingsRow s : settings.findWithRetention()) {
                TenantContext.setTenantId(s.getTenantId().toString());
                try {
                    total += files.purgeExpired(s.getTenantId(), s.getObsoleteRetentionYears());
                } catch (RuntimeException e) {
                    // Un client en échec n'emporte pas les suivants.
                    log.error("[documents-retention] passage en échec pour un client : {}", e.getMessage());
                } finally {
                    TenantContext.clear();
                }
            }
        } catch (RuntimeException e) {
            // Une exception remontée à l'ordonnanceur arrêterait la tâche pour de bon.
            log.error("[documents-retention] passage interrompu : {}", e.getMessage(), e);
        }
        if (total > 0) {
            log.info("[documents-retention] {} fichier(s) de version obsolète retiré(s)", total);
        }
    }
}
