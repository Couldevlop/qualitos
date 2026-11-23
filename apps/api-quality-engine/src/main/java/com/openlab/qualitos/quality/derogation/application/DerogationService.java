package com.openlab.qualitos.quality.derogation.application;

import com.openlab.qualitos.quality.derogation.domain.Analysis;
import com.openlab.qualitos.quality.derogation.domain.Derogation;
import com.openlab.qualitos.quality.derogation.domain.DerogationLanguage;
import com.openlab.qualitos.quality.derogation.domain.DerogationLine;
import com.openlab.qualitos.quality.derogation.domain.DerogationNotFoundException;
import com.openlab.qualitos.quality.derogation.domain.DerogationQuery;
import com.openlab.qualitos.quality.derogation.domain.DerogationRepository;
import com.openlab.qualitos.quality.derogation.domain.Identification;
import com.openlab.qualitos.quality.derogation.domain.Nature;
import com.openlab.qualitos.quality.derogation.domain.DerogationValidationException;
import com.openlab.qualitos.quality.derogation.domain.SignatureTrace;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Orchestre une demande de dérogation : charger, écrire, notifier, journaliser.
 *
 * <p><b>Ce service ne décide rien.</b> Les règles — qui peut quoi, dans quel
 * état, avec quels champs remplis — sont sur l'agrégat {@code Derogation}, parce
 * que ce sont ses invariants. Ici vit ce que le dossier ne peut pas faire
 * lui-même : lui trouver une référence libre, le relire, le ranger, prévenir les
 * gens, et laisser une trace.
 *
 * <p>Classe simple, sans annotation de cadre : elle est câblée par
 * {@code DerogationBeanConfiguration} et se teste avec des ports simulés, sans
 * démarrer Spring.
 */
public class DerogationService {

    private final DerogationRepository repository;
    private final DerogationAuditPort audit;
    private final DerogationNotificationPort notifications;
    private final DerogationMailPort mail;
    private final DerogationPdfPort pdf;
    private final DirectoryPort directory;
    private final TenantProvider tenants;
    private final ActorProvider actors;
    private final DerogationLinkBuilder links;
    private final Clock clock;

    public DerogationService(DerogationRepository repository,
                             DerogationAuditPort audit,
                             DerogationNotificationPort notifications,
                             DerogationMailPort mail,
                             DerogationPdfPort pdf,
                             DirectoryPort directory,
                             TenantProvider tenants,
                             ActorProvider actors,
                             DerogationLinkBuilder links,
                             Clock clock) {
        this.repository = repository;
        this.audit = audit;
        this.notifications = notifications;
        this.mail = mail;
        this.pdf = pdf;
        this.directory = directory;
        this.tenants = tenants;
        this.actors = actors;
        this.links = links;
        this.clock = clock;
    }

    // ---- lecture -------------------------------------------------------------

    /**
     * Le registre, filtré comme la présentation le demande : owner, produit,
     * client, statut.
     *
     * @param voitTout quand c'est faux, la recherche se borne à ce qui concerne
     *                 l'appelant — ses dossiers et ceux qu'il doit signer
     *                 (ADR 0081)
     */
    public DerogationDto.PageView<DerogationDto.RowView> search(
            DerogationDto.SearchCommand commande, boolean voitTout) {
        UUID tenantId = tenants.requireTenantId();
        UUID moi = actors.requireAccountId();
        DerogationQuery query = new DerogationQuery(tenantId, commande.owner(),
                commande.partReference(), commande.customerName(), commande.status(),
                voitTout ? null : moi, commande.page(), commande.size());
        var tranche = repository.search(query);
        return new DerogationDto.PageView<>(
                tranche.content().stream().map(DerogationDto.RowView::of).toList(),
                tranche.total(), tranche.page(), tranche.size());
    }

    /** La file d'attente du manager courant : « Mes validations ». */
    public DerogationDto.PageView<DerogationDto.RowView> myPendingApprovals(int page, int size) {
        UUID tenantId = tenants.requireTenantId();
        UUID moi = actors.requireAccountId();
        var tranche = repository.findPendingApprovals(tenantId, moi, page, size);
        return new DerogationDto.PageView<>(
                tranche.content().stream().map(DerogationDto.RowView::of).toList(),
                tranche.total(), tranche.page(), tranche.size());
    }

    /** L'alerte rouge et son nombre, au-dessus de « Mes validations ». */
    public DerogationDto.PendingCount myPendingApprovalCount() {
        UUID tenantId = tenants.requireTenantId();
        UUID moi = actors.requireAccountId();
        return new DerogationDto.PendingCount(
                repository.countPendingApprovals(tenantId, moi));
    }

    /**
     * La fiche d'un dossier.
     *
     * @param voitTout quand c'est faux, un dossier qui ne concerne pas l'appelant
     *                 répond « introuvable » et non « interdit » : dire
     *                 « interdit » révélerait son existence (ADR 0081)
     */
    public DerogationDto.View get(UUID id, boolean voitTout) {
        return DerogationDto.View.of(exigerVisible(charger(id), id, voitTout));
    }

    /**
     * La garde de visibilité, posée en un seul endroit.
     *
     * <p>Elle répond « introuvable » et non « interdit » : dire « interdit »
     * révélerait l'existence du dossier, et donc qu'un client donné a demandé une
     * dérogation — ce que l'ADR 0081 refuse pour les mêmes raisons sur le
     * registre des écarts.
     */
    private Derogation exigerVisible(Derogation d, UUID id, boolean voitTout) {
        if (voitTout) {
            return d;
        }
        UUID moi = actors.requireAccountId();
        if (!d.isInitiator(moi) && !d.isDesignatedManager(moi)) {
            throw new DerogationNotFoundException(id);
        }
        return d;
    }

    /**
     * Le PDF du dossier, dans la langue retenue à l'envoi, ou en français.
     *
     * <p>Il porte la <b>même</b> garde de visibilité que la fiche, et ce n'est pas
     * une précaution de forme : un document qui s'obtient sans le droit de lire
     * la fiche rend la garde de la fiche décorative, et celui-ci contient tout
     * son contenu — client, pièce, cotes, cause racine.
     */
    public byte[] renderPdf(UUID id, boolean voitTout) {
        Derogation d = exigerVisible(charger(id), id, voitTout);
        DerogationLanguage langue = d.getDispatch().language() == null
                ? DerogationLanguage.FR : d.getDispatch().language();
        return pdf.render(d, langue);
    }

    // ---- écriture ------------------------------------------------------------

    public DerogationDto.View create(DerogationDto.SaveCommand commande) {
        UUID tenantId = tenants.requireTenantId();
        UUID moi = actors.requireAccountId();
        Instant maintenant = clock.instant();
        Derogation d = Derogation.create(tenantId, referenceLibre(tenantId), moi,
                actors.displayName().orElse(null), identification(commande), maintenant);
        // La creation passe par `edit` : le dossier neuf est un brouillon dont
        // l'initiateur est l'acteur, donc la garde est satisfaite, et les six
        // sections s'ecrivent par le seul chemin qui les valide.
        d.edit(moi, identification(commande), nature(commande), lignes(commande),
                commande.justification(), analysis(commande), maintenant);
        Derogation enregistre = repository.save(d);
        audit.record(enregistre, DerogationAuditPort.Action.CREATED);
        return DerogationDto.View.of(enregistre);
    }

    public DerogationDto.View update(UUID id, DerogationDto.SaveCommand commande) {
        Derogation d = charger(id);
        d.edit(actors.requireAccountId(), identification(commande), nature(commande),
                lignes(commande), commande.justification(), analysis(commande), clock.instant());
        Derogation enregistre = repository.save(d);
        audit.record(enregistre, DerogationAuditPort.Action.UPDATED);
        return DerogationDto.View.of(enregistre);
    }

    /**
     * Demander la validation du manager.
     *
     * <p>Le courriel au manager est un <b>complément</b> : s'il échoue, la
     * demande tient et l'alerte rouge est déjà posée. Perdre la demande parce
     * qu'un relais SMTP tousse serait pire que de ne pas prévenir par courriel.
     */
    public DerogationDto.View requestApproval(UUID id,
                                              DerogationDto.RequestApprovalCommand commande) {
        Derogation d = charger(id);
        UUID manager = commande.managerAccountId();
        // Un compte que l'annuaire du client ne connait pas laisserait un dossier
        // que PERSONNE ne peut signer : l'agregat n'accepte que le manager
        // designe, et ce compte-la n'existe pas pour se presenter. On refuse
        // maintenant, pendant que l'initiateur a la main.
        String nom = directory.displayNameOf(manager).orElseThrow(() ->
                new DerogationValidationException(
                        "Ce compte ne figure pas dans l'annuaire du client :"
                                + " choisissez un manager existant."));
        d.requestApproval(actors.requireAccountId(), manager, nom, clock.instant());
        Derogation enregistre = repository.save(d);
        audit.record(enregistre, DerogationAuditPort.Action.APPROVAL_REQUESTED);
        notifications.approvalRequested(enregistre);
        mail.notifyManager(manager, enregistre.getReference(),
                links.linkTo(enregistre.getId()));
        return DerogationDto.View.of(enregistre);
    }

    public DerogationDto.View withdraw(UUID id) {
        Derogation d = charger(id);
        d.withdraw(actors.requireAccountId(), clock.instant());
        Derogation enregistre = repository.save(d);
        audit.record(enregistre, DerogationAuditPort.Action.WITHDRAWN);
        return DerogationDto.View.of(enregistre);
    }

    public DerogationDto.View sendBack(UUID id, DerogationDto.SendBackCommand commande) {
        Derogation d = charger(id);
        d.sendBack(actors.requireAccountId(), commande.reason(), clock.instant());
        Derogation enregistre = repository.save(d);
        audit.record(enregistre, DerogationAuditPort.Action.SENT_BACK);
        notifications.sentBack(enregistre);
        return DerogationDto.View.of(enregistre);
    }

    public DerogationDto.View approve(UUID id, DerogationDto.ApproveCommand commande) {
        Derogation d = charger(id);
        d.approve(actors.requireAccountId(), commande.signedByName(),
                SignatureTrace.of(commande.signaturePng()), clock.instant());
        Derogation enregistre = repository.save(d);
        audit.record(enregistre, DerogationAuditPort.Action.APPROVED);
        notifications.approved(enregistre);
        return DerogationDto.View.of(enregistre);
    }

    /**
     * « Rouvrir (annule la signature) ».
     *
     * <p>Aucune garde d'acteur : le bouton est visible de tous dans la maquette
     * et aucun droit ne lui est attribué. La garde de <b>visibilité</b>, elle,
     * s'applique — on ne rouvre pas un dossier qu'on ne peut même pas lire, et
     * sans elle un identifiant deviné suffirait à effacer la signature d'un
     * dossier d'une autre équipe.
     */
    public DerogationDto.View reopen(UUID id, boolean voitTout) {
        Derogation d = exigerVisible(charger(id), id, voitTout);
        d.reopen(actors.requireAccountId(), clock.instant());
        Derogation enregistre = repository.save(d);
        audit.record(enregistre, DerogationAuditPort.Action.REOPENED);
        return DerogationDto.View.of(enregistre);
    }

    /**
     * « Validation client » : le courriel part, la dérogation en pièce jointe.
     *
     * <p><b>L'ordre compte, et il est ici.</b> On inscrit d'abord le dossier
     * comme parti, on l'enregistre — ce qui fait échouer tout de suite un geste
     * concurrent, une réouverture par exemple — et on envoie <b>ensuite</b>.
     * L'inverse laisserait un client détenir un document signé pour un dossier
     * que la plateforme croit encore interne, et personne ne le relancerait.
     *
     * <p>Si l'envoi échoue, l'exception remonte et la transaction du contrôleur
     * défait l'enregistrement : le dossier reste signé, et renvoyable.
     */
    public DerogationDto.View sendToCustomer(UUID id,
                                             DerogationDto.SendToCustomerCommand commande) {
        UUID tenantId = tenants.requireTenantId();
        Derogation d = charger(id);
        DerogationLanguage langue = commande.language() == null
                ? DerogationLanguage.FR : commande.language();
        d.markSentToCustomer(actors.requireAccountId(), commande.customerEmail(),
                commande.comment(), langue, clock.instant());
        Derogation enregistre = repository.save(d);

        byte[] document = pdf.render(enregistre, langue);
        mail.sendToCustomer(new DerogationMailPort.CustomerMail(
                tenantId, enregistre.getDispatch().customerEmail(),
                enregistre.getCreatedByUserId(),
                sujet(enregistre, langue), corps(enregistre, langue),
                enregistre.getReference() + ".pdf", document));

        audit.record(enregistre, DerogationAuditPort.Action.SENT_TO_CUSTOMER);
        return DerogationDto.View.of(enregistre);
    }

    public DerogationDto.View recordCustomerDecision(
            UUID id, DerogationDto.CustomerDecisionCommand commande) {
        Derogation d = charger(id);
        d.recordCustomerAnswer(actors.requireAccountId(), commande.decision(),
                commande.decidedOn(), clock.instant());
        Derogation enregistre = repository.save(d);
        audit.record(enregistre, DerogationAuditPort.Action.CUSTOMER_DECIDED);
        return DerogationDto.View.of(enregistre);
    }

    // ---- outils --------------------------------------------------------------

    private Derogation charger(UUID id) {
        return repository.findById(tenants.requireTenantId(), id)
                .orElseThrow(() -> new DerogationNotFoundException(id));
    }

    /**
     * {@code DER-<année>-<4 chiffres>}, la première libre.
     *
     * <p>Le compte donne le rang attendu ; la boucle le décale si la place est
     * déjà prise — un dossier supprimé ou une écriture concurrente laissent des
     * trous, et la contrainte d'unicité est la seule autorité.
     */
    private String referenceLibre(UUID tenantId) {
        String prefixe = "DER-" + LocalDate.now(clock.withZone(ZoneOffset.UTC)).getYear() + "-";
        long rang = repository.countByReferencePrefix(tenantId, prefixe) + 1;
        String reference;
        do {
            reference = prefixe + String.format("%04d", rang);
            rang++;
        } while (repository.existsByReference(tenantId, reference));
        return reference;
    }

    /**
     * La fiche telle que la saisie la décrit, sans rien y ajouter.
     *
     * <p>La date de demande absente n'est PAS complétée ici : c'est l'agrégat qui
     * la garantit, parce qu'un second appelant contournerait un repli posé dans
     * ce service (§18.3 #1), et que la colonne est {@code NOT NULL}.
     */
    private static Identification identification(DerogationDto.SaveCommand c) {
        return new Identification(c.customerName(), c.customerQualityContact(),
                c.customerOrderNo(), c.partReference(), c.partIndex(), c.batchNumber(),
                c.quantity(), c.requestDate());
    }

    private static Nature nature(DerogationDto.SaveCommand c) {
        return new Nature(c.shipmentStage(), c.deviationOrigin(), c.occurrence(),
                c.durationKind(), c.durationDetail());
    }

    private static Analysis analysis(DerogationDto.SaveCommand c) {
        return new Analysis(c.rootCause(), c.containment(), c.correctiveAction(),
                c.capaReference(), c.attachmentsNote());
    }

    private static List<DerogationLine> lignes(DerogationDto.SaveCommand c) {
        if (c.lines() == null) {
            return List.of();
        }
        // Le rang definitif est refait par l'agregat : celui pose ici n'est que
        // l'ordre de reception.
        int[] rang = {1};
        return c.lines().stream()
                .map(l -> new DerogationLine(rang[0]++, l.specification(), l.actualValue(),
                        l.nonConformingQty()))
                .toList();
    }

    /**
     * Le sujet du courriel : la référence, parce que c'est elle que le client
     * citera en répondant.
     */
    private static String sujet(Derogation d, DerogationLanguage langue) {
        String piece = d.getIdentification() == null ? null
                : d.getIdentification().partReference();
        String queue = piece == null ? "" : " — " + piece;
        return langue == DerogationLanguage.EN
                ? "Concession request " + d.getReference() + queue
                : "Demande de dérogation " + d.getReference() + queue;
    }

    /**
     * Le corps : un simple courriel. Le commentaire de l'initiateur, s'il y en a
     * un, et le renvoi à la pièce jointe.
     */
    private static String corps(Derogation d, DerogationLanguage langue) {
        String commentaire = d.getDispatch().comment();
        StringBuilder texte = new StringBuilder();
        if (langue == DerogationLanguage.EN) {
            texte.append("Dear Sir or Madam,\n\n")
                    .append("Please find attached our concession request ")
                    .append(d.getReference()).append(".\n");
        } else {
            texte.append("Madame, Monsieur,\n\n")
                    .append("Veuillez trouver en pièce jointe notre demande de dérogation ")
                    .append(d.getReference()).append(".\n");
        }
        if (commentaire != null) {
            texte.append('\n').append(commentaire).append('\n');
        }
        texte.append(langue == DerogationLanguage.EN
                ? "\nKind regards,\n" : "\nCordialement,\n");
        String nom = d.getCreatedByName();
        if (nom != null) {
            texte.append(nom).append('\n');
        }
        return texte.toString();
    }
}
