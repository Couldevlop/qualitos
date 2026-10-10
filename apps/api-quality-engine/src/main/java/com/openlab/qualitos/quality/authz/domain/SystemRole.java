package com.openlab.qualitos.quality.authz.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Les rôles livrés par la plateforme (CLAUDE.md §16), tels que le jeton les porte.
 *
 * <p>Ils existent dans chaque client sans qu'on ait à les créer : leurs droits
 * par défaut viennent du code ({@link Permission}), et le client peut les
 * ajuster. Le rôle « super administrateur » est celui de l'ÉDITEUR de la
 * plateforme : un client ne le voit ni ne le modifie.
 *
 * <p>{@code aliases} : noms que la plateforme a portés pour le même rôle
 * (le jeton d'un ancien client, un code historique). Les accepter ici évite
 * qu'un même utilisateur ait deux jeux de droits selon le nom qu'on lui donne.
 */
public enum SystemRole {

    SUPER_ADMIN(Set.of()),
    ADMIN_TENANT(Set.of("ADMIN", "TENANT_ADMIN")),
    QUALITY_DIRECTOR(Set.of("DIRECTOR_QUALITY")),
    QUALITY_MANAGER(Set.of()),
    AUDITOR(Set.of()),
    USER(Set.of()),
    EXTERNAL_AUDITOR(Set.of());

    private final Set<String> aliases;

    SystemRole(Set<String> aliases) {
        this.aliases = aliases;
    }

    /** Le rôle système d'un nom de rôle du jeton ({@code quality_manager}, {@code ROLE_ADMIN}…). */
    public static Optional<SystemRole> fromTokenRole(String role) {
        if (role == null || role.isBlank()) {
            return Optional.empty();
        }
        String nom = role.strip().toUpperCase(Locale.ROOT);
        if (nom.startsWith("ROLE_")) {
            nom = nom.substring("ROLE_".length());
        }
        String cherche = nom;
        return Arrays.stream(values())
                .filter(r -> r.name().equals(cherche) || r.aliases.contains(cherche))
                .findFirst();
    }

    /** Le super administrateur appartient à l'éditeur : il ne s'administre pas depuis un client. */
    public boolean tenantManaged() {
        return this != SUPER_ADMIN;
    }
}
