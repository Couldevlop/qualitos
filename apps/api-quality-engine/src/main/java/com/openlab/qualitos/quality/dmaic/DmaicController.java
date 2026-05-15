package com.openlab.qualitos.quality.dmaic;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dmaic")
public class DmaicController {

    private final DmaicService service;

    public DmaicController(DmaicService service) { this.service = service; }

    // ---- Projects ----

    @GetMapping("/projects")
    public Page<DmaicDto.ProjectResponse> listProjects(
            @RequestParam(required = false) DmaicStatus status,
            @RequestParam(required = false) DmaicPhase phase,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.listProjects(status, phase, pageable);
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public DmaicDto.ProjectResponse createProject(@Valid @RequestBody DmaicDto.CreateProjectRequest req) {
        return service.createProject(req);
    }

    @GetMapping("/projects/{id}")
    public DmaicDto.ProjectResponse getProject(@PathVariable UUID id) {
        return service.getProject(id);
    }

    @PatchMapping("/projects/{id}")
    public DmaicDto.ProjectResponse update(@PathVariable UUID id,
                                           @Valid @RequestBody DmaicDto.UpdateProjectRequest req) {
        return service.updateProject(id, req);
    }

    @PatchMapping("/projects/{id}/advance")
    public DmaicDto.ProjectResponse advance(@PathVariable UUID id) {
        return service.advancePhase(id);
    }

    @PatchMapping("/projects/{id}/hold")
    public DmaicDto.ProjectResponse hold(@PathVariable UUID id) { return service.hold(id); }

    @PatchMapping("/projects/{id}/resume")
    public DmaicDto.ProjectResponse resume(@PathVariable UUID id) { return service.resume(id); }

    @PatchMapping("/projects/{id}/cancel")
    public DmaicDto.ProjectResponse cancel(@PathVariable UUID id) { return service.cancel(id); }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteProject(id); }

    // ---- Measures ----

    @PostMapping("/projects/{id}/measures")
    @ResponseStatus(HttpStatus.CREATED)
    public DmaicDto.MeasureResponse addMeasure(@PathVariable UUID id,
                                               @Valid @RequestBody DmaicDto.AddMeasureRequest req) {
        return service.addMeasure(id, req);
    }

    @DeleteMapping("/projects/{id}/measures/{mid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeasure(@PathVariable UUID id, @PathVariable UUID mid) {
        service.deleteMeasure(id, mid);
    }

    @GetMapping("/projects/{id}/capability")
    public DmaicDto.CapabilityResponse capability(@PathVariable UUID id) {
        return service.computeCapability(id);
    }

    // ---- Poka-Yoke catalog ----

    @GetMapping("/pokayoke")
    public Page<DmaicDto.DeviceSummary> listDevices(
            @RequestParam(required = false) PokaYokeType type,
            @RequestParam(required = false) PokaYokeMechanism mechanism,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.listDevices(type, mechanism, pageable);
    }

    @GetMapping("/pokayoke/{id}")
    public DmaicDto.DeviceDetail getDevice(@PathVariable UUID id) {
        return service.getDevice(id);
    }

    @GetMapping("/pokayoke/by-code/{code}")
    public DmaicDto.DeviceDetail getDeviceByCode(@PathVariable String code) {
        return service.getDeviceByCode(code);
    }

    // ---- Assignments ----

    @PostMapping("/projects/{id}/pokayoke")
    @ResponseStatus(HttpStatus.CREATED)
    public DmaicDto.AssignmentResponse assign(@PathVariable UUID id,
                                              @Valid @RequestBody DmaicDto.AssignPokaYokeRequest req) {
        return service.assignDevice(id, req);
    }

    @PatchMapping("/projects/{id}/pokayoke/{aid}")
    public DmaicDto.AssignmentResponse updateAssignment(@PathVariable UUID id, @PathVariable UUID aid,
                                                       @RequestBody DmaicDto.UpdateAssignmentRequest req) {
        return service.updateAssignment(id, aid, req);
    }

    @DeleteMapping("/projects/{id}/pokayoke/{aid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable UUID id, @PathVariable UUID aid) {
        service.deleteAssignment(id, aid);
    }
}
