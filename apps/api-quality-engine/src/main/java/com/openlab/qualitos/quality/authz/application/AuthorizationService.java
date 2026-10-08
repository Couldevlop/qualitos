package com.openlab.qualitos.quality.authz.application;

import com.openlab.qualitos.quality.authz.domain.AuthzNotFoundException;
import com.openlab.qualitos.quality.authz.domain.AuthzValidationException;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.domain.SystemRole;
import com.openlab.qualitos.quality.authz.domain.TenantRole;
import com.openlab.qualitos.quality.authz.domain.TenantRoles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Qui peut faire quoi, dans le client du jeton.
 *
 * <p>Les droits d'un utilisateur sont la réunion de ce qu'accordent ses rôles :
 * ceux que porte son jeton (rôles système de son compte) et ceux qu'on lui a
 * attribués dans l'application. Les droits d'un rôle sont ceux que le client a
 * réglés, ou à défaut ceux que la plateforme livre ({@link Permission}).
 *
 * <p><b>Cache.</b> La vérification tombe sur chaque requête d'écriture. Les rôles
 * d'un client et ceux d'un membre sont gardés {@link #TTL} en mémoire, et
 * oubliés sur ce nœud dès qu'on les modifie. Sur un autre nœud, un changement
 * de droit prend effet au plus tard après ce délai — un retrait de droit n'est
 * donc pas instantané partout ; c'est le prix d'une base qui n'est pas
 * interrogée à chaque clic, et il est écrit dans l'ADR 0078.
 */
public class AuthorizationService {

    static final Duration TTL = Duration.ofSeconds(30);

    private final AuthzPorts.Roles roles;
    private final AuthzPorts.Members members;
    private final AuthzPorts.Context context;
    private final AuthzPorts.Audit audit;
    private final Clock clock;

    private final Map<UUID, Cached<TenantRoles>> rolesCache = new ConcurrentHashMap<>();
    private final Map<MemberKey, Cached<Set<String>>> membersCache = new ConcurrentHashMap<>();

    public AuthorizationService(AuthzPorts.Roles roles, AuthzPorts.Members members, AuthzPorts.Context context,
                                AuthzPorts.Audit audit, Clock clock) {
        this.roles = roles;
        this.members = members;
        this.context = context;
        this.audit = audit;
        this.clock = clock;
    }

    // ---------- vérification ----------

    /** Vrai si l'utilisateur courant a ce droit dans son client. */
    public boolean has(Permission permission) {
        return effectivePermissions().contains(permission);
    }

    public AuthzDto.Me me() {
        Set<String> codes = effectiveRoleCodes();
        TenantRoles tous = tenantRoles(context.requireTenantId());
        return new AuthzDto.Me(context.userId().orElse(null), List.copyOf(new TreeSet<>(codes)),
                tous.effective(codes).stream().map(Permission::code).sorted().toList());
    }

    /** Les rôles de l'utilisateur courant : ceux du jeton et ceux attribués dans l'application. */
    public Set<String> currentRoleCodes() {
        return Set.copyOf(effectiveRoleCodes());
    }

    /** Les codes des rôles que le client connaît : rôles système et rôles créés. */
    public Set<String> tenantRoleCodes() {
        return tenantRoles(context.requireTenantId()).manageable().stream()
                .map(TenantRole::code)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Vrai si le rôle, tel que le client l'a réglé, accorde ce droit. */
    public boolean roleGrants(String roleCode, Permission permission) {
        return tenantRoles(context.requireTenantId()).effective(Set.of(roleCode)).contains(permission);
    }

    private Set<Permission> effectivePermissions() {
        return tenantRoles(context.requireTenantId()).effective(effectiveRoleCodes());
    }

    private Set<String> effectiveRoleCodes() {
        UUID tenant = context.requireTenantId();
        Set<String> codes = new HashSet<>(context.tokenRoles());
        context.userId().ifPresent(u -> codes.addAll(memberRoles(tenant, u)));
        return codes;
    }

    // ---------- catalogue et rôles ----------

    public List<AuthzDto.CatalogEntry> catalog() {
        return Arrays.stream(Permission.values()).map(p -> new AuthzDto.CatalogEntry(p.code(), p.module())).toList();
    }

    public List<AuthzDto.RoleView> roles() {
        UUID tenant = context.requireTenantId();
        Set<String> regles = new HashSet<>();
        roles.findByTenant(tenant).forEach(r -> regles.add(r.code()));
        return tenantRoles(tenant).manageable().stream()
                .map(r -> view(r, regles.contains(r.code())))
                .toList();
    }

    public AuthzDto.RoleView createRole(AuthzDto.RoleCommand cmd) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        if (cmd == null) {
            throw new AuthzValidationException("name", "Le nom du rôle est obligatoire.");
        }
        TenantRole role = TenantRole.custom(cmd.code(), cmd.name(), cmd.description(), permissions(cmd.permissions()));
        if (tenantRoles(tenant).find(role.code()).isPresent()) {
            throw new AuthzValidationException("code", "Un rôle porte déjà ce code.");
        }
        roles.save(tenant, role, acteur);
        forget(tenant);
        audit.roleSaved(tenant, role, true, acteur);
        return view(role, true);
    }

    /** Règle les droits (et, pour un rôle sur mesure, le libellé) d'un rôle existant. */
    public AuthzDto.RoleView updateRole(String code, AuthzDto.RoleCommand cmd) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        TenantRole actuel = manageable(tenant, code);
        if (cmd == null) {
            throw new AuthzValidationException("permissions", "Les droits du rôle sont attendus.");
        }
        Set<Permission> droits = permissions(cmd.permissions());
        TenantRole regle = actuel.system()
                ? TenantRole.system(SystemRole.valueOf(actuel.code()), cmd.name(), cmd.description(), droits)
                : TenantRole.custom(actuel.code(), cmd.name(), cmd.description(), droits);
        roles.save(tenant, regle, acteur);
        forget(tenant);
        audit.roleSaved(tenant, regle, false, acteur);
        return view(regle, true);
    }

    /**
     * Supprime un rôle sur mesure, ou rend à un rôle système ses droits livrés.
     * Les membres qui portaient un rôle sur mesure supprimé le perdent.
     */
    public void deleteRole(String code) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        TenantRole actuel = manageable(tenant, code);
        roles.delete(tenant, actuel.code());
        forget(tenant);
        audit.roleDeleted(tenant, actuel.code(), acteur);
    }

    // ---------- membres ----------

    public List<AuthzDto.MemberView> members() {
        UUID tenant = context.requireTenantId();
        return members.all(tenant).entrySet().stream()
                .map(e -> new AuthzDto.MemberView(e.getKey(), List.copyOf(new TreeSet<>(e.getValue()))))
                .toList();
    }

    public AuthzDto.MemberView replaceMemberRoles(UUID userId, Collection<String> codes) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        if (userId == null) {
            throw new AuthzValidationException("userId", "Le membre est obligatoire.");
        }
        TenantRoles tous = tenantRoles(tenant);
        Set<String> voulus = new TreeSet<>();
        for (String c : codes == null ? List.<String>of() : codes) {
            String code = c == null ? "" : c.strip();
            TenantRole r = tous.find(code).filter(x -> !SystemRole.SUPER_ADMIN.name().equals(x.code()))
                    .orElseThrow(() -> new AuthzValidationException("roles", "Rôle inconnu : " + code));
            voulus.add(r.code());
        }
        Set<String> avant = members.rolesOf(tenant, userId);
        members.replace(tenant, userId, voulus, acteur);
        membersCache.remove(new MemberKey(tenant, userId));
        audit.memberRolesReplaced(tenant, userId, avant, voulus, acteur);
        return new AuthzDto.MemberView(userId, List.copyOf(voulus));
    }

    // ---------- interne ----------

    private TenantRole manageable(UUID tenant, String code) {
        return tenantRoles(tenant).find(code)
                .filter(r -> !SystemRole.SUPER_ADMIN.name().equals(r.code()))
                .orElseThrow(() -> new AuthzNotFoundException("Rôle introuvable."));
    }

    private static Set<Permission> permissions(Collection<String> codes) {
        EnumSet<Permission> droits = EnumSet.noneOf(Permission.class);
        for (String c : codes == null ? List.<String>of() : codes) {
            droits.add(Permission.fromCode(c).orElseThrow(
                    () -> new AuthzValidationException("permissions", "Action inconnue : " + c)));
        }
        return droits;
    }

    private static AuthzDto.RoleView view(TenantRole r, boolean customized) {
        List<String> droits = new ArrayList<>(r.permissions().stream().map(Permission::code).sorted().toList());
        return new AuthzDto.RoleView(r.code(), r.name(), r.description(), r.system(), customized, droits);
    }

    private TenantRoles tenantRoles(UUID tenant) {
        Instant maintenant = clock.instant();
        Cached<TenantRoles> c = rolesCache.get(tenant);
        if (c != null && c.fresh(maintenant)) {
            return c.value();
        }
        TenantRoles lus = TenantRoles.of(roles.findByTenant(tenant));
        rolesCache.put(tenant, new Cached<>(lus, maintenant.plus(TTL)));
        return lus;
    }

    private Set<String> memberRoles(UUID tenant, UUID user) {
        Instant maintenant = clock.instant();
        MemberKey cle = new MemberKey(tenant, user);
        Cached<Set<String>> c = membersCache.get(cle);
        if (c != null && c.fresh(maintenant)) {
            return c.value();
        }
        Set<String> lus = Set.copyOf(members.rolesOf(tenant, user));
        membersCache.put(cle, new Cached<>(lus, maintenant.plus(TTL)));
        return lus;
    }

    /** Oublie ce qu'on sait du client sur ce nœud : ses rôles, et les rôles de ses membres. */
    private void forget(UUID tenant) {
        rolesCache.remove(tenant);
        membersCache.keySet().removeIf(k -> k.tenant().equals(tenant));
    }

    private record Cached<T>(T value, Instant expiresAt) {
        boolean fresh(Instant now) {
            return now.isBefore(expiresAt);
        }
    }

    private record MemberKey(UUID tenant, UUID user) {}
}
