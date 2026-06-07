package com.openlab.qualitos.quality.commconnector;

/**
 * Événement qualité entrant à notifier. Forme normalisée et découplée du flux source
 * (Kafka audit_events, ApplicationEvent…). Le {@link CommConnectorService} le transforme
 * en {@link CommMessage} premium avant envoi.
 *
 * @param kind     type d'événement métier (cf. {@link Kind})
 * @param title    titre lisible (ex: "Non-conformité détectée")
 * @param summary  résumé/description courte
 * @param resourceType type de ressource source (ex: "NON_CONFORMITY")
 * @param resourceId   identifiant de la ressource (deep-link)
 * @param severity sévérité de l'événement
 */
public record CommEvent(
        Kind kind,
        String title,
        String summary,
        String resourceType,
        String resourceId,
        CommSeverity severity
) {

    /**
     * Événements clés ciblés en V1 (CLAUDE.md §13.2). Le {@code wire} correspond au
     * type d'événement webhook / action d'audit, ce qui permet de router depuis le même
     * flux que les webhooks sortants sans réinventer la diffusion.
     */
    public enum Kind {
        NC_DETECTED("non-conformity.detected", CommSeverity.WARNING, "Non-conformité détectée"),
        CAPA_OVERDUE("capa.case.overdue", CommSeverity.WARNING, "CAPA en retard"),
        KPI_THRESHOLD_BREACHED("kpi.threshold.breached", CommSeverity.CRITICAL, "Seuil KPI franchi");

        private final String wire;
        private final CommSeverity defaultSeverity;
        private final String defaultTitle;

        Kind(String wire, CommSeverity defaultSeverity, String defaultTitle) {
            this.wire = wire;
            this.defaultSeverity = defaultSeverity;
            this.defaultTitle = defaultTitle;
        }

        public String wire() { return wire; }
        public CommSeverity defaultSeverity() { return defaultSeverity; }
        public String defaultTitle() { return defaultTitle; }

        /** Résout un {@link Kind} depuis le wire format d'audit/webhook, ou null si non ciblé. */
        public static Kind fromWire(String wire) {
            if (wire == null) return null;
            for (Kind k : values()) {
                if (k.wire.equals(wire)) return k;
            }
            return null;
        }
    }
}
