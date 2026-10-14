package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import com.openlab.qualitos.quality.change.domain.MocClassification;
import com.openlab.qualitos.quality.change.domain.MocException;
import com.openlab.qualitos.quality.change.domain.MocHistoryEvent;
import com.openlab.qualitos.quality.change.domain.MocScoring;
import com.openlab.qualitos.quality.change.domain.MocSettings;
import com.openlab.qualitos.quality.change.domain.ValidationRole;
import com.openlab.qualitos.quality.common.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La gestion des changements (MOC, ADR 0087) : demande, analyse d'impact,
 * validation par rôles, historique.
 *
 * <p><b>Le client et l'acteur viennent du jeton</b>, jamais du corps (§18.2.2,
 * ADR 0080) : le demandeur d'une demande, l'évaluateur d'un domaine et l'auteur
 * d'une décision sont l'utilisateur authentifié.
 *
 * <p><b>Règles tenues ici, quoi que l'écran ait laissé passer :</b>
 * <ul>
 *   <li>une demande n'est conduite (modifiée, supprimée, analysée, soumise,
 *       rouverte) que par son demandeur ou par un gestionnaire des changements
 *       ({@code change.analyze}) ;</li>
 *   <li>seul un brouillon se supprime ; brouillon et analyse se modifient ;</li>
 *   <li>la soumission exige les huit domaines évalués, et un titulaire (autre que
 *       le demandeur) pour chaque rôle requis ;</li>
 *   <li>seul un titulaire COURANT du rôle statue sur sa carte, jamais le demandeur ;
 *       un refus se motive, et un seul refus rejette la demande ;</li>
 *   <li>une demande rejetée peut être rouverte en analyse ; ses cartes repartent
 *       de zéro à la soumission suivante, l'historique garde les décisions.</li>
 * </ul>
 */
@Service
public class MocService {

    static final String CODE_PREFIX = "MOC-";

    private final ChangeRequestRepository requestRepo;
    private final ChangeImpactAssessmentRepository assessmentRepo;
    private final ChangeValidationRepository validationRepo;
    private final ChangeHistoryRepository historyRepo;
    private final ChangeCodeCounterRepository counterRepo;
    private final MocSettingsService settings;
    private final MocNotifier notifier;
    private final MocAuditTrail audit;
    private final AuthorizationService authz;
    private final Clock clock;

    public MocService(ChangeRequestRepository requestRepo, ChangeImpactAssessmentRepository assessmentRepo,
                      ChangeValidationRepository validationRepo, ChangeHistoryRepository historyRepo,
                      ChangeCodeCounterRepository counterRepo, MocSettingsService settings,
                      MocNotifier notifier, MocAuditTrail audit, AuthorizationService authz, Clock clock) {
        this.requestRepo = requestRepo;
        this.assessmentRepo = assessmentRepo;
        this.validationRepo = validationRepo;
        this.historyRepo = historyRepo;
        this.counterRepo = counterRepo;
        this.settings = settings;
        this.notifier = notifier;
        this.audit = audit;
        this.authz = authz;
        this.clock = clock;
    }

    // ---------- lecture ----------

    /** Toutes les demandes du client, la plus récente en haut, avec « à valider par moi ». */
    @Transactional(readOnly = true)
    public List<MocDto.Summary> list() {
        UUID tenantId = MocSettingsService.requireTenantId();
        UUID moi = CurrentUser.userId().orElse(null);
        List<ChangeRequest> demandes = requestRepo.findByTenantId(tenantId, Sort.by(Sort.Direction.DESC, "createdAt"));
        Set<UUID> aMoi = enAttenteDe(tenantId, moi, demandes);
        return demandes.stream().map(c -> new MocDto.Summary(c.getId(), c.getCode(), c.getTitle(), c.getType(),
                c.getStatus(), c.getClassification(), c.getImpactScore(), c.getRequesterName(), c.getCreatedAt(),
                aMoi.contains(c.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public MocDto.Detail get(UUID id) {
        UUID tenantId = MocSettingsService.requireTenantId();
        return detail(load(tenantId, id), settings.load(tenantId));
    }

    // ---------- demande ----------

    @Transactional
    public MocDto.Detail create(MocDto.RequestForm form) {
        UUID tenantId = MocSettingsService.requireTenantId();
        UUID acteur = CurrentUser.requireUserId();
        ChangeRequest c = new ChangeRequest();
        c.setTenantId(tenantId);
        c.setCode(nextCode(tenantId));
        appliquer(c, form);
        c.setStatus(ChangeRequestStatus.DRAFT);
        c.setClassification(MocClassification.NOT_ASSESSED);
        c.setRequesterUserId(acteur);
        c.setRequesterName(CurrentUser.displayName().orElse(null));
        c.setCreatedAt(clock.instant());
        c.setUpdatedAt(c.getCreatedAt());
        ChangeRequest saved = requestRepo.save(c);
        trace(saved, MocHistoryEvent.CREATED, null);
        audit.change(tenantId, acteur, "created", saved, null);
        return detail(saved, settings.load(tenantId));
    }

    @Transactional
    public MocDto.Detail update(UUID id, MocDto.RequestForm form) {
        UUID tenantId = MocSettingsService.requireTenantId();
        ChangeRequest c = load(tenantId, id);
        exigerConducteur(c);
        if (!c.getStatus().open()) {
            throw MocException.conflict("moc.not-editable",
                    "Une demande ne se modifie qu'en brouillon ou en analyse (statut : " + c.getStatus() + ").");
        }
        appliquer(c, form);
        requestRepo.save(c);
        trace(c, MocHistoryEvent.UPDATED, null);
        audit.change(tenantId, CurrentUser.requireUserId(), "updated", c, null);
        return detail(c, settings.load(tenantId));
    }

    /** Seul un brouillon se supprime : au-delà, la demande a une histoire que l'audit doit garder. */
    @Transactional
    public void delete(UUID id) {
        UUID tenantId = MocSettingsService.requireTenantId();
        ChangeRequest c = load(tenantId, id);
        exigerConducteur(c);
        if (c.getStatus() != ChangeRequestStatus.DRAFT) {
            throw MocException.conflict("moc.delete-not-draft",
                    "Seule une demande en brouillon peut être supprimée (statut : " + c.getStatus() + ").");
        }
        audit.change(tenantId, CurrentUser.requireUserId(), "deleted", c, null);
        assessmentRepo.deleteByTenantIdAndChangeId(tenantId, id);
        validationRepo.deleteByTenantIdAndChangeId(tenantId, id);
        historyRepo.deleteByTenantIdAndChangeId(tenantId, id);
        requestRepo.delete(c);
    }

    // ---------- cycle ----------

    @Transactional
    public MocDto.Detail sendToAnalysis(UUID id) {
        UUID tenantId = MocSettingsService.requireTenantId();
        ChangeRequest c = load(tenantId, id);
        exigerConducteur(c);
        exiger(c, ChangeRequestStatus.DRAFT, "moc.not-draft",
                "Seul un brouillon s'envoie en analyse.");
        c.setStatus(ChangeRequestStatus.IN_ANALYSIS);
        requestRepo.save(c);
        trace(c, MocHistoryEvent.SENT_TO_ANALYSIS, null);
        audit.change(tenantId, CurrentUser.requireUserId(), "sent_to_analysis", c, null);
        return detail(c, settings.load(tenantId));
    }

    /** Évalue un domaine. Le score et la classification suivent, tant que l'analyse est ouverte. */
    @Transactional
    public MocDto.Detail assess(UUID id, ImpactDomain domain, MocDto.ImpactForm form) {
        UUID tenantId = MocSettingsService.requireTenantId();
        UUID acteur = CurrentUser.requireUserId();
        ChangeRequest c = load(tenantId, id);
        exigerConducteur(c);
        exiger(c, ChangeRequestStatus.IN_ANALYSIS, "moc.not-in-analysis",
                "L'analyse d'impact ne se modifie que pendant l'analyse.");
        MocSettings s = settings.load(tenantId);
        List<ChangeImpactAssessment> evaluations = new ArrayList<>(
                assessmentRepo.findByTenantIdAndChangeId(tenantId, id));
        boolean completeAvant = MocScoring.evaluate(MocAnalysis.levels(evaluations), s.weights(),
                s.thresholds()).complete();

        ChangeImpactAssessment a = evaluations.stream().filter(e -> e.getDomain() == domain).findFirst()
                .orElseGet(() -> {
                    ChangeImpactAssessment n = new ChangeImpactAssessment();
                    n.setTenantId(tenantId);
                    n.setChangeId(id);
                    n.setDomain(domain);
                    evaluations.add(n);
                    return n;
                });
        a.setLevel(form.level());
        a.setNotes(blankToNull(form.notes()));
        a.setAssessedBy(acteur);
        a.setAssessedAt(clock.instant());
        assessmentRepo.save(a);

        MocScoring.Assessment resultat = MocAnalysis.apply(c, evaluations, s);
        requestRepo.save(c);
        if (resultat.complete() && !completeAvant) {
            trace(c, MocHistoryEvent.ANALYSIS_COMPLETED, resultat.score() + "/100");
        }
        audit.change(tenantId, acteur, "impact_assessed", c, domain + "=" + form.level());
        return detail(c, s);
    }

    /**
     * Soumet à validation : huit domaines évalués, un titulaire par rôle requis.
     * Le score, la classification et la liste des rôles sont FIGÉS ici.
     */
    @Transactional
    public MocDto.Detail submit(UUID id) {
        UUID tenantId = MocSettingsService.requireTenantId();
        UUID acteur = CurrentUser.requireUserId();
        ChangeRequest c = load(tenantId, id);
        exigerConducteur(c);
        exiger(c, ChangeRequestStatus.IN_ANALYSIS, "moc.not-in-analysis",
                "Seule une demande en analyse se soumet à validation.");
        MocSettings s = settings.load(tenantId);
        MocScoring.Assessment a = MocAnalysis.apply(c, assessmentRepo.findByTenantIdAndChangeId(tenantId, id), s);
        if (!a.complete()) {
            throw MocException.conflict("moc.analysis-incomplete",
                    "Les huit domaines d'impact doivent être évalués avant la soumission à validation.");
        }
        List<ValidationRole> requis = s.requiredRoles(a.classification());
        for (ValidationRole r : requis) {
            if (r.id() == null || r.holders().isEmpty()) {
                throw new MocException(MocException.Kind.CONFLICT, "moc.role-without-holder",
                        "Le rôle « " + r.label() + " » n'a aucun titulaire : l'administrateur doit en nommer un.",
                        Map.of("role", r.label()));
            }
            if (r.holders().stream().allMatch(h -> h.userId().equals(c.getRequesterUserId()))) {
                throw new MocException(MocException.Kind.CONFLICT, "moc.role-only-requester",
                        "Le rôle « " + r.label() + " » n'a pour titulaire que le demandeur, "
                                + "qui ne peut pas valider sa propre demande.",
                        Map.of("role", r.label()));
            }
        }
        Instant now = clock.instant();
        int ordinal = 0;
        Map<String, List<ValidationRole.RoleHolder>> aPrevenir = new LinkedHashMap<>();
        for (ValidationRole r : requis) {
            ChangeValidation v = new ChangeValidation();
            v.setTenantId(tenantId);
            v.setChangeId(id);
            v.setOrdinal(ordinal++);
            v.setRoleId(r.id());
            v.setRoleLabel(r.label());
            v.setDecision(ApprovalDecision.PENDING);
            v.setCreatedAt(now);
            validationRepo.save(v);
            aPrevenir.put(r.label(), r.holders());
        }
        c.setStatus(ChangeRequestStatus.IN_VALIDATION);
        c.setSubmittedAt(now);
        c.setClosedAt(null);
        requestRepo.save(c);
        trace(c, MocHistoryEvent.SUBMITTED, a.classification() + " · " + a.score() + "/100");
        audit.change(tenantId, acteur, "submitted", c, a.classification().name());
        notifier.awaitingValidation(c, MocNotifier.recipients(aPrevenir, c.getRequesterUserId()));
        return detail(c, s);
    }

    /**
     * Statue sur la carte d'un rôle. L'auteur est l'utilisateur du jeton ; il doit
     * être titulaire COURANT du rôle, et ne pas être le demandeur.
     */
    @Transactional
    public MocDto.Detail decide(UUID id, UUID validationId, MocDto.DecisionForm form) {
        UUID tenantId = MocSettingsService.requireTenantId();
        UUID acteur = CurrentUser.requireUserId();
        ChangeRequest c = load(tenantId, id);
        exiger(c, ChangeRequestStatus.IN_VALIDATION, "moc.not-in-validation",
                "Seule une demande en validation reçoit des décisions.");
        ChangeValidation v = validationRepo.findByTenantIdAndChangeIdOrderByOrdinalAsc(tenantId, id).stream()
                .filter(x -> x.getId().equals(validationId)).findFirst()
                .orElseThrow(() -> new MocException(MocException.Kind.NOT_FOUND, "moc.validation-not-found",
                        "Carte de validation introuvable."));
        if (v.getDecision() != ApprovalDecision.PENDING) {
            throw MocException.conflict("moc.already-decided", "Ce rôle a déjà statué.");
        }
        if (form.decision() == ApprovalDecision.PENDING) {
            throw MocException.invalid("moc.decision-required", "Approuver ou rejeter.");
        }
        if (acteur.equals(c.getRequesterUserId())) {
            throw new MocException(MocException.Kind.FORBIDDEN, "moc.self-validation",
                    "Le demandeur ne peut pas valider sa propre demande.");
        }
        MocSettings s = settings.load(tenantId);
        ValidationRole role = s.roles().stream().filter(r -> v.getRoleId().equals(r.id())).findFirst()
                .orElse(null);
        if (role == null || !role.heldBy(acteur)) {
            throw new MocException(MocException.Kind.FORBIDDEN, "moc.not-holder",
                    "Seul un titulaire du rôle « " + v.getRoleLabel() + " » peut statuer sur cette carte.",
                    Map.of("role", v.getRoleLabel()));
        }
        String commentaire = blankToNull(form.comment());
        boolean refus = form.decision() == ApprovalDecision.REJECTED;
        if (refus && commentaire == null) {
            throw MocException.invalid("moc.comment-required", "Un refus doit être motivé par un commentaire.");
        }
        Instant now = clock.instant();
        v.setDecision(form.decision());
        v.setComment(commentaire);
        v.setDecidedBy(acteur);
        v.setDecidedByName(CurrentUser.displayName().orElseGet(() -> role.holders().stream()
                .filter(h -> h.userId().equals(acteur)).map(ValidationRole.RoleHolder::label)
                .findFirst().orElse(null)));
        v.setDecidedAt(now);
        validationRepo.save(v);
        trace(c, refus ? MocHistoryEvent.VALIDATION_REJECTED : MocHistoryEvent.VALIDATION_APPROVED,
                v.getRoleLabel() + (commentaire == null ? "" : " — " + commentaire));
        audit.change(tenantId, acteur, refus ? "validation.rejected" : "validation.approved", c, v.getRoleLabel());
        notifier.retire(c, acteur);

        List<ChangeValidation> cartes = validationRepo.findByTenantIdAndChangeIdOrderByOrdinalAsc(tenantId, id);
        if (refus) {
            conclure(c, ChangeRequestStatus.REJECTED, MocHistoryEvent.REJECTED, commentaire, now, acteur);
            notifier.validationClosed(c, MocNotifier.recipients(enAttente(cartes, s), c.getRequesterUserId()));
            notifier.decided(c, false, commentaire);
        } else if (cartes.stream().allMatch(x -> x.getDecision() == ApprovalDecision.APPROVED)) {
            conclure(c, ChangeRequestStatus.APPROVED, MocHistoryEvent.APPROVED, null, now, acteur);
            notifier.decided(c, true, null);
        } else {
            requestRepo.save(c);
        }
        return detail(c, s);
    }

    /** Une demande rejetée repart en analyse ; ses cartes seront recréées à la soumission suivante. */
    @Transactional
    public MocDto.Detail reopen(UUID id) {
        UUID tenantId = MocSettingsService.requireTenantId();
        ChangeRequest c = load(tenantId, id);
        exigerConducteur(c);
        exiger(c, ChangeRequestStatus.REJECTED, "moc.not-rejected", "Seule une demande rejetée se rouvre.");
        MocSettings s = settings.load(tenantId);
        validationRepo.deleteByTenantIdAndChangeId(tenantId, id);
        c.setStatus(ChangeRequestStatus.IN_ANALYSIS);
        c.setSubmittedAt(null);
        c.setClosedAt(null);
        MocAnalysis.apply(c, assessmentRepo.findByTenantIdAndChangeId(tenantId, id), s);
        requestRepo.save(c);
        trace(c, MocHistoryEvent.REOPENED, null);
        audit.change(tenantId, CurrentUser.requireUserId(), "reopened", c, null);
        return detail(c, s);
    }

    // ---------- export ----------

    /** Toutes les demandes du client, avec leur analyse et leurs décisions, pour le classeur. */
    @Transactional(readOnly = true)
    public List<MocWorkbook.Row> exportRows() {
        UUID tenantId = MocSettingsService.requireTenantId();
        List<ChangeRequest> demandes = requestRepo.findByTenantId(tenantId, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<UUID> ids = demandes.stream().map(ChangeRequest::getId).toList();
        Map<UUID, List<ChangeImpactAssessment>> evaluations = ids.isEmpty() ? Map.of()
                : assessmentRepo.findByTenantIdAndChangeIdIn(tenantId, ids).stream()
                .collect(Collectors.groupingBy(ChangeImpactAssessment::getChangeId));
        Map<UUID, List<ChangeValidation>> cartes = ids.isEmpty() ? Map.of()
                : validationRepo.findByTenantIdAndChangeIdIn(tenantId, ids).stream()
                .collect(Collectors.groupingBy(ChangeValidation::getChangeId));
        return demandes.stream().map(c -> new MocWorkbook.Row(c,
                MocAnalysis.levels(evaluations.getOrDefault(c.getId(), List.of())),
                cartes.getOrDefault(c.getId(), List.of()).stream()
                        .sorted((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal())).toList()))
                .toList();
    }

    // ---------- interne ----------

    private void conclure(ChangeRequest c, ChangeRequestStatus statut, MocHistoryEvent evenement, String detail,
                          Instant now, UUID acteur) {
        c.setStatus(statut);
        c.setClosedAt(now);
        requestRepo.save(c);
        trace(c, evenement, detail);
        audit.change(c.getTenantId(), acteur, statut == ChangeRequestStatus.APPROVED ? "approved" : "rejected",
                c, null);
    }

    /** Les titulaires des cartes encore en attente, par libellé de rôle. */
    private static Map<String, List<ValidationRole.RoleHolder>> enAttente(List<ChangeValidation> cartes,
                                                                         MocSettings s) {
        Map<UUID, ValidationRole> roles = s.roles().stream().filter(r -> r.id() != null)
                .collect(Collectors.toMap(ValidationRole::id, Function.identity()));
        Map<String, List<ValidationRole.RoleHolder>> m = new LinkedHashMap<>();
        for (ChangeValidation v : cartes) {
            ValidationRole r = roles.get(v.getRoleId());
            if (v.getDecision() == ApprovalDecision.PENDING && r != null) {
                m.put(v.getRoleLabel(), r.holders());
            }
        }
        return m;
    }

    /** Les demandes en validation qui attendent une décision de cet utilisateur. */
    private Set<UUID> enAttenteDe(UUID tenantId, UUID moi, List<ChangeRequest> demandes) {
        if (moi == null) {
            return Set.of();
        }
        List<UUID> enValidation = demandes.stream()
                .filter(c -> c.getStatus() == ChangeRequestStatus.IN_VALIDATION
                        && !moi.equals(c.getRequesterUserId()))
                .map(ChangeRequest::getId).toList();
        if (enValidation.isEmpty()) {
            return Set.of();
        }
        Set<UUID> mesRoles = settings.load(tenantId).roles().stream().filter(r -> r.heldBy(moi))
                .map(ValidationRole::id).collect(Collectors.toSet());
        return validationRepo.findByTenantIdAndChangeIdIn(tenantId, enValidation).stream()
                .filter(v -> v.getDecision() == ApprovalDecision.PENDING && mesRoles.contains(v.getRoleId()))
                .map(ChangeValidation::getChangeId).collect(Collectors.toSet());
    }

    private MocDto.Detail detail(ChangeRequest c, MocSettings s) {
        UUID tenantId = c.getTenantId();
        UUID moi = CurrentUser.userId().orElse(null);
        Map<ImpactDomain, ChangeImpactAssessment> evaluations = assessmentRepo
                .findByTenantIdAndChangeId(tenantId, c.getId()).stream()
                .collect(Collectors.toMap(ChangeImpactAssessment::getDomain, Function.identity()));
        List<MocDto.ImpactView> impacts = new ArrayList<>();
        for (ImpactDomain d : ImpactDomain.values()) {
            ChangeImpactAssessment a = evaluations.get(d);
            impacts.add(new MocDto.ImpactView(d, s.weights().weightOf(d), a == null ? null : a.getLevel(),
                    a == null ? null : a.getNotes()));
        }
        boolean demandeur = moi != null && moi.equals(c.getRequesterUserId());
        return new MocDto.Detail(c.getId(), c.getCode(), c.getTitle(), c.getType(), c.getSite(),
                c.getDescription(), c.getJustification(), c.getPlannedFor(), c.getStatus(), c.getRequesterName(),
                demandeur, conducteur(c, moi), c.getCreatedAt(), c.getSubmittedAt(), c.getClosedAt(),
                c.getImpactScore(), c.getClassification(), impacts, cartes(c, s, moi, demandeur),
                historyRepo.findByTenantIdAndChangeIdOrderByOccurredAtDesc(tenantId, c.getId()).stream()
                        .map(h -> new MocDto.HistoryView(h.getOccurredAt(), h.getEvent(), h.getActorName(),
                                h.getDetail())).toList());
    }

    /**
     * Les cartes : figées (créées à la soumission) dès la validation ; en aperçu
     * avant, d'après la classification courante et le paramétrage du moment.
     */
    private List<MocDto.ValidationView> cartes(ChangeRequest c, MocSettings s, UUID moi, boolean demandeur) {
        Map<UUID, ValidationRole> roles = s.roles().stream().filter(r -> r.id() != null)
                .collect(Collectors.toMap(ValidationRole::id, Function.identity()));
        if (c.getStatus().open()) {
            if (!c.getClassification().assessed()) {
                return List.of();
            }
            return s.requiredRoles(c.getClassification()).stream().map(r -> new MocDto.ValidationView(
                    null, r.label(), ApprovalDecision.PENDING, null, null, null, labels(r),
                    r.heldBy(moi), false, true)).toList();
        }
        return validationRepo.findByTenantIdAndChangeIdOrderByOrdinalAsc(c.getTenantId(), c.getId()).stream()
                .map(v -> {
                    ValidationRole r = roles.get(v.getRoleId());
                    boolean titulaire = r != null && r.heldBy(moi);
                    boolean peut = c.getStatus() == ChangeRequestStatus.IN_VALIDATION
                            && v.getDecision() == ApprovalDecision.PENDING && titulaire && !demandeur;
                    return new MocDto.ValidationView(v.getId(), v.getRoleLabel(), v.getDecision(),
                            v.getDecidedByName(), v.getDecidedAt(), v.getComment(),
                            r == null ? List.of() : labels(r), titulaire, peut, false);
                }).toList();
    }

    private static List<String> labels(ValidationRole r) {
        return r.holders().stream().map(ValidationRole.RoleHolder::label).toList();
    }

    private void trace(ChangeRequest c, MocHistoryEvent event, String detail) {
        ChangeHistoryEntry h = new ChangeHistoryEntry();
        h.setTenantId(c.getTenantId());
        h.setChangeId(c.getId());
        h.setOccurredAt(clock.instant());
        h.setEvent(event);
        h.setActorId(CurrentUser.userId().orElse(null));
        h.setActorName(CurrentUser.displayName().orElse(null));
        h.setDetail(detail == null ? null : (detail.length() > 1000 ? detail.substring(0, 1000) : detail));
        historyRepo.save(h);
    }

    /**
     * Le prochain code {@code MOC-AAAA-NNN} du client pour l'année, sous verrou.
     * Une collision avec un code repris d'avant le MOC fait passer au rang suivant.
     */
    String nextCode(UUID tenantId) {
        int annee = clock.instant().atZone(ZoneOffset.UTC).getYear();
        ChangeCodeCounter.Key cle = new ChangeCodeCounter.Key(tenantId, annee);
        ChangeCodeCounter compteur = counterRepo.findById(cle)
                .orElseGet(() -> counterRepo.save(new ChangeCodeCounter(tenantId, annee, 0)));
        int rang = compteur.getLastValue();
        String code;
        do {
            rang++;
            code = CODE_PREFIX + annee + "-" + String.format(Locale.ROOT, "%03d", rang);
        } while (requestRepo.existsByTenantIdAndCode(tenantId, code));
        compteur.setLastValue(rang);
        counterRepo.save(compteur);
        return code;
    }

    private static void appliquer(ChangeRequest c, MocDto.RequestForm form) {
        c.setTitle(form.title().strip());
        c.setType(form.type());
        c.setSite(blankToNull(form.site()));
        c.setDescription(blankToNull(form.description()));
        c.setJustification(blankToNull(form.justification()));
        c.setPlannedFor(form.plannedFor());
    }

    /** Vrai si l'utilisateur du jeton conduit cette demande : son demandeur, ou un gestionnaire. */
    private boolean conducteur(ChangeRequest c, UUID moi) {
        return (moi != null && moi.equals(c.getRequesterUserId())) || authz.has(Permission.CHANGE_ANALYZE);
    }

    private void exigerConducteur(ChangeRequest c) {
        if (!conducteur(c, CurrentUser.requireUserId())) {
            throw new MocException(MocException.Kind.FORBIDDEN, "moc.not-owner",
                    "Seul le demandeur ou un gestionnaire des changements peut conduire cette demande.");
        }
    }

    private static void exiger(ChangeRequest c, ChangeRequestStatus attendu, String code, String message) {
        if (c.getStatus() != attendu) {
            throw MocException.conflict(code, message + " (statut : " + c.getStatus() + ")");
        }
    }

    private ChangeRequest load(UUID tenantId, UUID id) {
        return requestRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ChangeRequestNotFoundException(id));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
