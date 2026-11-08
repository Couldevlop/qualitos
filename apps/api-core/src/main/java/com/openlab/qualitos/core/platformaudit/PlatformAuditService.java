package com.openlab.qualitos.core.platformaudit;

import com.openlab.qualitos.core.common.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La piste d'audit de la plateforme : écriture, consultation, vérification,
 * export scellé.
 *
 * <p><b>Ce qu'elle garantit.</b>
 * <ul>
 *   <li>Un numéro de séquence strictement croissant, sans trou, réservé sous
 *       verrou — un acte manquant se voit donc, même si la ligne a disparu.</li>
 *   <li>Une chaîne d'empreintes SHA-256 : chaque acte scelle le précédent, si
 *       bien qu'en modifier un invalide tous les suivants.</li>
 *   <li>Une attribution que l'appelant ne choisit pas : l'acteur vient du
 *       jeton, l'origine du contexte de requête, l'horodatage de l'horloge
 *       serveur. Aucun des trois n'est un paramètre.</li>
 *   <li>Rien d'effaçable : la V12 pose un déclencheur qui refuse {@code UPDATE}
 *       et {@code DELETE} jusque pour le propriétaire de la base.</li>
 * </ul>
 *
 * <p><b>Ce qu'elle ne garantit pas, et qu'il faut dire.</b> La chaîne prouve
 * qu'un journal n'a pas été retouché ; elle ne prouve pas qu'il est complet,
 * puisqu'un acte jamais consigné ne laisse aucun vide. La complétude ne tient
 * qu'au branchement du journal dans chaque acte sensible — c'est une
 * discipline de code, revue par {@code PlatformAuditCoversSensitiveActsTest},
 * pas une propriété cryptographique.
 *
 * <p><b>Pourquoi l'écriture partage la transaction de l'acte.</b> Un journal
 * écrit dans une transaction séparée consignerait des actes avortés : une
 * création de client annulée par une erreur en fin de transaction laisserait
 * un « client créé » que rien ne contredit. Le journal consigne ce qui s'est
 * PRODUIT, donc il tombe avec l'acte. Le revers est accepté : un acte dont la
 * consignation échoue est lui-même annulé — on préfère refuser l'acte que de
 * l'exécuter sans trace.
 */
@Service
@Transactional(readOnly = true)
public class PlatformAuditService {

    private final PlatformAuditEventRepository events;
    private final PlatformAuditCounterRepository counter;
    private final Clock clock;

    public PlatformAuditService(PlatformAuditEventRepository events,
                                PlatformAuditCounterRepository counter,
                                Clock clock) {
        this.events = events;
        this.counter = counter;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- écriture

    /**
     * Consigne un acte de plateforme.
     *
     * @param action         l'acte, pris dans un vocabulaire fermé
     * @param targetType     la nature de l'objet visé (« tenant », « subscription »…)
     * @param targetId       l'identifiant de l'objet visé, s'il en a un
     * @param targetTenantId le client concerné, pour que « tout ce qui a touché
     *                       ce client » reste une requête indexée
     * @param summary        une phrase lisible par un auditeur, pas un code
     * @param payloadJson    le détail de l'acte, ou {@code null}. JAMAIS de
     *                       donnée personnelle ni de secret : ce journal est
     *                       destiné à sortir de la plateforme (§22 règle 9).
     */
    @Transactional
    public PlatformAuditEvent record(PlatformAuditAction action,
                                     String targetType,
                                     UUID targetId,
                                     UUID targetTenantId,
                                     String summary,
                                     String payloadJson) {
        Instant maintenant = horodatageStockable(Instant.now(clock));

        PlatformAuditCounter compteur = counter.lock().orElseThrow(() ->
                new PlatformAuditStateException(
                        "Le compteur de la piste d'audit de plateforme est absent : la migration V12 "
                                + "n'a pas ete jouee. Aucun acte ne peut etre consigne, donc aucun "
                                + "acte sensible ne doit etre execute."));
        long sequence = compteur.reserveNext();

        String empreintePrecedente = events.findTopByOrderBySequenceNoDesc()
                .map(PlatformAuditEvent::getIntegrityHash)
                .orElse(null);

        Optional<UUID> acteur = CurrentUser.userId();
        PlatformAuditOrigin origine = PlatformAuditOrigin.courante();

        PlatformAuditEvent evenement = new PlatformAuditEvent(
                UUID.randomUUID(),
                sequence,
                maintenant,
                maintenant,
                // Pas d'acteur exploitable : l'acte est celui de la machine, et
                // le journal le dit. Lui attribuer un utilisateur par defaut
                // serait la seule ligne de ce journal qui mente.
                acteur.isPresent() ? PlatformActorType.USER : PlatformActorType.SYSTEM,
                acteur.orElse(null),
                action,
                targetType,
                targetId,
                targetTenantId,
                summary,
                payloadJson,
                origine.adresse(),
                origine.agent(),
                empreintePrecedente);
        evenement.seal(PlatformAuditHasher.hash(evenement));
        return events.save(evenement);
    }

    /**
     * L'horodatage entre dans l'empreinte : il ne peut donc porter que ce que la
     * base rend à l'identique.
     *
     * <p>Un {@link Instant} Java porte la nanoseconde, une colonne
     * {@code TIMESTAMP WITH TIME ZONE} de PostgreSQL la microseconde — et elle
     * ARRONDIT ({@code .123456789} devient {@code .123457}). Sans cette
     * troncature avant calcul, aucune empreinte n'est recalculable après
     * relecture et la vérification annonce une falsification sur un journal
     * parfaitement intact. Un registre qui s'accuse lui-même ruine la seule
     * mécanique censée fonder la confiance. Le défaut a déjà été payé une fois
     * côté moteur de qualité (voir {@code StorableInstant}) ; il ne se
     * reproduit pas ici.
     */
    private static Instant horodatageStockable(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    // ------------------------------------------------------------ consultation

    @Transactional(readOnly = true)
    public Page<PlatformAuditDto.Event> list(PlatformAuditAction action,
                                             UUID targetTenantId,
                                             UUID actorUserId,
                                             Instant from,
                                             Instant to,
                                             Pageable pageable) {
        Page<PlatformAuditEvent> page;
        if (targetTenantId != null) {
            page = events.findByTargetTenantIdOrderBySequenceNoDesc(targetTenantId, pageable);
        } else if (action != null) {
            page = events.findByActionOrderBySequenceNoDesc(action, pageable);
        } else if (actorUserId != null) {
            page = events.findByActorUserIdOrderBySequenceNoDesc(actorUserId, pageable);
        } else if (from != null && to != null) {
            page = events.findByOccurredAtBetweenOrderBySequenceNoDesc(from, to, pageable);
        } else {
            page = events.findAllByOrderBySequenceNoDesc(pageable);
        }
        return page.map(PlatformAuditDto.Event::from);
    }

    /** Le dernier numéro attribué, pour borner une vérification ou un export. */
    @Transactional(readOnly = true)
    public long derniereSequence() {
        return events.findTopByOrderBySequenceNoDesc()
                .map(PlatformAuditEvent::getSequenceNo)
                .orElse(0L);
    }

    // ----------------------------------------------------------- vérification

    /**
     * Vérifie une fenêtre fermée {@code [fromSeq, toSeq]} : séquence contiguë,
     * empreinte de chaque acte recalculable, et chaînage sur le précédent.
     *
     * <p>Les trois contrôles sont distincts et c'est volontaire. Recalculer
     * l'empreinte attrape la ligne retouchée ; vérifier le chaînage attrape la
     * ligne retouchée AVEC son empreinte ; compter la séquence attrape la ligne
     * supprimée, que ni l'une ni l'autre ne verrait — un journal amputé de son
     * dernier acte reste parfaitement chaîné.
     */
    @Transactional(readOnly = true)
    public PlatformAuditDto.Verification verifyChain(long fromSeq, long toSeq) {
        if (fromSeq < 1) {
            throw new PlatformAuditStateException("La sequence commence a 1, pas a " + fromSeq + ".");
        }
        if (toSeq < fromSeq) {
            throw new PlatformAuditStateException(
                    "Fenetre vide : toSeq (" + toSeq + ") est avant fromSeq (" + fromSeq + ").");
        }
        List<PlatformAuditEvent> fenetre = events.findBySequenceNoBetweenOrderBySequenceNoAsc(fromSeq, toSeq);
        List<PlatformAuditDto.Break> ruptures = new ArrayList<>();

        long sequenceAttendue = fromSeq;
        String empreinteAttendue = fromSeq == 1
                ? null
                // Pour valider le chaînage du PREMIER acte de la fenêtre, il faut
                // l'empreinte de celui qui le précède — hors fenêtre.
                : events.findBySequenceNoBetweenOrderBySequenceNoAsc(fromSeq - 1, fromSeq - 1)
                        .stream().findFirst()
                        .map(PlatformAuditEvent::getIntegrityHash)
                        .orElse(null);

        for (PlatformAuditEvent acte : fenetre) {
            if (acte.getSequenceNo() != sequenceAttendue) {
                ruptures.add(new PlatformAuditDto.Break(acte.getId(), acte.getSequenceNo(),
                        "Trou de sequence : " + sequenceAttendue + " attendu"));
                // On se recale pour continuer l'analyse : un seul trou ne doit pas
                // faire déclarer tout le reste rompu.
                sequenceAttendue = acte.getSequenceNo();
            }
            if (!egales(acte.getPreviousHash(), empreinteAttendue)) {
                ruptures.add(new PlatformAuditDto.Break(acte.getId(), acte.getSequenceNo(),
                        "Chainage rompu : l'empreinte precedente ne correspond pas"));
            }
            if (!PlatformAuditHasher.hash(acte).equals(acte.getIntegrityHash())) {
                ruptures.add(new PlatformAuditDto.Break(acte.getId(), acte.getSequenceNo(),
                        "Empreinte invalide : l'acte a ete modifie"));
            }
            empreinteAttendue = acte.getIntegrityHash();
            sequenceAttendue++;
        }
        long attendus = toSeq - fromSeq + 1;
        if (fenetre.size() < attendus) {
            ruptures.add(new PlatformAuditDto.Break(null, sequenceAttendue,
                    "Acte(s) manquant(s) jusqu'au numero " + toSeq));
        }
        return new PlatformAuditDto.Verification(
                fromSeq, toSeq, fenetre.size(), ruptures.isEmpty(), ruptures);
    }

    // ------------------------------------------------------------------ export

    /**
     * Le dossier qu'on remet à une autorité : la fenêtre d'actes, son
     * verdict de vérification, et un sceau.
     *
     * <p><b>À quoi sert le sceau.</b> Chaque acte porte déjà son empreinte,
     * mais un extrait peut être amputé de ses dernières lignes sans qu'aucune
     * empreinte ne cesse de concorder. Le sceau est l'empreinte de la
     * CONCATÉNATION des empreintes, bornes de la fenêtre comprises : il fixe
     * l'extrait comme un tout. Deux copies d'un même dossier qui portent le
     * même sceau sont identiques jusqu'à la dernière ligne.
     *
     * <p>Le sceau n'est pas une signature : il atteste l'intégrité, pas
     * l'origine. Quiconque détient le dossier peut le recalculer — c'est
     * justement ce qui le rend vérifiable sans nous. Lier le dossier à
     * l'éditeur demanderait une signature ML-DSA et un ancrage, ce que le
     * service de preuve du moteur de qualité sait faire ; le brancher ici est
     * un chantier à part, pas un manque de ce dossier.
     *
     * <p>L'export est lui-même consigné ({@link PlatformAuditAction#AUDIT_EXPORTED}) :
     * le seul geste qui fait sortir le journal de la plateforme serait, sinon,
     * le seul qu'elle ne raconte pas.
     */
    @Transactional
    public PlatformAuditDto.Export export(long fromSeq, long toSeq) {
        PlatformAuditDto.Verification verdict = verifyChain(fromSeq, toSeq);
        List<PlatformAuditEvent> fenetre =
                events.findBySequenceNoBetweenOrderBySequenceNoAsc(fromSeq, toSeq);
        Instant genere = horodatageStockable(Instant.now(clock));

        StringBuilder aSceller = new StringBuilder(64 * fenetre.size() + 64);
        aSceller.append(fromSeq).append('\u001E').append(toSeq).append('\u001E').append(genere);
        for (PlatformAuditEvent acte : fenetre) {
            aSceller.append('\u001E').append(acte.getIntegrityHash());
        }
        String sceau = PlatformAuditHasher.sha256(aSceller.toString());

        record(PlatformAuditAction.AUDIT_EXPORTED, "platform_audit", null, null,
                "Dossier d'audit exporte : actes " + fromSeq + " a " + toSeq
                        + " (" + fenetre.size() + " lignes)",
                "{\"fromSeq\":" + fromSeq + ",\"toSeq\":" + toSeq
                        + ",\"count\":" + fenetre.size() + ",\"seal\":\"" + sceau + "\"}");

        return new PlatformAuditDto.Export(
                genere, fromSeq, toSeq, fenetre.size(), sceau, verdict,
                fenetre.stream().map(PlatformAuditDto.Event::from).toList());
    }

    private static boolean egales(String gauche, String droite) {
        return gauche == null ? droite == null : gauche.equals(droite);
    }
}
