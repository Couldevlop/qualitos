package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ouvre la CAPA d'un constat d'audit non conforme (ADR 0088).
 *
 * <p>Le dossier naît avec son action unique, saisis dans la fenêtre de
 * création commune à tous les écrans, et le constat garde le lien : c'est ce
 * qui permet à la fiche d'audit de dire « CAPA ouverte » au lieu de reproposer
 * le bouton. Les deux écritures partagent la transaction — un dossier ne
 * survit pas à l'échec du lien.
 *
 * <p>Service à part plutôt qu'une méthode de plus dans {@link AuditService} :
 * le cycle de l'audit (planifier, conduire, rapporter) n'a pas à connaître le
 * module CAPA, et ce pont est la seule chose qui les relie.
 */
@Service
@Transactional
public class AuditFindingCapaService {

    /** Seuls les écarts appellent une action : une observation ou une piste ne se « corrige » pas. */
    static final Set<FindingType> NON_CONFORMING = Set.of(FindingType.MINOR_NC, FindingType.MAJOR_NC);

    private final AuditPlanRepository planRepository;
    private final AuditFindingRepository findingRepository;
    private final CapaService capaService;

    public AuditFindingCapaService(AuditPlanRepository planRepository,
                                   AuditFindingRepository findingRepository,
                                   CapaService capaService) {
        this.planRepository = planRepository;
        this.findingRepository = findingRepository;
        this.capaService = capaService;
    }

    /** Ce que l'écran d'audit a besoin de savoir après l'ouverture. */
    public record FindingCapa(UUID findingId, UUID capaId, String sourceRef) {}

    public FindingCapa open(UUID planId, UUID findingId, CapaDto.OpenCaseRequest request) {
        UUID tenantId = requireTenantId();
        AuditPlan plan = planRepository.findByIdAndTenantId(planId, tenantId)
                .orElseThrow(() -> new AuditPlanNotFoundException(planId));
        if (plan.getStatus() == AuditStatus.CANCELLED) {
            throw new AuditStateException("A cancelled audit opens no CAPA");
        }
        AuditFinding finding = findingRepository.findForUpdateByIdAndPlanId(findingId, planId)
                .orElseThrow(() -> new AuditFindingNotFoundException(findingId));
        if (!NON_CONFORMING.contains(finding.getType())) {
            throw new AuditStateException("Only a non-conforming finding opens a CAPA");
        }
        if (finding.getCapaId() != null) {
            throw new AuditStateException("This finding already has its CAPA " + finding.getCapaId());
        }
        String reference = sourceRef(plan, finding);
        CapaDto.CaseResponse capa = capaService.openCase(request, CapaSourceType.AUDIT, reference);
        finding.setCapaId(capa.id());
        findingRepository.save(finding);
        return new FindingCapa(finding.getId(), capa.id(), reference);
    }

    /**
     * « AUD-2026-0003 / constat 2 » : la référence de l'audit et le rang du
     * constat dans l'audit, dans l'ordre où ils ont été relevés. C'est ce que
     * l'auditeur cite, et ce qui se retrouve en lisant la fiche d'audit.
     */
    static String sourceRef(AuditPlan plan, AuditFinding finding) {
        String audit = plan.getReference() != null && !plan.getReference().isBlank()
                ? plan.getReference().strip() : plan.getTitle();
        List<AuditFinding> findings = plan.getFindings();
        int rang = 0;
        for (int i = 0; i < findings.size(); i++) {
            if (finding.getId() != null && finding.getId().equals(findings.get(i).getId())) {
                rang = i + 1;
                break;
            }
        }
        String ref = rang > 0 ? audit + " / constat " + rang : audit;
        return ref.length() > 255 ? ref.substring(0, 255) : ref;
    }

    private static UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }
}
