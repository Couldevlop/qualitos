package com.openlab.qualitos.quality.common;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Exécute un traitement <b>dans</b> un client désigné, puis rend le contexte
 * tel qu'il était.
 *
 * <p><b>Pourquoi cette classe existe.</b> Depuis l'isolation par la base
 * (ADR 0086), chaque connexion empruntée porte {@code app.tenant_id} tiré de
 * {@link TenantContext}, et PostgreSQL refuse d'écrire une ligne dont le
 * {@code tenant_id} ne correspond pas. Les rares traitements qui agissent
 * légitimement sur un AUTRE client que celui de l'appelant — l'éditeur qui
 * ouvre un module chez un client, les travaux de fond qui parcourent les
 * clients un par un — doivent donc annoncer à la base le client qu'ils
 * visent, sinon leur écriture est rejetée. Sans ce passage, la surface
 * plateforme répondait 500 sur chaque activation : {@code new row violates
 * row-level security policy}.
 *
 * <p><b>Le contexte précédent est restauré, pas effacé.</b> C'est la
 * différence avec un {@code set} suivi d'un {@code clear} écrit à la main :
 * dans une requête HTTP, le fil porte déjà le client de l'appelant, et
 * l'effacer laisserait la suite du traitement — journal, réponse, filtres —
 * s'exécuter sans client, donc sans la protection que la politique apporte.
 * La restauration a lieu même quand le traitement lève une exception.
 *
 * <p><b>Ce n'est pas une porte dérobée au contrôle d'accès.</b> Changer le
 * client du contexte ne donne aucun droit : l'autorisation est décidée en
 * amont, sur le rôle de l'appelant ({@code SUPER_ADMIN} pour la surface
 * plateforme), et le client visé vient du chemin, jamais du corps d'une
 * requête (§18.2 règle 2).
 */
public final class TenantScope {

    private TenantScope() {
    }

    /** Exécute {@code work} dans le client désigné et rend son résultat. */
    public static <T> T callAs(UUID tenantId, Supplier<T> work) {
        if (tenantId == null) {
            throw new IllegalArgumentException("client désigné absent");
        }
        String precedent = TenantContext.hasTenant() ? TenantContext.getTenantId() : null;
        TenantContext.setTenantId(tenantId.toString());
        try {
            return work.get();
        } finally {
            if (precedent == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(precedent);
            }
        }
    }

    /** Exécute {@code work} dans le client désigné, sans résultat. */
    public static void runAs(UUID tenantId, Runnable work) {
        callAs(tenantId, () -> {
            work.run();
            return null;
        });
    }
}
