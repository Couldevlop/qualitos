package com.openlab.qualitos.quality.circle;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/circles")
public class CircleController {

    private final CircleService service;

    public CircleController(CircleService service) { this.service = service; }

    @GetMapping
    public Page<CircleDto.CircleResponse> list(
            @RequestParam(required = false) CircleStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(status, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CircleDto.CircleResponse create(@Valid @RequestBody CircleDto.CreateCircleRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public CircleDto.CircleResponse get(@PathVariable UUID id) { return service.findById(id); }

    @PatchMapping("/{id}")
    public CircleDto.CircleResponse update(@PathVariable UUID id,
                                           @Valid @RequestBody CircleDto.UpdateCircleRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/pause")
    public CircleDto.CircleResponse pause(@PathVariable UUID id) { return service.pause(id); }

    @PatchMapping("/{id}/resume")
    public CircleDto.CircleResponse resume(@PathVariable UUID id) { return service.resume(id); }

    @PatchMapping("/{id}/archive")
    public CircleDto.CircleResponse archive(@PathVariable UUID id) { return service.archive(id); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }

    // members
    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public CircleDto.MemberResponse addMember(@PathVariable UUID id,
                                              @Valid @RequestBody CircleDto.AddMemberRequest req) {
        return service.addMember(id, req);
    }

    @PatchMapping("/{id}/members/{mid}")
    public CircleDto.MemberResponse updateRole(@PathVariable UUID id, @PathVariable UUID mid,
                                               @Valid @RequestBody CircleDto.UpdateMemberRoleRequest req) {
        return service.updateMemberRole(id, mid, req);
    }

    @DeleteMapping("/{id}/members/{mid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID id, @PathVariable UUID mid) {
        service.removeMember(id, mid);
    }

    // meetings
    @PostMapping("/{id}/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    public CircleDto.MeetingResponse addMeeting(@PathVariable UUID id,
                                                @Valid @RequestBody CircleDto.MeetingRequest req) {
        return service.addMeeting(id, req);
    }

    @PatchMapping("/{id}/meetings/{mid}")
    public CircleDto.MeetingResponse updateMeeting(@PathVariable UUID id, @PathVariable UUID mid,
                                                  @Valid @RequestBody CircleDto.UpdateMeetingRequest req) {
        return service.updateMeeting(id, mid, req);
    }

    @PatchMapping("/{id}/meetings/{mid}/hold")
    public CircleDto.MeetingResponse hold(@PathVariable UUID id, @PathVariable UUID mid,
                                          @RequestBody(required = false) CircleDto.HoldMeetingRequest req) {
        return service.holdMeeting(id, mid, req);
    }

    @PatchMapping("/{id}/meetings/{mid}/cancel")
    public CircleDto.MeetingResponse cancelMeeting(@PathVariable UUID id, @PathVariable UUID mid) {
        return service.cancelMeeting(id, mid);
    }

    /** ANO-010 — génère un compte-rendu structuré par LLM à partir d'un transcript textuel. */
    @PostMapping("/{id}/meetings/{mid}/minutes/generate")
    @ResponseStatus(HttpStatus.OK)
    public CircleDto.MeetingMinutes generateMinutes(@PathVariable UUID id, @PathVariable UUID mid,
                                                    @Valid @RequestBody CircleDto.GenerateMinutesRequest req) {
        return service.generateMinutes(id, mid, req);
    }

    // proposals
    @PostMapping("/{id}/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    public CircleDto.ProposalResponse addProposal(@PathVariable UUID id,
                                                  @Valid @RequestBody CircleDto.ProposalRequest req) {
        return service.addProposal(id, req);
    }

    @PatchMapping("/{id}/proposals/{pid}/review")
    public CircleDto.ProposalResponse reviewProposal(@PathVariable UUID id, @PathVariable UUID pid) {
        return service.reviewProposal(id, pid);
    }

    @PatchMapping("/{id}/proposals/{pid}/approve")
    public CircleDto.ProposalResponse approveProposal(@PathVariable UUID id, @PathVariable UUID pid,
                                                     @Valid @RequestBody CircleDto.ApproveProposalRequest req) {
        return service.approveProposal(id, pid, req);
    }

    @PatchMapping("/{id}/proposals/{pid}/reject")
    public CircleDto.ProposalResponse rejectProposal(@PathVariable UUID id, @PathVariable UUID pid,
                                                    @Valid @RequestBody CircleDto.RejectProposalRequest req) {
        return service.rejectProposal(id, pid, req);
    }

    @PatchMapping("/{id}/proposals/{pid}/implement")
    public CircleDto.ProposalResponse implementProposal(@PathVariable UUID id, @PathVariable UUID pid) {
        return service.markImplemented(id, pid);
    }

    @PatchMapping("/{id}/proposals/{pid}/impact")
    public CircleDto.ProposalResponse recordImpact(@PathVariable UUID id, @PathVariable UUID pid,
                                                  @Valid @RequestBody CircleDto.ImpactRequest req) {
        return service.recordImpact(id, pid, req);
    }

    @DeleteMapping("/{id}/proposals/{pid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProposal(@PathVariable UUID id, @PathVariable UUID pid) {
        service.deleteProposal(id, pid);
    }
}
