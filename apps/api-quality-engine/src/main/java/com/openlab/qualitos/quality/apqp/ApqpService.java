package com.openlab.qualitos.quality.apqp;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    private final ApqpPhaseRepository repository;

    public ApqpService(ApqpPhaseRepository repository) {
        this.repository = repository;
    }

    /**
     * Le cycle, amorcé au premier appel.
     *
     * <p>Pas {@code readOnly} : la première lecture ÉCRIT, en copiant le
     * référentiel AIAG. Amorcer à la lecture plutôt qu'à la création du client
     * évite une reprise sur tous les clients existants, et laisse un client qui
     * n'ouvre jamais l'écran sans données inutiles.
     */
    @Transactional
    public List<ApqpDto.PhaseResponse> cycle() {
        UUID tenantId = requireTenantId();
        if (!repository.existsByTenantId(tenantId)) {
            amorcer(tenantId);
        }
        return enReponses(repository.findByTenantIdOrderByPositionAsc(tenantId));
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
        return enReponse(enregistree, niveau(rang, rang));
    }

    @Transactional
    public ApqpDto.PhaseResponse modifierPhase(UUID phaseId, ApqpDto.UpdatePhaseRequest requete) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(phaseId, tenantId);

        phase.setTitle(requete.title().trim());
        phase.setPurpose(nettoyer(requete.purpose()));
        phase.setQuestion(nettoyer(requete.question()));

        repository.save(phase);
        return enReponse(phase, niveau(phase.getPosition(), compte(tenantId)));
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
        return enReponses(repository.findByTenantIdOrderByPositionAsc(tenantId));
    }

    @Transactional
    public ApqpDto.PhaseResponse ajouterLivrable(UUID phaseId, ApqpDto.DeliverableRequest requete) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(phaseId, tenantId);

        ApqpDeliverable livrable = new ApqpDeliverable();
        livrable.setLabel(requete.label().trim());
        livrable.setPosition(phase.getDeliverables().size() + 1);
        phase.addDeliverable(livrable);

        repository.save(phase);
        return enReponse(phase, niveau(phase.getPosition(), compte(tenantId)));
    }

    @Transactional
    public ApqpDto.PhaseResponse modifierLivrable(
            UUID phaseId, UUID livrableId, ApqpDto.DeliverableRequest requete) {
        UUID tenantId = requireTenantId();
        ApqpPhase phase = charger(phaseId, tenantId);

        ApqpDeliverable livrable = phase.getDeliverables().stream()
                .filter(d -> d.getId().equals(livrableId))
                .findFirst()
                .orElseThrow(() -> new ApqpDeliverableNotFoundException(livrableId));

        livrable.setLabel(requete.label().trim());
        repository.save(phase);
        return enReponse(phase, niveau(phase.getPosition(), compte(tenantId)));
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
        return enReponse(phase, niveau(phase.getPosition(), compte(tenantId)));
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

    private List<ApqpDto.PhaseResponse> enReponses(List<ApqpPhase> phases) {
        int total = phases.size();
        return phases.stream()
                .map(p -> enReponse(p, niveau(p.getPosition(), total)))
                .toList();
    }

    private ApqpDto.PhaseResponse enReponse(ApqpPhase phase, int niveau) {
        return new ApqpDto.PhaseResponse(
                phase.getId(),
                phase.getPosition(),
                niveau,
                phase.getTitle(),
                phase.getPurpose(),
                phase.getQuestion(),
                phase.getDeliverables().stream()
                        .map(d -> new ApqpDto.DeliverableResponse(d.getId(), d.getPosition(), d.getLabel()))
                        .toList());
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
