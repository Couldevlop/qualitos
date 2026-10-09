package com.openlab.qualitos.quality.authz.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Tous les rôles d'un client : les rôles système, réglés ou par défaut, puis
 * ses rôles sur mesure.
 *
 * <p>Ce que la base contient se pose PAR-DESSUS les défauts du code : un rôle
 * système que le client n'a jamais touché garde les droits que la plateforme
 * livre, y compris ceux d'une action ajoutée au catalogue après coup. Le super
 * administrateur garde toujours tout — un enregistrement qui prétendrait le
 * régler est ignoré.
 */
public final class TenantRoles {

    private final Map<String, TenantRole> byCode;

    private TenantRoles(Map<String, TenantRole> byCode) {
        this.byCode = byCode;
    }

    public static TenantRoles of(Collection<TenantRole> stored) {
        Map<String, TenantRole> roles = new LinkedHashMap<>();
        for (SystemRole s : SystemRole.values()) {
            roles.put(s.name(), TenantRole.defaultsOf(s));
        }
        stored.stream()
                .filter(r -> !SystemRole.SUPER_ADMIN.name().equals(r.code()))
                .filter(r -> r.system() == SystemRole.fromTokenRole(r.code()).isPresent())
                .sorted(Comparator.comparing((TenantRole r) -> !r.system())
                        .thenComparing((TenantRole r) -> r.name() == null ? r.code() : r.name()))
                .forEach(r -> roles.put(r.code(), r));
        return new TenantRoles(roles);
    }

    /** Les rôles que le client administre : système d'abord (ordre de la plateforme), puis sur mesure. */
    public List<TenantRole> manageable() {
        return byCode.values().stream()
                .filter(r -> !SystemRole.SUPER_ADMIN.name().equals(r.code()))
                .toList();
    }

    public Optional<TenantRole> find(String code) {
        return Optional.ofNullable(code == null ? null : byCode.get(code));
    }

    /**
     * Les droits que donnent ces rôles, réunis. Un code inconnu — rôle supprimé,
     * rôle du jeton que la plateforme ne connaît pas — n'accorde rien.
     */
    public Set<Permission> effective(Collection<String> roleCodes) {
        EnumSet<Permission> droits = EnumSet.noneOf(Permission.class);
        for (String code : roleCodes) {
            TenantRole r = byCode.get(code);
            if (r != null) {
                droits.addAll(r.permissions());
            }
        }
        return droits;
    }
}
