package com.openlab.qualitos.quality.apqp;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le cycle APQP d'un PROJET : lecture, amorçage, et modification.
 *
 * <p>Les cinq phases du manuel AIAG ne sont pas une constante du code mais la
 * valeur de départ du cycle d'un projet, qui appartient au client. Il peut
 * renommer une phase, en retirer une, en ajouter une sixième, et remanier ses
 * livrables.
 *
 * <p>Toutes les opérations portent un {@code projectId} et le vérifient contre le
 * client du jeton : un cycle est celui d'un programme précis, et deux programmes
 * du même client n'ont ni les mêmes livrables ni le même dossier PPAP.
 */
@Service
public class ApqpService {

    private final ApqpPhaseRepository repository;
    private final ApqpProjectRepository projets;
    private final ApqpDeliverableEvidenceRepository evidences;
    private final ApqpLinkResolver linkResolver;
    private final ApqpCycleSeeder seeder;

    public ApqpService(ApqpPhaseRepository repository,
                       ApqpProjectRepository projets,
                       ApqpDeliverableEvidenceRepository evidences,
                       ApqpLinkResolver linkResolver,
                       ApqpCycleSeeder seeder) {
        this.repository = repository;
        this.projets = projets;
        this.evidences = evidences;
        this.linkResolver = linkResolver;
        this.seeder = seeder;
    }

    /**
     * Le cycle d'un projet, et l'état de son dossier PPAP.
     *
     * <p>Pas {@code readOnly} : un projet migré depuis l'époque du cycle unique
     * peut n'avoir aucune phase si le client les avait toutes supprimées. On
     * amorce alors, plutôt que de rendre un écran vide sans expliquer pourquoi.
     */
    @Transactional
    public ApqpDto.CycleResponse cycle(UUID projectId) {
        UUID tenantId = requireTenantId();
        ApqpProject projet = chargerProjet(projectId, tenantId);
        if (!repository.existsByProjectIdAndTenantId(projectId, tenantId)) {
            seeder.amorcer(projet);
        }
        return enCycle(projet, tenantId);
    }

    /**
     * Rend au projet le cycle du référentiel, en effaçant le sien.
     *
     * <p>Il faut une porte de sortie explicite : un client qui VEUT la nouvelle
     * liste n'aurait sinon aucun moyen de l'obtenir, sinon en supprimant ses phases
     * une à une.
     *
     * <p>Destructif et assumé comme tel : l'écran le demande deux fois, et la
     * suppression emporte les pièces versées aux livrables (cascade).
     */
    @Transactional
    public ApqpDto.CycleResponse reinitialiser(UUID projectId) {
        UUID tenantId = requireTenantId();
        ApqpProject projet = chargerProjet(projectId, tenantId);
        repository.deleteByProjectIdAndTenantId(projectId, tenantId);
        // Sans ce vidage, l'insertion qui suit bute sur la contrainte d'unicité
        // (projet, rang) : les suppressions ne sont pas encore parties en base.
        repository.flush();
        seeder.amorcer(projet);
        return enCycle(projet, tenantId);
    }

    @Transactional
    public ApqpDto.PhaseResponse creerPhase(UUID projectId, ApqpDto.CreatePhaseRequest requete) {
        UUID tenantId = requireTenantId();
        ApqpProject projet = chargerProjet(projectId, tenantId);

        int rang = repository.findFirstByProjectIdAndTenantIdOrderByPositionDesc(projectId, tenantId)
                .map(p -> p.getPosition() + 1)
                .orElse(1);

        ApqpPhase phase = new ApqpPhase();
        phase.setProject(projet);
        phase.setTenantId(tenantId);
        phase.setPosition(rang);
        phase.setTitle(requete.title().trim());
        phase.setPurpose(nettoyer(requete.purpose()));
        phase.setQuestion(nettoyer(requete.question()));

        ApqpPhase enregistree = repository.save(phase);
        return enReponse(enregistree, niveau(rang, rang), Map.of());
    }

    @Transactional
    public ApqpDto.PhaseResponse modifierPhase(
            UUID projectId, UUID phaseId, ApqpDto.UpdatePhaseRequest requete) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(projectId, phaseId, tenantId);

        phase.setTitle(requete.title().trim());
        phase.setPurpose(nettoyer(requete.purpose()));
        phase.setQuestion(nettoyer(requete.question()));

        repository.save(phase);
        return enReponse(phase, niveau(phase.getPosition(), compte(projectId, tenantId)),
                comptesDePieces(tenantId));
    }

    /**
     * Retire une phase, puis RESSERRE les rangs.
     *
     * <p>Sans resserrement, supprimer la phase 3 laisserait la suite en 1, 2, 4,
     * 5 : le V se dessinerait avec un trou, et la contrainte d'unicité
     * refuserait la prochaine insertion au rang libéré.
     */
    @Transactional
    public void supprimerPhase(UUID projectId, UUID phaseId) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(projectId, phaseId, tenantId);
        repository.delete(phase);
        repository.flush();

        List<ApqpPhase> restantes =
                repository.findByProjectIdAndTenantIdOrderByPositionAsc(projectId, tenantId);
        for (int i = 0; i < restantes.size(); i++) {
            restantes.get(i).setPosition(i + 1);
        }
        repository.saveAll(restantes);
    }

    /** Remet le cycle dans l'ordre donné. */
    @Transactional
    public List<ApqpDto.PhaseResponse> reorganiser(
            UUID projectId, ApqpDto.ReorderRequest requete) {
        UUID tenantId = requireTenantId();
        chargerProjet(projectId, tenantId);
        List<ApqpPhase> phases =
                repository.findByProjectIdAndTenantIdOrderByPositionAsc(projectId, tenantId);

        Map<UUID, ApqpPhase> parId = phases.stream()
                .collect(Collectors.toMap(ApqpPhase::getId, Function.identity()));

        // Un ordre partiel laisserait des phases sans rang, donc hors du V. On
        // exige la liste ENTIÈRE plutôt que de deviner où ranger les absentes.
        if (requete.phaseIds() == null
                || requete.phaseIds().size() != phases.size()
                || !parId.keySet().containsAll(requete.phaseIds())) {
            throw new ApqpReorderException();
        }

        int rang = 1;
        for (UUID id : requete.phaseIds()) {
            parId.get(id).setPosition(rang++);
        }
        repository.saveAll(phases);
        return enReponses(
                repository.findByProjectIdAndTenantIdOrderByPositionAsc(projectId, tenantId),
                comptesDePieces(tenantId));
    }

    @Transactional
    public ApqpDto.PhaseResponse ajouterLivrable(
            UUID projectId, UUID phaseId, ApqpDto.DeliverableRequest requete) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(projectId, phaseId, tenantId);

        ApqpDeliverable livrable = new ApqpDeliverable();
        livrable.setLabel(requete.label().trim());
        livrable.setExpectedArtifact(nettoyer(requete.expectedArtifact()));
        livrable.setPosition(phase.getDeliverables().size() + 1);
        livrable.setPpap(requete.ppap());
        livrable.setStatus(ApqpDeliverableStatus.NOT_STARTED);
        phase.addDeliverable(livrable);

        repository.save(phase);
        return enReponse(phase, niveau(phase.getPosition(), compte(projectId, tenantId)), Map.of());
    }

    /**
     * Déclare où en est un livrable : coché ou non, daté, affecté, commenté.
     *
     * <p>Rend le CYCLE entier et non la seule phase : cocher un livrable change le
     * compte du dossier PPAP, et laisser l'écran recomposer ce compte l'amènerait à
     * le deviner faux.
     */
    @Transactional
    public ApqpDto.CycleResponse completerLivrable(
            UUID projectId, UUID phaseId, UUID deliverableId,
            ApqpDto.CompletionRequest requete, UUID acteur) {
        UUID tenantId = requireTenantId();
        ApqpProject projet = chargerProjet(projectId, tenantId);
        ApqpPhase phase = charger(projectId, phaseId, tenantId);
        ApqpDeliverable livrable = livrable(phase, deliverableId);

        // Valider AVANT de toucher à l'entité : un renvoi mort ne doit pas laisser
        // un livrable coché à moitié.
        verifierRenvoi(requete, tenantId);

        if (requete.expectedArtifact() != null) {
            livrable.setExpectedArtifact(nettoyer(requete.expectedArtifact()));
        }
        if (requete.ppap() != null) {
            livrable.setPpap(requete.ppap());
        }
        livrable.setOwner(nettoyer(requete.owner()));
        livrable.setDueDate(requete.dueDate());
        livrable.setComment(nettoyer(requete.comment()));
        livrable.setLinkedKind(requete.linkedKind());
        livrable.setLinkedId(requete.linkedId());
        appliquerAvancement(livrable, requete, acteur);

        repository.save(phase);
        return enCycle(projet, tenantId);
    }

    /**
     * La case pilote ; le statut et l'avancement suivent (ADR 0072).
     *
     * <p>Une seule vérité, et c'est la case. Laisser les trois se régler
     * séparément produisait l'état que personne ne sait lire — « terminé à 40 % »,
     * ou « non démarré » sur un livrable coché. L'écran montre les trois, le
     * serveur en fait une.
     *
     * <p>Cocher pose {@code DONE} et 100 %, quoi qu'on ait envoyé d'autre.
     * Décocher les ramène en arrière : le statut envoyé s'il n'est pas
     * {@code DONE}, {@code IN_PROGRESS} sinon ; l'avancement envoyé s'il est
     * inférieur à 100, zéro sinon. Un champ absent — c'est le cas quand on décoche
     * depuis la liste, qui n'a pas de formulaire — vaut « applique la règle ».
     */
    private static void appliquerAvancement(ApqpDeliverable livrable,
                                            ApqpDto.CompletionRequest requete, UUID acteur) {
        if (requete.done()) {
            livrable.setDone(true);
            livrable.setStatus(ApqpDeliverableStatus.DONE);
            livrable.setPercentComplete(100);
            livrable.setDoneAt(Instant.now());
            livrable.setDoneBy(acteur);
            return;
        }
        livrable.setDone(false);
        ApqpDeliverableStatus demande = requete.status();
        livrable.setStatus(demande == null || demande == ApqpDeliverableStatus.DONE
                ? ApqpDeliverableStatus.IN_PROGRESS
                : demande);
        Integer avancement = requete.percentComplete();
        livrable.setPercentComplete(
                avancement == null || avancement >= 100 ? 0 : Math.max(0, avancement));
        // Décocher efface qui et quand : garder la trace d'un achèvement retiré la
        // rendrait fausse, et c'est cette trace que l'auditeur lit.
        livrable.setDoneAt(null);
        livrable.setDoneBy(null);
    }

    /**
     * Le renvoi est facultatif, mais jamais à moitié posé ni jamais mort.
     *
     * <p>Il n'est plus lié à un genre de livrable (ADR 0072) : tout livrable peut
     * désigner l'AMDEC, le cycle PDCA ou la CAPA qui le porte. Ce qu'on continue
     * de refuser, c'est un renvoi incomplet — l'écran afficherait un lien qui ne
     * mène nulle part — et un renvoi vers un enregistrement que le client n'a pas,
     * car un lien mort affirme qu'une preuve existe.
     */
    private void verifierRenvoi(ApqpDto.CompletionRequest requete, UUID tenantId) {
        boolean genre = requete.linkedKind() != null;
        boolean cible = requete.linkedId() != null;
        if (genre != cible) {
            throw new ApqpDeliverableValidationException(
                    "A record reference needs both its kind and its id");
        }
        if (genre) {
            linkResolver.verifier(requete.linkedKind(), requete.linkedId(), tenantId);
        }
    }

    @Transactional
    public ApqpDto.PhaseResponse modifierLivrable(
            UUID projectId, UUID phaseId, UUID livrableId, ApqpDto.DeliverableRequest requete) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(projectId, phaseId, tenantId);

        ApqpDeliverable livrable = livrable(phase, livrableId);
        livrable.setLabel(requete.label().trim());
        livrable.setExpectedArtifact(nettoyer(requete.expectedArtifact()));
        livrable.setPpap(requete.ppap());

        repository.save(phase);
        return enReponse(phase, niveau(phase.getPosition(), compte(projectId, tenantId)),
                comptesDePieces(tenantId));
    }

    /** Retire un livrable et resserre les rangs, pour la même raison qu'une phase. */
    @Transactional
    public ApqpDto.PhaseResponse supprimerLivrable(
            UUID projectId, UUID phaseId, UUID livrableId) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(projectId, phaseId, tenantId);

        boolean retire = phase.getDeliverables().removeIf(d -> d.getId().equals(livrableId));
        if (!retire) {
            throw new ApqpDeliverableNotFoundException(livrableId);
        }
        int rang = 1;
        for (ApqpDeliverable restant : phase.getDeliverables()) {
            restant.setPosition(rang++);
        }

        repository.save(phase);
        return enReponse(phase, niveau(phase.getPosition(), compte(projectId, tenantId)),
                comptesDePieces(tenantId));
    }

    // ---------- interne ----------

    /**
     * Le rang du jalon dans le V.
     *
     * <p>On descend jusqu'au milieu puis on remonte : {@code min(i, n+1-i)}.
     * Pour cinq phases cela donne 1, 2, 3, 2, 1 ; pour six, 1, 2, 3, 3, 2, 1 —
     * le V garde sa forme quel que soit le nombre de phases, ce qui était la
     * condition pour qu'on puisse en ajouter.
     */
    static int niveau(int position, int total) {
        return Math.min(position, total + 1 - position);
    }

    private ApqpProject chargerProjet(UUID projectId, UUID tenantId) {
        return projets.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ApqpProjectNotFoundException(projectId));
    }

    private ApqpPhase charger(UUID projectId, UUID phaseId, UUID tenantId) {
        return repository.findByIdAndProjectIdAndTenantId(phaseId, projectId, tenantId)
                .orElseThrow(() -> new ApqpPhaseNotFoundException(phaseId));
    }

    private int compte(UUID projectId, UUID tenantId) {
        return repository.findByProjectIdAndTenantIdOrderByPositionAsc(projectId, tenantId).size();
    }

    /**
     * Le cycle d'un projet et l'etat de son dossier PPAP.
     *
     * <p>Le compte est calcule par le SERVEUR : deux vues du meme cycle doivent
     * afficher le meme chiffre, et la regle changera le jour ou « requis » voudra
     * dire « coche ET prouve ».
     */
    private ApqpDto.CycleResponse enCycle(ApqpProject projet, UUID tenantId) {
        List<ApqpPhase> phases = repository.findByProjectIdAndTenantIdOrderByPositionAsc(
                projet.getId(), tenantId);
        Map<UUID, Integer> pieces = comptesDePieces(tenantId);
        List<ApqpDto.PhaseResponse> reponses = enReponses(phases, pieces);

        int total = 0;
        int acquis = 0;
        for (ApqpPhase phase : phases) {
            for (ApqpDeliverable livrable : phase.getDeliverables()) {
                if (livrable.isPpap()) {
                    total++;
                    if (livrable.isDone()) {
                        acquis++;
                    }
                }
            }
        }
        return new ApqpDto.CycleResponse(projet.getId(), projet.getName(), projet.getType(),
                projet.getCustomer(), reponses, acquis, total);
    }

    /**
     * Combien de pieces prouvent chaque livrable, en UNE requete groupee.
     *
     * <p>Une requete par livrable ferait une cinquantaine d'allers-retours pour
     * autant d'icones de trombone.
     */
    private Map<UUID, Integer> comptesDePieces(UUID tenantId) {
        Map<UUID, Integer> comptes = new HashMap<>();
        for (Object[] ligne : evidences.countByDeliverableForTenant(tenantId)) {
            comptes.put((UUID) ligne[0], ((Number) ligne[1]).intValue());
        }
        return comptes;
    }

    private List<ApqpDto.PhaseResponse> enReponses(List<ApqpPhase> phases,
                                                  Map<UUID, Integer> pieces) {
        int total = phases.size();
        return phases.stream()
                .map(p -> enReponse(p, niveau(p.getPosition(), total), pieces))
                .toList();
    }

    private ApqpDto.PhaseResponse enReponse(ApqpPhase phase, int niveau,
                                            Map<UUID, Integer> pieces) {
        Locale langue = LocaleContextHolder.getLocale();
        return new ApqpDto.PhaseResponse(
                phase.getId(),
                phase.getPosition(),
                niveau,
                traduit(phase.getReferenceKey(), ".title", phase.getTitle(), langue),
                traduit(phase.getReferenceKey(), ".purpose", phase.getPurpose(), langue),
                traduit(phase.getReferenceKey(), ".question", phase.getQuestion(), langue),
                phase.getDeliverables().stream()
                        .map(d -> enReponse(phase, d, comptePieces(pieces, d)))
                        .toList());
    }

    /**
     * Combien de pieces prouvent ce livrable.
     *
     * <p>L'identifiant peut manquer : un livrable tout juste ajoute n'a pas encore
     * ete rendu persistant quand la reponse se compose. Il n'a alors aucune piece,
     * par construction -- et interroger la table avec une cle nulle ferait echouer
     * l'ajout, ce que le banc d'ajout a mis au jour.
     */
    private static int comptePieces(Map<UUID, Integer> pieces, ApqpDeliverable livrable) {
        return livrable.getId() == null ? 0 : pieces.getOrDefault(livrable.getId(), 0);
    }

    private ApqpDto.DeliverableResponse enReponse(ApqpPhase phase, ApqpDeliverable livrable,
                                                  int pieces) {
        Locale langue = LocaleContextHolder.getLocale();
        return new ApqpDto.DeliverableResponse(
                livrable.getId(),
                livrable.getPosition(),
                traduit(livrable.getReferenceKey(), "", livrable.getLabel(), langue),
                artefact(phase, livrable, langue),
                livrable.isPpap(),
                livrable.getOwner(),
                livrable.getDueDate(),
                livrable.getStatus(),
                livrable.getPercentComplete(),
                livrable.isDone(),
                livrable.getDoneAt(),
                livrable.getDoneBy(),
                livrable.getComment(),
                livrable.getLinkedKind(),
                livrable.getLinkedId(),
                pieces);
    }

    /**
     * L'artefact attendu, dans la langue demandée.
     *
     * <p>Deux cas, et le second n'est pas cosmétique. Si le livrable en porte un,
     * il suit la règle commune : traduit tant qu'il est mot pour mot celui du
     * référentiel, littéral dès qu'un utilisateur l'a réécrit. S'il n'en porte
     * aucun, on rend celui du référentiel — c'est ce qui permet aux cycles écrits
     * AVANT que cette colonne n'existe (migration V131) d'afficher l'artefact sans
     * qu'on ait eu à recopier quarante-huit textes dans une migration SQL, où ils
     * auraient formé une seconde définition du référentiel.
     */
    private String artefact(ApqpPhase phase, ApqpDeliverable livrable, Locale langue) {
        ApqpReference.LivrableModele modele = modeleDuReferentiel(phase, livrable);
        String stocke = livrable.getExpectedArtifact();
        if (stocke == null || stocke.isBlank()) {
            return modele == null
                    ? null
                    : ApqpReferenceTranslations.texte(modele.artefactCle(), langue);
        }
        return modele == null ? stocke : traduit(modele.artefactCle(), "", stocke, langue);
    }

    /**
     * Le modèle du référentiel derrière ce livrable, ou {@code null}.
     *
     * <p>La recherche part de la PHASE et non du seul livrable : « Control plan »
     * paraît deux fois dans le référentiel, en pré-lancement et en production, et
     * la première correspondance trouvée donnerait le mauvais artefact à celui de
     * la phase de validation.
     */
    private ApqpReference.LivrableModele modeleDuReferentiel(ApqpPhase phase,
                                                             ApqpDeliverable livrable) {
        if (livrable.getReferenceKey() == null || phase.getReferenceKey() == null) {
            return null;
        }
        return ApqpReference.PHASES.stream()
                .filter(modele -> modele.cle().equals(phase.getReferenceKey()))
                .flatMap(modele -> modele.livrables().stream())
                .filter(modele -> modele.cle().equals(livrable.getReferenceKey()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Le texte d'une ligne du cycle, dans la langue demandée.
     *
     * <p>La question n'est pas « la LIGNE a-t-elle bougé ? » mais « CE TEXTE-CI
     * est-il encore celui de la plateforme ? ». La nuance n'est pas théorique :
     * cocher un livrable touche la ligne sans toucher son libellé, et faire
     * dépendre la traduction de {@code updatedAt} figeait le libellé dans la
     * langue d'amorçage dès la première case cochée — un défaut, pas une règle.
     *
     * <p>On compare donc le texte stocké à celui du référentiel dans sa langue
     * source. Tant qu'ils coïncident, le texte est celui que la plateforme a
     * écrit et il suit la langue demandée. Dès qu'ils diffèrent, un utilisateur
     * l'a reformulé : sa formulation gagne, dans toutes les langues, parce
     * qu'on ne traduit pas ce qu'un utilisateur a écrit.
     */
    private String traduit(String cle, String suffixe, String stocke, Locale langue) {
        if (cle == null || !estDeLaPlateforme(cle + suffixe, stocke)) {
            return stocke;
        }
        String traduction = ApqpReferenceTranslations.texte(cle + suffixe, langue);
        return traduction == null ? stocke : traduction;
    }

    /**
     * Le texte stocké est-il encore, mot pour mot, celui du référentiel ?
     *
     * <p>La comparaison se fait dans la langue SOURCE du référentiel, car c'est
     * dans cette langue que l'amorçage a écrit en base, quelle que soit la
     * langue de lecture.
     */
    private boolean estDeLaPlateforme(String cle, String stocke) {
        String source = ApqpReferenceTranslations.texte(cle, Locale.forLanguageTag(
                ApqpReferenceTranslations.DEFAUT));
        return source != null && source.equals(stocke);
    }

    private ApqpDeliverable livrable(ApqpPhase phase, UUID deliverableId) {
        return phase.getDeliverables().stream()
                .filter(d -> d.getId().equals(deliverableId))
                .findFirst()
                .orElseThrow(() -> new ApqpDeliverableNotFoundException(deliverableId));
    }

    /** `null` et blancs sont la même absence : on ne stocke pas une chaîne vide. */
    private String nettoyer(String valeur) {
        if (valeur == null) {
            return null;
        }
        String rogne = valeur.trim();
        return rogne.isEmpty() ? null : rogne;
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
