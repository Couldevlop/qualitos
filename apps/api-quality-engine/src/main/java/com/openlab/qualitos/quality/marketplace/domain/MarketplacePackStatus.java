package com.openlab.qualitos.quality.marketplace.domain;

/**
 * Cycle de vie d'un pack du marketplace de packs normatifs (CLAUDE.md §8.11).
 *
 * <pre>
 *   SUBMITTED ──takeForReview──▶ IN_REVIEW ──publish──▶ PUBLISHED ──deprecate──▶ DEPRECATED
 *       │                           │
 *       └──────reject───────────────┴──reject──▶ REJECTED
 * </pre>
 *
 * <p>Aucun pack n'atteint {@link #PUBLISHED} sans passer par {@link #IN_REVIEW}
 * puis une validation humaine explicite par l'éditeur (rôle SUPER_ADMIN). Le
 * catalogue public n'expose QUE les packs {@link #PUBLISHED}.</p>
 */
public enum MarketplacePackStatus {

    /** Soumis par un partenaire ; en attente de prise en charge. */
    SUBMITTED,

    /** Pris en revue par l'éditeur ; instruction en cours. */
    IN_REVIEW,

    /** Validé et publié au catalogue public — installable par les tenants. */
    PUBLISHED,

    /** Rejeté par l'éditeur (motif obligatoire). État terminal. */
    REJECTED,

    /** Retiré du catalogue après publication. Les installations existantes restent. */
    DEPRECATED;

    /** Un pack visible dans le catalogue public et installable. */
    public boolean isPubliclyVisible() {
        return this == PUBLISHED;
    }
}
