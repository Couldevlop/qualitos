package com.openlab.qualitos.quality.circle;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CircleServiceTest {

    @Mock QualityCircleRepository circleRepo;
    @Mock CircleMemberRepository memberRepo;
    @Mock CircleMeetingRepository meetingRepo;
    @Mock CircleProposalRepository proposalRepo;
    @InjectMocks CircleService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // --- create / list / find ---
    @Test
    void create_success() {
        when(circleRepo.save(any())).thenAnswer(inv -> {
            QualityCircle c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now()); c.setUpdatedAt(Instant.now());
            return c;
        });
        CircleDto.CircleResponse r = service.create(new CircleDto.CreateCircleRequest(
                "Cercle ligne 3", "desc", "production"));
        assertThat(r.status()).isEqualTo(CircleStatus.ACTIVE);
        assertThat(r.tenantId()).isEqualTo(TENANT);
    }

    @Test
    void create_missingTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.create(new CircleDto.CreateCircleRequest("c", null, null)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void findAll_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(circleRepo.findByTenantId(TENANT, p))
                .thenReturn(new PageImpl<>(List.of(circle(CircleStatus.ACTIVE))));
        Page<CircleDto.CircleResponse> r = service.findAll(null, p);
        assertThat(r.getContent()).hasSize(1);
    }

    @Test
    void findAll_withFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(circleRepo.findByTenantIdAndStatus(TENANT, CircleStatus.ARCHIVED, p))
                .thenReturn(new PageImpl<>(List.of(circle(CircleStatus.ARCHIVED))));
        Page<CircleDto.CircleResponse> r = service.findAll(CircleStatus.ARCHIVED, p);
        assertThat(r.getContent().get(0).status()).isEqualTo(CircleStatus.ARCHIVED);
    }

    @Test
    void findById_notFound() {
        UUID id = UUID.randomUUID();
        when(circleRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(CircleNotFoundException.class);
    }

    // --- update / state transitions ---
    @Test
    void update_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(circleRepo.save(c)).thenReturn(c);
        service.update(c.getId(), new CircleDto.UpdateCircleRequest("Nom2", "d2", "t2"));
        assertThat(c.getName()).isEqualTo("Nom2");
        assertThat(c.getTopic()).isEqualTo("t2");
    }

    @Test
    void update_archived_throws() {
        QualityCircle c = circle(CircleStatus.ARCHIVED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.update(c.getId(),
                new CircleDto.UpdateCircleRequest("x", null, null)))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void pause_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(circleRepo.save(c)).thenReturn(c);
        service.pause(c.getId());
        assertThat(c.getStatus()).isEqualTo(CircleStatus.PAUSED);
    }

    @Test
    void pause_notActive_throws() {
        QualityCircle c = circle(CircleStatus.PAUSED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.pause(c.getId())).isInstanceOf(CircleStateException.class);
    }

    @Test
    void resume_success() {
        QualityCircle c = circle(CircleStatus.PAUSED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(circleRepo.save(c)).thenReturn(c);
        service.resume(c.getId());
        assertThat(c.getStatus()).isEqualTo(CircleStatus.ACTIVE);
    }

    @Test
    void resume_notPaused_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.resume(c.getId())).isInstanceOf(CircleStateException.class);
    }

    @Test
    void archive_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(circleRepo.save(c)).thenReturn(c);
        service.archive(c.getId());
        assertThat(c.getStatus()).isEqualTo(CircleStatus.ARCHIVED);
    }

    @Test
    void archive_alreadyArchived_throws() {
        QualityCircle c = circle(CircleStatus.ARCHIVED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.archive(c.getId())).isInstanceOf(CircleStateException.class);
    }

    @Test
    void delete_archived_success() {
        QualityCircle c = circle(CircleStatus.ARCHIVED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        service.delete(c.getId());
        verify(circleRepo).delete(c);
    }

    @Test
    void delete_notArchived_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.delete(c.getId())).isInstanceOf(CircleStateException.class);
    }

    // --- members ---
    @Test
    void addMember_defaultRole() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(memberRepo.existsByCircleIdAndUserId(c.getId(), USER)).thenReturn(false);
        when(memberRepo.save(any())).thenAnswer(inv -> {
            CircleMember m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setJoinedAt(Instant.now());
            return m;
        });
        CircleDto.MemberResponse r = service.addMember(c.getId(),
                new CircleDto.AddMemberRequest(USER, null));
        assertThat(r.role()).isEqualTo(CircleRole.MEMBER);
    }

    @Test
    void addMember_facilitator_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(memberRepo.existsByCircleIdAndUserId(c.getId(), USER)).thenReturn(false);
        when(memberRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.addMember(c.getId(), new CircleDto.AddMemberRequest(USER, CircleRole.FACILITATOR));
    }

    @Test
    void addMember_archivedCircle_throws() {
        QualityCircle c = circle(CircleStatus.ARCHIVED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.addMember(c.getId(),
                new CircleDto.AddMemberRequest(USER, null)))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void addMember_alreadyMember_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(memberRepo.existsByCircleIdAndUserId(c.getId(), USER)).thenReturn(true);
        assertThatThrownBy(() -> service.addMember(c.getId(),
                new CircleDto.AddMemberRequest(USER, null)))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void addMember_maxMembers_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        for (int i = 0; i < 10; i++) {
            c.getMembers().add(member(c, UUID.randomUUID(), CircleRole.MEMBER));
        }
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.addMember(c.getId(),
                new CircleDto.AddMemberRequest(USER, null)))
                .isInstanceOf(CircleStateException.class)
                .hasMessageContaining("maximum");
    }

    @Test
    void addMember_duplicateFacilitator_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        c.getMembers().add(member(c, OTHER, CircleRole.FACILITATOR));
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(memberRepo.existsByCircleIdAndUserId(c.getId(), USER)).thenReturn(false);
        assertThatThrownBy(() -> service.addMember(c.getId(),
                new CircleDto.AddMemberRequest(USER, CircleRole.FACILITATOR)))
                .isInstanceOf(CircleStateException.class)
                .hasMessageContaining("unique");
    }

    @Test
    void updateMemberRole_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMember m = member(c, USER, CircleRole.MEMBER);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(memberRepo.findByIdAndCircleId(m.getId(), c.getId())).thenReturn(Optional.of(m));
        when(memberRepo.save(m)).thenReturn(m);
        service.updateMemberRole(c.getId(), m.getId(),
                new CircleDto.UpdateMemberRoleRequest(CircleRole.SECRETARY));
        assertThat(m.getRole()).isEqualTo(CircleRole.SECRETARY);
    }

    @Test
    void updateMemberRole_promoteToExistingSingleton_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMember target = member(c, USER, CircleRole.MEMBER);
        c.getMembers().add(member(c, OTHER, CircleRole.FACILITATOR));
        c.getMembers().add(target);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(memberRepo.findByIdAndCircleId(target.getId(), c.getId())).thenReturn(Optional.of(target));
        assertThatThrownBy(() -> service.updateMemberRole(c.getId(), target.getId(),
                new CircleDto.UpdateMemberRoleRequest(CircleRole.FACILITATOR)))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void updateMemberRole_promoteSelf_success() {
        // Quand on rebascule un membre déjà FACILITATOR vers FACILITATOR : pas de conflit.
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMember target = member(c, USER, CircleRole.FACILITATOR);
        c.getMembers().add(target);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(memberRepo.findByIdAndCircleId(target.getId(), c.getId())).thenReturn(Optional.of(target));
        when(memberRepo.save(target)).thenReturn(target);
        service.updateMemberRole(c.getId(), target.getId(),
                new CircleDto.UpdateMemberRoleRequest(CircleRole.FACILITATOR));
        assertThat(target.getRole()).isEqualTo(CircleRole.FACILITATOR);
    }

    @Test
    void updateMemberRole_notFound_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        UUID mid = UUID.randomUUID();
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(memberRepo.findByIdAndCircleId(mid, c.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateMemberRole(c.getId(), mid,
                new CircleDto.UpdateMemberRoleRequest(CircleRole.SECRETARY)))
                .isInstanceOf(CircleMemberNotFoundException.class);
    }

    @Test
    void removeMember_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMember m = member(c, USER, CircleRole.MEMBER);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(memberRepo.findByIdAndCircleId(m.getId(), c.getId())).thenReturn(Optional.of(m));
        service.removeMember(c.getId(), m.getId());
        verify(memberRepo).delete(m);
    }

    @Test
    void removeMember_archivedCircle_throws() {
        QualityCircle c = circle(CircleStatus.ARCHIVED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.removeMember(c.getId(), UUID.randomUUID()))
                .isInstanceOf(CircleStateException.class);
    }

    // --- meetings ---
    @Test
    void addMeeting_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(meetingRepo.save(any())).thenAnswer(inv -> {
            CircleMeeting m = inv.getArgument(0);
            m.setId(UUID.randomUUID()); m.setCreatedAt(Instant.now()); m.setUpdatedAt(Instant.now());
            return m;
        });
        CircleDto.MeetingResponse r = service.addMeeting(c.getId(),
                new CircleDto.MeetingRequest("Réunion mensuelle", "agenda", Instant.now().plusSeconds(86400),
                        60, "https://jitsi.example.com/room"));
        assertThat(r.status()).isEqualTo(MeetingStatus.PLANNED);
    }

    @Test
    void addMeeting_inactiveCircle_throws() {
        QualityCircle c = circle(CircleStatus.PAUSED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.addMeeting(c.getId(),
                new CircleDto.MeetingRequest("x", null, Instant.now(), null, null)))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void updateMeeting_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMeeting m = meeting(c, MeetingStatus.PLANNED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(meetingRepo.findByIdAndCircleId(m.getId(), c.getId())).thenReturn(Optional.of(m));
        when(meetingRepo.save(m)).thenReturn(m);
        Instant later = Instant.now().plusSeconds(7200);
        service.updateMeeting(c.getId(), m.getId(),
                new CircleDto.UpdateMeetingRequest("Nouveau", "ag", later, 90, "salle 2"));
        assertThat(m.getTitle()).isEqualTo("Nouveau");
        assertThat(m.getScheduledAt()).isEqualTo(later);
        assertThat(m.getDurationMinutes()).isEqualTo(90);
    }

    @Test
    void updateMeeting_notPlanned_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMeeting m = meeting(c, MeetingStatus.HELD);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(meetingRepo.findByIdAndCircleId(m.getId(), c.getId())).thenReturn(Optional.of(m));
        assertThatThrownBy(() -> service.updateMeeting(c.getId(), m.getId(),
                new CircleDto.UpdateMeetingRequest("x", null, null, null, null)))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void holdMeeting_success_withMinutes() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMeeting m = meeting(c, MeetingStatus.PLANNED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(meetingRepo.findByIdAndCircleId(m.getId(), c.getId())).thenReturn(Optional.of(m));
        when(meetingRepo.save(m)).thenReturn(m);
        service.holdMeeting(c.getId(), m.getId(), new CircleDto.HoldMeetingRequest("PV..."));
        assertThat(m.getStatus()).isEqualTo(MeetingStatus.HELD);
        assertThat(m.getMinutes()).isEqualTo("PV...");
        assertThat(m.getHeldAt()).isNotNull();
    }

    @Test
    void holdMeeting_nullRequest_ok() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMeeting m = meeting(c, MeetingStatus.PLANNED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(meetingRepo.findByIdAndCircleId(m.getId(), c.getId())).thenReturn(Optional.of(m));
        when(meetingRepo.save(m)).thenReturn(m);
        service.holdMeeting(c.getId(), m.getId(), null);
        assertThat(m.getStatus()).isEqualTo(MeetingStatus.HELD);
        assertThat(m.getMinutes()).isNull();
    }

    @Test
    void holdMeeting_notPlanned_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMeeting m = meeting(c, MeetingStatus.CANCELLED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(meetingRepo.findByIdAndCircleId(m.getId(), c.getId())).thenReturn(Optional.of(m));
        assertThatThrownBy(() -> service.holdMeeting(c.getId(), m.getId(), null))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void cancelMeeting_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMeeting m = meeting(c, MeetingStatus.PLANNED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(meetingRepo.findByIdAndCircleId(m.getId(), c.getId())).thenReturn(Optional.of(m));
        when(meetingRepo.save(m)).thenReturn(m);
        service.cancelMeeting(c.getId(), m.getId());
        assertThat(m.getStatus()).isEqualTo(MeetingStatus.CANCELLED);
    }

    @Test
    void cancelMeeting_held_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMeeting m = meeting(c, MeetingStatus.HELD);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(meetingRepo.findByIdAndCircleId(m.getId(), c.getId())).thenReturn(Optional.of(m));
        assertThatThrownBy(() -> service.cancelMeeting(c.getId(), m.getId()))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void cancelMeeting_alreadyCancelled_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMeeting m = meeting(c, MeetingStatus.CANCELLED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(meetingRepo.findByIdAndCircleId(m.getId(), c.getId())).thenReturn(Optional.of(m));
        assertThatThrownBy(() -> service.cancelMeeting(c.getId(), m.getId()))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void meeting_notFound_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        UUID mid = UUID.randomUUID();
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(meetingRepo.findByIdAndCircleId(mid, c.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancelMeeting(c.getId(), mid))
                .isInstanceOf(CircleMeetingNotFoundException.class);
    }

    // --- proposals ---
    @Test
    void addProposal_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.save(any())).thenAnswer(inv -> {
            CircleProposal p = inv.getArgument(0);
            p.setId(UUID.randomUUID()); p.setCreatedAt(Instant.now()); p.setUpdatedAt(Instant.now());
            return p;
        });
        CircleDto.ProposalResponse r = service.addProposal(c.getId(),
                new CircleDto.ProposalRequest("Idée", "desc", USER, null));
        assertThat(r.status()).isEqualTo(ProposalStatus.PROPOSED);
    }

    @Test
    void addProposal_withMeeting_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleMeeting m = meeting(c, MeetingStatus.PLANNED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(meetingRepo.findByIdAndCircleId(m.getId(), c.getId())).thenReturn(Optional.of(m));
        when(proposalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CircleDto.ProposalResponse r = service.addProposal(c.getId(),
                new CircleDto.ProposalRequest("Idée", null, USER, m.getId()));
        assertThat(r.meetingId()).isEqualTo(m.getId());
    }

    @Test
    void addProposal_inactiveCircle_throws() {
        QualityCircle c = circle(CircleStatus.PAUSED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.addProposal(c.getId(),
                new CircleDto.ProposalRequest("x", null, USER, null)))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void reviewProposal_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.PROPOSED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        when(proposalRepo.save(p)).thenReturn(p);
        service.reviewProposal(c.getId(), p.getId());
        assertThat(p.getStatus()).isEqualTo(ProposalStatus.UNDER_REVIEW);
    }

    @Test
    void reviewProposal_notProposed_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.APPROVED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.reviewProposal(c.getId(), p.getId()))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void approveProposal_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.UNDER_REVIEW);
        p.setProposedBy(USER);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        when(proposalRepo.save(p)).thenReturn(p);
        service.approveProposal(c.getId(), p.getId(), new CircleDto.ApproveProposalRequest(OTHER));
        assertThat(p.getStatus()).isEqualTo(ProposalStatus.APPROVED);
        assertThat(p.getValidatedBy()).isEqualTo(OTHER);
        assertThat(p.getValidatedAt()).isNotNull();
    }

    @Test
    void approveProposal_selfApproval_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.UNDER_REVIEW);
        p.setProposedBy(USER);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.approveProposal(c.getId(), p.getId(),
                new CircleDto.ApproveProposalRequest(USER)))
                .isInstanceOf(CircleStateException.class)
                .hasMessageContaining("cannot be the proposer");
    }

    @Test
    void approveProposal_notUnderReview_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.PROPOSED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.approveProposal(c.getId(), p.getId(),
                new CircleDto.ApproveProposalRequest(OTHER)))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void rejectProposal_fromProposed_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.PROPOSED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        when(proposalRepo.save(p)).thenReturn(p);
        service.rejectProposal(c.getId(), p.getId(),
                new CircleDto.RejectProposalRequest(OTHER, "hors scope"));
        assertThat(p.getStatus()).isEqualTo(ProposalStatus.REJECTED);
        assertThat(p.getRejectionReason()).isEqualTo("hors scope");
    }

    @Test
    void rejectProposal_approved_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.APPROVED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.rejectProposal(c.getId(), p.getId(),
                new CircleDto.RejectProposalRequest(OTHER, "r")))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void markImplemented_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.APPROVED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        when(proposalRepo.save(p)).thenReturn(p);
        service.markImplemented(c.getId(), p.getId());
        assertThat(p.getStatus()).isEqualTo(ProposalStatus.IMPLEMENTED);
        assertThat(p.getImplementedAt()).isNotNull();
    }

    @Test
    void markImplemented_notApproved_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.PROPOSED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.markImplemented(c.getId(), p.getId()))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void recordImpact_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.IMPLEMENTED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        when(proposalRepo.save(p)).thenReturn(p);
        service.recordImpact(c.getId(), p.getId(), new CircleDto.ImpactRequest("-15% defauts"));
        assertThat(p.getStatus()).isEqualTo(ProposalStatus.MEASURED);
        assertThat(p.getImpactNote()).isEqualTo("-15% defauts");
        assertThat(p.getMeasuredAt()).isNotNull();
    }

    @Test
    void recordImpact_notImplemented_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.APPROVED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.recordImpact(c.getId(), p.getId(),
                new CircleDto.ImpactRequest("x")))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void deleteProposal_proposed_success() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.PROPOSED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        service.deleteProposal(c.getId(), p.getId());
        verify(proposalRepo).delete(p);
    }

    @Test
    void deleteProposal_implemented_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        CircleProposal p = proposal(c, ProposalStatus.IMPLEMENTED);
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(p.getId(), c.getId())).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.deleteProposal(c.getId(), p.getId()))
                .isInstanceOf(CircleStateException.class);
    }

    @Test
    void deleteProposal_notFound_throws() {
        QualityCircle c = circle(CircleStatus.ACTIVE);
        UUID pid = UUID.randomUUID();
        when(circleRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(proposalRepo.findByIdAndCircleId(pid, c.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteProposal(c.getId(), pid))
                .isInstanceOf(CircleProposalNotFoundException.class);
    }

    // --- helpers ---
    private QualityCircle circle(CircleStatus status) {
        QualityCircle c = new QualityCircle();
        c.setId(UUID.randomUUID());
        c.setTenantId(TENANT);
        c.setName("Cercle");
        c.setStatus(status);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    private CircleMember member(QualityCircle c, UUID userId, CircleRole role) {
        CircleMember m = new CircleMember();
        m.setId(UUID.randomUUID());
        m.setCircle(c);
        m.setUserId(userId);
        m.setRole(role);
        m.setJoinedAt(Instant.now());
        return m;
    }

    private CircleMeeting meeting(QualityCircle c, MeetingStatus status) {
        CircleMeeting m = new CircleMeeting();
        m.setId(UUID.randomUUID());
        m.setCircle(c);
        m.setTitle("M");
        m.setScheduledAt(Instant.now());
        m.setStatus(status);
        m.setCreatedAt(Instant.now());
        m.setUpdatedAt(Instant.now());
        return m;
    }

    private CircleProposal proposal(QualityCircle c, ProposalStatus status) {
        CircleProposal p = new CircleProposal();
        p.setId(UUID.randomUUID());
        p.setCircle(c);
        p.setTitle("Idée");
        p.setStatus(status);
        p.setProposedBy(USER);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }
}
