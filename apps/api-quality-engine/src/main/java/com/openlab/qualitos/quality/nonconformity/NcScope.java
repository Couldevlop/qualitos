package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.authz.domain.Permission;

import java.util.Optional;
import java.util.UUID;

/**
 * Ce qui fait qu'une non-conformité concerne quelqu'un (ADR 0081) : il l'a
 * déclarée. Sans « voir toutes les NC », on ne voit qu'elles — la fiche, ses
 * photos, son rapport 8D.
 */
public final class NcScope {

    private NcScope() {}

    /** Vide : l'utilisateur voit toutes les NC du client. */
    public static Optional<UUID> restriction(RecordScope scope) {
        return scope.restrictTo(Permission.NC_VIEW_ALL);
    }

    public static boolean sees(Optional<UUID> restriction, NonConformity nc) {
        return restriction.map(moi -> moi.equals(nc.getReporterId())).orElse(true);
    }
}
