package com.openlab.qualitos.quality.apqp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le cycle APQP d'un client : lecture, amorçage, et modification.
 *
 * <p>Les cinq phases du manuel AIAG ne sont plus une constante du code mais la
 * valeur de départ d'un cycle qui appartient au client. Il peut renommer une
 * phase, en retirer une, en ajouter une sixième, et remanier ses livrables.
 */
@Service
public class ApqpService {

    private static final Logger log = LoggerFactory.getLogger(ApqpService.class);

    private final ApqpPhaseRepository repository;
    private final ApqpDeliverableEvidenceRepository evidences;
    private final ApqpDeliverableDataValidator validator;
    private final ApqpLinkResolver linkResolver;
    private final ObjectMapper mapper;

    public ApqpService(ApqpPhaseRepository repository,
                       ApqpDeliverableEvidenceRepository evidences,
                       ApqpDeliverableDataValidator validator,
                       ApqpLinkResolver linkResolver,
                       ObjectMapper mapper) {
        this.repository = repository;
        this.evidences = evidences;
        this.validator = validator;
        this.linkResolver = linkResolver;
        this.mapper = mapper;
    }

    /**
     * Le cycle, amorcé au premier appel, et l'état du dossier PPAP.
     *
     * <p>Pas {@code readOnly} : la première lecture ÉCRIT, en copiant le
     * référentiel. Amorcer à la lecture plutôt qu'à la création du client évite
     * une reprise sur tous les clients existants, et laisse un client qui n'ouvre
     * jamais l'écran sans données inutiles.
     */
    @Transactional
    public ApqpDto.CycleResponse cycle() {
        UUID tenantId = requireTenantId();
        if (!repository.existsByTenantId(tenantId)) {
            amorcer(tenantId);
        }
        return enCycle(repository.findByTenantIdOrderByPositionAsc(tenantId), tenantId);
    }

    /**
     * Rend au client le cycle du référentiel, en effaçant le sien.
     *
     * <p>Il faut une porte de sortie explicite : la reprise a laissé intacts les
     * cycles adaptés — c'était le bon choix — mais un client qui VEUT la nouvelle
     * liste n'aurait sinon aucun moyen de l'obtenir, sinon en supprimant ses phases
     * une à une.
     *
     * <p>Destructif et assumé comme tel : l'écran le demande deux fois, et la
     * suppression emporte les pièces versées aux livrables (cascade).
     */
    @Transactional
    public ApqpDto.CycleResponse reinitialiser() {
        UUID tenantId = requireTenantId();
        repository.deleteByTenantId(tenantId);
        // Sans ce vidage, l'insertion qui suit bute sur la contrainte d'unicité
        // (client, rang) : les suppressions ne sont pas encore parties en base.
        repository.flush();
        amorcer(tenantId);
        return enCycle(repository.findByTenantIdOrderByPositionAsc(tenantId), tenantId);
    }

    @Transactional
    public ApqpDto.PhaseResponse creerPhase(ApqpDto.CreatePhaseRequest requete) {
        UUID tenantId = requireTenantId();
        // Le cycle doit exister avant qu'on y ajoute : sans cela, la première
        // phase créée à la main serait aussitôt suivie des cinq phases d'amorçage.
        if (!repository.existsByTenantId(tenantId)) {
            amorcer(tenantId);
        }

        int rang = repository.findFirstByTenantIdOrderByPositionDesc(tenantId)
                .map(p -> p.getPosition() + 1)
                .orElse(1);

        ApqpPhase phase = new ApqpPhase();
        phase.setTenantId(tenantId);
        phase.setPosition(rang);
        phase.setTitle(requete.title().trim());
        phase.setPurpose(nettoyer(requete.purpose()));
        phase.setQuestion(nettoyer(requete.question()));

        ApqpPhase enregistree = repository.save(phase);
        return enReponse(enregistree, niveau(rang, rang), Map.of());
    }

    @Transactional
    public ApqpDto.PhaseResponse modifierPhase(UUID phaseId, ApqpDto.UpdatePhaseRequest requete) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(phaseId, tenantId);

        phase.setTitle(requete.title().trim());
        phase.setPurpose(nettoyer(requete.purpose()));
        phase.setQuestion(nettoyer(requete.question()));

        repository.save(phase);
        return enReponse(phase, niveau(phase.getPosition(), compte(tenantId)),
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
    public void supprimerPhase(UUID phaseId) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(phaseId, tenantId);
        repository.delete(phase);
        repository.flush();

        List<ApqpPhase> restantes = repository.findByTenantIdOrderByPositionAsc(tenantId);
        for (int i = 0; i < restantes.size(); i++) {
            restantes.get(i).setPosition(i + 1);
        }
        repository.saveAll(restantes);
    }

    /** Remet le cycle dans l'ordre donné. */
    @Transactional
    public List<ApqpDto.PhaseResponse> reorganiser(ApqpDto.ReorderRequest requete) {
        UUID tenantId = requireTenantId();
        List<ApqpPhase> phases = repository.findByTenantIdOrderByPositionAsc(tenantId);

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
        return enReponses(repository.findByTenantIdOrderByPositionAsc(tenantId),
                comptesDePieces(tenantId));
    }

    @Transactional
    public ApqpDto.PhaseResponse ajouterLivrable(UUID phaseId, ApqpDto.DeliverableRequest requete) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(phaseId, tenantId);

        ApqpDeliverable livrable = new ApqpDeliverable();
        livrable.setLabel(requete.label().trim());
        livrable.setPosition(phase.getDeliverables().size() + 1);
        livrable.setPpap(requete.ppap());
        livrable.setKind(requete.kind());
        phase.addDeliverable(livrable);

        repository.save(phase);
        return enReponse(phase, niveau(phase.getPosition(), compte(tenantId)), Map.of());
    }

    /**
     * Déclare où en est un livrable : coché ou non, commenté, prouvé, renvoyé.
     *
     * <p>Rend le CYCLE entier et non la seule phase : cocher un livrable change le
     * compte du dossier PPAP, affiché sous le schéma, et laisser l'écran recomposer
     * ce compte l'amènerait à le deviner faux.
     */
    @Transactional
    public ApqpDto.CycleResponse completerLivrable(
            UUID phaseId, UUID deliverableId, ApqpDto.CompletionRequest requete, UUID acteur) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(phaseId, tenantId);
        ApqpDeliverable livrable = livrable(phase, deliverableId);

        // Valider AVANT de toucher à l'entité : un renvoi mort ne doit pas laisser
        // un livrable coché à moitié.
        String data = validator.valider(livrable.getKind(), requete.data());
        verifierRenvoi(livrable, requete, tenantId);

        livrable.setComment(nettoyer(requete.comment()));
        livrable.setData(data);
        livrable.setLinkedKind(requete.linkedKind());
        livrable.setLinkedId(requete.linkedId());
        livrable.setDone(requete.done());
        if (requete.done()) {
            livrable.setDoneAt(Instant.now());
            livrable.setDoneBy(acteur);
        } else {
            // Décocher efface qui et quand : garder la trace d'un achèvement
            // retiré la rendrait fausse, et c'est cette trace que l'auditeur lit.
            livrable.setDoneAt(null);
            livrable.setDoneBy(null);
        }

        repository.save(phase);
        return enCycle(repository.findByTenantIdOrderByPositionAsc(tenantId), tenantId);
    }

    /**
     * Le renvoi va de pair avec le genre.
     *
     * <p>Un livrable {@code MODULE_LINK} coché sans son enregistrement affirmerait
     * qu'une AMDEC existe sans dire laquelle ; tout autre genre porteur d'un renvoi
     * afficherait un lien que son formulaire ne sait pas rendre.
     */
    private void verifierRenvoi(ApqpDeliverable livrable, ApqpDto.CompletionRequest requete,
                                UUID tenantId) {
        boolean renvoiFourni = requete.linkedKind() != null && requete.linkedId() != null;

        if (livrable.getKind() != ApqpDeliverableKind.MODULE_LINK) {
            if (requete.linkedKind() != null || requete.linkedId() != null) {
                throw new ApqpDeliverableValidationException(
                        "Only a MODULE_LINK deliverable carries a record reference");
            }
            return;
        }
        if (!renvoiFourni) {
            if (requete.linkedKind() != null || requete.linkedId() != null) {
                throw new ApqpDeliverableValidationException(
                        "A record reference needs both its kind and its id");
            }
            if (requete.done()) {
                throw new ApqpDeliverableValidationException(
                        "A MODULE_LINK deliverable cannot be done without its record");
            }
            return;
        }
        linkResolver.verifier(requete.linkedKind(), requete.linkedId(), tenantId);
    }

    @Transactional
    public ApqpDto.PhaseResponse modifierLivrable(
            UUID phaseId, UUID livrableId, ApqpDto.DeliverableRequest requete) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(phaseId, tenantId);

        ApqpDeliverable livrable = livrable(phase, livrableId);

        livrable.setLabel(requete.label().trim());
        livrable.setPpap(requete.ppap());
        if (livrable.getKind() != requete.kind()) {
            // Le contenu appartient au genre : une liste de points lue comme une
            // table de mesures ne veut rien dire. On vide, plutot que de garder
            // « au cas ou » un etat qu'aucun formulaire ne sait rendre.
            livrable.setKind(requete.kind());
            livrable.setData(null);
            livrable.setLinkedKind(null);
            livrable.setLinkedId(null);
        }
        repository.save(phase);
        return enReponse(phase, niveau(phase.getPosition(), compte(tenantId)),
                comptesDePieces(tenantId));
    }

    /** Retire un livrable et resserre les rangs, pour la même raison qu'une phase. */
    @Transactional
    public ApqpDto.PhaseResponse supprimerLivrable(UUID phaseId, UUID livrableId) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(phaseId, tenantId);

        boolean retire = phase.getDeliverables().removeIf(d -> d.getId().equals(livrableId));
        if (!retire) {
            throw new ApqpDeliverableNotFoundException(livrableId);
        }
        int rang = 1;
        for (ApqpDeliverable restant : phase.getDeliverables()) {
            restant.setPosition(rang++);
        }

        repository.save(phase);
        return enReponse(phase, niveau(phase.getPosition(), compte(tenantId)),
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

    private void amorcer(UUID tenantId) {
        int rang = 1;
        List<ApqpPhase> phases = new ArrayList<>();
        for (ApqpReference.PhaseModele modele : ApqpReference.PHASES) {
            ApqpPhase phase = new ApqpPhase();
            phase.setTenantId(tenantId);
            phase.setPosition(rang++);
            phase.setTitle(modele.titre());
            phase.setPurpose(modele.objet());
            phase.setQuestion(modele.question());

            int rangLivrable = 1;
            for (ApqpReference.LivrableModele modeleLivrable : modele.livrables()) {
                ApqpDeliverable livrable = new ApqpDeliverable();
                livrable.setLabel(modeleLivrable.libelle());
                livrable.setPosition(rangLivrable++);
                livrable.setPpap(modeleLivrable.ppap());
                livrable.setKind(modeleLivrable.genre());
                livrable.setData(amorceEnJson(modeleLivrable));
                phase.addDeliverable(livrable);
            }
            phases.add(phase);
        }
        repository.saveAll(phases);
    }

    /**
     * Le contenu d'amorçage d'un livrable, en JSON, ou {@code null}.
     *
     * <p>Écrit à la main plutôt que par un sérialiseur : deux formes fermées,
     * trois champs chacune, et des libellés qui viennent d'une constante du code.
     * Passer par un mapper ferait dépendre ce qui entre en base d'une
     * configuration tenue ailleurs, qui peut changer sans qu'on s'en avise.
     *
     * <p>Les guillemets sont malgré tout échappés : rien n'interdit qu'un libellé
     * du référentiel en contienne demain.
     */
    private static String amorceEnJson(ApqpReference.LivrableModele modele) {
        if (modele.amorce().isEmpty()) {
            return null;
        }
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < modele.amorce().size(); i++) {
            String libelle = modele.amorce().get(i).replace("\\", "\\\\").replace("\"", "\\\"");
            if (i > 0) {
                json.append(',');
            }
            json.append(switch (modele.genre()) {
                case CHECKLIST -> "{\"label\":\"" + libelle + "\",\"checked\":false}";
                case DATA_ENTRY -> "{\"label\":\"" + libelle
                        + "\",\"value\":\"\",\"unit\":\"\",\"measuredAt\":null}";
                case ATTACHMENT, MODULE_LINK -> throw new IllegalStateException(
                        "Un livrable " + modele.genre() + " ne s'amorce pas avec des sous-points");
            });
        }
        return json.append(']').toString();
    }

    private ApqpPhase charger(UUID phaseId, UUID tenantId) {
        return repository.findByIdAndTenantId(phaseId, tenantId)
                .orElseThrow(() -> new ApqpPhaseNotFoundException(phaseId));
    }

    private int compte(UUID tenantId) {
        return repository.findByTenantIdOrderByPositionAsc(tenantId).size();
    }

    /**
     * Le cycle et l'etat de son dossier PPAP.
     *
     * <p>Le compte est calcule par le SERVEUR : deux vues du meme cycle doivent
     * afficher le meme chiffre, et la regle changera le jour ou « acquis » voudra
     * dire « coche ET prouve ».
     */
    private ApqpDto.CycleResponse enCycle(List<ApqpPhase> phases, UUID tenantId) {
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
        return new ApqpDto.CycleResponse(reponses, acquis, total);
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
        return new ApqpDto.PhaseResponse(
                phase.getId(),
                phase.getPosition(),
                niveau,
                phase.getTitle(),
                phase.getPurpose(),
                phase.getQuestion(),
                phase.getDeliverables().stream()
                        .map(d -> enReponse(d, comptePieces(pieces, d)))
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

    private ApqpDto.DeliverableResponse enReponse(ApqpDeliverable livrable, int pieces) {
        return new ApqpDto.DeliverableResponse(
                livrable.getId(),
                livrable.getPosition(),
                livrable.getLabel(),
                livrable.isPpap(),
                livrable.getKind(),
                livrable.isDone(),
                livrable.getDoneAt(),
                livrable.getDoneBy(),
                livrable.getComment(),
                relire(livrable),
                livrable.getLinkedKind(),
                livrable.getLinkedId(),
                pieces);
    }

    /**
     * Relit le contenu stocke d'un livrable.
     *
     * <p>Lecture au mapper, ecriture a la main : on controle ce qui entre en base,
     * on ne se defie pas de ce qu'on en ressort. Une valeur illisible -- une donnee
     * ecrite avant ce lot, ou touchee a la main -- rend une liste vide et se
     * journalise, plutot que de faire echouer tout l'ecran pour une ligne.
     */
    private List<ApqpDto.DataRow> relire(ApqpDeliverable livrable) {
        String data = livrable.getData();
        if (data == null || data.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(data, new TypeReference<List<ApqpDto.DataRow>>() {});
        } catch (Exception ex) {
            log.warn("Contenu illisible sur le livrable APQP {} : {}",
                    livrable.getId(), ex.getMessage());
            return List.of();
        }
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
