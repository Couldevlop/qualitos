package com.openlab.qualitos.quality.authz.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEventRepository;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.application.AuthzDto;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.domain.SystemRole;
import com.openlab.qualitos.quality.authz.domain.TenantRole;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les droits sur le mapping JPA réel : collection d'actions d'un rôle, clé
 * composée des attributions, suppressions ciblées, cloisonnement par client.
 * Un dépôt simulé rendrait l'objet donné et ne dirait rien d'une collection
 * mal rechargée ou d'une suppression qui déborde sur un autre client.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class AuthzPersistenceIntegrationTest {

    @Autowired AuthzRoleRepository roleRepository;
    @Autowired AuthzMemberRoleRepository memberRepository;
    @Autowired AuditEventService auditEvents;
    @Autowired AuditEventRepository auditRepository;
    @Autowired TransactionTemplate tx;

    final UUID tenantA = UUID.randomUUID();
    final UUID tenantB = UUID.randomUUID();
    final UUID admin = UUID.randomUUID();
    final UUID marie = UUID.randomUUID();

    /** Une lecture dans sa propre transaction, typée — comme un appel de contrôleur. */
    <T> T lire(java.util.function.Supplier<T> f) {
        return tx.execute(s -> f.get());
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    void connecte(UUID tenant, UUID user, String... roles) {
        TenantContext.setTenantId(tenant.toString());
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(user.toString())
                .claim("tenant_id", tenant.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                java.util.Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList(),
                user.toString()));
    }

    @Test
    void unRoleEtSesActionsSeRelisent() {
        AuthzAdapters.Roles roles = new AuthzAdapters.Roles(roleRepository, memberRepository, Clock.systemUTC());
        tx.executeWithoutResult(s -> roles.save(tenantA, TenantRole.custom("PILOTE", "Pilote", "Site A",
                List.of(Permission.NC_CLOSE, Permission.CAPA_CREATE)), admin));
        tx.executeWithoutResult(s -> roles.save(tenantA, TenantRole.system(SystemRole.USER, "Opérateur", null,
                List.of(Permission.NC_PHOTO)), admin));

        List<TenantRole> relus = lire(() -> roles.findByTenant(tenantA));
        assertThat(relus).extracting(TenantRole::code).containsExactlyInAnyOrder("PILOTE", "USER");
        assertThat(relus).filteredOn(r -> r.code().equals("PILOTE")).singleElement().satisfies(r -> {
            assertThat(r.permissions()).containsExactlyInAnyOrder(Permission.NC_CLOSE, Permission.CAPA_CREATE);
            assertThat(r.description()).isEqualTo("Site A");
            assertThat(r.system()).isFalse();
        });
        assertThat(lire(() -> roles.findByTenant(tenantB))).isEmpty();

        // Réenregistrer remplace les actions, ne les cumule pas.
        tx.executeWithoutResult(s -> roles.save(tenantA, TenantRole.custom("PILOTE", "Pilote", null,
                List.of(Permission.NC_EDIT)), admin));
        assertThat(lire(() -> roles.findByTenant(tenantA))).filteredOn(r -> r.code().equals("PILOTE"))
                .singleElement().satisfies(r -> assertThat(r.permissions()).containsExactly(Permission.NC_EDIT));
    }

    @Test
    void lesAttributionsSeRemplacentEtSeSuppriment_dansLeSeulClientVise() {
        AuthzAdapters.Members members = new AuthzAdapters.Members(memberRepository, Clock.systemUTC());
        AuthzAdapters.Roles roles = new AuthzAdapters.Roles(roleRepository, memberRepository, Clock.systemUTC());
        tx.executeWithoutResult(s -> members.replace(tenantA, marie, Set.of("PILOTE", "AUDITOR"), admin));
        tx.executeWithoutResult(s -> members.replace(tenantB, marie, Set.of("PILOTE"), admin));
        tx.executeWithoutResult(s -> members.replace(tenantA, marie, Set.of("PILOTE", "USER"), admin));

        assertThat(lire(() -> members.rolesOf(tenantA, marie))).containsExactly("PILOTE", "USER");
        assertThat(lire(() -> members.all(tenantA))).containsOnlyKeys(marie);

        // Supprimer le rôle sur mesure « PILOTE » du client A ne touche pas au client B.
        tx.executeWithoutResult(s -> roles.save(tenantA, TenantRole.custom("PILOTE", "P", null, List.of()), admin));
        tx.executeWithoutResult(s -> roles.delete(tenantA, "PILOTE"));
        assertThat(lire(() -> members.rolesOf(tenantA, marie))).containsExactly("USER");
        assertThat(lire(() -> members.rolesOf(tenantB, marie))).containsExactly("PILOTE");

        // Réinitialiser un rôle SYSTÈME garde ses attributions.
        tx.executeWithoutResult(s -> roles.delete(tenantA, "USER"));
        assertThat(lire(() -> members.rolesOf(tenantA, marie))).containsExactly("USER");
    }

    @Test
    void leServiceDeBoutEnBoutAvecLeJetonEtLeJournal() {
        AuthorizationService service = new AuthorizationService(
                new AuthzAdapters.Roles(roleRepository, memberRepository, Clock.systemUTC()),
                new AuthzAdapters.Members(memberRepository, Clock.systemUTC()),
                new AuthzAdapters.JwtContext(), new AuthzAdapters.Audit(auditEvents), Clock.systemUTC());

        connecte(tenantA, admin, "ADMIN_TENANT");
        tx.executeWithoutResult(s -> service.createRole(new AuthzDto.RoleCommand("PILOTE", "Pilote", null,
                List.of("capa.create"))));
        tx.executeWithoutResult(s -> service.replaceMemberRoles(marie, List.of("PILOTE")));

        connecte(tenantA, marie, "USER");
        assertThat(lire(() -> service.has(Permission.CAPA_CREATE))).isTrue();
        assertThat(lire(() -> service.me().roles())).containsExactly("PILOTE", "USER");

        connecte(tenantB, marie, "USER");
        assertThat(lire(() -> service.has(Permission.CAPA_CREATE))).isFalse();

        assertThat(auditRepository.findByTenantIdOrderBySequenceNoDesc(tenantA, PageRequest.of(0, 10)).getContent())
                .extracting("action").contains("authz.role.created", "authz.member.roles-replaced");
    }

    @Test
    void uneLigneIncoherenteRetombeSurLesDefauts() {
        AuthzRoleJpaEntity e = new AuthzRoleJpaEntity();
        e.setTenantId(tenantA);
        e.setCode("ADMIN_TENANT");
        e.setSystemRole(true);
        e.setUpdatedBy(admin);
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        // L'administrateur privé du droit d'administrer : le domaine le refuserait.
        e.getPermissions().add("nc.create");
        e.getPermissions().add("action.retiree.du.catalogue");
        assertThat(AuthzAdapters.Roles.toDomain(e)).isEmpty();

        e.getPermissions().add("authz.manage");
        assertThat(AuthzAdapters.Roles.toDomain(e)).hasValueSatisfying(r ->
                assertThat(r.permissions()).containsExactlyInAnyOrder(Permission.NC_CREATE, Permission.AUTHZ_MANAGE));

        e.setSystemRole(false);
        assertThat(AuthzAdapters.Roles.toDomain(e)).isEmpty();
    }
}
