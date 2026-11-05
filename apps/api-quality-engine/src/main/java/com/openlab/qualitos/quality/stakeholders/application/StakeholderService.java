package com.openlab.qualitos.quality.stakeholders.application;

import com.openlab.qualitos.quality.stakeholders.domain.ImpactLevel;
import com.openlab.qualitos.quality.stakeholders.domain.Person;
import com.openlab.qualitos.quality.stakeholders.domain.Relevance;
import com.openlab.qualitos.quality.stakeholders.domain.RequirementLink;
import com.openlab.qualitos.quality.stakeholders.domain.RequirementStatus;
import com.openlab.qualitos.quality.stakeholders.domain.Stakeholder;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderNotFoundException;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderRepository;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderRequirement;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderSide;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Le registre des parties intéressées (ISO 9001 §4.2) : identification,
 * analyse, cotation, pilotage et exigences.
 *
 * <p>Trois décisions de conception portent tout le reste :
 * <ol>
 *   <li><b>La criticité se calcule</b>, elle ne se stocke pas. Un impact
 *       modifié ne peut donc jamais laisser derrière lui une criticité périmée,
 *       et la matrice d'évaluation n'est qu'une autre vue du même calcul.</li>
 *   <li><b>L'IA propose, l'humain décide.</b> Les trois assistants rendent des
 *       propositions ; aucune n'entre dans le registre sans un appel distinct,
 *       fait par quelqu'un. Le port de l'IA ne sait pas écrire.</li>
 *   <li><b>Une exigence pour laquelle on ouvre une action repasse « à
 *       réévaluer ».</b> Ouvrir une action, c'est constater un écart : on ne
 *       peut plus l'afficher maîtrisée dans la même seconde.</li>
 * </ol>
 */
public class StakeholderService {

    private final StakeholderRepository repository;
    private final StakeholderPorts.Context context;
    private final StakeholderPorts.CapaGateway capa;
    private final StakeholderPorts.Assistant assistant;
    private final Clock clock;

    public StakeholderService(StakeholderRepository repository, StakeholderPorts.Context context,
                              StakeholderPorts.CapaGateway capa, StakeholderPorts.Assistant assistant,
                              Clock clock) {
        this.repository = repository;
        this.context = context;
        this.capa = capa;
        this.assistant = assistant;
        this.clock = clock;
    }

    // ---------- registre ----------

    /**
     * Le registre, trié par criticité décroissante puis par nom.
     *
     * <p>Le tri est ici et non dans l'écran : l'ordre porte du sens — ce qui est
     * critique se lit d'abord — et deux écrans ne doivent pas en proposer deux.
     */
    public List<StakeholderDto.Row> list(StakeholderSide side, String search) {
        LocalDate today = today();
        String needle = search == null ? null : search.strip().toLowerCase(java.util.Locale.ROOT);
        return repository.findByTenant(context.requireTenantId()).stream()
                .filter(s -> side == null || s.side() == side)
                .filter(s -> needle == null || needle.isEmpty()
                        || s.name().toLowerCase(java.util.Locale.ROOT).contains(needle))
                .sorted(Comparator
                        .comparingInt((Stakeholder s) -> -s.criticality().weight())
                        .thenComparing(Stakeholder::name, String.CASE_INSENSITIVE_ORDER))
                .map(s -> StakeholderDto.Row.of(s, today))
                .toList();
    }

    public StakeholderDto.Sheet get(UUID id) {
        return StakeholderDto.Sheet.of(load(id), today());
    }

    public StakeholderDto.Statistics statistics() {
        LocalDate today = today();
        List<Stakeholder> all = repository.findByTenant(context.requireTenantId());
        return new StakeholderDto.Statistics(
                all.size(),
                all.stream().filter(s -> s.side() == StakeholderSide.INTERNAL).count(),
                all.stream().filter(s -> s.side() == StakeholderSide.EXTERNAL).count(),
                all.stream().filter(s -> s.criticality() == ImpactLevel.HIGH).count(),
                all.stream().filter(s -> s.reviewOverdue(today)).count(),
                all.stream().filter(s -> s.relevance() == Relevance.TO_DETERMINE).count(),
                all.stream().flatMap(s -> s.requirements().stream())
                        .filter(r -> r.status() == RequirementStatus.TO_REASSESS).count());
    }

    /**
     * Inscrit une partie intéressée. Trois champs suffisent — nom, famille,
     * dedans ou dehors : on inscrit d'abord, on analyse ensuite. Exiger la
     * fiche complète à l'inscription ferait remplir du vide.
     */
    public StakeholderDto.Sheet create(String name, StakeholderCategory category, StakeholderSide side) {
        UUID tenant = context.requireTenantId();
        refuseDuplicate(tenant, category, name, null);
        Stakeholder created = new Stakeholder(UUID.randomUUID(), tenant, name, category, side);
        return StakeholderDto.Sheet.of(repository.save(created), today());
    }

    /** Enregistre la fiche d'un bloc : les quatre parties partent ensemble. */
    public StakeholderDto.Sheet update(UUID id, Update update) {
        Stakeholder s = load(id);
        if (update.name() != null || update.category() != null || update.side() != null) {
            String name = update.name() == null ? s.name() : update.name();
            StakeholderCategory category = update.category() == null ? s.category() : update.category();
            refuseDuplicate(s.tenantId(), category, name, s.id());
            s.rename(name);
            s.reclassify(category, update.side() == null ? s.side() : update.side());
        }
        s.describe(update.description(), update.contact(), update.sites());
        s.analyse(update.needs(), update.requirementsSummary(), update.relevance(),
                update.relevanceJustification(), update.risksAndOpportunities());
        s.rate(update.qualityImpact(), update.regulatoryImpact());
        s.entrustTo(update.owner());
        s.steer(update.controls(), update.indicators(), update.documents(), update.actions(),
                update.nextReviewOn());
        return StakeholderDto.Sheet.of(repository.save(s), today());
    }

    /**
     * Retire une fiche du registre.
     *
     * <p>Refusée dès qu'elle porte une exigence : cette exigence est une preuve
     * de §4.2, et une preuve effacée ne se présente pas à l'auditeur. On écarte
     * une partie intéressée en la déclarant non pertinente — avec sa
     * justification — pas en la faisant disparaître.
     */
    public void delete(UUID id) {
        Stakeholder s = load(id);
        if (!s.requirements().isEmpty()) {
            throw new StakeholderValidationException(
                    "This stakeholder carries " + s.requirements().size() + " requirement(s): declare it "
                    + "not relevant with a justification rather than deleting the evidence");
        }
        repository.delete(s);
    }

    /** Marque la revue comme faite aujourd'hui. */
    public StakeholderDto.Sheet markReviewed(UUID id) {
        Stakeholder s = load(id);
        s.reviewed(today());
        return StakeholderDto.Sheet.of(repository.save(s), today());
    }

    // ---------- matrice d'évaluation ----------

    /**
     * Cote plusieurs parties intéressées d'un coup : c'est le geste de la
     * matrice, où l'on compare les lignes entre elles plutôt que de les ouvrir
     * une à une.
     */
    public List<StakeholderDto.Row> rate(Map<UUID, Rating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            throw new StakeholderValidationException("Nothing to rate");
        }
        UUID tenant = context.requireTenantId();
        for (Map.Entry<UUID, Rating> entry : ratings.entrySet()) {
            Stakeholder s = repository.findByIdAndTenant(entry.getKey(), tenant)
                    .orElseThrow(() -> new StakeholderNotFoundException(entry.getKey()));
            s.rate(entry.getValue().qualityImpact(), entry.getValue().regulatoryImpact());
            repository.save(s);
        }
        return list(null, null);
    }

    // ---------- exigences ----------

    public StakeholderDto.Requirement addRequirement(UUID id, StakeholderRequirement requirement) {
        Stakeholder s = load(id);
        StakeholderRequirement stored = s.addRequirement(requirement);
        repository.save(s);
        return StakeholderDto.Requirement.of(stored);
    }

    public StakeholderDto.Requirement setRequirementStatus(UUID id, UUID requirementId,
                                                           RequirementStatus status) {
        Stakeholder s = load(id);
        StakeholderRequirement updated = s.setRequirementStatus(requirementId, status);
        repository.save(s);
        return StakeholderDto.Requirement.of(updated);
    }

    public StakeholderDto.Requirement linkRequirement(UUID id, UUID requirementId,
                                                      RequirementLink link, UUID target) {
        Stakeholder s = load(id);
        StakeholderRequirement updated = s.linkRequirement(requirementId, link, target);
        repository.save(s);
        return StakeholderDto.Requirement.of(updated);
    }

    public void removeRequirement(UUID id, UUID requirementId) {
        Stakeholder s = load(id);
        s.removeRequirement(requirementId);
        repository.save(s);
    }

    /**
     * Ouvre une CAPA pour une exigence qui n'est pas tenue, et rattache le
     * dossier à la ligne.
     *
     * <p>Le dossier est ouvert par le module CAPA (ADR 0088), avec son action
     * unique. La référence porte la partie intéressée et l'exigence : c'est ce
     * qu'on lira dans le registre CAPA six mois plus tard.
     */
    public StakeholderDto.Requirement openCapa(UUID id, UUID requirementId, String title,
                                               UUID ownerId, String ownerName, LocalDate dueDate) {
        Stakeholder s = load(id);
        StakeholderRequirement r = s.requirement(requirementId)
                .orElseThrow(() -> new StakeholderValidationException(
                        "Unknown requirement: " + requirementId));
        if (r.capaId() != null) {
            throw new StakeholderValidationException("This requirement already has a CAPA case");
        }
        String reference = s.name() + " / " + r.requirement();
        String description = "Exigence de la partie intéressée « " + s.name() + " » : " + r.requirement()
                + (r.origin() == null ? "" : " (origine : " + r.origin() + ")");
        UUID capaId = capa.open(reference, title, description, ownerId, ownerName, dueDate);
        StakeholderRequirement updated = s.attachCapa(requirementId, capaId);
        repository.save(s);
        return StakeholderDto.Requirement.of(updated);
    }

    // ---------- assistant IA ----------

    /** Des parties intéressées plausibles. Aucune n'est inscrite : l'écran les propose. */
    public List<StakeholderPorts.SuggestedStakeholder> suggest(String sector, String characteristics) {
        if (sector == null || sector.isBlank()) {
            throw new StakeholderValidationException("The sector is needed to suggest anything useful");
        }
        return assistant.suggestStakeholders(sector.strip(), characteristics == null ? "" : characteristics.strip());
    }

    /**
     * Les exigences qu'un document porte, chacune citée dans le texte d'origine.
     * Rien n'est écrit : la validation se fait exigence par exigence, par
     * {@link #addRequirement}.
     */
    public List<StakeholderPorts.ExtractedRequirement> extract(String documentName, String documentText) {
        requireText(documentText);
        return assistant.extractRequirements(documentName == null ? "document" : documentName.strip(),
                documentText);
    }

    /** Ce qui a changé entre deux versions, et ce que cela remet en cause. */
    public List<StakeholderPorts.DetectedChange> changes(String documentName, String previousText,
                                                         String currentText) {
        requireText(previousText);
        requireText(currentText);
        return assistant.detectChanges(documentName == null ? "document" : documentName.strip(),
                previousText, currentText);
    }

    /**
     * Fait passer « à réévaluer » les exigences que l'utilisateur a jugées
     * touchées par un changement.
     *
     * <p>C'est l'acte de validation humaine du troisième assistant : l'IA a
     * signalé un changement, quelqu'un décide lesquelles de ses exigences il
     * remet en cause. Le registre ne bouge qu'ici.
     */
    public List<StakeholderDto.Requirement> markToReassess(UUID id, List<UUID> requirementIds) {
        if (requirementIds == null || requirementIds.isEmpty()) {
            throw new StakeholderValidationException("Name the requirements the change calls into question");
        }
        Stakeholder s = load(id);
        List<StakeholderDto.Requirement> updated = requirementIds.stream()
                .map(rid -> s.setRequirementStatus(rid, RequirementStatus.TO_REASSESS))
                .map(StakeholderDto.Requirement::of)
                .toList();
        repository.save(s);
        return updated;
    }

    // ---------- internes ----------

    private Stakeholder load(UUID id) {
        return repository.findByIdAndTenant(id, context.requireTenantId())
                .orElseThrow(() -> new StakeholderNotFoundException(id));
    }

    private void refuseDuplicate(UUID tenant, StakeholderCategory category, String name, UUID exceptId) {
        if (name != null && repository.nameTaken(tenant, category, name.strip(), exceptId)) {
            throw new StakeholderValidationException(
                    "This stakeholder is already in the register under the same category");
        }
    }

    private static void requireText(String text) {
        if (text == null || text.strip().length() < 40) {
            // Sous 40 caractères, il n'y a rien à lire : l'IA inventerait.
            throw new StakeholderValidationException("The document text is too short to be analysed");
        }
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    /** Ce que la fiche reçoit quand on l'enregistre. */
    public record Update(String name, StakeholderCategory category, StakeholderSide side,
                         String description, String contact, String sites,
                         String needs, String requirementsSummary, Relevance relevance,
                         String relevanceJustification, String risksAndOpportunities,
                         ImpactLevel qualityImpact, ImpactLevel regulatoryImpact, Person owner,
                         String controls, String indicators, String documents, String actions,
                         LocalDate nextReviewOn) {}

    /** Les deux notes d'une ligne de la matrice. */
    public record Rating(ImpactLevel qualityImpact, ImpactLevel regulatoryImpact) {}
}
