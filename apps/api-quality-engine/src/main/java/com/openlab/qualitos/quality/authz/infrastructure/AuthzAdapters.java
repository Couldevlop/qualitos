package com.openlab.qualitos.quality.authz.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.authz.application.AuthzPorts;
import com.openlab.qualitos.quality.authz.domain.AuthzValidationException;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.domain.SystemRole;
import com.openlab.qualitos.quality.authz.domain.TenantRole;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/** Les adaptateurs des droits : base, jeton, journal d'audit. */
public final class AuthzAdapters {

    private AuthzAdapters() {}

    // ---------- rôles ----------

    public static final class Roles implements AuthzPorts.Roles {

        private final AuthzRoleRepository repository;
        private final AuthzMemberRoleRepository memberRoles;
        private final Clock clock;

        public Roles(AuthzRoleRepository repository, AuthzMemberRoleRepository memberRoles, Clock clock) {
            this.repository = repository;
            this.memberRoles = memberRoles;
            this.clock = clock;
        }

        @Override
        public List<TenantRole> findByTenant(UUID tenantId) {
            return repository.findByTenantId(tenantId).stream()
                    .map(Roles::toDomain)
                    .flatMap(Optional::stream)
                    .toList();
        }

        @Override
        public void save(UUID tenantId, TenantRole role, UUID actor) {
            Instant maintenant = clock.instant();
            AuthzRoleJpaEntity e = repository.findByTenantIdAndCode(tenantId, role.code()).orElseGet(() -> {
                AuthzRoleJpaEntity n = new AuthzRoleJpaEntity();
                n.setTenantId(tenantId);
                n.setCode(role.code());
                n.setSystemRole(role.system());
                n.setCreatedAt(maintenant);
                return n;
            });
            e.setName(role.name());
            e.setDescription(role.description());
            e.getPermissions().clear();
            role.permissions().forEach(p -> e.getPermissions().add(p.code()));
            e.setUpdatedBy(actor);
            e.setUpdatedAt(maintenant);
            repository.save(e);
        }

        @Override
        public void delete(UUID tenantId, String code) {
            repository.findByTenantIdAndCode(tenantId, code).ifPresent(repository::delete);
            // Un rôle système réinitialisé reste attribué ; un rôle sur mesure disparu, non.
            if (SystemRole.fromTokenRole(code).isEmpty()) {
                memberRoles.deleteRole(tenantId, code);
            }
        }

        /**
         * Une ligne relue. Une action retirée du catalogue depuis l'enregistrement
         * est ignorée, et une ligne incohérente (code système marqué « sur mesure »)
         * n'accorde rien : le domaine ne la reconstruit pas.
         */
        static Optional<TenantRole> toDomain(AuthzRoleJpaEntity e) {
            EnumSet<Permission> droits = EnumSet.noneOf(Permission.class);
            e.getPermissions().forEach(c -> Permission.fromCode(c).ifPresent(droits::add));
            Optional<SystemRole> systeme = SystemRole.fromTokenRole(e.getCode())
                    .filter(s -> s.name().equals(e.getCode()));
            try {
                if (e.isSystemRole()) {
                    return systeme.map(s -> TenantRole.system(s, e.getName(), e.getDescription(), droits));
                }
                return systeme.isPresent() ? Optional.empty()
                        : Optional.of(TenantRole.custom(e.getCode(), e.getName(), e.getDescription(), droits));
            } catch (AuthzValidationException invalide) {
                // Une ligne que le domaine refuserait aujourd'hui (administrateur
                // privé du droit d'administrer, nom vide) retombe sur les défauts
                // plutôt que de bloquer la lecture de TOUS les droits du client.
                return Optional.empty();
            }
        }
    }

    // ---------- membres ----------

    public static final class Members implements AuthzPorts.Members {

        private final AuthzMemberRoleRepository repository;
        private final Clock clock;

        public Members(AuthzMemberRoleRepository repository, Clock clock) {
            this.repository = repository;
            this.clock = clock;
        }

        @Override
        public Set<String> rolesOf(UUID tenantId, UUID userId) {
            return repository.findByTenantIdAndUserId(tenantId, userId).stream()
                    .map(AuthzMemberRoleJpaEntity::getRoleCode)
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        @Override
        public Map<UUID, Set<String>> all(UUID tenantId) {
            Map<UUID, Set<String>> parMembre = new HashMap<>();
            for (AuthzMemberRoleJpaEntity m : repository.findByTenantId(tenantId)) {
                parMembre.computeIfAbsent(m.getUserId(), k -> new TreeSet<>()).add(m.getRoleCode());
            }
            return parMembre;
        }

        @Override
        public void replace(UUID tenantId, UUID userId, Set<String> roleCodes, UUID actor) {
            repository.deleteMember(tenantId, userId);
            repository.flush();
            Instant maintenant = clock.instant();
            for (String code : roleCodes) {
                AuthzMemberRoleJpaEntity m = new AuthzMemberRoleJpaEntity();
                m.setTenantId(tenantId);
                m.setUserId(userId);
                m.setRoleCode(code);
                m.setAssignedBy(actor);
                m.setAssignedAt(maintenant);
                repository.save(m);
            }
        }
    }

    // ---------- contexte du jeton ----------

    /** Client et acteur viennent du JETON validé, jamais du corps (§18.2). */
    public static final class JwtContext implements AuthzPorts.Context {

        @Override
        public UUID requireTenantId() {
            if (!TenantContext.hasTenant()) {
                throw new MissingTenantContextException();
            }
            return UUID.fromString(TenantContext.getTenantId());
        }

        @Override
        public Optional<UUID> userId() {
            return CurrentUser.userId();
        }

        @Override
        public UUID requireActorId() {
            return CurrentUser.requireUserId();
        }

        @Override
        public Set<String> tokenRoles() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                return Set.of();
            }
            return systemRoles(auth.getAuthorities());
        }

        /** {@code ROLE_DIRECTOR_QUALITY}, {@code ROLE_ADMIN}… ramenés aux codes des rôles système. */
        public static Set<String> systemRoles(Collection<? extends GrantedAuthority> authorities) {
            return authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a != null && a.startsWith("ROLE_"))
                    .map(SystemRole::fromTokenRole)
                    .flatMap(Optional::stream)
                    .map(SystemRole::name)
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    // ---------- journal d'audit ----------

    /**
     * La trace opposable : des codes de rôles et d'actions, des identifiants —
     * aucun texte libre (ni nom de rôle, ni description), comme le registre (ADR 0076).
     */
    public static final class Audit implements AuthzPorts.Audit {

        static final String ROLE_RESOURCE = "authz-role";
        static final String MEMBER_RESOURCE = "authz-member";

        private final AuditEventService auditEvents;

        public Audit(AuditEventService auditEvents) {
            this.auditEvents = auditEvents;
        }

        @Override
        public void roleSaved(UUID tenantId, TenantRole role, boolean created, UUID actor) {
            String droits = role.permissions().stream().map(Permission::code).sorted()
                    .map(c -> "\"" + c + "\"").collect(Collectors.joining(","));
            publish(tenantId, created ? "authz.role.created" : "authz.role.updated", ROLE_RESOURCE, null,
                    created ? "Droits — rôle créé" : "Droits — rôle réglé",
                    "{\"role\":\"" + role.code() + "\",\"system\":" + role.system()
                            + ",\"permissions\":[" + droits + "]}", actor);
        }

        @Override
        public void roleDeleted(UUID tenantId, String code, UUID actor) {
            publish(tenantId, "authz.role.deleted", ROLE_RESOURCE, null,
                    "Droits — rôle supprimé ou réinitialisé", "{\"role\":\"" + code + "\"}", actor);
        }

        @Override
        public void memberRolesReplaced(UUID tenantId, UUID userId, Set<String> before, Set<String> after,
                                        UUID actor) {
            publish(tenantId, "authz.member.roles-replaced", MEMBER_RESOURCE, userId,
                    "Droits — rôles d'un membre changés",
                    "{\"before\":" + codes(before) + ",\"after\":" + codes(after) + "}", actor);
        }

        private static String codes(Set<String> codes) {
            return codes.stream().sorted().map(c -> "\"" + c + "\"").collect(Collectors.joining(",", "[", "]"));
        }

        private void publish(UUID tenantId, String action, String resource, UUID resourceId, String summary,
                             String payload, UUID actor) {
            auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                    null, ActorType.USER, actor, action, resource, resourceId, summary, payload, null, null));
        }
    }
}
