package com.openlab.qualitos.quality.circle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * L'acteur d'une proposition vient du jeton, pas du corps.
 *
 * <p>Ce banc existe pour un défaut réel : `proposedBy` et `validatedBy` étaient
 * lus de la requête. N'importe qui pouvait déposer au nom d'un autre, ou se
 * déclarer validateur de sa propre proposition — la garde « le validateur n'est
 * pas le proposeur » ne comparant alors que deux valeurs du même client.
 *
 * <p>Il compte double depuis la boîte à idées : les deux façades écrivent LES
 * MÊMES LIGNES, donc une attribution forgée s'afficherait sur le tableau des
 * idées comme un fait, sous le nom d'une personne réelle.
 */
@ExtendWith(MockitoExtension.class)
class CircleProposalActorTest {

    @Mock QualityCircleRepository circleRepo;
    @Mock CircleMemberRepository memberRepo;
    @Mock CircleMeetingRepository meetingRepo;
    @Mock CircleProposalRepository proposalRepo;
    @Mock AiGatewayClient aiGatewayClient;
    @Mock ObjectMapper objectMapper;
    @InjectMocks CircleService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID MOI = UUID.randomUUID();
    private static final UUID QUELQUUN = UUID.randomUUID();

    @BeforeEach
    void poserLeContexte() {
        TenantContext.setTenantId(TENANT.toString());
        connecter(MOI);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("le déposant est l'utilisateur connecté, la requête n'en parle plus")
    void deposantPrisAuJeton() {
        QualityCircle cercle = cercleActif();
        when(circleRepo.findByIdAndTenantId(cercle.getId(), TENANT)).thenReturn(Optional.of(cercle));
        when(proposalRepo.save(any())).thenAnswer(inv -> {
            CircleProposal p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });

        CircleDto.ProposalResponse rendue = service.addProposal(cercle.getId(),
                new CircleDto.ProposalRequest("Eclairage LED atelier", "Moins de rebuts", null));

        assertThat(rendue.proposedBy()).isEqualTo(MOI);
    }

    @Test
    @DisplayName("la proposition porte le tenant de son cercle")
    void tenantPrisAuCercle() {
        // Depuis la V125 la colonne est NOT NULL : l'oublier ferait échouer
        // toute création par la façade cercle, et seulement à l'insertion.
        QualityCircle cercle = cercleActif();
        when(circleRepo.findByIdAndTenantId(cercle.getId(), TENANT)).thenReturn(Optional.of(cercle));
        when(proposalRepo.save(any())).thenAnswer(inv -> {
            CircleProposal p = inv.getArgument(0);
            assertThat(p.getTenantId()).isEqualTo(TENANT);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });

        service.addProposal(cercle.getId(),
                new CircleDto.ProposalRequest("Bac de tri", null, null));
    }

    @Test
    @DisplayName("l'arbitre est l'utilisateur connecté")
    void arbitrePrisAuJeton() {
        QualityCircle cercle = cercleActif();
        CircleProposal proposition = propositionDe(cercle, QUELQUUN, ProposalStatus.UNDER_REVIEW);
        when(circleRepo.findByIdAndTenantId(cercle.getId(), TENANT)).thenReturn(Optional.of(cercle));
        when(proposalRepo.findByIdAndCircleId(proposition.getId(), cercle.getId()))
                .thenReturn(Optional.of(proposition));
        when(proposalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CircleDto.ProposalResponse rendue = service.approveProposal(
                cercle.getId(), proposition.getId(), new CircleDto.ApproveProposalRequest());

        assertThat(rendue.validatedBy()).isEqualTo(MOI);
    }

    @Test
    @DisplayName("nul ne valide sa propre proposition, et cela ne dépend plus de ce qu'il envoie")
    void arbitreNEstPasLeProposeur() {
        QualityCircle cercle = cercleActif();
        CircleProposal mienne = propositionDe(cercle, MOI, ProposalStatus.UNDER_REVIEW);
        when(circleRepo.findByIdAndTenantId(cercle.getId(), TENANT)).thenReturn(Optional.of(cercle));
        when(proposalRepo.findByIdAndCircleId(mienne.getId(), cercle.getId()))
                .thenReturn(Optional.of(mienne));

        assertThatThrownBy(() -> service.approveProposal(
                cercle.getId(), mienne.getId(), new CircleDto.ApproveProposalRequest()))
                .isInstanceOf(CircleStateException.class);
    }

    // ---------- fabriques ----------

    private static void connecter(UUID sub) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sub.toString(), "n/a", List.of()));
    }

    private static QualityCircle cercleActif() {
        QualityCircle c = new QualityCircle();
        c.setId(UUID.randomUUID());
        c.setTenantId(TENANT);
        c.setName("Atelier mecanique");
        c.setStatus(CircleStatus.ACTIVE);
        return c;
    }

    private static CircleProposal propositionDe(QualityCircle cercle, UUID auteur, ProposalStatus statut) {
        CircleProposal p = new CircleProposal();
        p.setId(UUID.randomUUID());
        p.setCircle(cercle);
        p.setTenantId(TENANT);
        p.setTitle("Eclairage LED atelier");
        p.setStatus(statut);
        p.setProposedBy(auteur);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }
}
