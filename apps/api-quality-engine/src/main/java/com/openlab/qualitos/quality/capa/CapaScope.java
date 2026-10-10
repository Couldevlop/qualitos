package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.authz.domain.Permission;

import java.util.Optional;
import java.util.UUID;

/**
 * Ce qui fait qu'un dossier CAPA concerne quelqu'un (ADR 0081) : il le pilote,
 * il en vérifie l'efficacité, ou une de ses actions lui est confiée. Sans
 * « voir tous les dossiers », on ne voit qu'eux — la fiche et ses preuves.
 */
public final class CapaScope {

    private CapaScope() {}

    /** Vide : l'utilisateur voit tous les dossiers du client. */
    public static Optional<UUID> restriction(RecordScope scope) {
        return scope.restrictTo(Permission.CAPA_VIEW_ALL);
    }

    public static boolean sees(Optional<UUID> restriction, CapaCase capa) {
        return restriction.map(moi -> concerns(capa, moi)).orElse(true);
    }

    static boolean concerns(CapaCase capa, UUID moi) {
        return moi.equals(capa.getOwnerId())
                || moi.equals(capa.getVerificationAssigneeId())
                || capa.getActions().stream().anyMatch(a -> moi.equals(a.getAssigneeId()));
    }
}
