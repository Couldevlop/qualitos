package com.openlab.qualitos.quality.authz.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Un rôle d'un client et les actions qu'il accorde.
 *
 * <p>Deux sortes : les rôles SYSTÈME, livrés par la plateforme et que le jeton
 * nomme ({@link SystemRole}) — on peut changer leurs droits, pas les supprimer ;
 * et les rôles SUR MESURE, créés par le client, qu'il attribue lui-même à ses
 * membres.
 *
 * <p>Un invariant protège le client contre lui-même : l'administrateur du client
 * garde toujours le droit d'administrer les droits. Sans lui, un clic suffirait à
 * ce que plus personne ne puisse rendre un droit retiré par erreur.
 */
public final class TenantRole {

    public static final int NAME_MAX = 120;
    public static final int DESCRIPTION_MAX = 500;
    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,63}$");

    private final String code;
    private final String name;
    private final String description;
    private final boolean system;
    private final Set<Permission> permissions;

    private TenantRole(String code, String name, String description, boolean system,
                       Collection<Permission> permissions) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.system = system;
        this.permissions = permissions.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(permissions));
    }

    /** Un rôle système avec ses droits par défaut, tant que le client ne les a pas changés. */
    public static TenantRole defaultsOf(SystemRole role) {
        EnumSet<Permission> droits = EnumSet.noneOf(Permission.class);
        Arrays.stream(Permission.values()).filter(p -> p.grantedByDefaultTo(role)).forEach(droits::add);
        return new TenantRole(role.name(), null, null, true, droits);
    }

    /** Un rôle système dont le client a réglé les droits (et éventuellement le libellé). */
    public static TenantRole system(SystemRole role, String name, String description, Collection<Permission> permissions) {
        TenantRole r = new TenantRole(role.name(), optional("name", name, NAME_MAX),
                optional("description", description, DESCRIPTION_MAX), true, permissions);
        r.checkAdminKeepsControl();
        return r;
    }

    /** Un rôle créé par le client. */
    public static TenantRole custom(String code, String name, String description, Collection<Permission> permissions) {
        String c = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(c).matches()) {
            throw new AuthzValidationException("code",
                    "Le code d'un rôle : lettres majuscules, chiffres et _, 2 à 64 caractères, commençant par une lettre.");
        }
        if (SystemRole.fromTokenRole(c).isPresent()) {
            throw new AuthzValidationException("code", "Ce code est celui d'un rôle de la plateforme.");
        }
        String n = optional("name", name, NAME_MAX);
        if (n == null) {
            throw new AuthzValidationException("name", "Le nom du rôle est obligatoire.");
        }
        return new TenantRole(c, n, optional("description", description, DESCRIPTION_MAX), false, permissions);
    }

    /** Ce rôle, avec d'autres droits. Le libellé et la description sont gardés. */
    public TenantRole withPermissions(Collection<Permission> autres) {
        TenantRole r = new TenantRole(code, name, description, system, autres);
        r.checkAdminKeepsControl();
        return r;
    }

    private void checkAdminKeepsControl() {
        if (system && SystemRole.ADMIN_TENANT.name().equals(code) && !permissions.contains(Permission.AUTHZ_MANAGE)) {
            throw new AuthzValidationException("permissions",
                    "L'administrateur du client garde le droit d'administrer les droits : sans lui, plus personne ne pourrait les rétablir.");
        }
    }

    private static String optional(String field, String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.strip();
        if (v.length() > max) {
            throw new AuthzValidationException(field, "Ce champ dépasse " + max + " caractères.");
        }
        return v;
    }

    public String code() { return code; }
    public String name() { return name; }
    public String description() { return description; }
    public boolean system() { return system; }
    public Set<Permission> permissions() { return permissions; }

    public boolean grants(Permission p) {
        return permissions.contains(p);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TenantRole r && code.equals(r.code) && system == r.system
                && Objects.equals(name, r.name) && Objects.equals(description, r.description)
                && permissions.equals(r.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, system, name, description, permissions);
    }
}
