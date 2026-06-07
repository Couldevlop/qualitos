package com.openlab.qualitos.quality.circle;

import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Golden-master du chemin doré Cercle de Qualité.
 *
 * Verrouille le scénario nominal complet:
 *   create (ACTIVE) → addMember → addMeeting (PLANNED) → addProposal (PROPOSED)
 *     → reviewProposal (UNDER_REVIEW) → approveProposal (APPROVED)
 *     → markImplemented (IMPLEMENTED) → recordImpact (MEASURED)
 *   → pause (PAUSED) → resume (ACTIVE) → archive (ARCHIVED).
 *
 * Invariants de référence (régression):
 *   - création toujours en ACTIVE ;
 *   - cycle de vie de proposition: PROPOSED→UNDER_REVIEW→APPROVED→IMPLEMENTED→MEASURED ;
 *   - l'approbateur ne peut pas être le proposeur (séparation des rôles) ;
 *   - cycle d'état du cercle: ACTIVE↔PAUSED puis ARCHIVED terminal ;
 *   - resume impossible quand le cercle n'est pas en PAUSE (409 métier).
 *
 * Pattern: slice service Mockito (cohérent avec CircleServiceTest, sans Docker / Spring context).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cercle de Qualité — Golden Path")
class CircleGoldenPathTest {

    @Mock QualityCircleRepository circleRepo;
    @Mock CircleMemberRepository memberRepo;
    @Mock CircleMeetingRepository meetingRepo;
    @Mock CircleProposalRepository proposalRepo;
    @InjectMocks CircleService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID PROPOSER = UUID.randomUUID();
    private static final UUID APPROVER = UUID.randomUUID();

    @BeforeEach
    void ctx() {
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void clr() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create → member → meeting → proposition (cycle complet) → pause/resume/archive")
    void goldenPath_fullCircleLifecycle() {
        // --- 1. CREATE : toujours ACTIVE ---
        QualityCircle circle = new QualityCircle();
        when(circleRepo.save(any(QualityCircle.class))).thenAnswer(inv -> {
            QualityCircle c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
                c.setCreatedAt(Instant.now());
            }
            c.setUpdatedAt(Instant.now());
            return c;
        });

        CircleDto.CircleResponse created = service.create(
                new CircleDto.CreateCircleRequest("Cercle ligne 3", "amélioration continue", "production"));
        assertThat(created.status()).isEqualTo(CircleStatus.ACTIVE);
        assertThat(created.tenantId()).isEqualTo(TENANT);

        // Récupère l'instance persistée pour la filer dans tout le flux.
        // (le service crée une nouvelle entité ; on rebâtit une instance ACTIVE stable)
        circle.setId(created.id());
        circle.setTenantId(TENANT);
        circle.setName("Cercle ligne 3");
        circle.setStatus(CircleStatus.ACTIVE);
        circle.setCreatedAt(Instant.now());
        circle.setUpdatedAt(Instant.now());
        when(circleRepo.findByIdAndTenantId(circle.getId(), TENANT)).thenReturn(Optional.of(circle));

        // --- 2. ADD MEMBER (rôle par défaut = MEMBER) ---
        when(memberRepo.existsByCircleIdAndUserId(circle.getId(), PROPOSER)).thenReturn(false);
        when(memberRepo.save(any(CircleMember.class))).thenAnswer(inv -> {
            CircleMember m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setJoinedAt(Instant.now());
            return m;
        });
        CircleDto.MemberResponse member = service.addMember(circle.getId(),
                new CircleDto.AddMemberRequest(PROPOSER, null));
        assertThat(member.role()).isEqualTo(CircleRole.MEMBER);

        // --- 3. ADD MEETING : PLANNED ---
        when(meetingRepo.save(any(CircleMeeting.class))).thenAnswer(inv -> {
            CircleMeeting m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            m.setUpdatedAt(Instant.now());
            return m;
        });
        CircleDto.MeetingResponse meeting = service.addMeeting(circle.getId(),
                new CircleDto.MeetingRequest("Réunion mensuelle", "agenda",
                        Instant.now().plusSeconds(86400), 60, "https://jitsi.example.com/room"));
        assertThat(meeting.status()).isEqualTo(MeetingStatus.PLANNED);

        // --- 4. CYCLE DE VIE DE PROPOSITION ---
        CircleProposal proposal = new CircleProposal();
        when(proposalRepo.save(any(CircleProposal.class))).thenAnswer(inv -> {
            CircleProposal p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
                p.setCreatedAt(Instant.now());
            }
            p.setUpdatedAt(Instant.now());
            return p;
        });

        CircleDto.ProposalResponse proposed = service.addProposal(circle.getId(),
                new CircleDto.ProposalRequest("Réorganiser le poste", "desc", PROPOSER, null));
        assertThat(proposed.status()).isEqualTo(ProposalStatus.PROPOSED);

        // Reconstitue une proposition stable filée dans la suite du flux.
        proposal.setId(proposed.id());
        proposal.setCircle(circle);
        proposal.setTitle("Réorganiser le poste");
        proposal.setProposedBy(PROPOSER);
        proposal.setStatus(ProposalStatus.PROPOSED);
        proposal.setCreatedAt(Instant.now());
        proposal.setUpdatedAt(Instant.now());
        when(proposalRepo.findByIdAndCircleId(proposal.getId(), circle.getId()))
                .thenReturn(Optional.of(proposal));

        assertThat(service.reviewProposal(circle.getId(), proposal.getId()).status())
                .isEqualTo(ProposalStatus.UNDER_REVIEW);

        // L'approbateur DOIT différer du proposeur (séparation des rôles).
        CircleDto.ProposalResponse approved = service.approveProposal(circle.getId(), proposal.getId(),
                new CircleDto.ApproveProposalRequest(APPROVER));
        assertThat(approved.status()).isEqualTo(ProposalStatus.APPROVED);
        assertThat(proposal.getValidatedBy()).isEqualTo(APPROVER);

        assertThat(service.markImplemented(circle.getId(), proposal.getId()).status())
                .isEqualTo(ProposalStatus.IMPLEMENTED);
        assertThat(proposal.getImplementedAt()).isNotNull();

        CircleDto.ProposalResponse measured = service.recordImpact(circle.getId(), proposal.getId(),
                new CircleDto.ImpactRequest("-15% défauts poste"));
        assertThat(measured.status()).isEqualTo(ProposalStatus.MEASURED);
        assertThat(proposal.getMeasuredAt()).isNotNull();

        // --- 5. CYCLE D'ÉTAT DU CERCLE : ACTIVE → PAUSED → ACTIVE → ARCHIVED ---
        when(circleRepo.save(circle)).thenReturn(circle);
        assertThat(service.pause(circle.getId()).status()).isEqualTo(CircleStatus.PAUSED);
        assertThat(service.resume(circle.getId()).status()).isEqualTo(CircleStatus.ACTIVE);
        assertThat(service.archive(circle.getId()).status()).isEqualTo(CircleStatus.ARCHIVED);
    }

    @Test
    @DisplayName("invariant — resume impossible si le cercle n'est pas en PAUSE (409 métier)")
    void goldenPath_resumeRequiresPaused() {
        QualityCircle circle = new QualityCircle();
        circle.setId(UUID.randomUUID());
        circle.setTenantId(TENANT);
        circle.setName("Cercle");
        circle.setStatus(CircleStatus.ACTIVE);
        circle.setCreatedAt(Instant.now());
        circle.setUpdatedAt(Instant.now());
        when(circleRepo.findByIdAndTenantId(circle.getId(), TENANT)).thenReturn(Optional.of(circle));

        assertThatThrownBy(() -> service.resume(circle.getId()))
                .isInstanceOf(CircleStateException.class);
        assertThat(circle.getStatus()).isEqualTo(CircleStatus.ACTIVE);
    }
}
