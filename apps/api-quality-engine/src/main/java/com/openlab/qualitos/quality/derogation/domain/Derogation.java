package com.openlab.qualitos.quality.derogation.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Une demande de dérogation (concession) client : l'autorisation d'expédier des
 * pièces qui ne respectent pas les spécifications du client.
 *
 * <p><b>Ce n'est pas un écart.</b> L'écart constate un manquement et se traite ;
 * la dérogation demande à un <b>tiers</b> de l'accepter malgré tout, et son
 * cycle de vie est celui d'une négociation. Elle se signe d'abord en interne par
 * le manager qualité désigné, puis part au client, qui accepte ou refuse.
 *
 * <p><b>Le cycle de vie est ici, pas dans le service.</b> Chaque geste porte sa
 * garde — l'état de départ, et <i>qui</i> a le droit de le poser sur <i>ce</i>
 * dossier — parce que ce sont des invariants du dossier lui-même : un service
 * qui les contrôlerait de l'extérieur laisserait un second appelant les
 * contourner. Le service n'orchestre que ce qui est autour : charger, écrire,
 * notifier, journaliser.
 *
 * @see DerogationStatus pour le cycle, et pourquoi le manager ne refuse pas
 */
public final class Derogation {

    /**
     * Les six champs que la maquette exige pour demander la signature, dans
     * l'ordre du message affiché : « Champs obligatoires manquants : client,
     * réf. pièce, quantité, justification, cause, action corrective. »
     */
    private static final String CHAMP_CLIENT = "client";
    private static final String CHAMP_PIECE = "réf. pièce";
    private static final String CHAMP_QUANTITE = "quantité";
    private static final String CHAMP_JUSTIFICATION = "justification";
    private static final String CHAMP_CAUSE = "cause";
    private static final String CHAMP_ACTION = "action corrective";

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private DerogationStatus status;

    private Identification identification;
    private Nature nature;
    private final List<DerogationLine> lines = new ArrayList<>();
    private String justification;
    private Analysis analysis;
    private ManagerApproval approval;
    private CustomerDispatch dispatch;
    private CustomerAnswer answer;

    private final UUID createdByUserId;
    private final String createdByName;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    private Derogation(UUID id, UUID tenantId, String reference, DerogationStatus status,
                       Identification identification, Nature nature, String justification,
                       Analysis analysis, ManagerApproval approval, CustomerDispatch dispatch,
                       CustomerAnswer answer, UUID createdByUserId, String createdByName,
                       Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = Objects.requireNonNull(reference, "reference");
        this.status = status;
        this.identification = identification;
        this.nature = nature;
        this.justification = justification;
        this.analysis = analysis;
        this.approval = approval;
        this.dispatch = dispatch;
        this.answer = answer;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    /** Un dossier neuf, au brouillon, chez celui qui l'ouvre. */
    public static Derogation create(UUID tenantId, String reference, UUID createdBy,
                                    String createdByName, Identification identification,
                                    Instant now) {
        return new Derogation(null, tenantId, reference, DerogationStatus.DRAFT,
                identification, Nature.parDefaut(), null, Analysis.vide(),
                ManagerApproval.aucune(), CustomerDispatch.aucun(), CustomerAnswer.enAttente(),
                createdBy, createdByName, now, now, 0L);
    }

    /** Le dossier tel qu'il a été enregistré. Réservé à l'infrastructure. */
    public static Derogation rehydrate(UUID id, UUID tenantId, String reference,
                                       DerogationStatus status, Identification identification,
                                       Nature nature, List<DerogationLine> lines,
                                       String justification, Analysis analysis,
                                       ManagerApproval approval, CustomerDispatch dispatch,
                                       CustomerAnswer answer, UUID createdByUserId,
                                       String createdByName, Instant createdAt,
                                       Instant updatedAt, long version) {
        Derogation d = new Derogation(id, tenantId, reference, status, identification, nature,
                justification, analysis, approval, dispatch, answer, createdByUserId,
                createdByName, createdAt, updatedAt, version);
        if (lines != null) {
            d.lines.addAll(lines);
        }
        return d;
    }

    // ---- les gestes du cycle de vie -----------------------------------------

    /**
     * Modifier la fiche.
     *
     * <p>Hors brouillon, la maquette désactive tous les champs : on refuse donc,
     * plutôt que d'accepter puis d'ignorer. « À modifier » est modifiable, c'est
     * tout son objet.
     */
    public void edit(UUID acteur, Identification identification, Nature nature,
                     List<DerogationLine> lignes, String justification, Analysis analysis,
                     Instant now) {
        exigerModifiable();
        exigerInitiateur(acteur, "Seul l'initiateur du dossier peut le modifier.");
        this.identification = identification;
        this.nature = nature;
        this.justification = Texte.net(justification);
        this.analysis = analysis;
        remplacerLignes(lignes);
        this.updatedAt = now;
    }

    /**
     * Demander la validation du manager.
     *
     * <p>C'est ici que la complétude se contrôle, et nulle part ailleurs : un
     * brouillon a le droit d'être incomplet, une demande non.
     */
    public void requestApproval(UUID acteur, UUID manager, String nomDuManager, Instant now) {
        exigerModifiable();
        exigerInitiateur(acteur, "Seul l'initiateur du dossier peut en demander la validation.");
        List<String> manquants = missingFields();
        if (!manquants.isEmpty()) {
            throw new DerogationValidationException(
                    "Champs obligatoires manquants : " + String.join(", ", manquants) + ".",
                    manquants);
        }
        // Le motif du tour precedent ne survit pas a la correction : il dirait
        // encore ce qui manquait a une fiche qui a change.
        this.approval = approval.confieA(
                Objects.requireNonNull(manager, "manager"), Texte.net(nomDuManager));
        this.status = DerogationStatus.PENDING_INTERNAL;
        this.updatedAt = now;
    }

    /** « Retirer la demande » : la seule sortie par le bas, et elle est à l'initiateur. */
    public void withdraw(UUID acteur, Instant now) {
        if (status != DerogationStatus.PENDING_INTERNAL) {
            throw new DerogationStateException(
                    "Seule une demande en attente de validation peut être retirée.");
        }
        exigerInitiateur(acteur, "Seul l'initiateur du dossier peut retirer sa demande.");
        this.status = DerogationStatus.DRAFT;
        this.updatedAt = now;
    }

    /**
     * « Renvoyer pour correction » : le manager n'accepte pas, et dit pourquoi.
     *
     * <p>Le motif est obligatoire. La maquette renvoyait au brouillon sans un
     * mot ; l'initiateur ne savait alors pas quoi corriger, et la boucle pouvait
     * tourner à vide.
     */
    public void sendBack(UUID acteur, String motif, Instant now) {
        exigerEnAttenteInterne();
        exigerManagerDesigne(acteur);
        if (Texte.vide(motif)) {
            throw new DerogationValidationException(
                    "Dites ce qui doit être corrigé : sans motif, l'initiateur ne peut pas reprendre.");
        }
        this.approval = approval.renvoyee(motif);
        this.status = DerogationStatus.TO_AMEND;
        this.updatedAt = now;
    }

    /** « Approuver et signer » : le nom saisi et le tracé, tous deux exigés. */
    public void approve(UUID acteur, String nomSaisi, SignatureTrace trace, Instant now) {
        exigerEnAttenteInterne();
        exigerManagerDesigne(acteur);
        if (Texte.vide(nomSaisi)) {
            throw new DerogationValidationException("Indiquez votre nom et signez.");
        }
        Objects.requireNonNull(trace, "trace");
        this.approval = approval.signee(nomSaisi, trace.bytes(), now);
        this.status = DerogationStatus.QUALITY_APPROVED;
        this.updatedAt = now;
    }

    /**
     * « Rouvrir (annule la signature) » : la fiche redevient un brouillon, et la
     * signature disparaît.
     *
     * <p>Le bouton est visible de tous dans la maquette, et aucun droit ne lui est
     * attribué : aucune garde d'acteur n'est donc posée ici. Mais un dossier déjà
     * parti au client ne se rouvre <b>pas</b> — le client détient un document
     * signé, et le rouvrir le démentirait sans qu'il en sache rien.
     */
    public void reopen(Instant now) {
        if (status != DerogationStatus.QUALITY_APPROVED) {
            throw new DerogationStateException(dispatch.estParti()
                    ? "Ce dossier est parti au client : il ne peut plus être rouvert."
                    : "Seul un dossier approuvé par la qualité peut être rouvert.");
        }
        this.approval = approval.sansSignature();
        this.status = DerogationStatus.DRAFT;
        this.updatedAt = now;
    }

    /**
     * « Validation client » : le dossier est inscrit comme parti.
     *
     * <p>Le service appelle ceci <b>avant</b> l'envoi, et n'envoie que si
     * l'enregistrement a tenu : le contraire laisserait un client détenir un
     * document signé pour un dossier que la plateforme croit encore interne.
     */
    public void markSentToCustomer(UUID acteur, String courriel, String commentaire,
                                   DerogationLanguage langue, Instant now) {
        if (status != DerogationStatus.QUALITY_APPROVED) {
            throw new DerogationStateException(
                    "Le manager qualité doit avoir signé avant tout envoi au client.");
        }
        exigerInitiateur(acteur, "Seul l'initiateur du dossier peut l'envoyer au client.");
        if (Texte.vide(courriel)) {
            throw new DerogationValidationException("L'adresse du client est obligatoire.");
        }
        this.dispatch = new CustomerDispatch(courriel, commentaire,
                Objects.requireNonNull(langue, "langue"), now);
        this.answer = CustomerAnswer.enAttente();
        this.status = DerogationStatus.PENDING_CUSTOMER;
        this.updatedAt = now;
    }

    /**
     * La réponse du client, portée à la main par l'initiateur (diapositive 4 :
     * « on revient pour mettre sa décision en étape 6 »).
     *
     * <p>Elle ne se porte qu'une fois la demande partie : avant, il n'y a rien à
     * quoi le client aurait pu répondre. Elle se corrige en revanche après coup —
     * une décision saisie de travers doit pouvoir être reprise.
     */
    public void recordCustomerAnswer(UUID acteur, CustomerDecision decision,
                                     LocalDate quand, Instant now) {
        if (!dispatch.estParti()) {
            throw new DerogationStateException(
                    "La décision du client ne se porte qu'une fois la demande envoyée.");
        }
        exigerInitiateur(acteur, "Seul l'initiateur du dossier porte la décision du client.");
        if (decision == null || decision == CustomerDecision.PENDING) {
            throw new DerogationValidationException(
                    "« En attente » n'est pas une décision : choisissez la réponse du client.");
        }
        if (quand == null) {
            throw new DerogationValidationException("La date de la décision est obligatoire.");
        }
        this.answer = new CustomerAnswer(decision, quand);
        this.status = decision.toStatus();
        this.updatedAt = now;
    }

    // ---- les règles ----------------------------------------------------------

    /**
     * Les champs qui manquent pour demander la signature, dans l'ordre du message
     * de la maquette. Six, pas plus : c'est la liste qu'elle affiche.
     */
    public List<String> missingFields() {
        if (identification == null) {
            return List.of(CHAMP_CLIENT, CHAMP_PIECE, CHAMP_QUANTITE,
                    CHAMP_JUSTIFICATION, CHAMP_CAUSE, CHAMP_ACTION);
        }
        List<String> manquants = new ArrayList<>();
        if (identification.customerName() == null) {
            manquants.add(CHAMP_CLIENT);
        }
        if (identification.partReference() == null) {
            manquants.add(CHAMP_PIECE);
        }
        BigDecimal q = identification.quantity();
        if (q == null || q.compareTo(BigDecimal.ZERO) <= 0) {
            manquants.add(CHAMP_QUANTITE);
        }
        if (justification == null) {
            manquants.add(CHAMP_JUSTIFICATION);
        }
        if (analysis == null || analysis.rootCause() == null) {
            manquants.add(CHAMP_CAUSE);
        }
        if (analysis == null || analysis.correctiveAction() == null) {
            manquants.add(CHAMP_ACTION);
        }
        return manquants;
    }

    /** La première ligne d'écart : la colonne « défaut » du registre. */
    public Optional<DerogationLine> firstLine() {
        return lines.isEmpty() ? Optional.empty() : Optional.of(lines.get(0));
    }

    /**
     * Les lignes se réécrivent en bloc, et leur rang est refait.
     *
     * <p>Le tableau de la maquette n'a pas de bouton de suppression : il se
     * renvoie entier, et c'est l'ordre reçu qui fait l'ordre. Les lignes vides —
     * elle en ouvre deux — sont écartées.
     */
    private void remplacerLignes(List<DerogationLine> recues) {
        lines.clear();
        if (recues == null) {
            return;
        }
        int rang = 1;
        for (DerogationLine ligne : recues) {
            if (ligne == null || ligne.isBlank()) {
                continue;
            }
            lines.add(ligne.at(rang));
            rang++;
        }
    }

    private void exigerModifiable() {
        if (!status.isEditable()) {
            throw new DerogationStateException(switch (status) {
                case PENDING_INTERNAL ->
                        "Ce dossier attend la validation du manager : retirez la demande pour le modifier.";
                case QUALITY_APPROVED ->
                        "Ce dossier est signé : rouvrez-le pour le modifier, ce qui annulera la signature.";
                case PENDING_CUSTOMER -> "Ce dossier est parti au client : il ne se modifie plus.";
                default -> "Ce dossier est terminé : il ne se modifie plus.";
            });
        }
    }

    private void exigerEnAttenteInterne() {
        if (status != DerogationStatus.PENDING_INTERNAL) {
            throw new DerogationStateException(
                    "Ce dossier n'attend pas de validation du manager qualité.");
        }
    }

    /**
     * Le manager DÉSIGNÉ, et non n'importe quel manager qualité.
     *
     * <p>La permission dit qui peut signer des dérogations ; le dossier dit à qui
     * celle-ci a été confiée. Sans cette seconde garde, un manager signerait le
     * dossier d'un autre — et la maquette affiche « Demande envoyée à … », donc le
     * dossier a bien un destinataire.
     */
    private void exigerManagerDesigne(UUID acteur) {
        if (!approval.estLeManager(acteur)) {
            throw new DerogationForbiddenException(
                    "Ce dossier a été confié à un autre manager qualité.");
        }
    }

    private void exigerInitiateur(UUID acteur, String message) {
        if (!createdByUserId.equals(acteur)) {
            throw new DerogationForbiddenException(message);
        }
    }

    /** L'initiateur du dossier : l'{@code owner} du registre. */
    public boolean isInitiator(UUID acteur) {
        return createdByUserId.equals(acteur);
    }

    /** Le manager à qui ce dossier est confié. */
    public boolean isDesignatedManager(UUID acteur) {
        return approval.estLeManager(acteur);
    }

    /** L'identifiant attribué à l'enregistrement. Réservé à l'infrastructure. */
    public void assignId(UUID id) {
        this.id = id;
    }

    /** La version relue après écriture. Réservé à l'infrastructure. */
    public void assignVersion(long version) {
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getReference() {
        return reference;
    }

    public DerogationStatus getStatus() {
        return status;
    }

    public Identification getIdentification() {
        return identification;
    }

    public Nature getNature() {
        return nature;
    }

    public List<DerogationLine> getLines() {
        return List.copyOf(lines);
    }

    public String getJustification() {
        return justification;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public ManagerApproval getApproval() {
        return approval;
    }

    public CustomerDispatch getDispatch() {
        return dispatch;
    }

    public CustomerAnswer getAnswer() {
        return answer;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
