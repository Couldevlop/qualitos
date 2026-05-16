package com.openlab.qualitos.quality.dmaic;

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
class DmaicServiceTest {

    @Mock DmaicProjectRepository projectRepo;
    @Mock ProcessMeasureRepository measureRepo;
    @Mock PokaYokeDeviceRepository deviceRepo;
    @Mock PokaYokeAssignmentRepository assignmentRepo;
    @Mock CapabilityCalculator calculator;
    @InjectMocks DmaicService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID BB = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // --- create ---
    @Test
    void create_success_defaultsToDefineActive() {
        DmaicDto.CreateProjectRequest req = new DmaicDto.CreateProjectRequest(
                "Reduire defauts soudure", "desc", "goal", BB, null, null,
                10.0, 20.0, 15.0, "mm", 50000.0);
        when(projectRepo.save(any())).thenAnswer(inv -> {
            DmaicProject p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now()); p.setUpdatedAt(Instant.now());
            return p;
        });
        DmaicDto.ProjectResponse r = service.createProject(req);
        assertThat(r.phase()).isEqualTo(DmaicPhase.DEFINE);
        assertThat(r.status()).isEqualTo(DmaicStatus.ACTIVE);
        assertThat(r.tenantId()).isEqualTo(TENANT);
    }

    @Test
    void create_missingTenant_throws() {
        TenantContext.clear();
        DmaicDto.CreateProjectRequest req = new DmaicDto.CreateProjectRequest(
                "x", null, null, BB, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> service.createProject(req))
                .isInstanceOf(MissingTenantContextException.class);
    }

    // --- list ---
    @Test
    void listProjects_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(projectRepo.findByTenantId(TENANT, p))
                .thenReturn(new PageImpl<>(List.of(project(DmaicStatus.ACTIVE, DmaicPhase.DEFINE))));
        Page<DmaicDto.ProjectResponse> r = service.listProjects(null, null, p);
        assertThat(r.getContent()).hasSize(1);
    }

    @Test
    void listProjects_byStatus() {
        Pageable p = PageRequest.of(0, 10);
        when(projectRepo.findByTenantIdAndStatus(TENANT, DmaicStatus.COMPLETED, p))
                .thenReturn(new PageImpl<>(List.of(project(DmaicStatus.COMPLETED, DmaicPhase.CONTROL))));
        Page<DmaicDto.ProjectResponse> r = service.listProjects(DmaicStatus.COMPLETED, null, p);
        assertThat(r.getContent().get(0).status()).isEqualTo(DmaicStatus.COMPLETED);
    }

    @Test
    void listProjects_byPhase() {
        Pageable p = PageRequest.of(0, 10);
        when(projectRepo.findByTenantIdAndPhase(TENANT, DmaicPhase.IMPROVE, p))
                .thenReturn(new PageImpl<>(List.of(project(DmaicStatus.ACTIVE, DmaicPhase.IMPROVE))));
        service.listProjects(null, DmaicPhase.IMPROVE, p);
        verify(projectRepo).findByTenantIdAndPhase(TENANT, DmaicPhase.IMPROVE, p);
    }

    // --- get ---
    @Test
    void getProject_found() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.DEFINE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThat(service.getProject(p.getId()).id()).isEqualTo(p.getId());
    }

    @Test
    void getProject_notFound() {
        UUID id = UUID.randomUUID();
        when(projectRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getProject(id))
                .isInstanceOf(DmaicProjectNotFoundException.class);
    }

    // --- update ---
    @Test
    void update_success() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.MEASURE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(projectRepo.save(p)).thenReturn(p);
        service.updateProject(p.getId(), new DmaicDto.UpdateProjectRequest(
                "new", "pb2", "goal2", BB, null, null, 5.0, 25.0, 15.0, "mm", 60000.0));
        assertThat(p.getTitle()).isEqualTo("new");
        assertThat(p.getSpecLowerLimit()).isEqualTo(5.0);
        assertThat(p.getSpecUpperLimit()).isEqualTo(25.0);
        assertThat(p.getEstimatedSavingsEur()).isEqualTo(60000.0);
    }

    @Test
    void update_completed_throws() {
        DmaicProject p = project(DmaicStatus.COMPLETED, DmaicPhase.CONTROL);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.updateProject(p.getId(),
                new DmaicDto.UpdateProjectRequest("x", null, null, null, null, null,
                        null, null, null, null, null)))
                .isInstanceOf(DmaicStateException.class);
    }

    // --- advance phase ---
    @Test
    void advance_define_to_measure() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.DEFINE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(projectRepo.save(p)).thenReturn(p);
        service.advancePhase(p.getId());
        assertThat(p.getPhase()).isEqualTo(DmaicPhase.MEASURE);
    }

    @Test
    void advance_control_completes() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.CONTROL);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(projectRepo.save(p)).thenReturn(p);
        service.advancePhase(p.getId());
        assertThat(p.getStatus()).isEqualTo(DmaicStatus.COMPLETED);
        assertThat(p.getCompletedAt()).isNotNull();
    }

    @Test
    void advance_notActive_throws() {
        DmaicProject p = project(DmaicStatus.ON_HOLD, DmaicPhase.MEASURE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.advancePhase(p.getId()))
                .isInstanceOf(DmaicStateException.class);
    }

    // --- hold/resume/cancel ---
    @Test
    void hold_active_success() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.MEASURE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(projectRepo.save(p)).thenReturn(p);
        service.hold(p.getId());
        assertThat(p.getStatus()).isEqualTo(DmaicStatus.ON_HOLD);
    }

    @Test
    void hold_notActive_throws() {
        DmaicProject p = project(DmaicStatus.ON_HOLD, DmaicPhase.MEASURE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.hold(p.getId()))
                .isInstanceOf(DmaicStateException.class);
    }

    @Test
    void resume_onHold_success() {
        DmaicProject p = project(DmaicStatus.ON_HOLD, DmaicPhase.MEASURE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(projectRepo.save(p)).thenReturn(p);
        service.resume(p.getId());
        assertThat(p.getStatus()).isEqualTo(DmaicStatus.ACTIVE);
    }

    @Test
    void resume_notOnHold_throws() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.MEASURE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.resume(p.getId()))
                .isInstanceOf(DmaicStateException.class);
    }

    @Test
    void cancel_active_success() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.MEASURE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(projectRepo.save(p)).thenReturn(p);
        service.cancel(p.getId());
        assertThat(p.getStatus()).isEqualTo(DmaicStatus.CANCELLED);
    }

    @Test
    void cancel_completed_throws() {
        DmaicProject p = project(DmaicStatus.COMPLETED, DmaicPhase.CONTROL);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.cancel(p.getId()))
                .isInstanceOf(DmaicStateException.class);
    }

    @Test
    void cancel_alreadyCancelled_throws() {
        DmaicProject p = project(DmaicStatus.CANCELLED, DmaicPhase.MEASURE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.cancel(p.getId()))
                .isInstanceOf(DmaicStateException.class);
    }

    @Test
    void deleteProject_active_success() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.DEFINE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        service.deleteProject(p.getId());
        verify(projectRepo).delete(p);
    }

    @Test
    void deleteProject_completed_throws() {
        DmaicProject p = project(DmaicStatus.COMPLETED, DmaicPhase.CONTROL);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.deleteProject(p.getId()))
                .isInstanceOf(DmaicStateException.class);
    }

    // --- measures ---
    @Test
    void addMeasure_success() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.MEASURE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(measureRepo.save(any())).thenAnswer(inv -> {
            ProcessMeasure m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            return m;
        });
        DmaicDto.MeasureResponse r = service.addMeasure(p.getId(),
                new DmaicDto.AddMeasureRequest(15.4, "G1", "lot-2026-001", null, null, "ok"));
        assertThat(r.value()).isEqualTo(15.4);
        assertThat(r.recordedAt()).isNotNull();
    }

    @Test
    void addMeasure_notActive_throws() {
        DmaicProject p = project(DmaicStatus.COMPLETED, DmaicPhase.CONTROL);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.addMeasure(p.getId(),
                new DmaicDto.AddMeasureRequest(1.0, null, null, null, null, null)))
                .isInstanceOf(DmaicStateException.class);
    }

    @Test
    void deleteMeasure_success() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.MEASURE);
        ProcessMeasure m = measure(p, 15.0);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(measureRepo.findByIdAndProjectId(m.getId(), p.getId())).thenReturn(Optional.of(m));
        service.deleteMeasure(p.getId(), m.getId());
        verify(measureRepo).delete(m);
    }

    @Test
    void deleteMeasure_notFound_throws() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.MEASURE);
        UUID mid = UUID.randomUUID();
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(measureRepo.findByIdAndProjectId(mid, p.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteMeasure(p.getId(), mid))
                .isInstanceOf(ProcessMeasureNotFoundException.class);
    }

    @Test
    void computeCapability_delegates() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.MEASURE);
        p.setSpecLowerLimit(10.0); p.setSpecUpperLimit(20.0); p.setSpecTarget(15.0);
        ProcessMeasure m1 = measure(p, 15.0); ProcessMeasure m2 = measure(p, 16.0);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(measureRepo.findByProjectIdOrderByRecordedAtAsc(p.getId())).thenReturn(List.of(m1, m2));
        DmaicDto.CapabilityResponse expected = new DmaicDto.CapabilityResponse(
                2, 15.5, 0.7, 15.0, 16.0, 10.0, 20.0, 15.0,
                1.0, 1.0, 1.0, 1.0, 3.0, "ok", List.of());
        when(calculator.compute(List.of(15.0, 16.0), 10.0, 20.0, 15.0)).thenReturn(expected);

        DmaicDto.CapabilityResponse r = service.computeCapability(p.getId());
        assertThat(r).isSameAs(expected);
    }

    // --- devices ---
    @Test
    void listDevices_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(deviceRepo.findAll(p)).thenReturn(new PageImpl<>(List.of(device("DEV-1", PokaYokeType.PREVENTION))));
        Page<DmaicDto.DeviceSummary> r = service.listDevices(null, null, p);
        assertThat(r.getContent()).hasSize(1);
    }

    @Test
    void listDevices_byType() {
        Pageable p = PageRequest.of(0, 10);
        when(deviceRepo.findByType(PokaYokeType.DETECTION, p))
                .thenReturn(new PageImpl<>(List.of(device("DEV-DET", PokaYokeType.DETECTION))));
        service.listDevices(PokaYokeType.DETECTION, null, p);
        verify(deviceRepo).findByType(PokaYokeType.DETECTION, p);
    }

    @Test
    void listDevices_byMechanism() {
        Pageable p = PageRequest.of(0, 10);
        when(deviceRepo.findByMechanism(PokaYokeMechanism.SENSOR, p))
                .thenReturn(new PageImpl<>(List.of(device("DEV-SENSOR", PokaYokeType.DETECTION))));
        service.listDevices(null, PokaYokeMechanism.SENSOR, p);
        verify(deviceRepo).findByMechanism(PokaYokeMechanism.SENSOR, p);
    }

    @Test
    void getDevice_byId() {
        PokaYokeDevice d = device("X", PokaYokeType.PREVENTION);
        when(deviceRepo.findById(d.getId())).thenReturn(Optional.of(d));
        assertThat(service.getDevice(d.getId()).id()).isEqualTo(d.getId());
    }

    @Test
    void getDevice_notFound() {
        UUID id = UUID.randomUUID();
        when(deviceRepo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDevice(id))
                .isInstanceOf(PokaYokeDeviceNotFoundException.class);
    }

    @Test
    void getDeviceByCode_found() {
        PokaYokeDevice d = device("PY-ABC", PokaYokeType.PREVENTION);
        when(deviceRepo.findByCode("PY-ABC")).thenReturn(Optional.of(d));
        assertThat(service.getDeviceByCode("PY-ABC").code()).isEqualTo("PY-ABC");
    }

    @Test
    void getDeviceByCode_notFound() {
        when(deviceRepo.findByCode("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDeviceByCode("nope"))
                .isInstanceOf(PokaYokeDeviceNotFoundException.class);
    }

    // --- assignments ---
    @Test
    void assign_success() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.IMPROVE);
        PokaYokeDevice d = device("PY-1", PokaYokeType.PREVENTION);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(deviceRepo.findById(d.getId())).thenReturn(Optional.of(d));
        when(assignmentRepo.save(any())).thenAnswer(inv -> {
            PokaYokeAssignment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            a.setCreatedAt(Instant.now()); a.setUpdatedAt(Instant.now());
            return a;
        });
        DmaicDto.AssignmentResponse r = service.assignDevice(p.getId(),
                new DmaicDto.AssignPokaYokeRequest(d.getId(), "candidat"));
        assertThat(r.status()).isEqualTo(PokaYokeAssignmentStatus.PROPOSED);
        assertThat(r.deviceCode()).isEqualTo("PY-1");
    }

    @Test
    void assign_inactiveProject_throws() {
        DmaicProject p = project(DmaicStatus.ON_HOLD, DmaicPhase.IMPROVE);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.assignDevice(p.getId(),
                new DmaicDto.AssignPokaYokeRequest(UUID.randomUUID(), null)))
                .isInstanceOf(DmaicStateException.class);
    }

    @Test
    void assign_deviceNotFound_throws() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.IMPROVE);
        UUID did = UUID.randomUUID();
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(deviceRepo.findById(did)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.assignDevice(p.getId(),
                new DmaicDto.AssignPokaYokeRequest(did, null)))
                .isInstanceOf(PokaYokeDeviceNotFoundException.class);
    }

    @Test
    void updateAssignment_validTransition_setsTimestamp() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.IMPROVE);
        PokaYokeAssignment a = assignment(p, PokaYokeAssignmentStatus.IN_DESIGN);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(assignmentRepo.findByIdAndProjectId(a.getId(), p.getId())).thenReturn(Optional.of(a));
        when(assignmentRepo.save(a)).thenReturn(a);
        service.updateAssignment(p.getId(), a.getId(),
                new DmaicDto.UpdateAssignmentRequest(PokaYokeAssignmentStatus.IMPLEMENTED, "ok", null));
        assertThat(a.getStatus()).isEqualTo(PokaYokeAssignmentStatus.IMPLEMENTED);
        assertThat(a.getImplementedAt()).isNotNull();
    }

    @Test
    void updateAssignment_invalidTransition_throws() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.IMPROVE);
        PokaYokeAssignment a = assignment(p, PokaYokeAssignmentStatus.PROPOSED);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(assignmentRepo.findByIdAndProjectId(a.getId(), p.getId())).thenReturn(Optional.of(a));
        // PROPOSED → VERIFIED interdit
        assertThatThrownBy(() -> service.updateAssignment(p.getId(), a.getId(),
                new DmaicDto.UpdateAssignmentRequest(PokaYokeAssignmentStatus.VERIFIED, null, null)))
                .isInstanceOf(DmaicStateException.class);
    }

    @Test
    void updateAssignment_verified_setsVerifiedAt() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.CONTROL);
        PokaYokeAssignment a = assignment(p, PokaYokeAssignmentStatus.IMPLEMENTED);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(assignmentRepo.findByIdAndProjectId(a.getId(), p.getId())).thenReturn(Optional.of(a));
        when(assignmentRepo.save(a)).thenReturn(a);
        service.updateAssignment(p.getId(), a.getId(),
                new DmaicDto.UpdateAssignmentRequest(PokaYokeAssignmentStatus.VERIFIED, null, 25.0));
        assertThat(a.getStatus()).isEqualTo(PokaYokeAssignmentStatus.VERIFIED);
        assertThat(a.getVerifiedAt()).isNotNull();
        assertThat(a.getDefectReductionPct()).isEqualTo(25.0);
    }

    @Test
    void deleteAssignment_verified_throws() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.CONTROL);
        PokaYokeAssignment a = assignment(p, PokaYokeAssignmentStatus.VERIFIED);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(assignmentRepo.findByIdAndProjectId(a.getId(), p.getId())).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.deleteAssignment(p.getId(), a.getId()))
                .isInstanceOf(DmaicStateException.class);
    }

    @Test
    void deleteAssignment_proposed_success() {
        DmaicProject p = project(DmaicStatus.ACTIVE, DmaicPhase.IMPROVE);
        PokaYokeAssignment a = assignment(p, PokaYokeAssignmentStatus.PROPOSED);
        when(projectRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(assignmentRepo.findByIdAndProjectId(a.getId(), p.getId())).thenReturn(Optional.of(a));
        service.deleteAssignment(p.getId(), a.getId());
        verify(assignmentRepo).delete(a);
    }

    @Test
    void validateAssignmentTransition_unitMatrix() {
        // PROPOSED valid
        DmaicService.validateAssignmentTransition(PokaYokeAssignmentStatus.PROPOSED, PokaYokeAssignmentStatus.IN_DESIGN);
        DmaicService.validateAssignmentTransition(PokaYokeAssignmentStatus.PROPOSED, PokaYokeAssignmentStatus.ABANDONED);
        // IN_DESIGN valid
        DmaicService.validateAssignmentTransition(PokaYokeAssignmentStatus.IN_DESIGN, PokaYokeAssignmentStatus.IMPLEMENTED);
        DmaicService.validateAssignmentTransition(PokaYokeAssignmentStatus.IN_DESIGN, PokaYokeAssignmentStatus.ABANDONED);
        // IMPLEMENTED valid
        DmaicService.validateAssignmentTransition(PokaYokeAssignmentStatus.IMPLEMENTED, PokaYokeAssignmentStatus.VERIFIED);
        DmaicService.validateAssignmentTransition(PokaYokeAssignmentStatus.IMPLEMENTED, PokaYokeAssignmentStatus.ABANDONED);
        // VERIFIED terminal
        assertThatThrownBy(() -> DmaicService.validateAssignmentTransition(
                PokaYokeAssignmentStatus.VERIFIED, PokaYokeAssignmentStatus.IMPLEMENTED))
                .isInstanceOf(DmaicStateException.class);
        // ABANDONED terminal
        assertThatThrownBy(() -> DmaicService.validateAssignmentTransition(
                PokaYokeAssignmentStatus.ABANDONED, PokaYokeAssignmentStatus.PROPOSED))
                .isInstanceOf(DmaicStateException.class);
    }

    // --- helpers ---
    private DmaicProject project(DmaicStatus status, DmaicPhase phase) {
        DmaicProject p = new DmaicProject();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT);
        p.setTitle("Test project");
        p.setBlackBeltId(BB);
        p.setStatus(status);
        p.setPhase(phase);
        p.setCreatedAt(Instant.now()); p.setUpdatedAt(Instant.now());
        return p;
    }

    private ProcessMeasure measure(DmaicProject p, double value) {
        ProcessMeasure m = new ProcessMeasure();
        m.setId(UUID.randomUUID());
        m.setProject(p);
        m.setValue(value);
        m.setRecordedAt(Instant.now());
        m.setCreatedAt(Instant.now());
        return m;
    }

    private PokaYokeDevice device(String code, PokaYokeType type) {
        PokaYokeDevice d = new PokaYokeDevice();
        d.setId(UUID.randomUUID());
        d.setCode(code);
        d.setName("Device " + code);
        d.setType(type);
        d.setMechanism(PokaYokeMechanism.SENSOR);
        d.setCreatedAt(Instant.now()); d.setUpdatedAt(Instant.now());
        return d;
    }

    private PokaYokeAssignment assignment(DmaicProject p, PokaYokeAssignmentStatus s) {
        PokaYokeAssignment a = new PokaYokeAssignment();
        a.setId(UUID.randomUUID());
        a.setTenantId(TENANT);
        a.setProject(p);
        a.setDevice(device("D-A", PokaYokeType.PREVENTION));
        a.setStatus(s);
        a.setCreatedAt(Instant.now()); a.setUpdatedAt(Instant.now());
        return a;
    }
}
