package com.openlab.qualitos.quality.riskregister.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Une ligne du « Suivi » d'une fiche : ce qui a changé, quand.
 *
 * <p>Des codes, pas des phrases : {@code fromValue}/{@code toValue} portent
 * « 3x3 », « IN_TREATMENT », « FMEA »… et l'écran compose la phrase dans la
 * langue de l'utilisateur. {@code detail} garde la référence liée à la création
 * (« AMDEC-12 ») ou l'intitulé de l'action ouverte.
 *
 * <p>Ce journal est celui de la fiche, lisible de tous ; le journal d'audit
 * chaîné reste la trace opposable, et il ne contient aucun texte libre.
 */
public record RegisterEvent(
        UUID id,
        UUID tenantId,
        RegisterItemKind itemKind,
        UUID itemId,
        RegisterEventType type,
        String fromValue,
        String toValue,
        String detail,
        UUID actorId,
        Instant at) {

    public static final int DETAIL_MAX = 255;

    public static RegisterEvent of(UUID tenantId, RegisterItemKind kind, UUID itemId,
                                   RegisterEventType type, String from, String to, String detail,
                                   UUID actor, Instant at) {
        String note = detail == null || detail.length() <= DETAIL_MAX
                ? detail : detail.substring(0, DETAIL_MAX);
        return new RegisterEvent(null, tenantId, kind, itemId, type, from, to, note, actor, at);
    }
}
