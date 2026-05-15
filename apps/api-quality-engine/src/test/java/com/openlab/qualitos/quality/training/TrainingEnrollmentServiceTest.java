package com.openlab.qualitos.quality.training;

import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingEnrollmentServiceTest {

    @Mock TrainingEnrollmentRepository enrollmentRepo;
    @Mock TrainingPathRepository pathRepo;
    TrainingEnrollmentService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID PATH = UUID.randomUUID();
    static final UUID ENR = UUID.randomUUID();
    static final LocalDate TODAY = LocalDate.parse("2026-05-15");
    static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new TrainingEnrollmentService(enrollmentRepo, pathRepo, CLOCK);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void enroll_activePath_persists() {
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(activePath()));
        when(enrollmentRepo.findByTenantIdAndUserIdAndPathId(TENANT, USER, PATH))
                .thenReturn(Optional.empty());
        when(enrollmentRepo.save(any())).thenAnswer(inv -> {
            TrainingEnrollment e = inv.getArgument(0);
            e.setId(ENR); e.setCreatedAt(Instant.now()); e.setUpdatedAt(Instant.now());
            return e;
        });
        TrainingDto.EnrollmentResponse out = service.enroll(new TrainingDto.EnrollRequest(USER, PATH));
        assertThat(out.status()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(out.enrolledOn()).isEqualTo(TODAY);
    }

    @Test
    void enroll_draftPath_rejected() {
        TrainingPath p = activePath();
        p.setStatus(TrainingPathStatus.DRAFT);
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.enroll(new TrainingDto.EnrollRequest(USER, PATH)))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void enroll_pathCrossTenant_appearsNotFound() {
        TrainingPath p = activePath();
        p.setTenantId(UUID.randomUUID());
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.enroll(new TrainingDto.EnrollRequest(USER, PATH)))
                .isInstanceOf(TrainingPathNotFoundException.class);
    }

    @Test
    void enroll_alreadyEnrolled_rejected() {
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(activePath()));
        TrainingEnrollment existing = enrollment(EnrollmentStatus.IN_PROGRESS);
        when(enrollmentRepo.findByTenantIdAndUserIdAndPathId(TENANT, USER, PATH))
                .thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.enroll(new TrainingDto.EnrollRequest(USER, PATH)))
                .isInstanceOf(TrainingStateException.class)
                .hasMessageContaining("already enrolled");
    }

    @Test
    void start_fromEnrolled_setsInProgressAndStartedOn() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.ENROLLED);
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TrainingDto.EnrollmentResponse out = service.start(ENR);
        assertThat(out.status()).isEqualTo(EnrollmentStatus.IN_PROGRESS);
        assertThat(out.startedOn()).isEqualTo(TODAY);
    }

    @Test
    void start_fromInProgress_rejected() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.IN_PROGRESS);
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.start(ENR))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void updateProgress_advancesAndAutoStartsIfEnrolled() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.ENROLLED);
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.updateProgress(ENR, new TrainingDto.ProgressUpdateRequest(40));
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.IN_PROGRESS);
        assertThat(e.getProgressPct()).isEqualTo(40);
        assertThat(e.getStartedOn()).isEqualTo(TODAY);
    }

    @Test
    void updateProgress_zeroFromEnrolled_keepsEnrolled() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.ENROLLED);
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.updateProgress(ENR, new TrainingDto.ProgressUpdateRequest(0));
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
    }

    @Test
    void updateProgress_backwards_rejected() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.IN_PROGRESS);
        e.setProgressPct(50);
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.updateProgress(ENR,
                new TrainingDto.ProgressUpdateRequest(40)))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void updateProgress_completedStatus_rejected() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.COMPLETED);
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.updateProgress(ENR,
                new TrainingDto.ProgressUpdateRequest(100)))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void complete_aboveThreshold_issuesCertificateAndExpiry() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.IN_PROGRESS);
        TrainingPath p = activePath(); // passingScore=70, validity=24
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TrainingDto.EnrollmentResponse out = service.complete(ENR, new TrainingDto.CompleteRequest(85));
        assertThat(out.status()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(out.certificateCode()).isNotBlank();
        assertThat(out.expiresOn()).isEqualTo(TODAY.plusMonths(24));
        assertThat(out.progressPct()).isEqualTo(100);
    }

    @Test
    void complete_belowThreshold_marksFailedNoCertificate() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.IN_PROGRESS);
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(activePath()));
        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TrainingDto.EnrollmentResponse out = service.complete(ENR, new TrainingDto.CompleteRequest(50));
        assertThat(out.status()).isEqualTo(EnrollmentStatus.FAILED);
        assertThat(out.certificateCode()).isNull();
        assertThat(out.expiresOn()).isNull();
    }

    @Test
    void complete_pathWithoutValidity_noExpiry() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.IN_PROGRESS);
        TrainingPath p = activePath();
        p.setValidityMonths(null);
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(p));
        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TrainingDto.EnrollmentResponse out = service.complete(ENR, new TrainingDto.CompleteRequest(90));
        assertThat(out.expiresOn()).isNull();
        assertThat(out.certificateCode()).isNotBlank();
    }

    @Test
    void complete_terminalStatus_rejected() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.CANCELLED);
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.complete(ENR, new TrainingDto.CompleteRequest(80)))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void cancel_active_ok() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.IN_PROGRESS);
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        when(enrollmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.cancel(ENR).status()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void cancel_terminal_rejected() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.COMPLETED);
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.cancel(ENR))
                .isInstanceOf(TrainingStateException.class);
    }

    @Test
    void listByUser_returnsPage() {
        when(enrollmentRepo.findByTenantIdAndUserId(eq(TENANT), eq(USER), any()))
                .thenReturn(new PageImpl<>(List.of(enrollment(EnrollmentStatus.ENROLLED))));
        assertThat(service.listByUser(USER, PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    @Test
    void listByPath_returnsPage() {
        when(enrollmentRepo.findByTenantIdAndPathId(eq(TENANT), eq(PATH), any()))
                .thenReturn(new PageImpl<>(List.of(enrollment(EnrollmentStatus.IN_PROGRESS))));
        assertThat(service.listByPath(PATH, PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    @Test
    void listByStatus_returnsPage() {
        when(enrollmentRepo.findByTenantIdAndStatus(eq(TENANT), eq(EnrollmentStatus.COMPLETED), any()))
                .thenReturn(new PageImpl<>(List.of(enrollment(EnrollmentStatus.COMPLETED))));
        assertThat(service.listByStatus(EnrollmentStatus.COMPLETED, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    @Test
    void verifyCertificate_validCompletedNonExpired() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.COMPLETED);
        e.setCertificateCode("CODE-XYZ");
        e.setFinalScore(88);
        e.setCompletedOn(TODAY.minusMonths(1));
        e.setExpiresOn(TODAY.plusMonths(6));
        when(enrollmentRepo.findByCertificateCode("CODE-XYZ")).thenReturn(Optional.of(e));
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(activePath()));
        TrainingDto.CertificateVerification out = service.verifyCertificate("CODE-XYZ");
        assertThat(out.valid()).isTrue();
        assertThat(out.finalScore()).isEqualTo(88);
    }

    @Test
    void verifyCertificate_expired_invalid() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.COMPLETED);
        e.setCertificateCode("EXP");
        e.setExpiresOn(TODAY.minusDays(1));
        when(enrollmentRepo.findByCertificateCode("EXP")).thenReturn(Optional.of(e));
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(activePath()));
        assertThat(service.verifyCertificate("EXP").valid()).isFalse();
    }

    @Test
    void verifyCertificate_noExpiry_validIfCompleted() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.COMPLETED);
        e.setCertificateCode("NOEXP");
        e.setExpiresOn(null);
        when(enrollmentRepo.findByCertificateCode("NOEXP")).thenReturn(Optional.of(e));
        when(pathRepo.findById(PATH)).thenReturn(Optional.of(activePath()));
        assertThat(service.verifyCertificate("NOEXP").valid()).isTrue();
    }

    @Test
    void verifyCertificate_unknown_throws() {
        when(enrollmentRepo.findByCertificateCode("NONE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verifyCertificate("NONE"))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }

    @Test
    void get_crossTenant_appearsNotFound() {
        TrainingEnrollment e = enrollment(EnrollmentStatus.ENROLLED);
        e.setTenantId(UUID.randomUUID());
        when(enrollmentRepo.findById(ENR)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.get(ENR))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }

    // --- helpers ---

    private TrainingPath activePath() {
        TrainingPath p = new TrainingPath();
        p.setId(PATH); p.setTenantId(TENANT); p.setCode("p"); p.setName("Path");
        p.setStatus(TrainingPathStatus.ACTIVE);
        p.setDurationHours(16); p.setPassingScore(70); p.setValidityMonths(24);
        p.setCreatedBy(USER);
        p.setCreatedAt(Instant.now()); p.setUpdatedAt(Instant.now());
        return p;
    }

    private TrainingEnrollment enrollment(EnrollmentStatus status) {
        TrainingEnrollment e = new TrainingEnrollment();
        e.setId(ENR); e.setTenantId(TENANT);
        e.setUserId(USER); e.setPathId(PATH);
        e.setStatus(status); e.setProgressPct(status == EnrollmentStatus.IN_PROGRESS ? 30 : 0);
        e.setEnrolledOn(TODAY.minusDays(7));
        e.setCreatedAt(Instant.now()); e.setUpdatedAt(Instant.now());
        return e;
    }
}
