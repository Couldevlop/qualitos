package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.audit.AuditFinding;
import com.openlab.qualitos.quality.audit.AuditFindingRepository;
import com.openlab.qualitos.quality.audit.FindingType;
import com.openlab.qualitos.quality.change.ChangeRequest;
import com.openlab.qualitos.quality.change.ChangeRequestRepository;
import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.risk.ActionPriority;
import com.openlab.qualitos.quality.risk.FmeaItem;
import com.openlab.qualitos.quality.risk.FmeaProject;
import com.openlab.qualitos.quality.risk.FmeaProjectRepository;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import com.openlab.qualitos.quality.riskregister.application.RiskSourceCatalog;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;

import java.util.Optional;
import java.util.UUID;

/**
 * Relit l'objet source dans son module, et toujours dans le client du jeton.
 *
 * <p>Les dépôts sont interrogés en lecture seule ; aucun module source n'est
 * modifié par la création d'un risque. Un objet d'un autre client rend « vide »,
 * comme un objet inexistant : la réponse ne dit pas lequel des deux.
 */
public class PlatformRiskSourceCatalog implements RiskSourceCatalog {

    /** Ce qui manque à un objet pour justifier un risque, en code que l'écran traduit. */
    static final String BELOW_THRESHOLD = "BELOW_THRESHOLD";
    static final String NOT_A_GAP = "NOT_A_GAP";

    private static final int TITLE_MAX = 255;
    private static final int TEXT_MAX = 4000;
    private static final int REF_MAX = 120;

    private final FmeaItemRepository fmeaItems;
    private final FmeaProjectRepository fmeaProjects;
    private final NonConformityRepository nonConformities;
    private final AuditFindingRepository auditFindings;
    private final ChangeRequestRepository changes;

    public PlatformRiskSourceCatalog(FmeaItemRepository fmeaItems, FmeaProjectRepository fmeaProjects,
                                     NonConformityRepository nonConformities,
                                     AuditFindingRepository auditFindings, ChangeRequestRepository changes) {
        this.fmeaItems = fmeaItems;
        this.fmeaProjects = fmeaProjects;
        this.nonConformities = nonConformities;
        this.auditFindings = auditFindings;
        this.changes = changes;
    }

    @Override
    public Optional<SourceDraft> find(RegisterOrigin origin, UUID tenantId, UUID sourceId) {
        if (origin == null || tenantId == null || sourceId == null) {
            return Optional.empty();
        }
        return switch (origin) {
            case FMEA -> amdec(tenantId, sourceId);
            case NON_CONFORMITY -> nonConformite(tenantId, sourceId);
            case AUDIT -> constat(tenantId, sourceId);
            case CHANGE -> changement(tenantId, sourceId);
            default -> Optional.empty();
        };
    }

    /**
     * Une ligne d'AMDEC : mode de défaillance, cause, effet. Les notes AMDEC
     * vont de 1 à 10, celles du registre de 1 à 5 : on divise par deux en
     * arrondissant au-dessus, pour ne jamais minorer la gravité.
     *
     * <p>Seule une ligne au-delà du seuil justifie un risque : RPN au moins égal
     * au seuil critique du projet, ou priorité d'action HAUTE (AIAG-VDA).
     */
    private Optional<SourceDraft> amdec(UUID tenant, UUID itemId) {
        Optional<FmeaItem> ligne = fmeaItems.findByIdAndTenantId(itemId, tenant);
        if (ligne.isEmpty()) {
            return Optional.empty();
        }
        FmeaItem item = ligne.get();
        Optional<FmeaProject> projet = fmeaProjects.findById(item.getProjectId())
                .filter(p -> tenant.equals(p.getTenantId()));
        if (projet.isEmpty()) {
            return Optional.empty();
        }
        FmeaProject p = projet.get();
        boolean auDela = item.getRpn() >= p.getCriticalRpnThreshold()
                || item.getActionPriority() == ActionPriority.HIGH;
        return Optional.of(new SourceDraft(
                borne(p.getCode() + " #" + item.getSequenceNo(), REF_MAX),
                borne(item.getFailureMode(), TITLE_MAX),
                borne(item.getFailureCause(), TEXT_MAX),
                borne(item.getFailureEffect(), TEXT_MAX),
                demi(item.getSeverity()), demi(item.getOccurrence()),
                null, auDela, auDela ? null : BELOW_THRESHOLD));
    }

    /** Une non-conformité : la gravité suit la sienne, la probabilité reste à coter. */
    private Optional<SourceDraft> nonConformite(UUID tenant, UUID ncId) {
        return nonConformities.findByIdAndTenantId(ncId, tenant).map((NonConformity nc) -> new SourceDraft(
                borne(nc.getReference(), REF_MAX),
                borne(nc.getTitle(), TITLE_MAX),
                borne(nc.getRootCause(), TEXT_MAX),
                borne(nc.getDescription(), TEXT_MAX),
                nc.getSeverity() == null ? null : switch (nc.getSeverity()) {
                    case MINOR -> 2;
                    case MAJOR -> 4;
                    case CRITICAL -> 5;
                },
                null, null, true, null));
    }

    /**
     * Un constat d'audit. Le constat n'a pas de tenant à lui : on le vérifie par
     * son plan. Seuls un écart (mineur, majeur) ou une observation justifient un
     * risque — un constat de conformité, non ; une piste de progrès relève du
     * registre des opportunités.
     */
    private Optional<SourceDraft> constat(UUID tenant, UUID findingId) {
        Optional<AuditFinding> trouve = auditFindings.findById(findingId)
                .filter(f -> f.getPlan() != null && tenant.equals(f.getPlan().getTenantId()));
        if (trouve.isEmpty()) {
            return Optional.empty();
        }
        AuditFinding f = trouve.get();
        String clause = f.getClauseRef() == null || f.getClauseRef().isBlank() ? "" : " · " + f.getClauseRef();
        Integer gravite = gravite(f.getType());
        boolean ecart = gravite != null;
        return Optional.of(new SourceDraft(
                borne(f.getPlan().getReference() + clause, REF_MAX),
                borne(f.getDescription(), TITLE_MAX),
                null,
                borne(f.getDescription(), TEXT_MAX),
                gravite, null, null, ecart, ecart ? null : NOT_A_GAP));
    }

    private static Integer gravite(FindingType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case MAJOR_NC -> 4;
            case MINOR_NC -> 3;
            case OBSERVATION -> 2;
            case CONFORMITY, OPPORTUNITY -> null;
        };
    }

    /** Un changement (MOC) : l'analyse de risque en cause, l'analyse d'impact en effet. */
    private Optional<SourceDraft> changement(UUID tenant, UUID changeId) {
        return changes.findById(changeId)
                .filter(c -> tenant.equals(c.getTenantId()))
                .map((ChangeRequest c) -> new SourceDraft(
                        borne(c.getCode(), REF_MAX),
                        borne(c.getTitle(), TITLE_MAX),
                        borne(c.getRiskAssessment(), TEXT_MAX),
                        borne(c.getImpactSummary(), TEXT_MAX),
                        null, null, null, true, null));
    }

    static Integer demi(int noteSurDix) {
        if (noteSurDix < 1) {
            return null;
        }
        return Math.min(5, (noteSurDix + 1) / 2);
    }

    /** Rogne à la longueur que le registre accepte, pour qu'un brouillon soit toujours enregistrable. */
    static String borne(String texte, int max) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        String t = texte.strip();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
