package com.openlab.qualitos.quality.fivewhys;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Analyse des 5 Pourquoi rattachée à une non-conformité (§3.5).
 *
 * <p>Deux règles de méthode sont tenues ici, et pas seulement suggérées :
 * <ul>
 *   <li>on ne conclut pas une cause racine avant le troisième pourquoi — conclure
 *       au deuxième, c'est nommer un symptôme ;</li>
 *   <li>la chaîne s'arrête à sept — au-delà on n'remonte plus une cause, on
 *       énumère des circonstances, et la liste s'allonge sans jamais conclure.</li>
 * </ul>
 * Une méthode qu'on peut contourner sans s'en apercevoir ne produit pas d'analyse,
 * elle produit un formulaire.
 */
@Service
public class FiveWhysService {

    /** Borne haute de la chaîne. Cinq est un ordre de grandeur, sept une limite. */
    static final int MAX_WHYS = 7;

    /** En deçà, la « cause racine » est encore un symptôme. */
    static final int MIN_WHYS_BEFORE_ROOT_CAUSE = 3;

    private final FiveWhysAnalysisRepository analysisRepository;
    private final FiveWhysStepRepository stepRepository;
    private final NonConformityRepository ncRepository;

    public FiveWhysService(FiveWhysAnalysisRepository analysisRepository,
                           FiveWhysStepRepository stepRepository,
                           NonConformityRepository ncRepository) {
        this.analysisRepository = analysisRepository;
        this.stepRepository = stepRepository;
        this.ncRepository = ncRepository;
    }

    @Transactional(readOnly = true)
    public Page<FiveWhysDto.AnalysisResponse> findAll(Pageable pageable) {
        UUID tenantId = requireTenantId();
        return analysisRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(a -> toResponse(a, List.of()));
    }

    @Transactional(readOnly = true)
    public List<FiveWhysDto.AnalysisResponse> findByNc(UUID ncId) {
        UUID tenantId = requireTenantId();
        return analysisRepository.findByNonConformityIdAndTenantIdOrderByCreatedAtDesc(ncId, tenantId)
                .stream().map(a -> toResponse(a, List.of())).toList();
    }

    @Transactional(readOnly = true)
    public FiveWhysDto.AnalysisResponse get(UUID id) {
        UUID tenantId = requireTenantId();
        FiveWhysAnalysis analysis = requireAnalysis(id, tenantId);
        return toResponse(analysis,
                stepRepository.findByAnalysisIdAndTenantIdOrderByPositionAsc(id, tenantId));
    }

    @Transactional
    public FiveWhysDto.AnalysisResponse create(FiveWhysDto.CreateRequest request) {
        UUID tenantId = requireTenantId();
        NonConformity nc = ncRepository.findByIdAndTenantId(request.ncId(), tenantId)
                .orElseThrow(() -> new FiveWhysNotFoundException(request.ncId()));

        FiveWhysAnalysis analysis = new FiveWhysAnalysis();
        analysis.setTenantId(tenantId);
        analysis.setNonConformity(nc);
        // Reprendre le titre de la NC évite de retaper ce qui est déjà écrit, et
        // garantit que l'analyse parle du même écart.
        String problem = request.problem() == null || request.problem().isBlank()
                ? nc.getTitle()
                : request.problem().trim();
        analysis.setProblem(problem);
        return toResponse(analysisRepository.save(analysis), List.of());
    }

    @Transactional
    public FiveWhysDto.AnalysisResponse updateProblem(UUID id, FiveWhysDto.UpdateProblemRequest request) {
        UUID tenantId = requireTenantId();
        FiveWhysAnalysis analysis = requireAnalysis(id, tenantId);
        analysis.setProblem(requireText(request.problem(), "L'énoncé du problème est obligatoire"));
        return toResponse(analysisRepository.save(analysis),
                stepRepository.findByAnalysisIdAndTenantIdOrderByPositionAsc(id, tenantId));
    }

    @Transactional
    public FiveWhysDto.StepResponse addStep(UUID analysisId, FiveWhysDto.AddStepRequest request) {
        UUID tenantId = requireTenantId();
        requireAnalysis(analysisId, tenantId);
        String answer = requireText(request.answer(), "La réponse au pourquoi est obligatoire");

        long already = stepRepository.countByAnalysisIdAndTenantId(analysisId, tenantId);
        if (already >= MAX_WHYS) {
            throw new FiveWhysStateException(
                    "Au-delà de " + MAX_WHYS + " pourquoi, on n'remonte plus une cause : "
                            + "conclure la cause racine ou reformuler le problème");
        }

        FiveWhysStep step = new FiveWhysStep();
        step.setTenantId(tenantId);
        step.setAnalysis(requireAnalysis(analysisId, tenantId));
        step.setPosition((int) already + 1);
        step.setAnswer(answer);
        return toStepResponse(stepRepository.save(step));
    }

    @Transactional
    public FiveWhysDto.StepResponse updateStep(UUID stepId, FiveWhysDto.AddStepRequest request) {
        UUID tenantId = requireTenantId();
        FiveWhysStep step = stepRepository.findByIdAndTenantId(stepId, tenantId)
                .orElseThrow(() -> new FiveWhysNotFoundException(stepId));
        step.setAnswer(requireText(request.answer(), "La réponse au pourquoi est obligatoire"));
        return toStepResponse(stepRepository.save(step));
    }

    /**
     * Seul le DERNIER pourquoi peut être retiré : ôter un maillon du milieu
     * casserait la chaîne, le pourquoi suivant ne répondant plus à celui qui le
     * précède. Renuméroter à sa place changerait le sens de l'analyse sans le dire.
     */
    @Transactional
    public void deleteStep(UUID stepId) {
        UUID tenantId = requireTenantId();
        FiveWhysStep step = stepRepository.findByIdAndTenantId(stepId, tenantId)
                .orElseThrow(() -> new FiveWhysNotFoundException(stepId));
        long total = stepRepository.countByAnalysisIdAndTenantId(
                step.getAnalysis().getId(), tenantId);
        if (step.getPosition() != total) {
            throw new FiveWhysStateException(
                    "Seul le dernier pourquoi peut être retiré : la chaîne se lit dans l'ordre");
        }
        stepRepository.delete(step);
    }

    @Transactional
    public FiveWhysDto.AnalysisResponse setRootCause(UUID id, FiveWhysDto.RootCauseRequest request) {
        UUID tenantId = requireTenantId();
        FiveWhysAnalysis analysis = requireAnalysis(id, tenantId);
        long depth = stepRepository.countByAnalysisIdAndTenantId(id, tenantId);
        if (depth < MIN_WHYS_BEFORE_ROOT_CAUSE) {
            throw new FiveWhysStateException(
                    "Conclure avant " + MIN_WHYS_BEFORE_ROOT_CAUSE
                            + " pourquoi, c'est nommer un symptôme");
        }
        analysis.setRootCause(requireText(request.rootCause(), "La cause racine est obligatoire"));
        return toResponse(analysisRepository.save(analysis),
                stepRepository.findByAnalysisIdAndTenantIdOrderByPositionAsc(id, tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = requireTenantId();
        analysisRepository.delete(requireAnalysis(id, tenantId));
    }

    // ---- garde-fous -------------------------------------------------------------

    private FiveWhysAnalysis requireAnalysis(UUID id, UUID tenantId) {
        return analysisRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FiveWhysNotFoundException(id));
    }

    private static String requireText(String value, String message) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new FiveWhysStateException(message);
        }
        return text;
    }

    private static UUID requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(tenantId);
    }

    private static FiveWhysDto.AnalysisResponse toResponse(
            FiveWhysAnalysis a, List<FiveWhysStep> steps) {
        return new FiveWhysDto.AnalysisResponse(
                a.getId(),
                a.getNonConformity().getId(),
                a.getNonConformity().getReference(),
                a.getProblem(),
                a.getRootCause(),
                steps.stream().map(FiveWhysService::toStepResponse).toList(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }

    private static FiveWhysDto.StepResponse toStepResponse(FiveWhysStep s) {
        return new FiveWhysDto.StepResponse(
                s.getId(), s.getPosition(), s.getAnswer(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
