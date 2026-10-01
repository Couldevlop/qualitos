package com.openlab.qualitos.quality.riskregister.application;

import com.openlab.qualitos.quality.riskregister.domain.Identification;
import com.openlab.qualitos.quality.riskregister.domain.Opportunity;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityAction;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityDetails;
import com.openlab.qualitos.quality.riskregister.domain.RegisterChange;
import com.openlab.qualitos.quality.riskregister.domain.RegisterEvent;
import com.openlab.qualitos.quality.riskregister.domain.RegisterEventType;
import com.openlab.qualitos.quality.riskregister.domain.RegisterItemKind;
import com.openlab.qualitos.quality.riskregister.domain.RegisterNotFoundException;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRepositories;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRequirement;
import com.openlab.qualitos.quality.riskregister.domain.RegisterValidationException;
import com.openlab.qualitos.quality.riskregister.domain.Risk;
import com.openlab.qualitos.quality.riskregister.domain.RiskDetails;
import com.openlab.qualitos.quality.riskregister.domain.RiskStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Le registre des risques et opportunités : lire, saisir, réviser, et relier
 * chaque ligne à ce qui la traite.
 *
 * <p>Sans Spring ni JPA. Les écritures sont rendues transactionnelles par le
 * contrôleur : la fiche, sa ligne de suivi et sa trace d'audit sont validées
 * ensemble ou pas du tout.
 */
public class RiskRegisterService {

    static final String RISK_PREFIX = "R-";
    static final String OPPORTUNITY_PREFIX = "O-";
    static final int CAPA_TITLE_MAX = 255;
    static final int CAPA_DESCRIPTION_MAX = 4000;

    private final RegisterRepositories.Risks risks;
    private final RegisterRepositories.Opportunities opportunities;
    private final RegisterRepositories.Actions actions;
    private final RegisterRepositories.Events events;
    private final RiskCapaGateway capas;
    private final RiskSourceCatalog sources;
    private final RegisterContext context;
    private final RegisterAuditPublisher audit;
    private final Clock clock;

    @SuppressWarnings("java:S107") // un port par dépendance : c'est ce qui le rend testable sans Spring
    public RiskRegisterService(RegisterRepositories.Risks risks,
                               RegisterRepositories.Opportunities opportunities,
                               RegisterRepositories.Actions actions,
                               RegisterRepositories.Events events,
                               RiskCapaGateway capas, RiskSourceCatalog sources,
                               RegisterContext context, RegisterAuditPublisher audit, Clock clock) {
        this.risks = risks;
        this.opportunities = opportunities;
        this.actions = actions;
        this.events = events;
        this.capas = capas;
        this.sources = sources;
        this.context = context;
        this.audit = audit;
        this.clock = clock;
    }

    // ---------- risques ----------

    /** Le registre entier, par référence. Quelques centaines de lignes au plus par client. */
    public List<RiskRegisterDto.RiskView> risks() {
        UUID tenant = context.requireTenantId();
        return risks.findByTenant(tenant).stream()
                .sorted(Comparator.comparing(Risk::getReference))
                .map(RiskRegisterService::vue)
                .toList();
    }

    public RiskRegisterDto.RiskSheet risk(UUID id) {
        UUID tenant = context.requireTenantId();
        Risk risque = chargerRisque(id, tenant);
        return fiche(risque, tenant);
    }

    public RiskRegisterDto.RiskView createRisk(RiskRegisterDto.RiskCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        Instant maintenant = clock.instant();
        RiskDetails details = details(commande);
        UUID sourceId = commande.sourceId();
        if (sourceId != null) {
            details = depuisLaSource(details, tenant, sourceId);
        }
        String reference = prochaine(RISK_PREFIX, risks.countByTenant(tenant),
                ref -> risks.referenceTaken(tenant, ref));
        Risk risque = risks.save(Risk.create(tenant, reference, sourceId, details, acteur, maintenant));
        tracer(tenant, RegisterItemKind.RISK, risque.getId(), RegisterEventType.CREATED, null,
                risque.getIdentification().origin().name(), risque.getIdentification().originRef(),
                acteur, maintenant);
        audit.riskRecorded(risque, acteur);
        return vue(risque);
    }

    public RiskRegisterDto.RiskView reviseRisk(UUID id, RiskRegisterDto.RiskCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        Instant maintenant = clock.instant();
        Risk risque = chargerRisque(id, tenant);
        RiskDetails details = details(commande);
        if (risque.getSourceId() != null) {
            // Un risque issu d'un objet garde son origine : seule la source vérifiée la porte.
            Identification i = details.identification();
            Identification origine = risque.getIdentification();
            details = avecIdentification(details, new Identification(i.title(), i.type(), i.process(),
                    i.site(), i.owner(), origine.origin(), origine.originRef(), i.requirements()));
        }
        List<RegisterChange> changements = risque.revise(details, maintenant);
        Risk enregistre = risks.save(risque);
        tracerChangements(tenant, RegisterItemKind.RISK, enregistre.getId(), changements, acteur, maintenant);
        audit.riskRevised(enregistre, acteur);
        return vue(enregistre);
    }

    /**
     * Ouvre un dossier CAPA préventif sur ce risque.
     *
     * <p>Le porteur est celui qui l'ouvre : c'est lui qu'on sait joindre. Un
     * risque clos n'en ouvre plus — il n'y a plus rien à traiter.
     */
    public RiskRegisterDto.CapaView openCapa(UUID riskId, RiskRegisterDto.CapaCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        Risk risque = chargerRisque(riskId, tenant);
        if (risque.getStatus() == RiskStatus.CLOSED) {
            throw new RegisterValidationException("status",
                    "Un risque clos n'ouvre plus d'action CAPA.");
        }
        if (commande == null) {
            throw new RegisterValidationException("title", "L'intitulé est obligatoire.");
        }
        String titre = texte("title", commande.title(), CAPA_TITLE_MAX, true);
        String description = texte("description", commande.description(), CAPA_DESCRIPTION_MAX, false);

        RiskCapaGateway.LinkedCapa capa = capas.open(risque, titre, description, commande.dueDate(), acteur);
        tracer(tenant, RegisterItemKind.RISK, risque.getId(), RegisterEventType.ACTION_OPENED, null,
                null, titre, acteur, clock.instant());
        audit.riskCapaOpened(risque, capa.id(), acteur);
        return new RiskRegisterDto.CapaView(capa.id(), capa.title(), capa.dueDate(), capa.status());
    }

    /**
     * Le brouillon d'un risque issu d'une AMDEC, d'une NC, d'un audit ou d'un
     * changement : ce que l'objet propose, et les risques qui en sont déjà issus.
     */
    public RiskRegisterDto.RiskDraft draft(RegisterOrigin origin, UUID sourceId) {
        UUID tenant = context.requireTenantId();
        RiskSourceCatalog.SourceDraft src = source(origin, tenant, sourceId);
        List<RiskRegisterDto.RiskLink> existants = risks.findBySource(tenant, origin, sourceId).stream()
                .sorted(Comparator.comparing(Risk::getReference))
                .map(r -> new RiskRegisterDto.RiskLink(r.getId(), r.getReference()))
                .toList();
        return new RiskRegisterDto.RiskDraft(origin, sourceId, src.originRef(), src.title(), src.cause(),
                src.effect(), src.grossSeverity(), src.grossProbability(), src.process(), src.eligible(),
                src.reason(), existants);
    }

    // ---------- opportunités ----------

    public List<RiskRegisterDto.OpportunityView> opportunities() {
        UUID tenant = context.requireTenantId();
        return opportunities.findByTenant(tenant).stream()
                .sorted(Comparator.comparing(Opportunity::getReference))
                .map(RiskRegisterService::vue)
                .toList();
    }

    public RiskRegisterDto.OpportunitySheet opportunity(UUID id) {
        UUID tenant = context.requireTenantId();
        Opportunity o = chargerOpportunite(id, tenant);
        return fiche(o, tenant);
    }

    public RiskRegisterDto.OpportunityView createOpportunity(RiskRegisterDto.OpportunityCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        Instant maintenant = clock.instant();
        String reference = prochaine(OPPORTUNITY_PREFIX, opportunities.countByTenant(tenant),
                ref -> opportunities.referenceTaken(tenant, ref));
        Opportunity o = opportunities.save(
                Opportunity.create(tenant, reference, details(commande), acteur, maintenant));
        tracer(tenant, RegisterItemKind.OPPORTUNITY, o.getId(), RegisterEventType.CREATED, null,
                o.getIdentification().origin().name(), o.getIdentification().originRef(),
                acteur, maintenant);
        audit.opportunityRecorded(o, acteur);
        return vue(o);
    }

    public RiskRegisterDto.OpportunityView reviseOpportunity(UUID id,
                                                             RiskRegisterDto.OpportunityCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        Instant maintenant = clock.instant();
        Opportunity o = chargerOpportunite(id, tenant);
        List<RegisterChange> changements = o.revise(details(commande), maintenant);
        Opportunity enregistree = opportunities.save(o);
        tracerChangements(tenant, RegisterItemKind.OPPORTUNITY, enregistree.getId(), changements, acteur,
                maintenant);
        audit.opportunityRevised(enregistree, acteur);
        return vue(enregistree);
    }

    public RiskRegisterDto.ActionView addAction(UUID opportunityId, RiskRegisterDto.ActionCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        Instant maintenant = clock.instant();
        Opportunity o = chargerOpportunite(opportunityId, tenant);
        RiskRegisterDto.ActionCommand c = commandeAction(commande);
        OpportunityAction action = actions.save(OpportunityAction.open(tenant, o.getId(),
                actions.maxNumber(tenant) + 1, c.title(), c.dueDate(), c.status(), maintenant));
        tracer(tenant, RegisterItemKind.OPPORTUNITY, o.getId(), RegisterEventType.ACTION_OPENED, null,
                "ACT-" + action.getNumber(), action.getTitle(), acteur, maintenant);
        audit.actionRecorded(action, acteur);
        return vue(action);
    }

    public RiskRegisterDto.ActionView reviseAction(UUID opportunityId, UUID actionId,
                                                   RiskRegisterDto.ActionCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        OpportunityAction action = chargerAction(opportunityId, actionId, tenant);
        RiskRegisterDto.ActionCommand c = commandeAction(commande);
        action.revise(c.title(), c.dueDate(), c.status(), clock.instant());
        OpportunityAction enregistree = actions.save(action);
        audit.actionRevised(enregistree, acteur);
        return vue(enregistree);
    }

    public void deleteAction(UUID opportunityId, UUID actionId) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        OpportunityAction action = chargerAction(opportunityId, actionId, tenant);
        actions.delete(action);
        audit.actionDeleted(action, acteur);
    }

    // ---------- saisie assistée ----------

    /**
     * Processus, sites et propriétaires déjà employés dans les deux registres,
     * sans doublon de casse ni d'espace — la première orthographe rencontrée,
     * dans l'ordre des références, fait foi.
     */
    public RiskRegisterDto.Suggestions suggestions() {
        UUID tenant = context.requireTenantId();
        List<Identification> lignes = Stream.concat(
                risks.findByTenant(tenant).stream()
                        .sorted(Comparator.comparing(Risk::getReference))
                        .map(Risk::getIdentification),
                opportunities.findByTenant(tenant).stream()
                        .sorted(Comparator.comparing(Opportunity::getReference))
                        .map(Opportunity::getIdentification))
                .toList();
        return new RiskRegisterDto.Suggestions(
                distinctes(lignes, Identification::process),
                distinctes(lignes, Identification::site),
                distinctes(lignes, Identification::owner));
    }

    // ---------- interne ----------

    private RiskRegisterDto.RiskSheet fiche(Risk risque, UUID tenant) {
        List<RiskRegisterDto.CapaView> liees = capas.linkedTo(risque).stream()
                .map(c -> new RiskRegisterDto.CapaView(c.id(), c.title(), c.dueDate(), c.status()))
                .toList();
        return new RiskRegisterDto.RiskSheet(vue(risque), liees,
                suivi(tenant, RegisterItemKind.RISK, risque.getId()));
    }

    private RiskRegisterDto.OpportunitySheet fiche(Opportunity o, UUID tenant) {
        List<RiskRegisterDto.ActionView> siennes = actions.findByOpportunity(tenant, o.getId()).stream()
                .sorted(Comparator.comparingInt(OpportunityAction::getNumber))
                .map(RiskRegisterService::vue)
                .toList();
        return new RiskRegisterDto.OpportunitySheet(vue(o), siennes,
                suivi(tenant, RegisterItemKind.OPPORTUNITY, o.getId()));
    }

    private List<RiskRegisterDto.EventView> suivi(UUID tenant, RegisterItemKind kind, UUID itemId) {
        return events.findByItem(tenant, kind, itemId).stream()
                .map(e -> new RiskRegisterDto.EventView(e.id(), e.type(), e.fromValue(), e.toValue(),
                        e.detail(), e.at()))
                .toList();
    }

    /**
     * Les changements d'une même révision, une microseconde d'écart chacun.
     *
     * <p>Sans cet écart, ils partageraient l'horodatage et la base les rendrait
     * dans un ordre quelconque : le suivi changerait d'une lecture à l'autre. La
     * microseconde est le grain que PostgreSQL conserve.
     */
    private void tracerChangements(UUID tenant, RegisterItemKind kind, UUID itemId,
                                   List<RegisterChange> changements, UUID acteur, Instant maintenant) {
        for (int i = 0; i < changements.size(); i++) {
            RegisterChange c = changements.get(i);
            tracer(tenant, kind, itemId, c.type(), c.from(), c.to(), null, acteur,
                    maintenant.plus(i, ChronoUnit.MICROS));
        }
    }

    private void tracer(UUID tenant, RegisterItemKind kind, UUID itemId, RegisterEventType type,
                        String from, String to, String detail, UUID actor, Instant at) {
        events.save(RegisterEvent.of(tenant, kind, itemId, type, from, to, detail, actor, at));
    }

    private Risk chargerRisque(UUID id, UUID tenant) {
        return risks.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new RegisterNotFoundException("Risk", id));
    }

    private Opportunity chargerOpportunite(UUID id, UUID tenant) {
        return opportunities.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new RegisterNotFoundException("Opportunity", id));
    }

    /** Une action d'une AUTRE opportunité répond 404 : l'adresse est fausse, quel que soit le client. */
    private OpportunityAction chargerAction(UUID opportunityId, UUID actionId, UUID tenant) {
        chargerOpportunite(opportunityId, tenant);
        return actions.findByIdAndTenant(actionId, tenant)
                .filter(a -> a.getOpportunityId().equals(opportunityId))
                .orElseThrow(() -> new RegisterNotFoundException("Action", actionId));
    }

    /**
     * La prochaine référence libre : le rang suivant, puis au-delà si un trou
     * laissé par une saisie concurrente l'a déjà pris. La contrainte d'unicité
     * en base reste le filet pour deux créations simultanées (409).
     */
    static String prochaine(String prefixe, long deja, Predicate<String> prise) {
        long rang = deja + 1;
        String reference = format(prefixe, rang);
        while (prise.test(reference)) {
            rang++;
            reference = format(prefixe, rang);
        }
        return reference;
    }

    private RiskSourceCatalog.SourceDraft source(RegisterOrigin origin, UUID tenant, UUID sourceId) {
        if (origin == null || !origin.hasSource()) {
            throw new RegisterValidationException("origin",
                    "Cette origine ne désigne pas un objet de la plateforme.");
        }
        if (sourceId == null) {
            throw new RegisterValidationException("sourceId", "L'objet source est obligatoire.");
        }
        return sources.find(origin, tenant, sourceId)
                .orElseThrow(() -> new RegisterNotFoundException("Source", sourceId));
    }

    /**
     * La référence affichée vient de l'objet relu, pas du corps de la requête :
     * un risque ne peut pas se dire issu d'une AMDEC qui n'existe pas, ni d'une
     * ligne restée sous le seuil.
     */
    private RiskDetails depuisLaSource(RiskDetails details, UUID tenant, UUID sourceId) {
        Identification i = details.identification();
        RiskSourceCatalog.SourceDraft src = source(i.origin(), tenant, sourceId);
        if (!src.eligible()) {
            throw new RegisterValidationException("sourceId",
                    "Cet objet ne justifie pas un risque (" + src.reason() + ").");
        }
        return avecIdentification(details, new Identification(i.title(), i.type(), i.process(), i.site(),
                i.owner(), i.origin(), src.originRef(), i.requirements()));
    }

    private static RiskDetails avecIdentification(RiskDetails d, Identification i) {
        return new RiskDetails(i, d.cause(), d.effect(), d.grossSeverity(), d.grossProbability(),
                d.residualSeverity(), d.residualProbability(), d.decision(), d.status(), d.nextReviewOn(),
                d.effectivenessCriterion());
    }

    private static String format(String prefixe, long rang) {
        return prefixe + String.format(Locale.ROOT, "%03d", rang);
    }

    private static RiskRegisterDto.ActionCommand commandeAction(RiskRegisterDto.ActionCommand c) {
        if (c == null) {
            throw new RegisterValidationException("title", "L'intitulé est obligatoire.");
        }
        return c;
    }

    private static String texte(String field, String value, int max, boolean obligatoire) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            if (obligatoire) {
                throw new RegisterValidationException(field, "Ce champ est obligatoire.");
            }
            return null;
        }
        if (v.length() > max) {
            throw new RegisterValidationException(field, "Ce champ dépasse " + max + " caractères.");
        }
        return v;
    }

    private static List<String> distinctes(List<Identification> lignes,
                                           Function<Identification, String> champ) {
        Map<String, String> parCle = new TreeMap<>();
        for (Identification i : lignes) {
            String v = champ.apply(i);
            if (v != null) {
                parCle.putIfAbsent(v.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT), v);
            }
        }
        return List.copyOf(parCle.values());
    }

    private static Set<RegisterRequirement> exigences(List<RegisterRequirement> liste) {
        Set<RegisterRequirement> set = EnumSet.noneOf(RegisterRequirement.class);
        if (liste != null) {
            liste.stream().filter(Objects::nonNull).forEach(set::add);
        }
        return set;
    }

    private static RiskDetails details(RiskRegisterDto.RiskCommand c) {
        if (c == null) {
            throw new RegisterValidationException("title", "L'intitulé est obligatoire.");
        }
        return new RiskDetails(
                new Identification(c.title(), c.type(), c.process(), c.site(), c.owner(),
                        c.origin(), c.originRef(), exigences(c.requirements())),
                c.cause(), c.effect(), c.grossSeverity(), c.grossProbability(),
                c.residualSeverity(), c.residualProbability(), c.decision(), c.status(),
                c.nextReviewOn(), c.effectivenessCriterion());
    }

    private static OpportunityDetails details(RiskRegisterDto.OpportunityCommand c) {
        if (c == null) {
            throw new RegisterValidationException("title", "L'intitulé est obligatoire.");
        }
        return new OpportunityDetails(
                new Identification(c.title(), c.type(), c.process(), c.site(), c.owner(),
                        c.origin(), c.originRef(), exigences(c.requirements())),
                c.targetDate(), c.context(), c.benefit(), c.gain(), c.feasibility(),
                c.decision(), c.status(), c.benefitCriterion());
    }

    private static List<RegisterRequirement> trie(Set<RegisterRequirement> exigences) {
        return exigences.stream().sorted().toList();
    }

    static RiskRegisterDto.RiskView vue(Risk r) {
        Identification i = r.getIdentification();
        boolean residuelle = r.getResidual() != null;
        return new RiskRegisterDto.RiskView(r.getId(), r.getReference(), i.title(), i.type(),
                i.process(), i.site(), i.owner(), r.getCause(), r.getEffect(), i.origin(), i.originRef(),
                r.getSourceId(), r.getGross().first(), r.getGross().second(), r.getGross().score(), r.grossLevel(),
                residuelle ? r.getResidual().first() : null,
                residuelle ? r.getResidual().second() : null,
                residuelle ? r.getResidual().score() : null,
                r.residualLevel(), r.getDecision(), r.getStatus(), trie(i.requirements()),
                r.getNextReviewOn(), r.getEffectivenessCriterion(), r.getCreatedAt(), r.getUpdatedAt());
    }

    static RiskRegisterDto.OpportunityView vue(Opportunity o) {
        Identification i = o.getIdentification();
        return new RiskRegisterDto.OpportunityView(o.getId(), o.getReference(), i.title(), i.type(),
                i.process(), i.site(), i.owner(), o.getTargetDate(), o.getContext(), o.getBenefit(),
                i.origin(), i.originRef(), o.getEvaluation().first(), o.getEvaluation().second(),
                o.getEvaluation().score(), o.level(), o.getDecision(), o.getStatus(),
                trie(i.requirements()), o.getBenefitCriterion(), o.getCreatedAt(), o.getUpdatedAt());
    }

    static RiskRegisterDto.ActionView vue(OpportunityAction a) {
        return new RiskRegisterDto.ActionView(a.getId(), a.getNumber(), a.getTitle(), a.getDueDate(),
                a.getStatus());
    }
}
