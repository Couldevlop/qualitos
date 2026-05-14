package com.openlab.qualitos.quality.common;

/**
 * Stocke le tenant_id courant dans un ThreadLocal.
 * Le tenant_id est TOUJOURS extrait depuis le JWT — jamais depuis le body de la requête.
 * Copié depuis api-core pour éviter la dépendance cyclique entre modules.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

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
