package com.openlab.qualitos.core.security;

/**
 * Stocke le tenant_id courant dans un ThreadLocal.
 * Le tenant_id est TOUJOURS extrait depuis le JWT — jamais depuis le body de la requête.
 * Doit être cleared à la fin de chaque requête (voir TenantJwtFilter).
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Classe utilitaire — pas d'instanciation
    }

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
