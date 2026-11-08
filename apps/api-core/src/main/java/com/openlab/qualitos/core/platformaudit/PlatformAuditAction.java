package com.openlab.qualitos.core.platformaudit;

/**
 * Le vocabulaire fermé des actes de plateforme consignés.
 *
 * <p><b>Pourquoi une énumération et non une chaîne libre.</b> Le journal du
 * moteur de qualité porte une chaîne contrainte par une expression régulière,
 * parce que ses actes sont ouverts : chaque module métier en ajoute. Ceux de
 * la plateforme ne le sont pas — l'éditeur a un petit nombre de pouvoirs sur
 * le dossier d'un client, et ils tiennent ici. Une énumération donne à
 * l'auditeur une liste qu'il peut lire en entier et confronter au code, là où
 * une chaîne libre l'obligerait à deviner si {@code tenant.deactivate} et
 * {@code tenant.deactivated} sont le même acte.
 *
 * <p>La conséquence utile est l'inverse de la souplesse : ajouter un pouvoir à
 * l'éditeur force à ajouter une valeur ici, donc à décider s'il est
 * auditable. Un acte sensible non consigné devient un oubli visible en revue
 * plutôt qu'un silence.
 */
public enum PlatformAuditAction {

    // ---- le dossier du client ----
    /** Un client est entré dans la plateforme. */
    TENANT_CREATED,
    /** Nom, palier ou état d'activité d'un client modifiés. */
    TENANT_UPDATED,
    /** Un client est désactivé : ses comptes ne passent plus. */
    TENANT_DEACTIVATED,

    // ---- ce que le client peut utiliser ----
    /** Un module est ouvert à un client (et lui sera facturé). */
    MODULE_SUBSCRIBED,
    /** Un module est fermé pour un client. */
    MODULE_CANCELLED,
    /** Un tarif du catalogue est fixé ou remplacé. */
    MODULE_PRICE_SET,

    // ---- les cabinets partenaires ----
    /** Un cabinet partenaire est créé. */
    PARTNER_FIRM_CREATED,
    /** L'identité ou le contact d'un cabinet est modifié. */
    PARTNER_FIRM_UPDATED,
    /** Un cabinet est activé ou désactivé. */
    PARTNER_FIRM_ACTIVE_CHANGED,
    /** Un consultant est rattaché à un cabinet. */
    PARTNER_CONSULTANT_ADDED,
    /** Un consultant est activé ou désactivé dans son cabinet. */
    PARTNER_CONSULTANT_ACTIVE_CHANGED,
    /** Un cabinet propose une mission à un client. */
    PARTNER_ENGAGEMENT_PROPOSED,
    /** Un client ACCORDE un accès à un cabinet sur ses données. */
    PARTNER_ENGAGEMENT_GRANTED,
    /** Un client refuse une mission proposée. */
    PARTNER_ENGAGEMENT_DECLINED,
    /** Un client RETIRE un accès précédemment accordé. */
    PARTNER_ENGAGEMENT_REVOKED,

    // ---- le journal lui-même ----
    /**
     * Un dossier d'audit a été exporté. L'export est lui-même un acte : savoir
     * qui a extrait le journal, et quelle fenêtre, fait partie de ce qu'un
     * auditeur vérifie. Sans cette ligne, le seul geste qui sort les données de
     * la plateforme serait le seul qu'elle ne consigne pas.
     */
    AUDIT_EXPORTED
}
