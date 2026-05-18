package com.openlab.qualitos.quality.training;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cycle de vie d'une inscription + génération du code certificat sur COMPLETED.
 *
 * Transitions :
 *   ENROLLED → IN_PROGRESS → (COMPLETED | FAILED)
 *   ENROLLED | IN_PROGRESS → CANCELLED
 *   Terminal sinon.
 *
 * Le passage à COMPLETED exige finalScore ≥ TrainingPath.passingScore — sinon
 * le résultat est FAILED. Ce verrou évite les certifs "complétés mais sans
 * score suffisant" qui apparaissaient dans la V0.
 */
@Service
public class TrainingEnrollmentService {

    private final TrainingEnrollmentRepository enrollmentRepo;
    private final TrainingPathRepository pathRepo;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public TrainingEnrollmentService(TrainingEnrollmentRepository enrollmentRepo,
                                     TrainingPathRepository pathRepo) {
        this(enrollmentRepo, pathRepo, Clock.systemUTC());
    }

    TrainingEnrollmentService(TrainingEnrollmentRepository enrollmentRepo,
                              TrainingPathRepository pathRepo,
                              Clock clock) {
        this.enrollmentRepo = enrollmentRepo;
        this.pathRepo = pathRepo;
        this.clock = clock;
    }

    @Transactional
    public TrainingDto.EnrollmentResponse enroll(TrainingDto.EnrollRequest req) {
        UUID tenantId = requireTenantId();
        TrainingPath p = pathRepo.findById(req.pathId())
                .orElseThrow(() -> new TrainingPathNotFoundException(req.pathId()));
        if (!p.getTenantId().equals(tenantId)) throw new TrainingPathNotFoundException(req.pathId());
        if (p.getStatus() != TrainingPathStatus.ACTIVE) {
            throw new TrainingStateException("Cannot enroll in path with status " + p.getStatus());
        }
        enrollmentRepo.findByTenantIdAndUserIdAndPathId(tenantId, req.userId(), req.pathId())
                .ifPresent(e -> {
                    throw new TrainingStateException(
                            "User already enrolled in this path (status: " + e.getStatus() + ")");
                });
        TrainingEnrollment e = new TrainingEnrollment();
        e.setTenantId(tenantId);
        e.setUserId(req.userId());
        e.setPathId(req.pathId());
        e.setStatus(EnrollmentStatus.ENROLLED);
        e.setProgressPct(0);
        e.setEnrolledOn(LocalDate.now(clock));
        return toResponse(enrollmentRepo.save(e));
    }

    @Transactional
    public TrainingDto.EnrollmentResponse start(UUID id) {
        TrainingEnrollment e = load(id);
        if (e.getStatus() != EnrollmentStatus.ENROLLED) {
            throw new TrainingStateException(
                    "Cannot start an enrollment in status " + e.getStatus());
        }
        e.setStatus(EnrollmentStatus.IN_PROGRESS);
        e.setStartedOn(LocalDate.now(clock));
        return toResponse(enrollmentRepo.save(e));
    }

    @Transactional
    public TrainingDto.EnrollmentResponse updateProgress(UUID id, TrainingDto.ProgressUpdateRequest req) {
        TrainingEnrollment e = load(id);
        if (e.getStatus() != EnrollmentStatus.ENROLLED
                && e.getStatus() != EnrollmentStatus.IN_PROGRESS) {
            throw new TrainingStateException(
                    "Progress update not allowed in status " + e.getStatus());
        }
        if (req.progressPct() < e.getProgressPct()) {
            throw new TrainingStateException("Progress cannot go backwards");
        }
        e.setProgressPct(req.progressPct());
        if (e.getStatus() == EnrollmentStatus.ENROLLED && req.progressPct() > 0) {
            e.setStatus(EnrollmentStatus.IN_PROGRESS);
            if (e.getStartedOn() == null) e.setStartedOn(LocalDate.now(clock));
        }
        return toResponse(enrollmentRepo.save(e));
    }

    @Transactional
    public TrainingDto.EnrollmentResponse complete(UUID id, TrainingDto.CompleteRequest req) {
        TrainingEnrollment e = load(id);
        if (e.getStatus() != EnrollmentStatus.IN_PROGRESS
                && e.getStatus() != EnrollmentStatus.ENROLLED) {
            throw new TrainingStateException(
                    "Cannot complete an enrollment in status " + e.getStatus());
        }
        TrainingPath p = pathRepo.findById(e.getPathId())
                .orElseThrow(() -> new TrainingPathNotFoundException(e.getPathId()));
        LocalDate today = LocalDate.now(clock);
        e.setFinalScore(req.finalScore());
        e.setProgressPct(100);
        e.setCompletedOn(today);
        if (req.finalScore() >= p.getPassingScore()) {
            e.setStatus(EnrollmentStatus.COMPLETED);
            e.setCertificateCode(UUID.randomUUID().toString());
            if (p.getValidityMonths() != null) {
                e.setExpiresOn(today.plusMonths(p.getValidityMonths()));
            }
        } else {
            e.setStatus(EnrollmentStatus.FAILED);
            // pas de certificat sur échec
        }
        return toResponse(enrollmentRepo.save(e));
    }

    @Transactional
    public TrainingDto.EnrollmentResponse cancel(UUID id) {
        TrainingEnrollment e = load(id);
        if (e.getStatus() == EnrollmentStatus.COMPLETED
                || e.getStatus() == EnrollmentStatus.FAILED
                || e.getStatus() == EnrollmentStatus.CANCELLED) {
            throw new TrainingStateException(
                    "Cannot cancel an enrollment in terminal status " + e.getStatus());
        }
        e.setStatus(EnrollmentStatus.CANCELLED);
        return toResponse(enrollmentRepo.save(e));
    }

    @Transactional(readOnly = true)
    public Page<TrainingDto.EnrollmentResponse> listByUser(UUID userId, Pageable pageable) {
        UUID tenantId = requireTenantId();
        return enrollmentRepo.findByTenantIdAndUserId(tenantId, userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TrainingDto.EnrollmentResponse> listByPath(UUID pathId, Pageable pageable) {
        UUID tenantId = requireTenantId();
        return enrollmentRepo.findByTenantIdAndPathId(tenantId, pathId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TrainingDto.EnrollmentResponse> listByStatus(EnrollmentStatus status, Pageable pageable) {
        UUID tenantId = requireTenantId();
        return enrollmentRepo.findByTenantIdAndStatus(tenantId, status, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TrainingDto.EnrollmentResponse get(UUID id) { return toResponse(load(id)); }

    /**
     * Vérification publique d'un certificat (pas de filtre tenant — le code UUID
     * EST l'autorité). Renvoie {@code valid=false} si le code est introuvable,
     * l'enrollment n'est pas COMPLETED, ou le certificat a expiré.
     */
    @Transactional(readOnly = true)
    public TrainingDto.CertificateVerification verifyCertificate(String code) {
        TrainingEnrollment e = enrollmentRepo.findByCertificateCode(code)
                .orElseThrow(() -> new EnrollmentNotFoundException(code));
        TrainingPath p = pathRepo.findById(e.getPathId())
                .orElseThrow(() -> new TrainingPathNotFoundException(e.getPathId()));
        LocalDate today = LocalDate.now(clock);
        boolean valid = e.getStatus() == EnrollmentStatus.COMPLETED
                && (e.getExpiresOn() == null || !e.getExpiresOn().isBefore(today));
        return new TrainingDto.CertificateVerification(
                code, e.getId(), e.getTenantId(), e.getUserId(), e.getPathId(),
                p.getCode(), p.getName(),
                e.getFinalScore() == null ? 0 : e.getFinalScore(),
                e.getCompletedOn(), e.getExpiresOn(), valid);
    }

    TrainingEnrollment load(UUID id) {
        UUID tenantId = requireTenantId();
        TrainingEnrollment e = enrollmentRepo.findById(id)
                .orElseThrow(() -> new EnrollmentNotFoundException(id));
        if (!e.getTenantId().equals(tenantId)) throw new EnrollmentNotFoundException(id);
        return e;
    }

    private TrainingDto.EnrollmentResponse toResponse(TrainingEnrollment e) {
        return new TrainingDto.EnrollmentResponse(
                e.getId(), e.getTenantId(), e.getUserId(), e.getPathId(),
                e.getStatus(), e.getProgressPct(), e.getFinalScore(),
                e.getEnrolledOn(), e.getStartedOn(), e.getCompletedOn(),
                e.getExpiresOn(), e.getCertificateCode(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
