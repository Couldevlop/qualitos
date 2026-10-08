package com.openlab.qualitos.quality.authz.application;

import com.openlab.qualitos.quality.authz.domain.AuthzNotFoundException;
import com.openlab.qualitos.quality.authz.domain.AuthzValidationException;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.domain.TenantRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationServiceTest {

    static final UUID TENANT_A = UUID.randomUUID();
    static final UUID TENANT_B = UUID.randomUUID();
    static final UUID ADMIN = UUID.randomUUID();
    static final UUID MARIE = UUID.randomUUID();

    FakeRoles roles;
    FakeMembers members;
    FakeContext context;
    FakeAudit audit;
    MutableClock clock;
    AuthorizationService service;

    @BeforeEach
    void setUp() {
        roles = new FakeRoles();
        members = new FakeMembers();
        context = new FakeContext();
        audit = new FakeAudit();
        clock = new MutableClock(Instant.parse("2026-10-08T09:00:00Z"));
        service = new AuthorizationService(roles, members, context, audit, clock);
        context.tenant = TENANT_A;
        context.user = ADMIN;
        context.token = Set.of("ADMIN_TENANT");
    }

    @Test
    void unClientSansReglageALesDroitsLivres() {
        context.user = MARIE;
        context.token = Set.of("USER");

        assertThat(service.has(Permission.NC_CREATE)).isTrue();
        assertThat(service.has(Permission.CAPA_CREATE)).isFalse();
        AuthzDto.Me moi = service.me();
        assertThat(moi.userId()).isEqualTo(MARIE);
        assertThat(moi.roles()).containsExactly("USER");
        assertThat(moi.permissions()).contains("nc.create").doesNotContain("capa.create").isSorted();
    }

    @Test
    void unRoleAttribueDansLApplicationSAjouteAuJeton() {
        service.createRole(new AuthzDto.RoleCommand("PILOTE_SITE", "Pilote de site", null, List.of("capa.create")));
        service.replaceMemberRoles(MARIE, List.of("PILOTE_SITE"));

        context.user = MARIE;
        context.token = Set.of("USER");
        assertThat(service.has(Permission.CAPA_CREATE)).isTrue();
        assertThat(service.me().roles()).containsExactly("PILOTE_SITE", "USER");
    }

    @Test
    void regler_unRoleSystemeChangeLesDroitsDeTousSesPorteurs() {
        AuthzDto.RoleView regle = service.updateRole("USER",
                new AuthzDto.RoleCommand(null, "Opérateur", null, List.of("nc.photo")));

        assertThat(regle.customized()).isTrue();
        assertThat(regle.permissions()).containsExactly("nc.photo");
        context.user = MARIE;
        context.token = Set.of("USER");
        assertThat(service.has(Permission.NC_CREATE)).isFalse();
        assertThat(service.has(Permission.NC_PHOTO)).isTrue();
        assertThat(audit.events).contains("updated:USER");

        // Réinitialiser rend les droits livrés.
        context.user = ADMIN;
        context.token = Set.of("ADMIN_TENANT");
        service.deleteRole("USER");
        context.user = MARIE;
        context.token = Set.of("USER");
        assertThat(service.has(Permission.NC_CREATE)).isTrue();
    }

    @Test
    void laListeDesRolesDitCeQuiEstRegle() {
        service.updateRole("AUDITOR", new AuthzDto.RoleCommand(null, null, null, List.of("capa.verify")));
        List<AuthzDto.RoleView> liste = service.roles();

        assertThat(liste).extracting(AuthzDto.RoleView::code).doesNotContain("SUPER_ADMIN").first().isEqualTo("ADMIN_TENANT");
        assertThat(liste).filteredOn(r -> r.code().equals("AUDITOR")).singleElement()
                .satisfies(r -> assertThat(r.customized()).isTrue());
        assertThat(liste).filteredOn(r -> r.code().equals("USER")).singleElement()
                .satisfies(r -> assertThat(r.customized()).isFalse());
        assertThat(service.catalog()).extracting(AuthzDto.CatalogEntry::code).contains("authz.manage", "capa.create");
    }

    @Test
    void lesSaisiesInvalidesSontRefuseesSurLeBonChamp() {
        assertThatThrownBy(() -> service.createRole(null)).extracting("field").isEqualTo("name");
        assertThatThrownBy(() -> service.createRole(new AuthzDto.RoleCommand("PILOTE", "P", null, List.of("capa.voler"))))
                .extracting("field").isEqualTo("permissions");
        service.createRole(new AuthzDto.RoleCommand("PILOTE", "P", null, null));
        assertThatThrownBy(() -> service.createRole(new AuthzDto.RoleCommand("PILOTE", "P2", null, List.of())))
                .extracting("field").isEqualTo("code");
        assertThatThrownBy(() -> service.updateRole("ADMIN_TENANT", new AuthzDto.RoleCommand(null, null, null, List.of())))
                .extracting("field").isEqualTo("permissions");
        assertThatThrownBy(() -> service.updateRole("USER", null)).extracting("field").isEqualTo("permissions");
        assertThatThrownBy(() -> service.updateRole("SUPER_ADMIN", new AuthzDto.RoleCommand(null, null, null, List.of())))
                .isInstanceOf(AuthzNotFoundException.class);
        assertThatThrownBy(() -> service.deleteRole("INCONNU")).isInstanceOf(AuthzNotFoundException.class);
        assertThatThrownBy(() -> service.replaceMemberRoles(MARIE, List.of("SUPER_ADMIN")))
                .extracting("field").isEqualTo("roles");
        assertThatThrownBy(() -> service.replaceMemberRoles(MARIE, List.of("FANTOME")))
                .extracting("field").isEqualTo("roles");
        assertThatThrownBy(() -> service.replaceMemberRoles(null, List.of())).extracting("field").isEqualTo("userId");
    }

    @Test
    void supprimerUnRoleSurMesureLeRetireAuxMembres() {
        service.createRole(new AuthzDto.RoleCommand("PILOTE", "P", null, List.of("nc.close")));
        service.replaceMemberRoles(MARIE, List.of("PILOTE", "AUDITOR"));
        assertThat(service.members()).singleElement()
                .satisfies(m -> assertThat(m.roles()).containsExactly("AUDITOR", "PILOTE"));

        service.deleteRole("PILOTE");

        context.user = MARIE;
        context.token = Set.of();
        assertThat(service.has(Permission.NC_CLOSE)).isFalse();
        assertThat(audit.events).contains("created:PILOTE", "deleted:PILOTE", "member:" + MARIE);
    }

    @Test
    void unClientNeVoitNiNUtiliseLesRolesDUnAutre() {
        service.createRole(new AuthzDto.RoleCommand("PILOTE", "P", null, List.of("capa.create")));
        service.replaceMemberRoles(MARIE, List.of("PILOTE"));

        context.tenant = TENANT_B;
        assertThat(service.roles()).extracting(AuthzDto.RoleView::code).doesNotContain("PILOTE");
        assertThat(service.members()).isEmpty();
        context.user = MARIE;
        context.token = Set.of("USER");
        assertThat(service.has(Permission.CAPA_CREATE)).isFalse();
    }

    @Test
    void leCacheExpireEtSOublieAuChangement() {
        context.user = MARIE;
        context.token = Set.of("USER");
        assertThat(service.has(Permission.NC_CREATE)).isTrue();
        int lectures = roles.reads;

        // Une écriture en base qui ne passe pas par le service (autre nœud) :
        roles.store(TENANT_A, TenantRole.custom("AUTRE", "Autre", null, List.of()));
        roles.byTenant.get(TENANT_A).put("USER", TenantRole.defaultsOf(
                com.openlab.qualitos.quality.authz.domain.SystemRole.USER).withPermissions(List.of()));
        assertThat(service.has(Permission.NC_CREATE)).isTrue();
        assertThat(roles.reads).isEqualTo(lectures);

        clock.advance(AuthorizationService.TTL.plusSeconds(1));
        assertThat(service.has(Permission.NC_CREATE)).isFalse();
        assertThat(roles.reads).isEqualTo(lectures + 1);
    }

    @Test
    void sansUtilisateurSeulsLesRolesDuJetonComptent() {
        context.user = null;
        context.token = Set.of("QUALITY_MANAGER");
        assertThat(service.has(Permission.CAPA_CREATE)).isTrue();
        assertThat(service.me().userId()).isNull();
    }

    // ---------- doublures ----------

    static final class FakeRoles implements AuthzPorts.Roles {
        final Map<UUID, Map<String, TenantRole>> byTenant = new HashMap<>();
        int reads;

        void store(UUID tenant, TenantRole role) {
            byTenant.computeIfAbsent(tenant, k -> new LinkedHashMap<>()).put(role.code(), role);
        }

        @Override
        public List<TenantRole> findByTenant(UUID tenantId) {
            reads++;
            return new ArrayList<>(byTenant.getOrDefault(tenantId, Map.of()).values());
        }

        @Override
        public void save(UUID tenantId, TenantRole role, UUID actor) {
            store(tenantId, role);
        }

        @Override
        public void delete(UUID tenantId, String code) {
            Optional.ofNullable(byTenant.get(tenantId)).ifPresent(m -> m.remove(code));
            members.ifPresent(m -> m.dropRole(tenantId, code));
        }

        Optional<FakeMembers> members = Optional.empty();
    }

    final class FakeMembers implements AuthzPorts.Members {
        final Map<UUID, Map<UUID, Set<String>>> byTenant = new HashMap<>();

        FakeMembers() {
            roles.members = Optional.of(this);
        }

        void dropRole(UUID tenant, String code) {
            byTenant.getOrDefault(tenant, Map.of()).values().forEach(s -> s.remove(code));
        }

        @Override
        public Set<String> rolesOf(UUID tenantId, UUID userId) {
            return new TreeSet<>(byTenant.getOrDefault(tenantId, Map.of()).getOrDefault(userId, Set.of()));
        }

        @Override
        public Map<UUID, Set<String>> all(UUID tenantId) {
            return byTenant.getOrDefault(tenantId, Map.of());
        }

        @Override
        public void replace(UUID tenantId, UUID userId, Set<String> roleCodes, UUID actor) {
            byTenant.computeIfAbsent(tenantId, k -> new HashMap<>()).put(userId, new TreeSet<>(roleCodes));
        }
    }

    static final class FakeContext implements AuthzPorts.Context {
        UUID tenant;
        UUID user;
        Set<String> token = Set.of();

        @Override
        public UUID requireTenantId() {
            return tenant;
        }

        @Override
        public Optional<UUID> userId() {
            return Optional.ofNullable(user);
        }

        @Override
        public UUID requireActorId() {
            return user;
        }

        @Override
        public Set<String> tokenRoles() {
            return token;
        }
    }

    static final class FakeAudit implements AuthzPorts.Audit {
        final List<String> events = new ArrayList<>();

        @Override
        public void roleSaved(UUID tenantId, TenantRole role, boolean created, UUID actor) {
            events.add((created ? "created:" : "updated:") + role.code());
        }

        @Override
        public void roleDeleted(UUID tenantId, String code, UUID actor) {
            events.add("deleted:" + code);
        }

        @Override
        public void memberRolesReplaced(UUID tenantId, UUID userId, Set<String> before, Set<String> after,
                                        UUID actor) {
            events.add("member:" + userId);
        }
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(java.time.Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
