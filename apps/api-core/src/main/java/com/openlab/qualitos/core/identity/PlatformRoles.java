package com.openlab.qualitos.core.identity;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Les rôles de plateforme qu'un client peut donner à ses membres (§16).
 *
 * <p>{@code super_admin} n'y est pas : il appartient à l'éditeur, et un
 * administrateur de client ne doit ni se l'attribuer ni l'attribuer. Les autres
 * rôles du realm (offline_access, rôles par défaut…) ne sont pas administrés
 * par QualitOS : on ne les touche jamais.
 */
public final class PlatformRoles {

    public static final Set<String> ASSIGNABLE = Set.of(
            "admin_tenant", "quality_director", "quality_manager", "auditor", "user", "external_auditor");

    public static final String TENANT_ADMIN = "admin_tenant";

    private PlatformRoles() {}

    /** Les rôles demandés, normalisés ; un rôle hors liste est refusé, pas ignoré. */
    public static Set<String> validated(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new InvalidRoleException("Au moins un rôle est attendu.");
        }
        Set<String> normalises = new TreeSet<>();
        for (String r : roles) {
            String nom = r == null ? "" : r.strip().toLowerCase(Locale.ROOT);
            if (!ASSIGNABLE.contains(nom)) {
                throw new InvalidRoleException("Rôle non attribuable : " + r);
            }
            normalises.add(nom);
        }
        return normalises;
    }
}
