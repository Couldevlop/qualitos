package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.training.Skill;
import com.openlab.qualitos.quality.training.SkillRepository;
import com.openlab.qualitos.quality.training.competencymatrix.domain.LevelCell;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixOperator;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixSkill;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le contexte (client et acteur du jeton), la trace d'audit sans donnée
 * personnelle, et les gardes des adaptateurs qu'une base saine n'atteint pas.
 */
class CompetencyMatrixInfrastructureTest {

    final UUID tenant = UUID.randomUUID();
    final UUID acteur = UUID.randomUUID();

    @AfterEach
    void nettoyer() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------- contexte

    @Test
    void sansTenantNiJetonRienNePasse() {
        JwtCompetencyMatrixContext context = new JwtCompetencyMatrixContext();
        assertThatThrownBy(context::requireTenantId).isInstanceOf(MissingTenantContextException.class);
        assertThatThrownBy(context::requireActorId).isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void leTenantEtLActeurViennentDuJeton() {
        UUID sub = UUID.randomUUID();
        TenantContext.setTenantId(tenant.toString());
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub.toString())
                .issuedAt(Instant.EPOCH).expiresAt(Instant.EPOCH.plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        JwtCompetencyMatrixContext context = new JwtCompetencyMatrixContext();
        assertThat(context.requireTenantId()).isEqualTo(tenant);
        assertThat(context.requireActorId()).isEqualTo(sub);
    }

    // ---------------------------------------------------------------- audit

    @Test
    void chaqueEcritureLaisseSaTraceSansNom() {
        AuditEventService events = mock(AuditEventService.class);
        AuditLogCompetencyMatrixPublisher publisher = new AuditLogCompetencyMatrixPublisher(events);
        UUID personne = UUID.randomUUID();
        UUID formateur = UUID.randomUUID();
        UUID competence = UUID.randomUUID();
        MatrixOperator o = new MatrixOperator(UUID.randomUUID(), personne, "Martin Dubois", "Usinage", Instant.EPOCH);
        MatrixSkill s = new MatrixSkill(competence, "Tournage", "Usinage", formateur, "Karim Benali", Instant.EPOCH);

        publisher.operatorAdded(tenant, o, acteur);
        publisher.operatorRemoved(tenant, o, acteur);
        publisher.skillAdded(tenant, s, acteur);
        publisher.skillRemoved(tenant, s, acteur);
        publisher.levelSet(tenant, new LevelCell(personne, competence, 3), OptionalInt.of(1), acteur);
        publisher.levelSet(tenant, new LevelCell(personne, competence, 2), OptionalInt.empty(), acteur);
        publisher.workshopsChanged(tenant, List.of("A", "B"), acteur);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> traces =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(events, times(7)).recordForTenant(eq(tenant), traces.capture());
        List<AuditEventDto.RecordEventRequest> t = traces.getAllValues();
        assertThat(t).extracting(AuditEventDto.RecordEventRequest::action).containsExactly(
                "training.matrix.operator-added", "training.matrix.operator-removed",
                "training.matrix.skill-added", "training.matrix.skill-removed",
                "training.matrix.level-set", "training.matrix.level-set",
                "training.matrix.workshops-updated");
        assertThat(t).allSatisfy(r -> {
            assertThat(r.actorType()).isEqualTo(ActorType.USER);
            assertThat(r.actorUserId()).isEqualTo(acteur);
            assertThat(r.payloadJson()).doesNotContain("Martin").doesNotContain("Karim");
            assertThat(r.resourceType()).matches("^[a-z][a-z0-9_-]{1,63}$");
            assertThat(r.action()).matches("^[a-z][a-z0-9._-]{1,99}$");
        });
        assertThat(t.get(0).resourceId()).isEqualTo(o.id());
        assertThat(t.get(0).payloadJson()).contains(personne.toString());
        assertThat(t.get(2).payloadJson()).contains(formateur.toString());
        assertThat(t.get(4).payloadJson()).contains("\"previousLevel\":1,\"level\":3");
        assertThat(t.get(5).payloadJson()).contains("\"previousLevel\":null,\"level\":2");
        assertThat(t.get(6).payloadJson()).isEqualTo("{\"count\":2}");
        assertThat(t.get(6).resourceId()).isEqualTo(tenant);
    }

    // ---------------------------------------------------------------- gardes

    @Test
    void uneCompetenceDisparueDuCatalogueNEntrePasEnColonne() {
        SkillRepository catalogue = mock(SkillRepository.class);
        MatrixSkillJpaRepository jpa = mock(MatrixSkillJpaRepository.class);
        UUID id = UUID.randomUUID();
        when(catalogue.findById(id)).thenReturn(Optional.empty());

        CompetencyMatrixAdapters.Skills skills = new CompetencyMatrixAdapters.Skills(jpa, catalogue);

        assertThatThrownBy(() -> skills.add(tenant, id, UUID.randomUUID(), "X", acteur, Instant.EPOCH))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void laRechercheDUnCodeLibreSArrete() {
        SkillRepository catalogue = mock(SkillRepository.class);
        when(catalogue.existsByTenantIdAndCode(eq(tenant), anyString())).thenReturn(true);
        CompetencyMatrixAdapters.Skills skills =
                new CompetencyMatrixAdapters.Skills(mock(MatrixSkillJpaRepository.class), catalogue);

        assertThatThrownBy(() -> skills.createCatalogueSkill(tenant, "Tournage", "Usinage"))
                .isInstanceOf(IllegalStateException.class);
        verify(catalogue, times(CompetencyMatrixAdapters.MAX_CODE_ATTEMPTS))
                .existsByTenantIdAndCode(eq(tenant), anyString());
        verify(catalogue, times(0)).save(any(Skill.class));
    }
}
