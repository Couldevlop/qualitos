package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.kpi.KpiDefinition;
import com.openlab.qualitos.quality.kpi.KpiDefinitionRepository;
import com.openlab.qualitos.quality.kpi.KpiMeasurement;
import com.openlab.qualitos.quality.kpi.KpiMeasurementRepository;
import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.domain.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Les objectifs qualité (ISO 9001 §6.2) : les définir, les confronter aux KPI
 * réels, et proposer une CAPA quand l'écart dure.
 *
 * <p><b>La CAPA est proposée, jamais imposée.</b> Le service constate l'écart et
 * rédige le dossier ; c'est le pilote qui l'accepte, le rejette avec
 * justification, ou lui ajoute une action. C'est l'invariant de la plateforme
 * (CLAUDE.md §12) et ce que l'ISO demande : une action décidée par qui en répond.
 *
 * <p><b>Rien n'est figé en base de ce qui se calcule.</b> Le statut d'un
 * objectif se recalcule à chaque lecture depuis les relevés du KPI. Le figer le
 * ferait mentir dès qu'un relevé est corrigé — et un relevé se corrige.
 *
 * <p>Voir l'ADR 0133.
 */
@Service
public class QualityObjectiveService {

    private static final Logger log = LoggerFactory.getLogger(QualityObjectiveService.class);

    /** Le droit de piloter : il garde la confrontation, pas la lecture. */
    private final AuthorizationService authz;

    /** Au-delà, une revue ne lit plus : vingt-quatre périodes couvrent deux ans de mensuel. */
    private static final int MAX_PERIODS = 24;

    /** Les dates du dossier sont écrites en français : le corps est de la prose. */
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QualityObjectiveRepository objectives;
    private final QualityObjectiveProposalRepository proposals;
    private final KpiDefinitionRepository kpis;
    private final KpiMeasurementRepository measurements;
    private final CapaService capaService;
    private final CapaCaseRepository capaCases;

    public QualityObjectiveService(QualityObjectiveRepository objectives,
                                   QualityObjectiveProposalRepository proposals,
                                   KpiDefinitionRepository kpis,
                                   KpiMeasurementRepository measurements,
                                   CapaService capaService,
                                   CapaCaseRepository capaCases,
                                   AuthorizationService authz) {
        this.objectives = objectives;
        this.proposals = proposals;
        this.kpis = kpis;
        this.measurements = measurements;
        this.capaService = capaService;
        this.capaCases = capaCases;
        this.authz = authz;
    }

    // ---------- définition ----------

    @Transactional
    public ObjectiveDto.ObjectiveResponse create(ObjectiveDto.CreateRequest req) {
        UUID tenantId = requireTenantId();
        KpiDefinition kpi = loadKpi(req.kpiId(), tenantId);
        validerSeuils(req.periodMonths(), req.targetValue(),
                req.thresholdWarning(), req.thresholdCritical(), kpi);

        QualityObjective o = new QualityObjective();
        o.setTenantId(tenantId);
        o.setTitle(req.title().trim());
        o.setKpiId(kpi.getId());
        o.setProcessId(req.processId());
        o.setProcessName(blankToNull(req.processName()));
        o.setIsoClause(blankToNull(req.isoClause()));
        o.setPeriodMonths(req.periodMonths());
        o.setTargetValue(req.targetValue());
        o.setThresholdWarning(req.thresholdWarning());
        o.setThresholdCritical(req.thresholdCritical());
        o.setWarningPeriods(req.warningPeriods());
        o.setCriticalPeriods(req.criticalPeriods());
        o.setRecoveryPeriods(req.recoveryPeriods());
        o.setStatus(ObjectiveLifecycle.ACTIVE);
        o.setCreatedBy(CurrentUser.requireUserId());
        return toResponse(objectives.save(o), kpi, null);
    }

    @Transactional
    public ObjectiveDto.ObjectiveResponse update(UUID id, ObjectiveDto.UpdateRequest req) {
        UUID tenantId = requireTenantId();
        QualityObjective o = load(id, tenantId);
        // Le KPI visé ne change PAS : un objectif qui changerait d'indicateur
        // rendrait incomparable son propre historique, et les dossiers qu'il a
        // ouverts cesseraient de se rapporter à ce qu'ils mesuraient.
        if (req.title() != null && !req.title().isBlank()) o.setTitle(req.title().trim());
        if (req.processId() != null) o.setProcessId(req.processId());
        if (req.processName() != null) o.setProcessName(blankToNull(req.processName()));
        if (req.isoClause() != null) o.setIsoClause(blankToNull(req.isoClause()));
        if (req.periodMonths() != null) o.setPeriodMonths(req.periodMonths());
        if (req.targetValue() != null) o.setTargetValue(req.targetValue());
        if (req.thresholdWarning() != null) o.setThresholdWarning(req.thresholdWarning());
        if (req.thresholdCritical() != null) o.setThresholdCritical(req.thresholdCritical());
        if (req.warningPeriods() != null) o.setWarningPeriods(req.warningPeriods());
        if (req.criticalPeriods() != null) o.setCriticalPeriods(req.criticalPeriods());
        if (req.recoveryPeriods() != null) o.setRecoveryPeriods(req.recoveryPeriods());
        if (req.lifecycle() != null) o.setStatus(req.lifecycle());

        KpiDefinition kpi = loadKpi(o.getKpiId(), tenantId);
        validerSeuils(o.getPeriodMonths(), o.getTargetValue(),
                o.getThresholdWarning(), o.getThresholdCritical(), kpi);
        return toResponse(objectives.save(o), kpi, apprecier(o, kpi, null));
    }

    /**
     * Retire un objectif du suivi, sans l'effacer.
     *
     * <p>Effacer l'objectif d'un trimestre passé effacerait la raison des CAPA
     * qu'il a fait ouvrir, et une revue de direction relue l'année suivante ne se
     * comprendrait plus.
     */
    @Transactional
    public void archive(UUID id) {
        UUID tenantId = requireTenantId();
        QualityObjective o = load(id, tenantId);
        o.setStatus(ObjectiveLifecycle.ARCHIVED);
        objectives.save(o);
    }

    @Transactional(readOnly = true)
    public ObjectiveDto.ObjectiveResponse get(UUID id) {
        UUID tenantId = requireTenantId();
        QualityObjective o = load(id, tenantId);
        KpiDefinition kpi = loadKpi(o.getKpiId(), tenantId);
        return toResponse(o, kpi, apprecier(o, kpi, null));
    }

    @Transactional(readOnly = true)
    public List<ObjectiveDto.ProposalResponse> history(UUID objectiveId) {
        UUID tenantId = requireTenantId();
        QualityObjective o = load(objectiveId, tenantId);
        return proposals.findByTenantIdAndObjectiveIdOrderByCreatedAtDesc(tenantId, o.getId())
                .stream().map(p -> toResponse(p, o.getTitle())).toList();
    }

    // ---------- revue ----------

    /**
     * L'écran « Objectifs vs KPI réels » : chaque objectif face à son KPI, les
     * quatre compteurs, et ce qui attend une décision.
     *
     * @param processId filtre sur un processus, ou {@code null} pour tous
     * <p><b>CETTE MÉTHODE N'ÉCRIT RIEN</b>, et c'est volontaire : la lecture de
     * la revue est ouverte à tout authentifié (c'est ce qu'une organisation
     * montre à ses équipes), alors que confronter aux mesures est un acte de
     * pilotage. Tant que la confrontation vivait ici, la lecture ouverte
     * exécutait ce que {@code objective.manage} refuse — et une violation de
     * clé d'unicité, qui ne survient qu'au commit, emportait la revue entière.
     * La confrontation est donc sortie dans {@link #confronterAuxMesures()},
     * appelée hors de cette transaction.
     *
     * @param processId filtre sur un processus, ou {@code null} pour tous
     * @param asOf      fin de la période observée, ou {@code null} pour l'état courant.
     *                  Une revue mensuelle se tient souvent après la fin du mois, et doit
     *                  pouvoir se rejouer telle qu'elle a été lue.
     */
    @Transactional(readOnly = true)
    public ObjectiveDto.ReviewResponse review(UUID processId, LocalDate asOf) {
        UUID tenantId = requireTenantId();
        List<QualityObjective> suivis = objectives
                .findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, ObjectiveLifecycle.ACTIVE)
                .stream()
                .filter(o -> processId == null || processId.equals(o.getProcessId()))
                .toList();

        Map<UUID, KpiDefinition> catalogue = chargerKpis(suivis, tenantId);
        List<ObjectiveDto.ObjectiveResponse> lignes = new ArrayList<>();
        int atteints = 0, alertes = 0, critiques = 0;
        for (QualityObjective o : suivis) {
            KpiDefinition kpi = catalogue.get(o.getKpiId());
            if (kpi == null) continue; // KPI disparu : l'objectif ne se compare à rien
            ObjectiveAssessment a = apprecier(o, kpi, asOf);
            lignes.add(toResponse(o, kpi, a));
            switch (a.status()) {
                case ON_TARGET -> atteints++;
                case WARNING -> alertes++;
                case CRITICAL -> critiques++;
                case NOT_EVALUATED -> { }
            }
        }

        Map<UUID, String> titres = suivis.stream().collect(LinkedHashMap::new,
                (m, o) -> m.put(o.getId(), o.getTitle()), Map::putAll);
        List<ObjectiveDto.ProposalResponse> enAttente = proposals
                .findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, ProposalStatus.PENDING)
                .stream()
                .filter(p -> titres.containsKey(p.getObjectiveId()))
                .map(p -> toResponse(p, titres.get(p.getObjectiveId())))
                .toList();

        return new ObjectiveDto.ReviewResponse(asOf,
                new ObjectiveDto.Counters(lignes.size(), atteints, alertes, critiques),
                lignes, enAttente);
    }

    /**
     * Confronte tous les objectifs suivis à leurs mesures — l'automatisme de
     * l'ADR 0146, devenu un ACTE DE PILOTAGE au lieu d'un effet de la lecture.
     *
     * <p><b>LE DROIT SE VÉRIFIE ICI</b>, pas à l'annotation du point d'entrée :
     * la garde voyage avec le comportement, donc tout appelant futur la
     * rencontre. Sans {@code objective.manage}, la méthode ne fait rien et ne
     * refuse rien — elle n'est pas demandée par qui lit, elle est seulement
     * déclenchée à l'occasion de sa visite. Lever un 403 fermerait une revue
     * que le produit veut ouverte.
     *
     * <p>Un objectif qu'on ne peut pas confronter — KPI retiré du catalogue,
     * sens d'amélioration absent, mesures illisibles — est PASSÉ : il n'arrête
     * pas les autres, et il paraît simplement sans proposition.
     *
     * <p>Réévaluer la même période ne crée rien de neuf : {@link #evaluate} rend
     * la proposition déjà là, et la clé {@code uk_qo_proposals_periode} dit la
     * même chose en base. Un écran rafraîchi trois fois ne propose donc pas
     * trois fois le même dossier. Deux visites SIMULTANÉES peuvent en revanche
     * se croiser : la seconde violera la clé au commit, hors de portée du
     * {@code catch} ci-dessous. C'est pourquoi cette transaction est distincte
     * de celle de la revue — elle échoue seule, la lecture reste servie, et la
     * proposition paraît à la visite suivante.
     */
    @Transactional
    public void confronterAuxMesures() {
        if (!authz.has(Permission.OBJECTIVE_MANAGE)) {
            return;
        }
        UUID tenantId = requireTenantId();
        for (QualityObjective o : objectives
                .findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, ObjectiveLifecycle.ACTIVE)) {
            try {
                evaluate(o.getId());
            } catch (RuntimeException e) {
                log.warn("Objectif {} non confronté à ses mesures : {}",
                        o.getId(), e.toString());
            }
        }
    }

    // ---------- règle de déclenchement ----------

    /**
     * Confronte un objectif à ses mesures et, si la règle déclenche, RÉDIGE une
     * CAPA à soumettre au pilote.
     *
     * <p>Aucun dossier n'est créé ici. La proposition est une étape : elle se
     * relit, s'accepte ou se rejette avec justification. Réévaluer la même
     * période ne crée pas une seconde proposition — un écran rafraîchi trois fois
     * proposerait sinon trois fois le même dossier.
     *
     * @return la proposition en attente pour cette période, ou vide si la règle
     *         ne déclenche pas
     */
    @Transactional
    public Optional<ObjectiveDto.ProposalResponse> evaluate(UUID objectiveId) {
        UUID tenantId = requireTenantId();
        QualityObjective o = load(objectiveId, tenantId);
        if (o.getStatus() != ObjectiveLifecycle.ACTIVE) {
            throw new ObjectiveStateException(
                    "An archived objective no longer raises CAPA proposals");
        }
        KpiDefinition kpi = loadKpi(o.getKpiId(), tenantId);
        List<ObjectivePeriodSeries.Period> periodes = periodes(o, null);
        ObjectiveAssessment a = ObjectiveEvaluator.evaluate(o, kpi.getDirection(),
                ObjectivePeriodSeries.values(periodes), niveauMaintenu(tenantId, o));
        if (a.triggeredLevel() == null || periodes.isEmpty()) {
            return Optional.empty();
        }
        ObjectivePeriodSeries.Period derniere = periodes.get(periodes.size() - 1);

        // La même période et le même niveau ne se proposent qu'une fois. La
        // contrainte uk_qo_proposals_periode dit la même chose en base : ici pour
        // l'éviter, là pour l'interdire.
        Optional<QualityObjectiveProposal> deja = proposals
                .findByTenantIdAndObjectiveIdAndLevelAndPeriodStart(
                        tenantId, o.getId(), a.triggeredLevel(), derniere.start());
        if (deja.isPresent()) {
            return deja.map(p -> toResponse(p, o.getTitle()));
        }

        QualityObjectiveProposal p = new QualityObjectiveProposal();
        p.setTenantId(tenantId);
        p.setObjectiveId(o.getId());
        p.setLevel(a.triggeredLevel());
        p.setPeriodStart(derniere.start());
        p.setPeriodEnd(derniere.end());
        p.setObservedValue(derniere.value());
        p.setBreachPeriods(a.triggeredLevel() == ObjectiveLevel.CRITICAL
                ? a.criticalBreaches() : a.warningBreaches());
        p.setTitle(titreDuDossier(o, a.triggeredLevel()));
        p.setDescription(corpsDuDossier(o, kpi, a, derniere));
        p.setEffectivenessCriterion(critereDEfficacite(o, kpi));
        p.setStatus(ProposalStatus.PENDING);
        return Optional.of(toResponse(proposals.save(p), o.getTitle()));
    }

    /** Confronte tous les objectifs suivis et rend les propositions nées de ce passage. */
    @Transactional
    public List<ObjectiveDto.ProposalResponse> evaluateAll() {
        UUID tenantId = requireTenantId();
        List<ObjectiveDto.ProposalResponse> nees = new ArrayList<>();
        for (QualityObjective o : objectives.findByTenantIdAndStatusOrderByCreatedAtDesc(
                tenantId, ObjectiveLifecycle.ACTIVE)) {
            evaluate(o.getId()).ifPresent(nees::add);
        }
        return nees;
    }

    // ---------- décision du pilote ----------

    /**
     * « Ajouter une action » : le pilote décide d'agir, et le dossier naît de ce
     * qu'il écrit.
     *
     * <p><b>Rien n'est prérédigé.</b> La fenêtre CAPA s'ouvre vide ; l'intitulé,
     * la description, le responsable et l'échéance sont ceux qu'on y saisit. La
     * recette l'a demandé ainsi : un dossier prérempli par la règle se signait
     * sans être lu, et son action ne disait pas ce qu'on allait vraiment faire.
     *
     * <p><b>Quatre choses restent au serveur</b>, et la fenêtre ne les demande
     * pas : la source ({@code QUALITY_OBJECTIVE}), la criticité — celle du
     * niveau atteint —, la vérification d'efficacité, OBLIGATOIRE sans question
     * posée, et son critère, VERROUILLÉ. Un écart d'objectif ne se referme pas
     * sur une déclaration : il se referme quand le KPI est revenu sous la cible,
     * et laisser saisir le critère reviendrait à clore sur une cible plus facile
     * que celle qu'on s'était donnée (ADR 0133 §4).
     *
     * <p><b>Le dossier laisse TOUJOURS sa trace sur l'objectif</b>, sous la
     * forme d'une proposition {@code ACCEPTED} qui porte sa référence. Une
     * proposition encore en attente est soldée ; s'il n'y en a pas — la recette
     * veut le bouton dès la première ligne en alerte, avant que la règle n'ait
     * déclenché — elle est ÉCRITE ici.
     *
     * <p>Cette ligne n'est pas une écriture de confort : elle est le SEUL lien
     * entre le dossier et l'objectif. {@link ObjectiveRecoveryAdapter#forCase}
     * la cherche pour répondre « le KPI est-il revenu sous la cible ? », et sans
     * elle il répond « ce dossier ne vient pas d'un objectif » — le dossier se
     * clôt alors librement, pendant que la fenêtre a annoncé une vérification
     * d'efficacité verrouillée. Le contrôle affiché n'existerait pas (ADR 0133).
     * Elle tient aussi la revue : {@code niveauMaintenu} la lit pour retenir
     * l'objectif jusqu'au retour DURABLE, et la clé {@code uk_qo_proposals_periode}
     * empêche le moteur de proposer une seconde fois la période déjà traitée.
     *
     * <p>Voir l'ADR 0143.
     */
    @Transactional
    public ObjectiveDto.CapaOpenedResponse openCapa(UUID objectiveId,
                                                    ObjectiveDto.OpenCapaRequest req) {
        UUID tenantId = requireTenantId();
        QualityObjective o = load(objectiveId, tenantId);
        if (o.getStatus() != ObjectiveLifecycle.ACTIVE) {
            throw new ObjectiveStateException(
                    "An archived objective no longer opens CAPA cases");
        }
        KpiDefinition kpi = loadKpi(o.getKpiId(), tenantId);
        ObjectiveAssessment a = apprecier(o, kpi, null);
        ObjectiveLevel niveau = niveauATraiter(a);
        if (niveau == null) {
            throw new ObjectiveStateException(
                    "This objective is not in alert or critical: there is no deviation to treat");
        }

        List<ObjectivePeriodSeries.Period> periodes = periodes(o, null);
        if (periodes.isEmpty()) {
            throw new ObjectiveStateException(
                    "This objective has no measured period: there is no deviation to treat");
        }
        ObjectivePeriodSeries.Period derniere = periodes.get(periodes.size() - 1);

        // La trace a trois sources possibles, dans cet ordre : la proposition
        // DE CETTE PÉRIODE si elle existe deja -- sans quoi la cle
        // uk_qo_proposals_periode refuserait la ligne --, une proposition
        // encore en attente sur une autre periode, puis la creation.
        QualityObjectiveProposal lien = proposals
                .findByTenantIdAndObjectiveIdAndLevelAndPeriodStart(
                        tenantId, o.getId(), niveau, derniere.start())
                .or(() -> proposals
                        .findByTenantIdAndObjectiveIdOrderByCreatedAtDesc(tenantId, o.getId())
                        .stream().filter(QualityObjectiveProposal::isPending).findFirst())
                .orElseGet(() -> traceDeLEcart(tenantId, o, kpi, a, niveau, derniere,
                        req.title().trim()));

        String source = referenceSource(o, niveau, derniere.start());

        CapaDto.CaseResponse dossier = capaService.openCase(
                new CapaDto.OpenCaseRequest(
                        req.title().trim(),
                        blankToNull(req.description()),
                        // Un écart déjà survenu se traite par un correctif : le
                        // nommer préventif laisserait croire qu'il ne s'est pas
                        // produit (ISO 9001 §10.2).
                        CapaType.CORRECTIVE,
                        // La nature de l'action suit celle du dossier.
                        null,
                        niveau.criticity(),
                        CapaSourceType.QUALITY_OBJECTIVE,
                        source,
                        req.ownerId(),
                        blankToNull(req.ownerName()),
                        req.dueDate(),
                        // Obligatoire, sans question posée : voir plus haut.
                        Boolean.TRUE,
                        req.ownerId(),
                        blankToNull(req.ownerName()),
                        instructionsDeVerification(o),
                        critereDEfficacite(o, kpi)),
                CapaSourceType.QUALITY_OBJECTIVE,
                source);

        lien.setStatus(ProposalStatus.ACCEPTED);
        lien.setCapaCaseId(dossier.id());
        // Une proposition d'abord DECLINEE puis traitee ne garde pas son motif
        // de refus : la ligne porte la decision finale, et le refus reste dans
        // le journal d'audit, qui lui est horodate.
        lien.setRejectionReason(null);
        lien.setDecidedBy(CurrentUser.userId().orElse(null));
        lien.setDecidedAt(Instant.now());
        return new ObjectiveDto.CapaOpenedResponse(
                dossier.id(), proposals.save(lien).getId());
    }

    /**
     * La ligne qui relie ce dossier a l'ecart qu'il traite, quand la regle n'a
     * pas encore propose.
     *
     * <p>Elle dit l'ECART CONSTATE, jamais un declenchement qui n'a pas eu lieu :
     * {@code breachPeriods} porte le nombre de periodes consecutives reellement
     * depassees, qui peut rester sous le compte exige par la regle -- c'est
     * precisement le cas que la recette veut pouvoir traiter a la main.
     *
     * <p>Son intitule est celui du dossier REELLEMENT ouvert, pas un titre que
     * personne n'a propose.
     */
    private QualityObjectiveProposal traceDeLEcart(
            UUID tenantId, QualityObjective o, KpiDefinition kpi,
            ObjectiveAssessment a, ObjectiveLevel niveau,
            ObjectivePeriodSeries.Period derniere, String titreDuDossier) {
        QualityObjectiveProposal p = new QualityObjectiveProposal();
        p.setTenantId(tenantId);
        p.setObjectiveId(o.getId());
        p.setLevel(niveau);
        p.setPeriodStart(derniere.start());
        p.setPeriodEnd(derniere.end());
        p.setObservedValue(derniere.value());
        p.setBreachPeriods(niveau == ObjectiveLevel.CRITICAL
                ? a.criticalBreaches() : a.warningBreaches());
        p.setTitle(titreDuDossier);
        p.setDescription(corpsDuDossier(o, kpi, a, derniere));
        p.setEffectivenessCriterion(critereDEfficacite(o, kpi));
        p.setStatus(ProposalStatus.PENDING);
        return p;
    }

    /**
     * Le niveau que l'écart appelle aujourd'hui, ou {@code null} s'il n'y a rien
     * à traiter.
     *
     * <p>Le STATUT et non le seul niveau déclenché : la règle ne déclenche qu'au
     * bout de ses périodes consécutives, alors que la recette veut pouvoir agir
     * dès la première ligne passée en alerte.
     */
    private ObjectiveLevel niveauATraiter(ObjectiveAssessment a) {
        return switch (a.status()) {
            case CRITICAL -> ObjectiveLevel.CRITICAL;
            case WARNING -> ObjectiveLevel.WARNING;
            case ON_TARGET, NOT_EVALUATED -> null;
        };
    }

    /**
     * « Rejeter avec justification » : le pilote ne retient pas la proposition.
     *
     * <p>Le motif est obligatoire. Sans lui, un objectif critique sans dossier ne
     * se distinguerait pas d'un objectif critique que personne n'a regardé — et
     * c'est précisément la question qu'un auditeur pose.
     */
    @Transactional
    public ObjectiveDto.ProposalResponse reject(UUID proposalId, String reason) {
        UUID tenantId = requireTenantId();
        QualityObjectiveProposal p = loadProposal(proposalId, tenantId);
        if (!p.isPending()) {
            throw new ObjectiveStateException(
                    "This proposal has already been decided (" + p.getStatus() + ")");
        }
        if (reason == null || reason.isBlank()) {
            throw new ObjectiveValidationException(
                    "Rejecting a proposed CAPA requires a justification: an objective left "
                    + "without action must say why");
        }
        QualityObjective o = load(p.getObjectiveId(), tenantId);
        p.setStatus(ProposalStatus.REJECTED);
        p.setRejectionReason(reason.trim());
        p.setDecidedBy(CurrentUser.userId().orElse(null));
        p.setDecidedAt(Instant.now());
        return toResponse(proposals.save(p), o.getTitle());
    }

    // ---------- appréciation ----------

    /** Confronte l'objectif à ses mesures, en tenant compte d'une CAPA encore ouverte. */
    ObjectiveAssessment apprecier(QualityObjective o, KpiDefinition kpi, LocalDate asOf) {
        List<ObjectivePeriodSeries.Period> periodes = periodes(o, asOf);
        return ObjectiveEvaluator.evaluate(o, kpi.getDirection(),
                ObjectivePeriodSeries.values(periodes), niveauMaintenu(o.getTenantId(), o));
    }

    private List<ObjectivePeriodSeries.Period> periodes(QualityObjective o, LocalDate asOf) {
        List<KpiMeasurement> releves = new ArrayList<>(
                measurements.findTop24ByKpiIdOrderByPeriodStartDesc(o.getKpiId()));
        if (asOf != null) {
            // La revue d'octobre ne voit pas novembre : la borne est la FIN du
            // mois demandé, sans quoi une revue rejouée ne dirait plus ce qu'on
            // y avait lu.
            Instant borne = asOf.withDayOfMonth(1).plusMonths(1)
                    .atStartOfDay(ZoneOffset.UTC).toInstant();
            releves.removeIf(m -> m.getPeriodStart() == null
                    || !m.getPeriodStart().isBefore(borne));
        }
        releves.sort(Comparator.comparing(KpiMeasurement::getPeriodStart,
                Comparator.nullsFirst(Comparator.naturalOrder())));
        return ObjectivePeriodSeries.split(releves, o.getPeriodMonths(), MAX_PERIODS);
    }

    /**
     * Le niveau d'une CAPA acceptée encore ouverte sur cet objectif, ou
     * {@code null}.
     *
     * <p>Tant qu'un tel dossier existe, l'objectif ne repasse pas « atteint » au
     * premier relevé redevenu bon : il attend le retour DURABLE sous la cible.
     */
    private ObjectiveLevel niveauMaintenu(UUID tenantId, QualityObjective o) {
        return proposals.findByTenantIdAndObjectiveIdAndStatus(
                        tenantId, o.getId(), ProposalStatus.ACCEPTED).stream()
                .filter(p -> p.getCapaCaseId() != null)
                .filter(p -> dossierEncoreOuvert(tenantId, p.getCapaCaseId()))
                .map(QualityObjectiveProposal::getLevel)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(null);
    }

    /**
     * Le dossier est-il encore en cours ?
     *
     * <p>Clos ou rejeté, il ne maintient plus rien : clos, son efficacité a été
     * jugée ; rejeté, il n'a jamais traité l'écart, et c'est la règle de
     * déclenchement qui reprend la main sur les périodes suivantes. Un dossier
     * introuvable ne maintient rien non plus — on ne bloque pas un objectif sur
     * une référence morte.
     */
    private boolean dossierEncoreOuvert(UUID tenantId, UUID capaCaseId) {
        return capaCases.findByIdAndTenantId(capaCaseId, tenantId)
                .map(CapaCase::getStatus)
                .map(s -> s != CapaStatus.CLOSED && s != CapaStatus.REJECTED)
                .orElse(false);
    }

    // ---------- rédaction du dossier ----------

    private String titreDuDossier(QualityObjective o, ObjectiveLevel niveau) {
        String prefixe = niveau == ObjectiveLevel.CRITICAL
                ? "Objectif qualité critique" : "Objectif qualité en alerte";
        String titre = prefixe + " — " + o.getTitle();
        return titre.length() > 255 ? titre.substring(0, 255) : titre;
    }

    private String corpsDuDossier(QualityObjective o, KpiDefinition kpi,
                                  ObjectiveAssessment a,
                                  ObjectivePeriodSeries.Period derniere) {
        String unite = o.getThresholdWarning() == null || kpi.getUnit() == null
                ? "" : " " + kpi.getUnit();
        int periodes = a.triggeredLevel() == ObjectiveLevel.CRITICAL
                ? a.criticalBreaches() : a.warningBreaches();
        BigDecimal seuil = a.triggeredLevel() == ObjectiveLevel.CRITICAL
                ? o.getThresholdCritical() : o.getThresholdWarning();
        StringBuilder b = new StringBuilder()
                .append("Origine : écart sur objectif qualité")
                .append(o.getIsoClause() == null ? "" : " (ISO 9001 §" + o.getIsoClause() + ")")
                .append(".\n")
                .append("Objectif : ").append(o.getTitle())
                .append(" — cible ").append(nombre(o.getTargetValue())).append(unite).append(".\n")
                .append("KPI suivi : ").append(kpi.getName())
                .append(" (").append(kpi.getCode()).append(").\n")
                .append("Écart constaté : ").append(nombre(derniere.value())).append(unite)
                .append(" sur la période du ").append(jour(derniere.start()))
                .append(" au ").append(jour(derniere.end())).append(".\n")
                .append("Règle : le KPI dépasse le seuil ")
                .append(a.triggeredLevel() == ObjectiveLevel.CRITICAL ? "critique" : "d'alerte")
                .append(" (").append(nombre(seuil)).append(unite).append(") depuis ")
                .append(periodes).append(" période(s) consécutive(s) de ")
                .append(o.getPeriodMonths()).append(" mois.\n");
        if (o.getProcessName() != null) {
            b.append("Processus : ").append(o.getProcessName()).append(".\n");
        }
        return b.toString().stripTrailing();
    }

    /**
     * Le critère d'efficacité, rédigé par la règle et verrouillé.
     *
     * <p>Pour un écart d'objectif, l'efficacité EST le retour du KPI sous la
     * cible sur la durée définie — et l'objectif ne repasse « atteint » qu'à ce
     * moment. Le laisser saisir reviendrait à permettre de clore sur un critère
     * plus facile que celui qu'on s'était donné.
     */
    private String critereDEfficacite(QualityObjective o, KpiDefinition kpi) {
        String unite = kpi.getUnit() == null ? "" : " " + kpi.getUnit();
        String sens = switch (kpi.getDirection()) {
            case LOWER_IS_BETTER -> "revient à ou sous";
            case HIGHER_IS_BETTER -> "revient à ou au-dessus de";
        };
        String critere = "Le KPI « " + kpi.getName() + " » (" + kpi.getCode() + ") "
                + sens + " sa cible de " + nombre(o.getTargetValue()) + unite
                + " pendant " + o.getRecoveryPeriods() + " période(s) consécutive(s) de "
                + o.getPeriodMonths() + " mois. L'objectif « " + o.getTitle()
                + " » ne repasse « atteint » qu'à ce moment.";
        // Le champ du dossier CAPA s'arrête à 2000 caractères : on tronque ici
        // plutôt que de laisser l'ouverture échouer sur un titre de KPI bavard.
        return critere.length() > 2000 ? critere.substring(0, 2000) : critere;
    }

    /**
     * Ce qu'il y a à vérifier, en mots — distinct du critère, qui dit à quoi on
     * reconnaîtra l'effet.
     *
     * <p>Le vérificateur n'a rien à mesurer lui-même : la plateforme compte les
     * périodes. L'instruction le dit, pour qu'il ne se croie pas chargé d'un
     * calcul et ne conclue pas de son côté.
     */
    private String instructionsDeVerification(QualityObjective o) {
        return "Relire la revue de l'objectif « " + o.getTitle() + " » : la plateforme compte "
                + "les périodes passées sous la cible et refuse la clôture tant que les "
                + o.getRecoveryPeriods() + " période(s) exigée(s) ne sont pas tenues. "
                + "Aucun calcul à faire : constater, et consigner ce que montre la courbe.";
    }

    /**
     * {@code objective:<id>:<niveau>:<début de période>} — ce qui relie le dossier
     * à l'écart.
     *
     * <p>La période est celle de la dernière appréciation, et non celle d'une
     * proposition : on peut ouvrir un dossier sans qu'aucune proposition n'ait
     * été rédigée. Sans mesure, la date est omise — la référence reste unique
     * par le niveau.
     */
    private String referenceSource(QualityObjective o, ObjectiveLevel niveau,
                                   Instant debutDeLaPeriode) {
        return "objective:" + o.getId() + ":" + niveau
                + (debutDeLaPeriode == null ? "" : ":" + debutDeLaPeriode);
    }

    // ---------- helpers ----------

    /**
     * Les seuils doivent être ORDONNÉS dans le sens du KPI.
     *
     * <p>Un seuil d'alerte placé du bon côté de la cible ne déclencherait jamais,
     * et un seuil critique plus clément que l'alerte ferait passer un objectif
     * « critique » avant d'être « en alerte ». L'objectif aurait l'air réglé et
     * ne dirait rien.
     */
    private void validerSeuils(int periodMonths, BigDecimal target,
                               BigDecimal warning, BigDecimal critical, KpiDefinition kpi) {
        if (!ObjectivePeriod.isValidMonths(periodMonths)) {
            throw new ObjectiveValidationException(
                    "The objective period is 1, 3, 6 or 12 months: " + periodMonths);
        }
        if (target == null || warning == null || critical == null) {
            throw new ObjectiveValidationException(
                    "A target and both thresholds are required: an objective without them "
                    + "cannot say whether it is met");
        }
        boolean ordonnes = switch (kpi.getDirection()) {
            // On veut descendre : cible ≤ alerte ≤ critique.
            case LOWER_IS_BETTER -> target.compareTo(warning) <= 0
                    && warning.compareTo(critical) <= 0;
            // On veut monter : cible ≥ alerte ≥ critique.
            case HIGHER_IS_BETTER -> target.compareTo(warning) >= 0
                    && warning.compareTo(critical) >= 0;
        };
        if (!ordonnes) {
            throw new ObjectiveValidationException(
                    "Thresholds must be ordered in the KPI's direction of improvement ("
                    + kpi.getDirection() + "): target, then warning, then critical. "
                    + "A warning threshold on the good side of the target would never trigger");
        }
    }

    private Map<UUID, KpiDefinition> chargerKpis(List<QualityObjective> suivis, UUID tenantId) {
        List<UUID> ids = suivis.stream().map(QualityObjective::getKpiId).distinct().toList();
        Map<UUID, KpiDefinition> m = new LinkedHashMap<>();
        for (KpiDefinition d : kpis.findAllById(ids)) {
            if (d.getTenantId().equals(tenantId)) m.put(d.getId(), d);
        }
        return m;
    }

    private QualityObjective load(UUID id, UUID tenantId) {
        return objectives.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ObjectiveNotFoundException(id));
    }

    private QualityObjectiveProposal loadProposal(UUID id, UUID tenantId) {
        return proposals.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ProposalNotFoundException(id));
    }

    private KpiDefinition loadKpi(UUID kpiId, UUID tenantId) {
        KpiDefinition d = kpis.findById(kpiId)
                .filter(k -> k.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ObjectiveValidationException(
                        "Unknown KPI " + kpiId + ": an objective is measured against a KPI "
                        + "from the catalogue"));
        if (d.getDirection() == null) {
            throw new ObjectiveValidationException(
                    "KPI " + d.getCode() + " has no direction of improvement: without it, "
                    + "\"exceeds the threshold\" has no meaning");
        }
        return d;
    }

    private ObjectiveDto.ObjectiveResponse toResponse(QualityObjective o, KpiDefinition kpi,
                                                      ObjectiveAssessment a) {
        ObjectiveAssessment app = a != null ? a : apprecier(o, kpi, null);
        List<ObjectivePeriodSeries.Period> periodes = periodes(o, null);
        ObjectivePeriodSeries.Period derniere = periodes.isEmpty()
                ? null : periodes.get(periodes.size() - 1);
        return new ObjectiveDto.ObjectiveResponse(
                o.getId(), o.getTitle(), o.getKpiId(), kpi.getCode(), kpi.getName(),
                kpi.getUnit(), kpi.getDirection(),
                o.getProcessId(), o.getProcessName(), o.getIsoClause(),
                o.getPeriodMonths(), o.getTargetValue(),
                o.getThresholdWarning(), o.getThresholdCritical(),
                o.getWarningPeriods(), o.getCriticalPeriods(), o.getRecoveryPeriods(),
                critereDEfficacite(o, kpi),
                o.getStatus(),
                new ObjectiveDto.AssessmentResponse(
                        app.status(), app.observedValue(),
                        app.deviationAbsolute(), app.deviationPercent(),
                        app.warningBreaches(), app.criticalBreaches(), app.onTargetPeriods(),
                        app.triggeredLevel(), app.recoveryAchieved(), app.heldByOpenCapa(),
                        app.periodsConsidered(),
                        derniere == null ? null : derniere.start(),
                        derniere == null ? null : derniere.end()),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private ObjectiveDto.ProposalResponse toResponse(QualityObjectiveProposal p,
                                                     String objectiveTitle) {
        return new ObjectiveDto.ProposalResponse(
                p.getId(), p.getObjectiveId(), objectiveTitle, p.getLevel(),
                p.getPeriodStart(), p.getPeriodEnd(), p.getObservedValue(), p.getBreachPeriods(),
                p.getTitle(), p.getDescription(), p.getEffectivenessCriterion(),
                p.getStatus(), p.getCapaCaseId(), p.getRejectionReason(),
                p.getDecidedBy(), p.getDecidedAt(), p.getCreatedAt());
    }

    private static String nombre(BigDecimal v) {
        if (v == null) return "—";
        return v.stripTrailingZeros().setScale(
                Math.max(0, Math.min(6, v.stripTrailingZeros().scale())),
                RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Le jour, écrit comme le lit un francophone : « 01/10/2026 ».
     *
     * <p>Le corps du dossier est de la prose française ; une date ISO au
     * milieu d'une phrase se lit mal et n'est pas la forme employée par les
     * autres rapports de la plateforme.
     */
    private static String jour(Instant t) {
        return t == null ? "—" : JOUR.format(t.atZone(ZoneOffset.UTC).toLocalDate());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
