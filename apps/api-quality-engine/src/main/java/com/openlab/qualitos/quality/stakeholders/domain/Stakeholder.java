package com.openlab.qualitos.quality.stakeholders.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Une partie intéressée du système de management (ISO 9001 §4.2) : qui elle
 * est, ce qu'elle attend, ce que cela nous fait courir, et comment on s'en
 * occupe.
 *
 * <p>La fiche se lit en quatre temps, et c'est l'ordre dans lequel on la
 * remplit :
 * <ol>
 *   <li><b>Identification</b> — nom, famille, dedans ou dehors, sites concernés ;</li>
 *   <li><b>Analyse</b> — besoins, exigences, pertinence et sa justification,
 *       risques et opportunités, cotation ;</li>
 *   <li><b>Pilotage</b> — mesures de maîtrise, indicateurs, documents, actions,
 *       date de réévaluation ;</li>
 *   <li><b>Exigences</b> — la liste auditable, rattachée aux autres modules.</li>
 * </ol>
 *
 * <p>Deux cotations, et une criticité qui s'en déduit : l'impact sur la qualité
 * et l'impact réglementaire se notent séparément parce qu'ils ne se compensent
 * pas. La criticité retient le plus élevé des deux ({@link ImpactLevel#worst}).
 *
 * <p>Classe du DOMAINE : aucune annotation de persistance, aucune dépendance
 * de framework. Elle porte les règles, pas le stockage.
 */
public class Stakeholder {

    public static final int NAME_MAX = 255;
    public static final int TEXT_MAX = 4000;
    public static final int LINE_MAX = 500;
    /** Au-delà, ce n'est plus un registre : c'est un annuaire. */
    public static final int REQUIREMENTS_MAX = 100;

    private final UUID id;
    private final UUID tenantId;

    // --- identification ---
    private String name;
    private StakeholderCategory category;
    private StakeholderSide side;
    private String description;
    private String contact;
    private String sites;

    // --- analyse ---
    private String needs;
    private String requirementsSummary;
    private Relevance relevance;
    private String relevanceJustification;
    private String risksAndOpportunities;
    private ImpactLevel qualityImpact;
    private ImpactLevel regulatoryImpact;
    private Person owner;

    // --- pilotage ---
    private String controls;
    private String indicators;
    private String documents;
    private String actions;
    private LocalDate nextReviewOn;
    private LocalDate lastReviewedOn;

    // --- exigences ---
    private final List<StakeholderRequirement> requirements = new ArrayList<>();

    public Stakeholder(UUID id, UUID tenantId, String name, StakeholderCategory category,
                       StakeholderSide side) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        rename(name);
        reclassify(category, side);
        // Une partie intéressée tout juste inscrite n'est pas encore jugée : la
        // pertinence est « à déterminer », et sa criticité la plus basse. Poser
        // « pertinente » d'office ferait entrer dans le système des parties
        // qu'on n'a pas examinées.
        this.relevance = Relevance.TO_DETERMINE;
        this.qualityImpact = ImpactLevel.LOW;
        this.regulatoryImpact = ImpactLevel.LOW;
    }

    // ---------- identification ----------

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new StakeholderValidationException("A stakeholder is designated by a name");
        }
        String t = newName.strip();
        if (t.length() > NAME_MAX) {
            throw new StakeholderValidationException("The name is at most " + NAME_MAX + " characters");
        }
        this.name = t;
    }

    public void reclassify(StakeholderCategory newCategory, StakeholderSide newSide) {
        if (newCategory == null) {
            throw new StakeholderValidationException("A stakeholder belongs to a category");
        }
        if (newSide == null) {
            throw new StakeholderValidationException("A stakeholder is internal or external");
        }
        this.category = newCategory;
        this.side = newSide;
    }

    public void describe(String newDescription, String newContact, String newSites) {
        this.description = text(newDescription, TEXT_MAX);
        this.contact = text(newContact, LINE_MAX);
        this.sites = text(newSites, LINE_MAX);
    }

    // ---------- analyse ----------

    public void analyse(String newNeeds, String newRequirementsSummary, Relevance newRelevance,
                        String justification, String newRisksAndOpportunities) {
        this.needs = text(newNeeds, TEXT_MAX);
        this.requirementsSummary = text(newRequirementsSummary, TEXT_MAX);
        this.relevance = newRelevance == null ? Relevance.TO_DETERMINE : newRelevance;
        this.relevanceJustification = text(justification, TEXT_MAX);
        this.risksAndOpportunities = text(newRisksAndOpportunities, TEXT_MAX);
        requireJustifiedDecision();
    }

    /**
     * Repose l'analyse telle qu'elle a été enregistrée, sans rejouer les règles
     * de saisie.
     *
     * <p>Réservé à la relecture depuis la base. Une règle durcie après coup —
     * la justification de pertinence, par exemple — ne doit pas rendre
     * illisible une fiche écrite avant elle : on ne réécrit pas l'histoire, on
     * la corrige à la prochaine modification, où {@link #analyse} s'applique.
     */
    public void restoreAnalysis(String storedNeeds, String storedRequirementsSummary,
                                Relevance storedRelevance, String storedJustification,
                                String storedRisksAndOpportunities) {
        this.needs = text(storedNeeds, TEXT_MAX);
        this.requirementsSummary = text(storedRequirementsSummary, TEXT_MAX);
        this.relevance = storedRelevance == null ? Relevance.TO_DETERMINE : storedRelevance;
        this.relevanceJustification = text(storedJustification, TEXT_MAX);
        this.risksAndOpportunities = text(storedRisksAndOpportunities, TEXT_MAX);
    }

    /** Repose la date de la dernière revue lue en base, passée ou non. */
    public void restoreLastReview(LocalDate storedLastReviewedOn) {
        this.lastReviewedOn = storedLastReviewedOn;
    }

    /**
     * Noter les deux impacts. La criticité n'est pas stockée : elle se calcule,
     * pour qu'un impact modifié ne laisse jamais derrière lui une criticité
     * périmée.
     */
    public void rate(ImpactLevel quality, ImpactLevel regulatory) {
        this.qualityImpact = quality == null ? ImpactLevel.LOW : quality;
        this.regulatoryImpact = regulatory == null ? ImpactLevel.LOW : regulatory;
    }

    public ImpactLevel criticality() {
        return ImpactLevel.worst(qualityImpact, regulatoryImpact);
    }

    public void entrustTo(Person newOwner) {
        this.owner = newOwner;
    }

    /**
     * Une décision de pertinence se justifie — dans les deux sens.
     *
     * <p>Écarter une partie intéressée sans dire pourquoi est indéfendable
     * devant un auditeur, qui vient précisément vérifier que le périmètre du
     * système a été déterminé et non subi. La retenir sans dire pourquoi l'est
     * tout autant : on ne saurait plus, un an plus tard, ce qui motive sa
     * présence.
     */
    private void requireJustifiedDecision() {
        boolean decided = relevance == Relevance.RELEVANT || relevance == Relevance.NOT_RELEVANT;
        if (decided && (relevanceJustification == null || relevanceJustification.isBlank())) {
            throw new StakeholderValidationException(
                    "A relevance decision must be justified: say why it is relevant, or why it is not");
        }
    }

    // ---------- pilotage ----------

    public void steer(String newControls, String newIndicators, String newDocuments,
                      String newActions, LocalDate newNextReviewOn) {
        this.controls = text(newControls, TEXT_MAX);
        this.indicators = text(newIndicators, LINE_MAX);
        this.documents = text(newDocuments, LINE_MAX);
        this.actions = text(newActions, LINE_MAX);
        this.nextReviewOn = newNextReviewOn;
    }

    /**
     * Marque la revue comme faite, à cette date.
     *
     * <p>Une revue dans le futur n'est pas une revue : on ne peut pas attester
     * d'un examen qui n'a pas eu lieu.
     */
    public void reviewed(LocalDate on) {
        if (on == null) {
            throw new StakeholderValidationException("A review has a date");
        }
        if (on.isAfter(LocalDate.now())) {
            throw new StakeholderValidationException("A review cannot be dated in the future");
        }
        this.lastReviewedOn = on;
    }

    /** Vrai quand la date de réévaluation est passée : l'écran le signale. */
    public boolean reviewOverdue(LocalDate today) {
        return nextReviewOn != null && today != null && nextReviewOn.isBefore(today);
    }

    // ---------- exigences ----------

    public List<StakeholderRequirement> requirements() {
        return List.copyOf(requirements);
    }

    public StakeholderRequirement addRequirement(StakeholderRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement");
        if (requirements.size() >= REQUIREMENTS_MAX) {
            throw new StakeholderValidationException(
                    "At most " + REQUIREMENTS_MAX + " requirements per stakeholder");
        }
        StakeholderRequirement stored = requirement.id() == null
                ? requirement.withId(UUID.randomUUID())
                : requirement;
        requirements.add(stored);
        return stored;
    }

    /** Reprend une exigence déjà persistée, sans rejouer les règles d'ajout. */
    public void restoreRequirement(StakeholderRequirement requirement) {
        requirements.add(Objects.requireNonNull(requirement, "requirement"));
    }

    public Optional<StakeholderRequirement> requirement(UUID requirementId) {
        return requirements.stream().filter(r -> r.id().equals(requirementId)).findFirst();
    }

    private StakeholderRequirement replace(UUID requirementId,
                                           java.util.function.UnaryOperator<StakeholderRequirement> change) {
        for (int i = 0; i < requirements.size(); i++) {
            if (requirements.get(i).id().equals(requirementId)) {
                StakeholderRequirement next = change.apply(requirements.get(i));
                requirements.set(i, next);
                return next;
            }
        }
        throw new StakeholderValidationException("Unknown requirement: " + requirementId);
    }

    public StakeholderRequirement setRequirementStatus(UUID requirementId, RequirementStatus status) {
        if (status == null) {
            throw new StakeholderValidationException("A requirement has a status");
        }
        return replace(requirementId, r -> r.withStatus(status));
    }

    public StakeholderRequirement linkRequirement(UUID requirementId, RequirementLink link, UUID target) {
        if (link == null || target == null) {
            throw new StakeholderValidationException("A link designates a kind and a target");
        }
        return replace(requirementId, r -> r.withLink(link, target));
    }

    public StakeholderRequirement attachCapa(UUID requirementId, UUID capaId) {
        if (capaId == null) {
            throw new StakeholderValidationException("A CAPA link needs the case identifier");
        }
        return replace(requirementId, r -> r.withCapa(capaId));
    }

    /**
     * Le dossier {@code capaId} est clôturé : l'exigence qu'il traitait est
     * maîtrisée.
     *
     * <p>C'est la boucle de retour de l'ouverture d'action. Ouvrir fait passer
     * l'exigence « à réévaluer » ({@link StakeholderRequirement#withCapa}) ;
     * clôturer la rend maîtrisée, sans que personne ait à tenir deux tableaux
     * à jour. Seule une exigence encore « à réévaluer » bascule : si quelqu'un
     * l'a entre-temps replacée « applicable » parce que l'écart courait encore,
     * la clôture du dossier ne vient pas écraser ce jugement.
     *
     * @return {@code true} si une exigence a changé d'état
     */
    public boolean closeCapa(UUID capaId) {
        if (capaId == null) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < requirements.size(); i++) {
            StakeholderRequirement r = requirements.get(i);
            if (capaId.equals(r.capaId()) && r.status() == RequirementStatus.TO_REASSESS) {
                requirements.set(i, r.withStatus(RequirementStatus.CONTROLLED));
                changed = true;
            }
        }
        return changed;
    }

    public void removeRequirement(UUID requirementId) {
        boolean removed = requirements.removeIf(r -> r.id().equals(requirementId));
        if (!removed) {
            throw new StakeholderValidationException("Unknown requirement: " + requirementId);
        }
    }

    // ---------- lecture ----------

    private static String text(String value, int max) {
        if (value == null) {
            return null;
        }
        String t = value.strip();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > max ? t.substring(0, max) : t;
    }

    public UUID id() { return id; }

    public UUID tenantId() { return tenantId; }

    public String name() { return name; }

    public StakeholderCategory category() { return category; }

    public StakeholderSide side() { return side; }

    public String description() { return description; }

    public String contact() { return contact; }

    public String sites() { return sites; }

    public String needs() { return needs; }

    public String requirementsSummary() { return requirementsSummary; }

    public Relevance relevance() { return relevance; }

    public String relevanceJustification() { return relevanceJustification; }

    public String risksAndOpportunities() { return risksAndOpportunities; }

    public ImpactLevel qualityImpact() { return qualityImpact; }

    public ImpactLevel regulatoryImpact() { return regulatoryImpact; }

    public Person owner() { return owner; }

    public String controls() { return controls; }

    public String indicators() { return indicators; }

    public String documents() { return documents; }

    public String actions() { return actions; }

    public LocalDate nextReviewOn() { return nextReviewOn; }

    public LocalDate lastReviewedOn() { return lastReviewedOn; }
}
