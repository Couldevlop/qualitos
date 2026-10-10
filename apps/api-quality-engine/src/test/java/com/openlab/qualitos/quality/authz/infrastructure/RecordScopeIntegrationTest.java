package com.openlab.qualitos.quality.authz.infrastructure;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.application.AuthzDto;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.domain.SystemRole;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaNotFoundException;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NcDto;
import com.openlab.qualitos.quality.nonconformity.NcNotFoundException;
import com.openlab.qualitos.quality.nonconformity.NcService;
import com.openlab.qualitos.quality.nonconformity.NcSeverity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * « Qui voit quoi » sur les vrais services, les vraies requêtes et les vrais
 * droits (ADR 0081) : le client retire à l'utilisateur « voir toutes les NC » et
 * « voir tous les dossiers CAPA » ; il ne voit plus que ce qui le concerne, en
 * liste comme en fiche, et le reste répond comme s'il n'existait pas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class RecordScopeIntegrationTest {

    @Autowired AuthorizationService authorization;
    @Autowired NcService ncs;
    @Autowired CapaService capas;
    @Autowired TransactionTemplate tx;

    final UUID tenant = UUID.randomUUID();
    final UUID admin = UUID.randomUUID();
    final UUID marie = UUID.randomUUID();
    final UUID paul = UUID.randomUUID();

    <T> T dans(Supplier<T> f) {
        return tx.execute(s -> f.get());
    }

    void connecte(UUID user, String... roles) {
        TenantContext.setTenantId(tenant.toString());
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(user.toString())
                .claim("tenant_id", tenant.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList(), user.toString()));
    }

    /** L'utilisateur garde ses droits livrés, moins « voir tout » des NC et des CAPA. */
    @BeforeEach
    void restreindreLUtilisateur() {
        connecte(admin, "ADMIN_TENANT");
        EnumSet<Permission> droits = EnumSet.noneOf(Permission.class);
        for (Permission p : Permission.values()) {
            if (p.grantedByDefaultTo(SystemRole.USER)) droits.add(p);
        }
        droits.remove(Permission.NC_VIEW_ALL);
        droits.remove(Permission.CAPA_VIEW_ALL);
        dans(() -> authorization.updateRole("USER", new AuthzDto.RoleCommand(null, null, null,
                droits.stream().map(Permission::code).toList())));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    UUID declarer(String titre) {
        return dans(() -> ncs.create(new NcDto.CreateRequest(titre, "Constat", NcCategory.PROCESS, NcSeverity.MINOR,
                Instant.now(), null, null, null, null, null, null, null, null))).id();
    }

    UUID dossier(String titre, UUID pilote) {
        return dans(() -> capas.createCase(new CapaDto.CreateCaseRequest(titre, null, CapaType.CORRECTIVE,
                CapaCriticity.MEDIUM, CapaSourceType.INTERNAL, null, pilote, null, null))).id();
    }

    @Test
    void lUtilisateurNeVoitQueLesNcQuIlADeclarees() {
        connecte(marie, "USER");
        UUID sienne = declarer("Fuite atelier B");
        connecte(paul, "QUALITY_MANAGER");
        UUID autre = declarer("Écart fournisseur");

        connecte(marie, "USER");
        assertThat(dans(() -> ncs.findAll(null, null, null, null, null, PageRequest.of(0, 50))).getContent())
                .extracting(NcDto.Response::id).containsExactly(sienne);
        assertThat(dans(() -> ncs.findById(sienne)).id()).isEqualTo(sienne);
        // Ce qu'on ne voit pas n'existe pas : ni lecture ni action.
        assertThatThrownBy(() -> dans(() -> ncs.findById(autre))).isInstanceOf(NcNotFoundException.class);
        assertThatThrownBy(() -> dans(() -> ncs.close(autre))).isInstanceOf(NcNotFoundException.class);
        // Les tuiles comptent le même périmètre que le tableau.
        assertThat(dans(() -> ncs.statistics(null)).total()).isEqualTo(1);

        // Le manager garde « voir tout ».
        connecte(paul, "QUALITY_MANAGER");
        assertThat(dans(() -> ncs.findAll(null, null, null, null, null, PageRequest.of(0, 50))).getContent())
                .extracting(NcDto.Response::id).contains(sienne, autre);
        assertThat(dans(() -> ncs.statistics(null)).total()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void lUtilisateurNeVoitQueLesDossiersQuiLeConcernent() {
        connecte(paul, "QUALITY_MANAGER");
        UUID dePaul = dossier("Dossier de Paul", paul);
        UUID pilote = dossier("Piloté par Marie", marie);
        UUID avecAction = dossier("Une action pour Marie", paul);
        dans(() -> capas.addAction(avecAction, new CapaDto.ActionRequest("Remplacer le joint", null, null, null,
                marie, "Marie", null, null)));

        connecte(marie, "USER");
        assertThat(dans(() -> capas.findAll(null, PageRequest.of(0, 50))).getContent())
                .extracting(CapaDto.CaseResponse::id).containsExactlyInAnyOrder(pilote, avecAction);
        // Le filtre de statut se combine à la portée, et la pagination compte juste.
        assertThat(dans(() -> capas.findAll(CapaStatus.OPEN, PageRequest.of(0, 1))).getTotalElements()).isEqualTo(2);
        assertThat(dans(() -> capas.findAll(CapaStatus.CLOSED, PageRequest.of(0, 50))).getContent()).isEmpty();
        assertThat(dans(() -> capas.findById(avecAction)).id()).isEqualTo(avecAction);
        assertThatThrownBy(() -> dans(() -> capas.findById(dePaul))).isInstanceOf(CapaNotFoundException.class);

        connecte(paul, "QUALITY_MANAGER");
        assertThat(dans(() -> capas.findAll(null, PageRequest.of(0, 50))).getContent())
                .extracting(CapaDto.CaseResponse::id).contains(dePaul, pilote, avecAction);
    }

    @Test
    void sansUtilisateurNiVoirToutRienNEstRendu() {
        connecte(paul, "QUALITY_MANAGER");
        declarer("Écart visible des seuls managers");
        // Un compte de service qui ne porte que le rôle utilisateur et aucun sujet.
        TenantContext.setTenantId(tenant.toString());
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "service", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(dans(() -> ncs.findAll(null, null, null, null, null, PageRequest.of(0, 50))).getContent())
                .isEmpty();
    }
}
