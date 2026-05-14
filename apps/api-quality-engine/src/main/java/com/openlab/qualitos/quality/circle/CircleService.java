package com.openlab.qualitos.quality.circle;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class CircleService {

    /** Bornes 5-10 membres recommandées par les méthodologies qualité (CLAUDE.md §3.3). */
    static final int MIN_RECOMMENDED_MEMBERS = 5;
    static final int MAX_MEMBERS = 10;

    private final QualityCircleRepository circleRepository;
    private final CircleMemberRepository memberRepository;
    private final CircleMeetingRepository meetingRepository;
    private final CircleProposalRepository proposalRepository;

    public CircleService(QualityCircleRepository circleRepository,
                         CircleMemberRepository memberRepository,
                         CircleMeetingRepository meetingRepository,
                         CircleProposalRepository proposalRepository) {
        this.circleRepository = circleRepository;
        this.memberRepository = memberRepository;
        this.meetingRepository = meetingRepository;
        this.proposalRepository = proposalRepository;
    }

    // ===== Circles =====

    @Transactional(readOnly = true)
    public Page<CircleDto.CircleResponse> findAll(CircleStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        Page<QualityCircle> page = status != null
                ? circleRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : circleRepository.findByTenantId(tenantId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CircleDto.CircleResponse findById(UUID id) {
        return toResponse(loadCircle(id));
    }

    public CircleDto.CircleResponse create(CircleDto.CreateCircleRequest req) {
        UUID tenantId = requireTenantId();
        QualityCircle c = new QualityCircle();
        c.setTenantId(tenantId);
        c.setName(req.name());
        c.setDescription(req.description());
        c.setTopic(req.topic());
        c.setStatus(CircleStatus.ACTIVE);
        return toResponse(circleRepository.save(c));
    }

    public CircleDto.CircleResponse update(UUID id, CircleDto.UpdateCircleRequest req) {
        QualityCircle c = loadCircle(id);
        if (c.getStatus() == CircleStatus.ARCHIVED) {
            throw new CircleStateException("Archived circle cannot be modified");
        }
        if (req.name() != null) c.setName(req.name());
        if (req.description() != null) c.setDescription(req.description());
        if (req.topic() != null) c.setTopic(req.topic());
        return toResponse(circleRepository.save(c));
    }

    public CircleDto.CircleResponse pause(UUID id) {
        QualityCircle c = loadCircle(id);
        if (c.getStatus() != CircleStatus.ACTIVE) {
            throw new CircleStateException("Only ACTIVE circles can be paused");
        }
        c.setStatus(CircleStatus.PAUSED);
        return toResponse(circleRepository.save(c));
    }

    public CircleDto.CircleResponse resume(UUID id) {
        QualityCircle c = loadCircle(id);
        if (c.getStatus() != CircleStatus.PAUSED) {
            throw new CircleStateException("Only PAUSED circles can be resumed");
        }
        c.setStatus(CircleStatus.ACTIVE);
        return toResponse(circleRepository.save(c));
    }

    public CircleDto.CircleResponse archive(UUID id) {
        QualityCircle c = loadCircle(id);
        if (c.getStatus() == CircleStatus.ARCHIVED) {
            throw new CircleStateException("Circle already archived");
        }
        c.setStatus(CircleStatus.ARCHIVED);
        return toResponse(circleRepository.save(c));
    }

    public void delete(UUID id) {
        QualityCircle c = loadCircle(id);
        if (c.getStatus() != CircleStatus.ARCHIVED) {
            throw new CircleStateException("Only ARCHIVED circles can be deleted");
        }
        circleRepository.delete(c);
    }

    // ===== Members =====

    public CircleDto.MemberResponse addMember(UUID circleId, CircleDto.AddMemberRequest req) {
        QualityCircle c = loadCircle(circleId);
        if (c.getStatus() == CircleStatus.ARCHIVED) {
            throw new CircleStateException("Cannot add a member to an archived circle");
        }
        if (c.getMembers().size() >= MAX_MEMBERS) {
            throw new CircleStateException("Circle already has the maximum of " + MAX_MEMBERS + " members");
        }
        if (memberRepository.existsByCircleIdAndUserId(circleId, req.userId())) {
            throw new CircleStateException("User is already a member of this circle");
        }
        CircleRole role = req.role() != null ? req.role() : CircleRole.MEMBER;
        if (role == CircleRole.FACILITATOR || role == CircleRole.SECRETARY) {
            checkSingletonRole(c, role, null);
        }
        CircleMember m = new CircleMember();
        m.setCircle(c);
        m.setUserId(req.userId());
        m.setRole(role);
        return toMemberResponse(memberRepository.save(m));
    }

    public CircleDto.MemberResponse updateMemberRole(UUID circleId, UUID memberId,
                                                    CircleDto.UpdateMemberRoleRequest req) {
        QualityCircle c = loadCircle(circleId);
        if (c.getStatus() == CircleStatus.ARCHIVED) {
            throw new CircleStateException("Cannot modify a member on an archived circle");
        }
        CircleMember m = memberRepository.findByIdAndCircleId(memberId, circleId)
                .orElseThrow(() -> new CircleMemberNotFoundException(memberId));
        if (req.role() == CircleRole.FACILITATOR || req.role() == CircleRole.SECRETARY) {
            checkSingletonRole(c, req.role(), m.getId());
        }
        m.setRole(req.role());
        return toMemberResponse(memberRepository.save(m));
    }

    public void removeMember(UUID circleId, UUID memberId) {
        QualityCircle c = loadCircle(circleId);
        if (c.getStatus() == CircleStatus.ARCHIVED) {
            throw new CircleStateException("Cannot remove a member from an archived circle");
        }
        CircleMember m = memberRepository.findByIdAndCircleId(memberId, circleId)
                .orElseThrow(() -> new CircleMemberNotFoundException(memberId));
        memberRepository.delete(m);
    }

    // ===== Meetings =====

    public CircleDto.MeetingResponse addMeeting(UUID circleId, CircleDto.MeetingRequest req) {
        QualityCircle c = loadCircle(circleId);
        if (c.getStatus() != CircleStatus.ACTIVE) {
            throw new CircleStateException("Meetings can only be scheduled on ACTIVE circles");
        }
        CircleMeeting m = new CircleMeeting();
        m.setCircle(c);
        m.setTitle(req.title());
        m.setAgenda(req.agenda());
        m.setScheduledAt(req.scheduledAt());
        m.setDurationMinutes(req.durationMinutes());
        m.setLocation(req.location());
        m.setStatus(MeetingStatus.PLANNED);
        return toMeetingResponse(meetingRepository.save(m));
    }

    public CircleDto.MeetingResponse updateMeeting(UUID circleId, UUID meetingId,
                                                  CircleDto.UpdateMeetingRequest req) {
        loadCircle(circleId);
        CircleMeeting m = loadMeeting(circleId, meetingId);
        if (m.getStatus() != MeetingStatus.PLANNED) {
            throw new CircleStateException("Only PLANNED meetings can be edited");
        }
        if (req.title() != null) m.setTitle(req.title());
        if (req.agenda() != null) m.setAgenda(req.agenda());
        if (req.scheduledAt() != null) m.setScheduledAt(req.scheduledAt());
        if (req.durationMinutes() != null) m.setDurationMinutes(req.durationMinutes());
        if (req.location() != null) m.setLocation(req.location());
        return toMeetingResponse(meetingRepository.save(m));
    }

    public CircleDto.MeetingResponse holdMeeting(UUID circleId, UUID meetingId,
                                                CircleDto.HoldMeetingRequest req) {
        loadCircle(circleId);
        CircleMeeting m = loadMeeting(circleId, meetingId);
        if (m.getStatus() != MeetingStatus.PLANNED) {
            throw new CircleStateException("Only PLANNED meetings can be marked as held");
        }
        m.setStatus(MeetingStatus.HELD);
        m.setHeldAt(Instant.now());
        if (req != null && req.minutes() != null) {
            m.setMinutes(req.minutes());
        }
        return toMeetingResponse(meetingRepository.save(m));
    }

    public CircleDto.MeetingResponse cancelMeeting(UUID circleId, UUID meetingId) {
        loadCircle(circleId);
        CircleMeeting m = loadMeeting(circleId, meetingId);
        if (m.getStatus() == MeetingStatus.HELD) {
            throw new CircleStateException("A held meeting cannot be cancelled");
        }
        if (m.getStatus() == MeetingStatus.CANCELLED) {
            throw new CircleStateException("Meeting already cancelled");
        }
        m.setStatus(MeetingStatus.CANCELLED);
        return toMeetingResponse(meetingRepository.save(m));
    }

    // ===== Proposals =====

    public CircleDto.ProposalResponse addProposal(UUID circleId, CircleDto.ProposalRequest req) {
        QualityCircle c = loadCircle(circleId);
        if (c.getStatus() != CircleStatus.ACTIVE) {
            throw new CircleStateException("Proposals can only be added to ACTIVE circles");
        }
        CircleProposal p = new CircleProposal();
        p.setCircle(c);
        p.setTitle(req.title());
        p.setDescription(req.description());
        p.setProposedBy(req.proposedBy());
        p.setStatus(ProposalStatus.PROPOSED);
        if (req.meetingId() != null) {
            CircleMeeting m = loadMeeting(circleId, req.meetingId());
            p.setMeeting(m);
        }
        return toProposalResponse(proposalRepository.save(p));
    }

    public CircleDto.ProposalResponse reviewProposal(UUID circleId, UUID proposalId) {
        loadCircle(circleId);
        CircleProposal p = loadProposal(circleId, proposalId);
        if (p.getStatus() != ProposalStatus.PROPOSED) {
            throw new CircleStateException("Only PROPOSED items can be moved to UNDER_REVIEW");
        }
        p.setStatus(ProposalStatus.UNDER_REVIEW);
        return toProposalResponse(proposalRepository.save(p));
    }

    public CircleDto.ProposalResponse approveProposal(UUID circleId, UUID proposalId,
                                                     CircleDto.ApproveProposalRequest req) {
        loadCircle(circleId);
        CircleProposal p = loadProposal(circleId, proposalId);
        if (p.getStatus() != ProposalStatus.UNDER_REVIEW) {
            throw new CircleStateException("Only UNDER_REVIEW proposals can be approved");
        }
        if (req.validatedBy().equals(p.getProposedBy())) {
            throw new CircleStateException("Validator cannot be the proposer");
        }
        p.setStatus(ProposalStatus.APPROVED);
        p.setValidatedBy(req.validatedBy());
        p.setValidatedAt(Instant.now());
        return toProposalResponse(proposalRepository.save(p));
    }

    public CircleDto.ProposalResponse rejectProposal(UUID circleId, UUID proposalId,
                                                    CircleDto.RejectProposalRequest req) {
        loadCircle(circleId);
        CircleProposal p = loadProposal(circleId, proposalId);
        if (p.getStatus() != ProposalStatus.PROPOSED && p.getStatus() != ProposalStatus.UNDER_REVIEW) {
            throw new CircleStateException("Only PROPOSED or UNDER_REVIEW proposals can be rejected");
        }
        p.setStatus(ProposalStatus.REJECTED);
        p.setValidatedBy(req.validatedBy());
        p.setValidatedAt(Instant.now());
        p.setRejectionReason(req.reason());
        return toProposalResponse(proposalRepository.save(p));
    }

    public CircleDto.ProposalResponse markImplemented(UUID circleId, UUID proposalId) {
        loadCircle(circleId);
        CircleProposal p = loadProposal(circleId, proposalId);
        if (p.getStatus() != ProposalStatus.APPROVED) {
            throw new CircleStateException("Only APPROVED proposals can be marked as IMPLEMENTED");
        }
        p.setStatus(ProposalStatus.IMPLEMENTED);
        p.setImplementedAt(Instant.now());
        return toProposalResponse(proposalRepository.save(p));
    }

    public CircleDto.ProposalResponse recordImpact(UUID circleId, UUID proposalId,
                                                  CircleDto.ImpactRequest req) {
        loadCircle(circleId);
        CircleProposal p = loadProposal(circleId, proposalId);
        if (p.getStatus() != ProposalStatus.IMPLEMENTED) {
            throw new CircleStateException("Only IMPLEMENTED proposals can record impact (status MEASURED)");
        }
        p.setStatus(ProposalStatus.MEASURED);
        p.setMeasuredAt(Instant.now());
        p.setImpactNote(req.impactNote());
        return toProposalResponse(proposalRepository.save(p));
    }

    public void deleteProposal(UUID circleId, UUID proposalId) {
        loadCircle(circleId);
        CircleProposal p = loadProposal(circleId, proposalId);
        if (p.getStatus() == ProposalStatus.IMPLEMENTED || p.getStatus() == ProposalStatus.MEASURED) {
            throw new CircleStateException(
                    "Implemented or measured proposals cannot be deleted (audit trail)");
        }
        proposalRepository.delete(p);
    }

    // ===== helpers =====

    private void checkSingletonRole(QualityCircle c, CircleRole role, UUID excludeMemberId) {
        boolean exists = c.getMembers().stream()
                .anyMatch(m -> m.getRole() == role
                        && (excludeMemberId == null || !m.getId().equals(excludeMemberId)));
        if (exists) {
            throw new CircleStateException(
                    "Role " + role + " is unique per circle and already assigned");
        }
    }

    private QualityCircle loadCircle(UUID id) {
        UUID tenantId = requireTenantId();
        return circleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CircleNotFoundException(id));
    }

    private CircleMeeting loadMeeting(UUID circleId, UUID meetingId) {
        return meetingRepository.findByIdAndCircleId(meetingId, circleId)
                .orElseThrow(() -> new CircleMeetingNotFoundException(meetingId));
    }

    private CircleProposal loadProposal(UUID circleId, UUID proposalId) {
        return proposalRepository.findByIdAndCircleId(proposalId, circleId)
                .orElseThrow(() -> new CircleProposalNotFoundException(proposalId));
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private CircleDto.CircleResponse toResponse(QualityCircle c) {
        return new CircleDto.CircleResponse(
                c.getId(), c.getTenantId(), c.getName(), c.getDescription(), c.getTopic(),
                c.getStatus(), c.getMembers().size(),
                c.getCreatedAt(), c.getUpdatedAt(),
                c.getMembers().stream().map(this::toMemberResponse).toList(),
                c.getMeetings().stream().map(this::toMeetingResponse).toList(),
                c.getProposals().stream().map(this::toProposalResponse).toList());
    }

    private CircleDto.MemberResponse toMemberResponse(CircleMember m) {
        return new CircleDto.MemberResponse(
                m.getId(), m.getCircle().getId(), m.getUserId(), m.getRole(), m.getJoinedAt());
    }

    private CircleDto.MeetingResponse toMeetingResponse(CircleMeeting m) {
        return new CircleDto.MeetingResponse(
                m.getId(), m.getCircle().getId(), m.getTitle(), m.getAgenda(),
                m.getScheduledAt(), m.getDurationMinutes(), m.getLocation(),
                m.getStatus(), m.getMinutes(), m.getHeldAt(),
                m.getCreatedAt(), m.getUpdatedAt());
    }

    private CircleDto.ProposalResponse toProposalResponse(CircleProposal p) {
        return new CircleDto.ProposalResponse(
                p.getId(), p.getCircle().getId(),
                p.getMeeting() != null ? p.getMeeting().getId() : null,
                p.getTitle(), p.getDescription(), p.getStatus(),
                p.getProposedBy(), p.getValidatedBy(), p.getValidatedAt(),
                p.getImplementedAt(), p.getMeasuredAt(),
                p.getImpactNote(), p.getRejectionReason(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
