package com.openlab.qualitos.core.identity;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Les rôles de plateforme qu'un client peut donner à ses membres (§16).
 *
 * <p><b>Une seule écriture : celle du realm.</b> Les rôles s'écrivent en
 * minuscules, exactement comme Keycloak les émet dans le jeton
 * ({@code realm_access.roles}) : {@code admin_tenant}, {@code quality_director}…
 * C'est cette forme que gardent {@code app_user_roles} (contrainte
 * {@code chk_app_user_roles_role}, migration V9) et que lit l'écran. Une saisie
 * en majuscules ou un ancien nom ({@code ADMIN}, {@code DIRECTOR_QUALITY}) est
 * ramené à la forme du realm avant d'être écrit : la base ne voit jamais deux
 * orthographes du même rôle.
 *
 * <p>{@code super_admin} n'est pas attribuable : il appartient à l'éditeur, et un
 * administrateur de client ne doit ni se l'attribuer ni l'attribuer. Il reste
 * CONNU — il peut figurer dans le jeton d'un membre provisionné à sa première
 * connexion. Les autres rôles du realm (offline_access, rôles par défaut…) ne
 * sont pas administrés par QualitOS : on ne les touche jamais.
 */
public final class PlatformRoles {

    public static final Set<String> ASSIGNABLE = Set.of(
            "admin_tenant", "quality_director", "quality_manager", "auditor", "user", "external_auditor");

    public static final String TENANT_ADMIN = "admin_tenant";

    public static final String SUPER_ADMIN = "super_admin";

    /** Tous les rôles de plateforme du realm, attribuables ou non. */
    public static final Set<String> KNOWN = Set.of(
            SUPER_ADMIN, "admin_tenant", "quality_director", "quality_manager", "auditor", "user",
            "external_auditor");

    /**
     * Anciens noms, ramenés à celui du realm : {@code ADMIN} des premières
     * versions, {@code DIRECTOR_QUALITY} du moteur qualité.
     */
    private static final Map<String, String> ALIASES = Map.of(
            "admin", TENANT_ADMIN,
            "director_quality", "quality_director");

    private PlatformRoles() {}

    /** La forme du realm d'un rôle de plateforme, ou rien si ce n'en est pas un. */
    public static Optional<String> canonical(String role) {
        if (role == null) {
            return Optional.empty();
        }
        String nom = role.strip().toLowerCase(Locale.ROOT);
        nom = ALIASES.getOrDefault(nom, nom);
        return KNOWN.contains(nom) ? Optional.of(nom) : Optional.empty();
    }

    /** Les rôles demandés, normalisés ; un rôle hors liste est refusé, pas ignoré. */
    public static Set<String> validated(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new InvalidRoleException("Au moins un rôle est attendu.");
        }
        Set<String> normalises = new TreeSet<>();
        for (String r : roles) {
            String nom = canonical(r).filter(ASSIGNABLE::contains)
                    .orElseThrow(() -> new InvalidRoleException("Rôle non attribuable : " + r));
            normalises.add(nom);
        }
        return normalises;
    }

    /**
     * Les rôles de plateforme d'un jeton, normalisés ; les autres rôles du realm
     * (offline_access, default-roles-…) sont ignorés, pas refusés : le jeton dit
     * ce qu'il veut, l'annuaire n'en garde que ce qui le concerne.
     */
    public static Set<String> fromToken(Collection<String> roles) {
        Set<String> normalises = new TreeSet<>();
        if (roles != null) {
            for (String r : roles) {
                canonical(r).ifPresent(normalises::add);
            }
        }
        return normalises;
    }
}
