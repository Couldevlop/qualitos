package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.capa.effectiveness.application.EffectivenessReviewPort;
import com.openlab.qualitos.quality.capa.effectiveness.domain.CaseSettlement;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.nonconformity.NcOrigin;
import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.UUID;

@Service
@Transactional
public class CapaService {

    private final CapaCaseRepository caseRepository;
    private final CapaActionRepository actionRepository;
    /**
     * Un seul collaborateur pour tout ce qui se consigne : le service décrit ce
     * qui arrive au dossier, sans connaître ni le journal d'audit ni les abonnés.
     */
    private final CapaLifecycleJournal journal;
    /**
     * Lecture seule des non-conformités, pour le verrou de clôture : un dossier
     * ne se referme pas au-dessus d'un écart encore ouvert.
     */
    private final NonConformityRepository ncRepository;
    /**
     * Lecture seule des preuves : uniquement pour savoir si une action en porte
     * une avant de la supprimer. Le dépôt lui-même reste l'affaire de
     * {@link CapaEvidenceService} — ce service-ci ne connaît pas le stockage
     * objet et n'a pas à le connaître.
     */
    private final CapaEvidenceRepository evidenceRepository;
    /** Sans « voir tous les dossiers », on ne voit que ceux qui nous concernent (ADR 0081). */
    private final RecordScope scope;
    /**
     * L'historique des évaluations d'efficacité (ADR 0108).
     *
     * <p>Écriture seule depuis ici, et à travers un port : le dossier CAPA
     * détient la machine à états, le contexte d'efficacité détient la trace du
     * jugement. Deux endroits capables de changer le statut d'un dossier
     * finiraient par en changer différemment.
     */
    private final EffectivenessReviewPort effectivenessReviews;
    /**
     * Ce que vaut le retour sous la cible, pour un dossier né d'un écart
     * d'objectif qualité. Interrogé avant tout verdict qui clôt (ADR 0133).
     */
    private final ObjectiveRecoveryPort objectiveRecovery;

    public CapaService(CapaCaseRepository caseRepository, CapaActionRepository actionRepository,
                       CapaLifecycleJournal journal,
                       NonConformityRepository ncRepository,
                       CapaEvidenceRepository evidenceRepository,
                       RecordScope scope,
                       EffectivenessReviewPort effectivenessReviews,
                       ObjectiveRecoveryPort objectiveRecovery) {
        this.caseRepository = caseRepository;
        this.actionRepository = actionRepository;
        this.journal = journal;
        this.ncRepository = ncRepository;
        this.evidenceRepository = evidenceRepository;
        this.scope = scope;
        this.effectivenessReviews = effectivenessReviews;
        this.objectiveRecovery = objectiveRecovery;
    }

    /**
     * Statuts où une non-conformité est refermée. CANCELLED en fait partie : un
     * écart annulé n'attend plus rien, contrairement à un écart en cours.
     */
    private static final List<NcStatus> NC_SETTLED_STATUSES =
            List.of(NcStatus.RESOLVED, NcStatus.CLOSED, NcStatus.CANCELLED);

    /**
     * Statuts où un dossier CAPA ne demande plus rien : clos, ou écarté.
     * Tout le reste — ouvert, en cours, résolu — est du travail en attente.
     * Résolu EN FAIT PARTIE du travail en attente : il reste à juger de
     * l'efficacité, et c'est ce jugement qui clôt (ADR 0108).
     */
    private static final List<CapaStatus> CAPA_SETTLED_STATUSES =
            List.of(CapaStatus.CLOSED, CapaStatus.REJECTED);

    @Transactional(readOnly = true)
    public Page<CapaDto.CaseResponse> findAll(CapaStatus status, Pageable pageable) {
        return findAll(status, null, null, pageable);
    }

    /** Le registre filtré par statut et par criticité, sans filtre de pilote. */
    @Transactional(readOnly = true)
    public Page<CapaDto.CaseResponse> findAll(CapaStatus status, CapaCriticity criticity,
                                              Pageable pageable) {
        return findAll(status, criticity, null, pageable);
    }

    /**
     * Le registre filtré par statut, criticité et pilote, en base : la
     * pagination reste juste.
     *
     * <p>Le pilote est un filtre comme les autres, et non une portée : il
     * restreint ce qu'on REGARDE, dans ce qu'on a déjà le droit de voir
     * (ADR 0081). Un lecteur restreint à ses propres dossiers qui filtre sur
     * quelqu'un d'autre obtient donc une liste vide, et c'est juste.
     */
    @Transactional(readOnly = true)
    public Page<CapaDto.CaseResponse> findAll(CapaStatus status, CapaCriticity criticity,
                                              UUID ownerId, Pageable pageable) {
        return findAll(status, criticity, ownerId, null, pageable);
    }

    /**
     * Le registre filtré par statut, criticité, pilote et échéance limite.
     *
     * <p>{@code dueBefore} retient les dossiers à échoir à cette date ou avant,
     * et écarte ceux qui n'ont pas d'échéance : on demande « ce qui tombe avant
     * le 15 », et un dossier qui ne tombe jamais n'y répond pas.
     */
    @Transactional(readOnly = true)
    public Page<CapaDto.CaseResponse> findAll(CapaStatus status, CapaCriticity criticity,
                                              UUID ownerId, LocalDate dueBefore,
                                              Pageable pageable) {
        UUID tenantId = requireTenantId();
        Optional<UUID> seulement = CapaScope.restriction(scope);
        if (seulement.isPresent()) {
            return caseRepository
                    .findConcerning(tenantId, status, criticity, ownerId, dueBefore,
                            seulement.get(), pageable)
                    .map(c -> toResponse(c, false));
        }
        Page<CapaCase> page;
        if (criticity != null || ownerId != null || dueBefore != null) {
            page = caseRepository.search(tenantId, status, criticity, ownerId, dueBefore, pageable);
        } else {
            page = status != null
                    ? caseRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                    : caseRepository.findByTenantId(tenantId, pageable);
        }
        return page.map(c -> toResponse(c, false));
    }

    @Transactional(readOnly = true)
    public CapaDto.CaseResponse findById(UUID id) {
        return toResponse(loadCase(id));
    }

    /**
     * Le statut d'un dossier, pour un INVARIANT d'un autre registre.
     *
     * <p>Le registre des écarts en a besoin : une non-conformité ne se clôt pas
     * tant que le dossier qui la traite est encore en cours. Il lui faut donc le
     * statut, et rien d'autre — pas la fiche, qui porte le pilote, les pièces et
     * l'historique.
     *
     * <p>Sans filtre de portée (ADR 0081), à la différence de {@link #findById} :
     * ici personne ne LIT le dossier, c'est le domaine qui vérifie une règle.
     * Filtrer ferait dépendre la clôture d'un écart du périmètre de celui qui
     * clôt — deux personnes obtiendraient deux réponses sur le même fait.
     *
     * @return le statut, ou vide si le dossier n'existe plus dans ce client
     */
    @Transactional(readOnly = true)
    public Optional<CapaStatus> statusOf(UUID caseId) {
        return caseRepository.findByIdAndTenantId(caseId, requireTenantId())
                .map(CapaCase::getStatus);
    }

    /**
     * Les dossiers CAPA encore en travail qui traitent cet écart.
     *
     * <p>Un écart n'a pas UN dossier, il en a autant qu'on en a ouvert sur lui :
     * celui de l'escalade, qui laisse sa trace dans {@code capaCaseId}, et ceux
     * qu'on crée depuis le registre des CAPA en citant la référence de l'écart
     * comme origine. Les seconds étaient invisibles au moment de clore, et on
     * refermait l'écart au-dessus d'un traitement en cours.
     *
     * <p>Les deux natures d'origine sont interrogées : un constat interne et une
     * réclamation client portent la même référence d'écart, pas le même type.
     *
     * <p>Sans filtre de portée (ADR 0081), pour la même raison que
     * {@link #statusOf} : c'est une règle du domaine, pas une lecture. Si elle
     * dépendait du périmètre de celui qui clôt, deux personnes obtiendraient
     * deux réponses sur le même fait.
     *
     * @param escaladeId  le dossier né de l'escalade, s'il y en a un
     * @param ncReference la référence de l'écart, telle qu'elle s'affiche
     * @return les titres des dossiers qui ne sont ni clos ni écartés, sans
     *         doublon et dans l'ordre d'ouverture
     */
    @Transactional(readOnly = true)
    public List<String> unsettledCasesFor(UUID escaladeId, String ncReference) {
        return casesFor(escaladeId, ncReference,
                statut -> !CAPA_SETTLED_STATUSES.contains(statut));
    }

    /**
     * Les dossiers de l'écart dont les actions ne sont pas encore exécutées :
     * ceux qui restent {@code OPEN} ou {@code IN_PROGRESS}.
     *
     * <p>C'est la condition pour <b>résoudre</b> l'écart, plus souple que celle
     * de la clôture. Résoudre, c'est dire « ce qui devait être fait l'a été » ;
     * clore, c'est dire « et ça a marché ». Un dossier {@code RESOLVED} a donc
     * sa place sous un écart résolu — ses actions sont exécutées, il reste à
     * juger de leur efficacité (ADR 0108) — mais pas sous un écart clos.
     *
     * <p>Sans cette garde, un écart passait « résolu » pendant que son dossier
     * affichait « en cours » : la fiche de l'écart disait le travail fait,
     * celle du dossier disait le contraire, et les deux étaient lues par les
     * mêmes personnes.
     */
    @Transactional(readOnly = true)
    public List<String> unexecutedCasesFor(UUID escaladeId, String ncReference) {
        return casesFor(escaladeId, ncReference,
                statut -> statut == CapaStatus.OPEN || statut == CapaStatus.IN_PROGRESS);
    }

    /**
     * COMBIEN d'actions retiennent encore cet ecart, tous dossiers confondus.
     *
     * <p>Un DECOMPTE, et aucun nom : l'invariant porte sur tous les dossiers, y
     * compris ceux que le lecteur ne voit pas (ADR 0081) -- sinon la resolution
     * dependrait de qui la demande -- et nommer ici divulguerait un dossier
     * hors de portee par un message d'erreur.
     *
     * <p>SEULES LES ACTIONS QUI RESTENT A EXECUTER, c'est-a-dire celles qui ne
     * sont pas {@code DONE}. L'ADR 0132 borne son verrou a la CLOTURE et l'ecrit
     * noir sur blanc : « la resolution n'est pas concernee » — une verification
     * d'efficacite se mesure sur des semaines, et l'exiger avant la resolution
     * bloquerait le dossier a l'etape d'avant.
     *
     * <p>Ce decompte reutilisait pourtant le predicat de la cloture
     * ({@link #actionsQuiBloquent}), qui compte aussi les actions FAITES dont la
     * verification promise n'a pas conclu. Une action terminee depuis le 8D
     * retenait donc la resolution de l'ecart, sous un motif qui demandait de la
     * TERMINER alors qu'elle l'etait. Pour avancer, il fallait rendre tout de
     * suite le verdict d'efficacite de cette action — celui-la meme que l'ADR
     * 0132 range dans la CLOTURE, et qu'on ne mesure pas en un clic. Ce qui
     * reste a verifier se dit donc la ou c'est vrai
     * ({@link ClosureBlockerCode#ACTION_VERIFICATIONS_PENDING}), pas ici.
     *
     * <p>Pourquoi des actions et plus des dossiers : « 2 dossiers CAPA ont des
     * actions a mener » obligeait a les ouvrir un a un pour savoir combien de
     * gestes restaient, alors que le serveur tient le compte.
     */
    @Transactional(readOnly = true)
    public int unexecutedActionCountFor(UUID escaladeId, String ncReference) {
        return dossiersDe(escaladeId, ncReference).stream()
                .mapToInt(c -> actionsNonExecutees(c).size())
                .sum();
    }

    /**
     * Parmi les dossiers qui retiennent la RESOLUTION de l'ecart, les seuls que
     * ce lecteur a le droit de nommer.
     *
     * <p>Le pendant SOUS PORTEE de {@link #unexecutedCasesFor}, et la raison
     * d'etre des deux : le DECOMPTE doit porter sur tous les dossiers -- sinon
     * la resolution dependrait de qui regarde -- mais les NOMS, non. Un refus
     * qui enumerait les titres hors portee les divulguait par un message
     * d'erreur, alors que la fiche du dossier les refuse a ce meme lecteur
     * (ADR 0081).
     */
    @Transactional(readOnly = true)
    public List<String> unexecutedCaseNamesInScopeFor(UUID escaladeId, String ncReference) {
        return casesInScopeFor(escaladeId, ncReference,
                statut -> statut == CapaStatus.OPEN || statut == CapaStatus.IN_PROGRESS);
    }

    /**
     * Parmi les dossiers qui retiennent la CLOTURE de l'ecart, les seuls que ce
     * lecteur a le droit de nommer. Le pendant sous portee de
     * {@link #unsettledCasesFor}.
     */
    @Transactional(readOnly = true)
    public List<String> unsettledCaseNamesInScopeFor(UUID escaladeId, String ncReference) {
        return casesInScopeFor(escaladeId, ncReference,
                statut -> !CAPA_SETTLED_STATUSES.contains(statut));
    }

    /**
     * Comme {@link #casesFor}, mais en ne nommant que ce que ce lecteur voit.
     *
     * <p>Rend une liste VIDE pour un lecteur qui ne voit aucun des dossiers en
     * cause : le refus se contente alors de son decompte, qui lui est juste.
     */
    private List<String> casesInScopeFor(UUID escaladeId, String ncReference,
                                         java.util.function.Predicate<CapaStatus> retenu) {
        Optional<UUID> seulement = CapaScope.restriction(scope);
        return dossiersDe(escaladeId, ncReference).stream()
                .filter(c -> retenu.test(c.getStatus()))
                .filter(c -> CapaScope.sees(seulement, c))
                .map(CapaCase::getTitle)
                .toList();
    }

    /**
     * Les dossiers d'un écart dont le statut remplit la condition, sans doublon
     * et dans l'ordre d'ouverture, nommés par leur titre.
     *
     * <p>Deux chemins de rattachement, et il faut les deux : le lien posé à
     * l'escalade ({@code escaladeId}) et la référence de l'écart citée par un
     * dossier ouvert depuis la liste. Les deux natures d'origine sont
     * interrogées — un constat interne et une réclamation client portent la même
     * référence d'écart, pas le même type de source.
     */
    private List<String> casesFor(UUID escaladeId, String ncReference,
                                  java.util.function.Predicate<CapaStatus> retenu) {
        return dossiersDe(escaladeId, ncReference).stream()
                .filter(c -> retenu.test(c.getStatus()))
                .map(CapaCase::getTitle)
                .toList();
    }

    /**
     * TOUS les dossiers de l'\u00e9cart, nomm\u00e9s, du plus ancien au plus r\u00e9cent.
     *
     * <p>C'est le m\u00eame ensemble que celui dont d\u00e9pend la cl\u00f4ture de l'\u00e9cart.
     * La fiche de l'\u00e9cart n'en montrait qu'UN \u2014 celui que porte son lien \u2014 :
     * les actions d'un second dossier retenaient donc sa cl\u00f4ture sans appara\u00eetre
     * nulle part, ni sur la fiche, ni dans le rapport 8D remis au client.
     */
    @Transactional(readOnly = true)
    public List<CapaDto.CaseRef> casesOf(UUID escaladeId, String ncReference) {
        // SOUS PORT\u00c9E (ADR 0081), parce que ceci NOMME les dossiers. Sans
        // \u00ab voir tous les dossiers \u00bb, on ne voit que ceux qui vous concernent :
        // rendre ici le titre d'un dossier hors de port\u00e9e le divulguerait par un
        // chemin d\u00e9tourn\u00e9 -- la fiche d'un \u00e9cart -- alors que sa propre fiche le
        // refuse.
        //
        // C'est la DIFF\u00c9RENCE avec {@link #casesFor} : un invariant doit compter
        // TOUS les dossiers, y compris ceux qu'on ne voit pas, sinon la cl\u00f4ture
        // d\u00e9pendrait de qui regarde. C'est pr\u00e9cis\u00e9ment pourquoi un motif de
        // blocage porte un D\u00c9COMPTE et jamais un titre.
        Optional<UUID> seulement = CapaScope.restriction(scope);
        return dossiersDe(escaladeId, ncReference).stream()
                .filter(c -> CapaScope.sees(seulement, c))
                .map(c -> new CapaDto.CaseRef(c.getId(), c.getTitle(), c.getStatus(),
                        c.getEffectivenessComment(), actionsQuiBloquent(c),
                        isContainmentOnly(c.getActions())))
                .toList();
    }

    /**
     * Les dossiers que l'\u00e9cart a engendr\u00e9s, d\u00e9doublonn\u00e9s et ordonn\u00e9s.
     *
     * <p>Deux chemins y m\u00e8nent, et il faut les deux : le lien port\u00e9 par la
     * fiche, et la R\u00c9F\u00c9RENCE cit\u00e9e en source par les dossiers ouverts depuis la
     * liste. Les deux natures d'origine sont interrog\u00e9es \u2014 un constat interne
     * et une r\u00e9clamation client portent la m\u00eame r\u00e9f\u00e9rence d'\u00e9cart, pas le m\u00eame
     * type de source.
     */
    /**
     * Un dossier au moins traite-t-il cet ecart ? HORS PORTEE, expres.
     *
     * <p>C'est un INVARIANT, pas un affichage : il compte TOUS les dossiers, y
     * compris ceux que le lecteur ne voit pas. {@link #casesOf} filtre par
     * portee parce qu'il NOMME les dossiers ; s'en servir ici aurait fait
     * dependre la resolution d'un ecart de qui regarde -- un lecteur restreint
     * a ses propres dossiers (ADR 0081) se serait vu refuser la resolution, et
     * annoncer qu'aucune action n'est inscrite, la ou un responsable qualite en
     * voyait trois.
     *
     * <p>Ne rend donc qu'un booleen : ni titre, ni identifiant. Meme regle que
     * {@link #unexecutedCasesFor}, dont le resultat ne sort qu'en decompte.
     */
    @Transactional(readOnly = true)
    public boolean hasAnyCaseFor(UUID escaladeId, String ncReference) {
        return !dossiersDe(escaladeId, ncReference).isEmpty();
    }

    /**
     * Les sources qui DESIGNENT UN ECART, et qu'il faut donc toutes regarder
     * pour retrouver les dossiers d'une non-conformite.
     *
     * <p>Trois valeurs pour deux choses. {@code EXTERNAL_NON_CONFORMITY} et
     * {@code COMPLAINT} nomment le meme registre -- celui des ecarts signales
     * du dehors -- et seule la seconde est proposee a la creation manuelle. Un
     * dossier ouvert depuis le registre CAPA portait donc une source que la
     * fiche de l'ecart ne regardait pas : il citait la bonne reference, et
     * restait pourtant invisible au D3, au D5, au D7 et dans le rapport 8D,
     * alors que ses actions retenaient la cloture.
     *
     * <p>Elargir ICI plutot que renommer l'enumeration : la reference reste la
     * condition, et une reclamation du registre des reclamations ne porte pas
     * la reference d'un ecart -- elle ne peut donc pas entrer par cette porte.
     */
    private static final List<CapaSourceType> SOURCES_D_UN_ECART = List.of(
            CapaSourceType.NON_CONFORMITY,
            CapaSourceType.EXTERNAL_NON_CONFORMITY,
            CapaSourceType.COMPLAINT);

    private List<CapaCase> dossiersDe(UUID escaladeId, String ncReference) {
        UUID tenantId = requireTenantId();
        Map<UUID, CapaCase> retenus = new LinkedHashMap<>();

        if (escaladeId != null) {
            caseRepository.findByIdAndTenantId(escaladeId, tenantId)
                    .ifPresent(c -> retenus.put(c.getId(), c));
        }

        if (ncReference != null && !ncReference.isBlank()) {
            String reference = ncReference.strip();
            for (CapaSourceType origine : SOURCES_D_UN_ECART) {
                for (CapaCase c : caseRepository
                        .findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                                tenantId, origine, reference)) {
                    retenus.putIfAbsent(c.getId(), c);
                }
            }
        }

        return List.copyOf(retenus.values());
    }

    public CapaDto.CaseResponse createCase(CapaDto.CreateCaseRequest request) {
        UUID tenantId = requireTenantId();
        CapaCase c = new CapaCase();
        c.setTenantId(tenantId);
        c.setTitle(request.title());
        c.setDescription(request.description());
        c.setType(request.type());
        c.setCriticity(request.criticity());
        c.setSourceType(request.sourceType());
        c.setSourceRef(request.sourceRef());
        c.setOwnerId(request.ownerId());
        c.setRootCauseId(request.rootCauseId());
        c.setDueDate(request.dueDate());
        c.setStatus(CapaStatus.OPEN);
        CapaCase saved = caseRepository.save(c);
        journal.record(saved, CapaTransition.OPENED);
        return toResponse(saved);
    }

    /**
     * Ouvre un dossier ET son action unique, dans la même transaction (ADR 0088).
     *
     * <p>Une CAPA porte une action : on ne crée plus un dossier vide à remplir
     * ensuite. L'intitulé, la nature, le responsable et l'échéance de l'action
     * sont ceux du dossier, et le responsable choisi dans l'annuaire en devient
     * le pilote. Un échec de la seconde écriture annule la première : il ne
     * reste jamais un dossier sans son action.
     *
     * <p>{@code sourceType} et {@code sourceRef} sont fixés par l'appelant
     * serveur — la fiche NC, le constat d'audit, le risque — et jamais lus dans
     * le corps quand le dossier s'ouvre depuis l'un d'eux. Le tenant vient du
     * jeton, comme partout.
     *
     * <p>L'endiguement est refusé comme <b>type de dossier</b> : un dossier dont
     * la finalité se limite à endiguer ne pourrait jamais être clos (ISO 9001
     * §10.2). Il reste permis comme <b>nature de la première action</b>
     * ({@code actionType}) — endiguer le jour même puis traiter la cause est
     * l'étape D3 de la 8D. Le dossier naît alors avec un motif de blocage
     * {@code CONTAINMENT_ONLY}, que la réponse d'ouverture porte (elle est
     * détaillée) : l'ouvrant voit donc immédiatement qu'il lui reste à ajouter
     * l'action corrective, plutôt que de le découvrir à la clôture.
     *
     * <p>Une échéance passée est refusée : le dossier naîtrait en retard.
     */
    public CapaDto.CaseResponse openCase(CapaDto.OpenCaseRequest request,
                                         CapaSourceType sourceType, String sourceRef) {
        if (request == null) {
            throw new CapaValidationException("The CAPA and its action must be described");
        }
        if (request.type() == null || request.type() == CapaType.CONTAINMENT) {
            throw new CapaValidationException(
                    "A CAPA case is corrective or preventive: a case whose very purpose is "
                    + "containment could never be closed. Containment is allowed as the nature "
                    + "of the action (actionType), not as the type of the case");
        }
        if (request.criticity() == null) {
            throw new CapaValidationException("Criticity is required");
        }
        if (request.ownerId() == null) {
            throw new CapaValidationException("The CAPA must be entrusted to someone");
        }
        if (request.dueDate() == null) {
            throw new CapaValidationException("A due date is required");
        }
        if (request.dueDate().isBefore(LocalDate.now())) {
            throw new CapaValidationException("The due date cannot be in the past");
        }
        if (sourceType == null) {
            throw new CapaValidationException("The CAPA source is required");
        }
        // L'ECART CITE, s'il existe : c'est lui qui dit la source et qui porte le
        // lien. Un dossier ouvert depuis la liste en tapant « Ext-NC-2026-0004 »
        // s'affichait « Non-conformite interne » — la liste ne propose que
        // celle-la — et restait orphelin : la fiche de l'ecart n'y menait pas,
        // son rapport 8D ne le voyait pas, et sa cloture ne l'attendait pas.
        // Deriver plutot que demander : l'origine est deja ecrite sur l'ecart.
        NonConformity ecart = ecartCite(sourceType, sourceRef);
        CapaSourceType source = ecart == null ? sourceType : sourceDe(ecart);
        String titre = requireUsableTitle(request.title());
        String description = nettoyer(request.description());
        String pilote = normalizeName(request.ownerName());

        CapaCase c = new CapaCase();
        c.setTenantId(requireTenantId());
        c.setTitle(titre);
        c.setDescription(description);
        c.setType(request.type());
        c.setCriticity(request.criticity());
        c.setSourceType(source);
        c.setSourceRef(nettoyer(sourceRef));
        c.setOwnerId(request.ownerId());
        c.setOwnerName(pilote);
        c.setDueDate(request.dueDate());
        c.setStatus(CapaStatus.OPEN);
        // Le critère d'efficacité part avec l'ouverture (ADR 0108) : c'est au
        // moment où l'on décide de l'action qu'on décide à quoi on reconnaîtra
        // qu'elle a marché. Écrit plus tard, il se choisit en regardant le
        // résultat — et ce n'est plus un critère.
        c.setEffectivenessCriterion(nettoyer(request.effectivenessCriterion()));
        // La décision de vérification, prise à l'ouverture : mêmes gardes que
        // la mise à jour, avant toute écriture — un refus ne laisse rien.
        appliquerVerification(c, request.verificationRequired(), request.verificationAssigneeId(),
                request.verificationAssigneeName(), request.verificationInstructions());

        CapaAction a = new CapaAction();
        a.setCapa(c);
        a.setTitle(titre);
        a.setDescription(description);
        a.setStatus(CapaActionStatus.PENDING);
        // La nature de l'action, telle qu'elle a été choisie ; à défaut, celle que
        // le type du dossier implique.
        a.setActionType(request.actionType() != null
                ? request.actionType()
                : (request.type() == CapaType.PREVENTIVE
                        ? CapaActionType.PREVENTIVE : CapaActionType.CORRECTIVE));
        a.setAssigneeId(request.ownerId());
        a.setAssigneeName(pilote);
        a.setDecidedOn(LocalDate.now());
        a.setDueDate(request.dueDate());
        // CE QU'ON ATTEND DE CETTE PREMIÈRE ACTION, recopié de ce qui vient
        // d'être posé sur le dossier (ADR 0131). La question n'a été posée
        // qu'une fois, en écrivant cette action-là : la porter aussi sur elle
        // évite qu'elle soit la seule du dossier à ne rien dire de son effet,
        // alors qu'elle est précisément celle devant laquelle on a répondu.
        //
        // Rien sous un endiguement : il ne promet aucun effet, et le dossier
        // qu'il ouvre ne se clôture de toute façon pas sans correctif
        // (CONTAINMENT_ONLY). La question se reposera à l'écriture de celui-là.
        if (a.getActionType() != CapaActionType.CONTAINMENT) {
            a.setEffectivenessCriterion(c.getEffectivenessCriterion());
            a.setVerificationRequired(c.getVerificationRequired());
            a.setVerificationAssigneeId(c.getVerificationAssigneeId());
            a.setVerificationAssigneeName(c.getVerificationAssigneeName());
            a.setVerificationInstructions(c.getVerificationInstructions());
        }
        // L'action part avec le dossier (cascade) : une seule écriture, donc
        // jamais l'un sans l'autre.
        c.getActions().add(a);

        CapaCase saved = caseRepository.save(c);
        journal.record(saved, CapaTransition.OPENED);
        rattacher(ecart, saved);
        return toResponse(saved);
    }

    /**
     * L'ecart du registre que ce dossier cite, sous verrou d'ecriture.
     *
     * <p>{@code null} des que la source ne designe pas un ecart, que la
     * reference est vide, ou qu'aucun ecart ne la porte : une reference tapee
     * peut ne designer personne, et ce n'est pas une erreur — le dossier existe
     * alors seul, avec sa reference pour memoire.
     *
     * <p>Le verrou serialise deux ouvertures simultanees qui citeraient le meme
     * ecart : sans lui, chacune lirait un lien vide et la seconde ecraserait le
     * premier rattachement.
     */
    private NonConformity ecartCite(CapaSourceType sourceType, String sourceRef) {
        if (!citeUnEcart(sourceType) || sourceRef == null || sourceRef.isBlank()) {
            return null;
        }
        UUID tenantId = requireTenantId();
        return ncRepository.findByTenantIdAndReference(tenantId, sourceRef.strip())
                .flatMap(nc -> ncRepository.findForUpdateByIdAndTenantId(nc.getId(), tenantId))
                .orElse(null);
    }

    /** La source d'un dossier ne a sur un ecart : son origine, jamais un choix. */
    private static CapaSourceType sourceDe(NonConformity ecart) {
        return NcOrigin.orDefault(ecart.getOrigin()) == NcOrigin.EXTERNAL
                ? CapaSourceType.EXTERNAL_NON_CONFORMITY
                : CapaSourceType.NON_CONFORMITY;
    }

    /**
     * Pose sur l'ecart le lien vers son dossier, s'il n'en a pas deja un.
     *
     * <p>Un ecart a UN dossier (ADR 0088) : le premier ouvert reste le sien, et
     * un second ouvert depuis la liste ne le remplace pas — il garde sa
     * reference, donc la cloture de l'ecart l'attend quand meme
     * ({@link #unsettledCasesFor}). Remplacer le lien ferait disparaitre de la
     * fiche le tableau d'actions du premier.
     */
    private void rattacher(NonConformity ecart, CapaCase dossier) {
        if (ecart != null && ecart.getCapaCaseId() == null) {
            ecart.setCapaCaseId(dossier.getId());
            ncRepository.save(ecart);
        }
    }

    public CapaDto.CaseResponse updateCase(UUID id, CapaDto.UpdateCaseRequest request) {
        CapaCase c = loadCase(id);
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.REJECTED) {
            throw new CapaStateException("Cannot modify a " + c.getStatus() + " CAPA");
        }
        if (request.title() != null) c.setTitle(request.title());
        if (request.description() != null) c.setDescription(request.description());
        if (request.criticity() != null) c.setCriticity(request.criticity());
        if (request.sourceRef() != null) c.setSourceRef(request.sourceRef());
        if (request.rootCauseId() != null) c.setRootCauseId(request.rootCauseId());
        if (request.dueDate() != null) c.setDueDate(request.dueDate());
        if (request.effectivenessCriterion() != null) {
            appliquerCritere(c, request.effectivenessCriterion());
        }
        // Le commentaire n'a pas la garde du critere : il ne fonde aucun verdict,
        // donc le reecrire ne retire son appui a personne. La seule borne est
        // celle de la methode -- un dossier clos ne bouge plus.
        if (request.effectivenessComment() != null) {
            c.setEffectivenessComment(nettoyer(request.effectivenessComment()));
        }
        appliquerVerification(c, request.verificationRequired(), request.verificationAssigneeId(),
                request.verificationAssigneeName(), request.verificationInstructions());
        CapaCase saved = caseRepository.save(c);
        journal.record(saved, CapaTransition.UPDATED);
        return toResponse(saved);
    }

    /**
     * Le critère d'efficacité, modifiable tant qu'aucun jugement ne s'y appuie
     * (ADR 0108).
     *
     * <p><b>Pourquoi un verrou.</b> Les évaluations recopient le critère, donc
     * le modifier ici ne réécrit aucune trace — l'historique est sauf dans tous
     * les cas. Ce qui ne l'est pas, c'est le sens du dossier : changer le critère
     * après un premier verdict, c'est déplacer la cible entre deux mesures, et
     * la suite des verdicts ne se lirait plus comme une progression mais comme
     * une comparaison de choses différentes.
     *
     * <p>Un dossier jamais évalué, lui, se corrige librement : un critère mal
     * formulé doit pouvoir être reformulé avant qu'il ne serve.
     */
    private void appliquerCritere(CapaCase c, String critere) {
        String propre = nettoyer(critere);
        if (java.util.Objects.equals(propre, c.getEffectivenessCriterion())) {
            // Un PATCH qui renvoie le champ inchangé ne doit pas être refusé :
            // l'écran poste souvent la fiche entière.
            return;
        }
        if (!effectivenessReviews.findByCase(c.getTenantId(), c.getId()).isEmpty()) {
            throw new CapaStateException(
                    "This CAPA has already been evaluated: its effectiveness criterion can no "
                    + "longer change, as past verdicts were rendered against it");
        }
        c.setEffectivenessCriterion(propre);
    }

    /**
     * L'exigence de vérification, et ce qu'elle entraîne.
     *
     * <p>Trois règles, et chacune répond à une question qu'un auditeur pose :
     *
     * <ol>
     *   <li><b>Exiger sans désigner ne veut rien dire.</b> Une vérification que
     *       personne ne doit faire n'aura pas lieu ; on refuse donc (422) plutôt
     *       que d'enregistrer une intention sans titulaire.</li>
     *   <li><b>Ne plus exiger efface ce qui n'a plus d'objet.</b> Passer à
     *       « non » retire le vérificateur et les consignes : les laisser
     *       traîner laisserait croire qu'une vérification est attendue.</li>
     *   <li><b>Désigner quelqu'un sans exiger la vérification est un refus.</b>
     *       C'est presque toujours une case oubliée, et l'accepter en silence
     *       produirait un dossier qui se croit couvert.</li>
     * </ol>
     *
     * <p>{@code null} sur {@code verificationRequired} laisse tout en l'état :
     * la mise à jour est partielle, et ne rien dire n'est pas dire « non ».
     * À l'ouverture (ADR 0088), le dossier neuf n'a encore rien décidé : les
     * mêmes règles s'appliquent, et {@code null} laisse la question ouverte.
     */
    private void appliquerVerification(CapaCase c, Boolean exigee, UUID assigneeId,
                                       String assigneeName, String instructions) {

        if (Boolean.FALSE.equals(exigee)) {
            c.setVerificationRequired(false);
            c.setVerificationAssigneeId(null);
            c.setVerificationAssigneeName(null);
            c.setVerificationInstructions(null);
            return;
        }

        boolean exigeeApres = Boolean.TRUE.equals(exigee)
                || (exigee == null && Boolean.TRUE.equals(c.getVerificationRequired()));

        if (!exigeeApres
                && (assigneeId != null
                    || nonVide(instructions))) {
            throw new CapaValidationException(
                    "A verifier or verification instructions require verificationRequired=true");
        }

        if (Boolean.TRUE.equals(exigee)) {
            c.setVerificationRequired(true);
        }
        if (assigneeId != null) {
            c.setVerificationAssigneeId(assigneeId);
        }
        if (assigneeName != null) {
            c.setVerificationAssigneeName(nettoyer(assigneeName));
        }
        if (instructions != null) {
            c.setVerificationInstructions(nettoyer(instructions));
        }

        if (exigeeApres && c.getVerificationAssigneeId() == null) {
            throw new CapaValidationException(
                    "A required verification must be assigned to someone");
        }
    }

    /** `null` et blancs sont la même absence : on ne stocke pas une chaîne vide. */
    private String nettoyer(String valeur) {
        if (valeur == null) return null;
        String rogne = valeur.trim();
        return rogne.isEmpty() ? null : rogne;
    }

    private boolean nonVide(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    /**
     * Le statut du dossier est le REFLET de ses actions, jamais une seconde
     * machine à états conduite à la main (ADR 0117, qui révise l'ADR 0088 et
     * prolonge l'ADR 0115).
     *
     * <p>Le dossier portait son propre cycle, avancé par deux boutons
     * « Démarrer » et « Résoudre ». Or ce service refusait déjà de résoudre
     * tant qu'une action restait à faire : les deux boutons ne décidaient rien,
     * ils recopiaient à la main ce que les actions disaient déjà. Le pilote qui
     * terminait sa dernière action voyait son dossier rester « en cours », et
     * le mot « Statut » s'écrivait deux fois sur la fiche pour deux réalités
     * qu'on finissait par confondre.
     *
     * <p>On fait donc avancer les ACTIONS, et le dossier suit :
     * <ul>
     *   <li>aucune action, ou toutes encore à faire → {@code OPEN} ;</li>
     *   <li>au moins une commencée → {@code IN_PROGRESS} ;</li>
     *   <li>toutes réalisées → {@code RESOLVED}.</li>
     * </ul>
     *
     * <p>{@code CLOSED} et {@code REJECTED} restent hors d'atteinte : ce sont
     * les deux seules décisions humaines qui subsistent — le verdict
     * d'efficacité (ADR 0108) et l'abandon du dossier. Un dossier clos ne
     * redeviendrait pas « en cours » parce qu'on rouvre une de ses actions.
     */
    private void refleterAvancement(CapaCase c) {
        CapaStatus actuel = c.getStatus();
        if (actuel == CapaStatus.CLOSED || actuel == CapaStatus.REJECTED) {
            return;
        }
        CapaStatus deduit = deduireStatut(c.getActions());
        if (deduit == actuel) {
            return;
        }
        c.setStatus(deduit);
        // L'horodatage de résolution suit le statut. Sans cette remise à null,
        // un dossier reparti en traitement afficherait une date de résolution :
        // la fiche dirait à la fois « en cours » et « résolu le 12 mars ».
        c.setResolvedAt(deduit == CapaStatus.RESOLVED ? Instant.now() : null);
        CapaCase saved = caseRepository.save(c);
        journal.record(saved, transitionVers(deduit, actuel));
    }

    /**
     * L'état que les actions dictent, sans tenir compte de celui du dossier.
     *
     * <p>Un dossier SANS action reste {@code OPEN} : il n'a rien commencé, et le
     * dire « résolu » parce qu'aucune action n'est inachevée clôturerait les
     * dossiers vides — exactement ceux qui attendent qu'on écrive quoi faire.
     */
    private static CapaStatus deduireStatut(java.util.Collection<CapaAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return CapaStatus.OPEN;
        }
        if (actions.stream().allMatch(a -> a.getStatus() == CapaActionStatus.DONE)) {
            return CapaStatus.RESOLVED;
        }
        boolean entame = actions.stream().anyMatch(a -> a.getStatus() != CapaActionStatus.PENDING);
        return entame ? CapaStatus.IN_PROGRESS : CapaStatus.OPEN;
    }

    /**
     * Ce qu'on inscrit au journal : avancer et revenir en arrière ne se lisent
     * pas de la même façon pour un auditeur.
     */
    private static CapaTransition transitionVers(CapaStatus deduit, CapaStatus actuel) {
        if (deduit == CapaStatus.RESOLVED) {
            return CapaTransition.RESOLVED;
        }
        boolean recul = actuel == CapaStatus.RESOLVED || deduit == CapaStatus.OPEN;
        return recul ? CapaTransition.REOPENED : CapaTransition.STARTED;
    }

    /**
     * L'évaluation d'efficacité d'un dossier (ADR 0108).
     *
     * <p><b>Trois verdicts, une seule machine à états.</b> {@code EFFECTIVE}
     * clôt ; {@code PARTIAL} et {@code INEFFECTIVE} renvoient le dossier en
     * traitement. Ce que chaque verdict entraîne est porté par
     * {@link EffectivenessVerdict} et non par une suite de conditions ici :
     * ajouter un verdict sans décider de son effet devient impossible à oublier.
     *
     * <p><b>Le verrou « toutes les actions réalisées » s'applique désormais à
     * TOUTE évaluation</b>, et non plus à la seule clôture. Juger l'effet d'une
     * action qu'on n'a pas menée n'a aucun sens, et consigner un refus
     * d'efficacité sur un dossier inachevé ne voulait rien dire. C'est le verrou
     * que décrit la maquette, désormais tenu par le serveur et pas seulement
     * annoncé par l'écran.
     *
     * <p>Les autres garde-fous — pas d'endiguement seul, aucune non-conformité
     * rattachée encore ouverte — ne concernent que {@code EFFECTIVE} : ils
     * protègent la clôture, et rien d'autre.
     */
    public CapaDto.CaseResponse verifyEffectiveness(UUID id, CapaDto.EffectivenessRequest request) {
        CapaCase c = loadCase(id);
        EffectivenessVerdict verdict = resolveVerdict(request);
        if (c.getStatus() != CapaStatus.RESOLVED) {
            throw new CapaStateException("Effectiveness check requires status RESOLVED");
        }
        // Les trois obstacles ci-dessous ne valent QUE pour un verdict qui clôt.
        // Constater l'échec — INEFFICACE — ou l'insuffisance — PARTIEL — ne
        // ferme rien : l'interdire empêcherait de consigner que les actions
        // n'ont pas produit leur effet, et c'est précisément ce constat que la
        // boucle d'amélioration a besoin d'écrire pour repartir.
        if (verdict.closesCase()) {
            // Toutes les actions doivent être faites — y compris celles ajoutées
            // APRÈS la résolution. `resolveCase` l'a vérifié à son moment, mais
            // rien n'interdit d'ajouter une action à un dossier RESOLVED, et le
            // dossier se serait alors clos au-dessus d'une action jamais menée.
            // C'est aussi ce que la fiche annonce (ACTIONS_NOT_DONE) : une liste
            // d'obstacles qui n'engage pas le serveur ne vaut rien, et l'écran
            // interdirait un geste que l'API accorderait.
            long notDone = c.getActions().stream()
                    .filter(a -> a.getStatus() != CapaActionStatus.DONE).count();
            if (notDone > 0) {
                throw new CapaStateException(notDone + " action(s) are not DONE: "
                        + "complete them before closing this CAPA");
            }
            // Un dossier qui n'a fait qu'endiguer n'a rien réglé : les mesures
            // d'endiguement arrêtent l'effet et laissent la cause intacte
            // (ISO 9001 §10.2). Le clore reviendrait à inscrire au registre que
            // le problème est traité, alors qu'il reviendra dès la mesure levée.
            if (isContainmentOnly(c.getActions())) {
                throw new CapaStateException(
                        "This CAPA carries containment actions only: they stop the effect "
                        + "without removing the cause. Add a corrective or preventive action "
                        + "before closing it");
            }
            // Clore une CAPA au-dessus d'un écart encore ouvert reviendrait à
            // déclarer le problème réglé pendant que le constat, lui, dit le
            // contraire. Le refus est explicite et chiffré : l'utilisateur sait
            // combien de non-conformités il lui reste à traiter.
            long stillOpen = ncRepository.countByTenantIdAndCapaCaseIdAndStatusNotIn(
                    c.getTenantId(), c.getId(), NC_SETTLED_STATUSES);
            if (stillOpen > 0) {
                throw new CapaStateException(stillOpen + " linked non-conformity(ies) are still open: "
                        + "close them before closing this CAPA");
            }
            // TOUTES LES ACTIONS DOIVENT ETRE CLOSES, et une action qui a promis
            // une verification n'est close que verifiee (ADR 0132). Le refus est
            // ici et pas seulement dans la liste d'obstacles : une liste qui
            // n'engage pas le serveur ne vaut rien, et l'ecran interdirait un
            // geste que l'API accorderait.
            long aVerifier = verificationsDActionEnAttente(c);
            if (aVerifier > 0) {
                throw new CapaStateException(aVerifier + " action(s) promised an effectiveness "
                        + "verification that has not concluded: verify them before closing this CAPA");
            }
            // UN ECART D'OBJECTIF NE SE REFERME PAS SUR UNE DECLARATION.
            //
            // Pour un dossier ne d'un objectif qualite, l'efficacite EST le
            // retour du KPI sous la cible sur la duree que l'objectif s'est
            // donnee -- pas l'avis de qui a mene les actions. Le critere a ete
            // verrouille a l'ouverture ; ici on verifie qu'il est satisfait.
            //
            // Le refus est mesure, pas declaratif : il cite les periodes
            // constatees et celles qu'il faut, pour que le verificateur sache
            // combien de temps il lui reste a attendre plutot que de reessayer.
            // Voir l'ADR 0133.
            objectiveRecovery.forCase(c.getTenantId(), c.getId())
                    .filter(r -> !r.achieved())
                    .ifPresent(r -> {
                        throw new CapaStateException(
                                "This CAPA comes from a quality objective: its effectiveness is "
                                + "the KPI returning to target for " + r.requiredPeriods()
                                + " consecutive period(s), and " + r.onTargetPeriods()
                                + " have been observed. Criterion: " + r.criterion());
                    });
        }

        Instant now = Instant.now();
        // Le critère est RECOPIÉ dans l'évaluation. Juger sans critère annoncé
        // revient à choisir le critère en regardant le résultat : le domaine
        // refuse, et son refus dit quoi écrire et où.
        EffectivenessReview review = EffectivenessReview.record(
                UUID.randomUUID(), c.getTenantId(), c.getId(), verdict,
                criterionOf(request, c),
                request.baseline(), request.target(), observedOf(request),
                request.comment(),
                CurrentUser.userId().orElse(null),
                CurrentUser.displayName().orElse(null),
                now);
        effectivenessReviews.save(review);

        // L'ancien booléen devient la projection du verdict, et n'est plus écrit
        // à la main. PARTIAL y vaut false : ce champ n'a jamais su dire autre
        // chose que oui ou non.
        c.setEffectivenessVerified(verdict.asLegacyVerifiedFlag());
        c.setEffectivenessVerifiedAt(now);
        if (verdict.closesCase()) {
            c.setStatus(CapaStatus.CLOSED);
            c.setClosedAt(now);
        } else {
            c.setStatus(CapaStatus.IN_PROGRESS);
            c.setResolvedAt(null);
        }
        CapaCase saved = caseRepository.save(c);
        journal.record(saved, transitionFor(verdict));
        return toResponse(saved);
    }

    /**
     * Vrai quand ce dossier a PROMIS de verifier son effet.
     *
     * <p>Lu par la cloture directe ET par la liste d'obstacles : les deux
     * doivent dire la meme chose, sans quoi l'ecran eteindrait un bouton que le
     * serveur aurait accepte -- ou l'allumerait pour un refus.
     *
     * <p>Trois formes de promesse : la reponse du dossier, celle d'une seule de
     * ses actions (ADR 0131), et un critere annonce, qui dit ce qu'on mesurera.
     */
    static boolean verificationPromise(CapaCase c) {
        if (Boolean.TRUE.equals(c.getVerificationRequired())) {
            return true;
        }
        if (c.getEffectivenessCriterion() != null && !c.getEffectivenessCriterion().isBlank()) {
            return true;
        }
        return c.getActions().stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getVerificationRequired()));
    }

    /**
     * CLÔTURE DIRECTE : le dossier se ferme sans qu'aucun verdict ne soit écrit.
     *
     * <p>La question « vérifiera-t-on l'effet ? » se tranche à l'ouverture. Répondue
     * « non » — ou jamais posée, ce qui est le cas d'un endiguement — elle
     * engage : il n'y aura pas d'évaluation, et la clôture ne doit donc pas en
     * réclamer une. L'écran offrait pourtant « Efficace — clôturer » comme seul
     * chemin : on fermait le dossier en inscrivant au registre une efficacité
     * que personne n'avait constatée, sur un critère que personne n'avait mesuré.
     *
     * <p><b>L'endiguement se clot par ici, et seulement par ici.</b> Depuis
     * l'ADR 0135 chaque action porte son propre dossier : le dossier
     * d'endiguement ne contient que de l'endiguement, et le refus opposé par
     * {@link #verifyEffectiveness} à un dossier qui n'endigue que le rendait
     * définitivement infermable — il restait ouvert pour toujours, à encombrer
     * le registre. Ce refus garde tout son sens là où il est :
     * on ne déclare pas une efficacité démontrée sur une mesure qui n'a fait
     * qu'arrêter l'effet. Ici, on ne déclare rien — on constate que le geste a
     * été fait.
     *
     * <p>Tous les autres obstacles de la clôture restent opposés : un dossier
     * dont une action traîne, dont l'écart reste ouvert, ou dont l'objectif
     * n'est pas revenu à la cible ne se ferme pas davantage par ce chemin.
     *
     * @throws CapaStateException si le dossier n'est pas résolu, si une
     *         vérification a été promise — elle se solde par un verdict, pas en
     *         la contournant — ou si un autre obstacle demeure.
     */
    public CapaDto.CaseResponse closeWithoutVerification(UUID id) {
        CapaCase c = loadCase(id);
        if (c.getStatus() != CapaStatus.RESOLVED) {
            throw new CapaStateException("A direct closure requires status RESOLVED");
        }
        // UNE PROMESSE DE VÉRIFICATION NE SE CONTOURNE PAS : elle se solde par
        // un verdict. Qu'elle vienne du dossier, d'une seule de ses actions ou
        // d'un critère annoncé — qui dit ce qu'on mesurera —, c'est le MÊME
        // prédicat que lit la liste d'obstacles : un écran qui annonce autre
        // chose que ce que le serveur fera ne vaut rien.
        if (verificationPromise(c)) {
            throw new CapaStateException("This CAPA promised an effectiveness verification: "
                    + "record a verdict instead of closing it directly");
        }
        long notDone = c.getActions().stream()
                .filter(a -> a.getStatus() != CapaActionStatus.DONE).count();
        if (notDone > 0) {
            throw new CapaStateException(notDone + " action(s) are not DONE: "
                    + "complete them before closing this CAPA");
        }
        long stillOpen = ncRepository.countByTenantIdAndCapaCaseIdAndStatusNotIn(
                c.getTenantId(), c.getId(), NC_SETTLED_STATUSES);
        if (stillOpen > 0) {
            throw new CapaStateException(stillOpen + " linked non-conformity(ies) are still open: "
                    + "close them before closing this CAPA");
        }
        // Un dossier né d'un objectif qualité ne se ferme jamais sur une
        // déclaration : son efficacité EST le retour du KPI sous la cible
        // (ADR 0133). Le garde vaut ici aussi, sans quoi ce chemin deviendrait
        // la porte dérobée par laquelle l'autre se contourne.
        objectiveRecovery.forCase(c.getTenantId(), c.getId())
                .filter(r -> !r.achieved())
                .ifPresent(r -> {
                    throw new CapaStateException(
                            "This CAPA comes from a quality objective: its effectiveness is "
                            + "the KPI returning to target for " + r.requiredPeriods()
                            + " consecutive period(s), and " + r.onTargetPeriods()
                            + " have been observed. Criterion: " + r.criterion());
                });

        Instant now = Instant.now();
        // `effectivenessVerified` reste INTOUCHÉ. Le mettre à `true` dirait que
        // l'efficacité a été vérifiée, le mettre à `false` qu'elle a échoué :
        // aucune des deux n'est vraie. Nul veut dire « on n'a pas jugé », et
        // c'est exactement ce qui s'est passé.
        c.setStatus(CapaStatus.CLOSED);
        c.setClosedAt(now);
        CapaCase saved = caseRepository.save(c);
        journal.record(saved, CapaTransition.CLOSED_WITHOUT_VERIFICATION);
        return toResponse(saved);
    }

    /**
     * Le verdict de la requête, quelle que soit la forme employée.
     *
     * <p>Exactement l'un des deux champs : {@code verdict}, la forme à employer,
     * ou {@code effective}, l'ancien booléen conservé pour les intégrations
     * (§13.1). Les deux ensemble sont refusés plutôt qu'arbitrés — deux
     * jugements dans une même requête disent que l'appelant ne sait pas lequel
     * il envoie, et en choisir un pour lui enterrerait son défaut.
     */
    private static EffectivenessVerdict resolveVerdict(CapaDto.EffectivenessRequest request) {
        boolean hasVerdict = request.verdict() != null;
        boolean hasFlag = request.effective() != null;
        if (hasVerdict == hasFlag) {
            throw new CapaValidationException(hasVerdict
                    ? "Provide either verdict or the deprecated effective flag, not both"
                    : "A verdict is required: EFFECTIVE, PARTIAL or INEFFECTIVE");
        }
        return hasVerdict
                ? request.verdict()
                : EffectivenessVerdict.fromLegacyFlag(request.effective());
    }

    /**
     * Le relevé constaté, obligatoire sur la forme moderne.
     *
     * <p>L'ancien champ booléen n'en demandait aucun : l'exiger de lui casserait
     * les appelants existants. On inscrit alors une mention explicite plutôt
     * qu'un vide — l'historique doit pouvoir expliquer, des années plus tard,
     * pourquoi cette ligne-là ne porte pas de mesure.
     */
    private static String observedOf(CapaDto.EffectivenessRequest request) {
        if (request.observed() != null && !request.observed().isBlank()) {
            return request.observed();
        }
        if (request.verdict() != null) {
            throw new CapaValidationException(
                    "The observed reading is required: an effectiveness verdict rests on a measure");
        }
        return LEGACY_OBSERVED;
    }

    /**
     * Ce qu'on inscrit comme relevé quand l'appel vient de l'ancienne API.
     *
     * <p>Une constante et non un littéral dispersé : c'est la valeur qu'un
     * auditeur rencontrera dans l'historique, et elle doit se chercher.
     */
    static final String LEGACY_OBSERVED = "n/a (API héritée)";

    /**
     * Le critère contre lequel le verdict est rendu, recopié dans l'évaluation.
     *
     * <p>Sur la forme moderne, il est EXIGÉ et doit avoir été posé sur le
     * dossier AVANT l'évaluation (ADR 0108) : juger sans critère annoncé revient
     * à choisir le critère en regardant le résultat, et le refus dit où le
     * poser. L'écran n'ouvre d'ailleurs le panneau d'évaluation que lorsqu'il
     * est là — mais une règle tenue par le seul écran n'est pas une règle.
     *
     * <p>L'ancien booléen, lui, n'en a jamais demandé : l'exiger de lui
     * casserait les intégrations existantes (§13.1) et rendrait inévaluables
     * tous les dossiers ouverts avant cette fonctionnalité. On inscrit alors une
     * mention explicite plutôt qu'un vide, pour que l'historique puisse
     * expliquer, des années plus tard, pourquoi cette ligne-là n'en porte pas.
     */
    private static String criterionOf(CapaDto.EffectivenessRequest request, CapaCase c) {
        String posed = c.getEffectivenessCriterion();
        if (posed != null && !posed.isBlank()) {
            return posed;
        }
        if (request.verdict() != null) {
            throw new CapaValidationException(
                    "No effectiveness criterion is set on this CAPA: define it on the case "
                    + "before evaluating it, so the verdict is judged against a criterion "
                    + "chosen in advance");
        }
        return LEGACY_CRITERION;
    }

    /** Ce qu'on inscrit comme critère quand l'appel vient de l'ancienne API. */
    static final String LEGACY_CRITERION = "n/a (aucun critère posé — API héritée)";

    private static CapaTransition transitionFor(EffectivenessVerdict verdict) {
        return switch (verdict) {
            case EFFECTIVE -> CapaTransition.CLOSED;
            case PARTIAL -> CapaTransition.EFFECTIVENESS_PARTIAL;
            case INEFFECTIVE -> CapaTransition.EFFECTIVENESS_REJECTED;
        };
    }

    public CapaDto.CaseResponse rejectCase(UUID id) {
        CapaCase c = loadCase(id);
        if (c.getStatus() != CapaStatus.OPEN && c.getStatus() != CapaStatus.IN_PROGRESS) {
            throw new CapaStateException("Only OPEN or IN_PROGRESS CAPA can be rejected");
        }
        c.setStatus(CapaStatus.REJECTED);
        CapaCase saved = caseRepository.save(c);
        journal.record(saved, CapaTransition.REJECTED);
        return toResponse(saved);
    }

    public void deleteCase(UUID id) {
        CapaCase c = loadCase(id);
        if (c.getStatus() == CapaStatus.CLOSED) {
            throw new CapaStateException("Closed CAPA cannot be deleted");
        }
        // Consigné AVANT l'effacement : après, il ne reste rien à décrire.
        journal.record(c, CapaTransition.DELETED);
        caseRepository.delete(c);
    }

    // --- actions ---

    public CapaDto.ActionResponse addAction(UUID capaId, CapaDto.ActionRequest request) {
        CapaCase c = loadCase(capaId);
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.REJECTED) {
            throw new CapaStateException("Cannot add actions to a " + c.getStatus() + " CAPA");
        }
        // Plusieurs actions par dossier (ADR 0115, qui revise l'ADR 0088 sur ce
        // seul point). Un ecart reel se traite rarement d'un seul geste : on
        // endigue, on corrige la cause, on previent la recidive, et le 8D nomme
        // ces etapes separement (D3, D5, D6). La regle « une seule action »
        // obligeait a ouvrir un second dossier sur le meme ecart, ou a tout
        // ecrire dans un intitule unique : dans les deux cas, l'avancement
        // n'etait plus mesurable action par action.
        boolean premiere = c.getActions().isEmpty();
        CapaAction a = new CapaAction();
        a.setCapa(c);
        a.setTitle(requireUsableTitle(request.title()));
        a.setDescription(request.description());
        a.setStatus(request.status() != null ? request.status() : CapaActionStatus.PENDING);
        // UNE ACTION NEE FAITE EST FAITE A CET INSTANT. La date d'achevement
        // n'etait posee qu'a la mise a jour : une action creee directement
        // « Faite » -- une reprise consignee apres coup, un geste deja mene
        // quand on ouvre le dossier -- portait donc l'etat sans la date. Les
        // deux champs du meme fait se contredisaient dans le meme
        // enregistrement, et tout ce qui se fie a la date l'ignorait : le D6
        // du rapport 8D la declarait jamais menee sous un D5 qui l'affichait
        // faite. La regle est celle de la mise a jour, a l'identique.
        if (a.getStatus() == CapaActionStatus.DONE) {
            a.setCompletedAt(Instant.now());
        }
        // Corrective par défaut : c'est le cas de loin le plus fréquent, et
        // supposer l'endiguement obligerait à qualifier chaque action pour
        // écrire ce que tout le monde entend déjà par « action d'une CAPA ».
        a.setActionType(request.actionType() != null ? request.actionType() : CapaActionType.CORRECTIVE);
        a.setAssigneeId(request.assigneeId());
        a.setAssigneeName(normalizeName(request.assigneeName()));
        // La date de décision est SAISIE si l'appelant la donne, sinon DÉDUITE
        // du jour de l'enregistrement — et cette déduction est faite ici,
        // explicitement, pas en recyclant createdAt à l'affichage. Une action
        // décidée en comité et saisie plus tard se corrige par une mise à jour ;
        // une colonne qui afficherait created_at sous le nom « décidé le »
        // ne se corrigerait jamais, parce qu'elle mentirait sans le dire.
        a.setDecidedOn(request.decidedOn() != null ? request.decidedOn() : LocalDate.now());
        a.setDueDate(request.dueDate());
        appliquerEfficaciteDeLAction(a, request);
        CapaAction saved = actionRepository.save(a);
        // Le responsable de la PREMIERE action pilote le dossier : c'est a lui
        // qu'on demandera des nouvelles, et il doit pouvoir le voir (ADR 0081).
        // Les suivantes ne le changent pas : la troisieme action d'un dossier
        // aurait sinon deplace le pilotage vers qui l'execute.
        if (premiere && request.assigneeId() != null) {
            c.setOwnerId(request.assigneeId());
            c.setOwnerName(saved.getAssigneeName());
            caseRepository.save(c);
        }
        // L'action neuve compte dans l'avancement du dossier : l'ajouter à un
        // dossier résolu le ramène en traitement, puisqu'il reste à faire.
        c.getActions().add(saved);
        refleterAvancement(c);
        return toActionResponse(saved);
    }

    public CapaDto.ActionResponse updateAction(UUID capaId, UUID actionId, CapaDto.ActionRequest request) {
        CapaCase c = loadCase(capaId);
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.REJECTED) {
            throw new CapaStateException("Cannot modify actions on a " + c.getStatus() + " CAPA");
        }
        CapaAction a = actionRepository.findByIdAndCapaId(actionId, capaId)
                .orElseThrow(() -> new CapaActionNotFoundException(actionId));
        // PATCH : un champ absent ne se touche pas. Un champ PRÉSENT, en
        // revanche, est validé ici — le contrôleur ne peut pas déléguer à
        // Jakarta sans rendre le titre obligatoire à chaque appel partiel.
        // Sans ce garde-fou, l'édition en ligne du tableau viderait le libellé
        // d'une action sur un simple espace.
        if (request.title() != null) a.setTitle(requireUsableTitle(request.title()));
        if (request.description() != null) a.setDescription(request.description());
        if (request.assigneeId() != null) a.setAssigneeId(request.assigneeId());
        if (request.assigneeName() != null) a.setAssigneeName(normalizeName(request.assigneeName()));
        if (request.decidedOn() != null) a.setDecidedOn(request.decidedOn());
        if (request.dueDate() != null) a.setDueDate(request.dueDate());
        if (request.actionType() != null) a.setActionType(request.actionType());
        appliquerEfficaciteDeLAction(a, request);
        if (request.status() != null) {
            a.setStatus(request.status());
            if (request.status() == CapaActionStatus.DONE && a.getCompletedAt() == null) {
                a.setCompletedAt(Instant.now());
            } else if (request.status() != CapaActionStatus.DONE) {
                a.setCompletedAt(null);
            }
        }
        CapaAction enregistree = actionRepository.save(a);
        refleterAvancement(c);
        return toActionResponse(enregistree);
    }

    /**
     * JUGER L'EFFICACITE D'UNE ACTION (ADR 0132).
     *
     * <p>Meme geste que sur le dossier, a l'echelle ou la promesse a ete faite :
     * chaque action porte sa propre exigence de verification (ADR 0131), et le
     * dossier ne se cloture pas tant qu'une action promise verifiee ne l'a pas
     * ete.
     *
     * <p>Quatre refus :
     * <ul>
     *   <li>une action qui n'a RIEN PROMIS ne se juge pas : il n'y a pas de
     *       critere auquel la comparer, et juger sans critere annonce revient a
     *       choisir le critere en regardant le resultat (ADR 0108) ;</li>
     *   <li>une action PAS ENCORE FAITE ne se juge pas : on mesurerait l'effet
     *       d'un geste qui n'a pas eu lieu ;</li>
     *   <li>un dossier CLOS ou REJETE ne recoit plus de jugement : ce qui est
     *       clos l'a ete sur l'etat d'alors ;</li>
     *   <li>le critere est OBLIGATOIRE -- celui de l'action, a defaut celui du
     *       dossier pour les actions anterieures a la colonne (V177).</li>
     * </ul>
     *
     * <p>Le jugement s'AJOUTE a l'historique, il n'ecrase rien : une action
     * jugee insuffisante, reprise, puis jugee efficace laisse les trois lignes,
     * et c'est cette suite qu'un auditeur lit.
     *
     * <p><b>QUI PEUT JUGER</b> : celui qui porte {@code capa.verify}, et LA
     * PERSONNE DESIGNEE sur l'action — a defaut celle du dossier (ADR 0143).
     * Une verification exigee doit etre confiee a quelqu'un (ADR 0131) ; la
     * refuser ensuite a cette personne-la, parce que son role ne porte pas
     * l'action du catalogue, rendait la designation sans effet.
     *
     * <p><b>ET CE QUE LE JUGEMENT ENTRAINE</b> : un verdict qui clot une action
     * clot son dossier, des lors que plus rien ne s'y oppose (ADR 0143). Sans
     * cela, l'ecran d'efficacite redemandait une seconde evaluation du meme
     * geste pour faire ce que la premiere avait deja etabli.
     */
    public CapaDto.ActionResponse verifyActionEffectiveness(
            UUID capaId, UUID actionId, CapaDto.EffectivenessRequest request) {

        CapaCase c = loadCase(capaId);
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.REJECTED) {
            throw new CapaStateException(
                    "Cannot verify actions on a " + c.getStatus() + " CAPA");
        }
        CapaAction a = actionRepository.findByIdAndCapaId(actionId, capaId)
                .orElseThrow(() -> new CapaActionNotFoundException(actionId));
        exigerLeDroitDeJuger(c, a);

        if (!Boolean.TRUE.equals(a.getVerificationRequired())) {
            throw new CapaValidationException(
                    "This action did not require an effectiveness verification: "
                            + "nothing was promised, and there is no criterion to judge it against");
        }
        if (a.getStatus() != CapaActionStatus.DONE) {
            throw new CapaStateException(
                    "This action is not DONE yet: complete it before judging its effect");
        }

        EffectivenessVerdict verdict = resolveVerdict(request);
        String critere = a.getEffectivenessCriterion() != null
                ? a.getEffectivenessCriterion()
                : c.getEffectivenessCriterion();
        if (!nonVide(critere)) {
            throw new CapaValidationException(
                    "No effectiveness criterion was set on this action or its case: "
                            + "set one before judging it");
        }

        Instant now = Instant.now();
        EffectivenessReview review = EffectivenessReview.recordForAction(
                UUID.randomUUID(), c.getTenantId(), c.getId(), a.getId(), verdict,
                critere, request.baseline(), request.target(), observedOf(request),
                request.comment(),
                CurrentUser.userId().orElse(null),
                CurrentUser.displayName().orElse(null),
                now);
        effectivenessReviews.save(review);
        journal.record(c, CapaTransition.UPDATED);
        // LE DOSSIER SUIT LE VERDICT DE SON ACTION (ADR 0143). La liste
        // d'obstacles est celle que le verdict du dossier doit franchir
        // (ADR 0137) : le jugement d'une action ne contourne donc rien, et un
        // obstacle qui demeure laisse le dossier RESOLU — l'ecran le nomme
        // avant le clic, comme il le faisait deja.
        if (CaseSettlement.settlesCase(verdict, c.getStatus() == CapaStatus.RESOLVED,
                () -> closureBlockers(c).isEmpty())) {
            c.setEffectivenessVerified(true);
            c.setEffectivenessVerifiedAt(now);
            c.setStatus(CapaStatus.CLOSED);
            c.setClosedAt(now);
            journal.record(caseRepository.save(c), CapaTransition.CLOSED);
        }
        return toActionResponse(a, review);
    }

    /**
     * QUI PEUT PORTER LE VERDICT D'UNE ACTION (ADR 0143).
     *
     * <p>Le droit {@code capa.verify} l'accorde, comme avant. A defaut, la
     * PERSONNE DESIGNEE sur l'action — a defaut celle du dossier — le porte sur
     * cette action-la, et sur aucune autre : c'est a elle que la verification a
     * ete confiee, et un role qui ne porte pas l'action du catalogue ne doit pas
     * lui retirer ce qu'on lui a demande de faire.
     *
     * <p>La portee est lue avec {@code capa.verify} elle-meme : vide, l'appelant
     * a le droit ; presente, elle designe l'appelant du jeton, et c'est a lui
     * que la designation doit correspondre. Le {@code sub} du jeton, jamais un
     * champ du corps (CLAUDE.md §18.2 #2).
     */
    private void exigerLeDroitDeJuger(CapaCase c, CapaAction a) {
        Optional<UUID> restreinteA = scope.restrictTo(Permission.CAPA_VERIFY);
        if (restreinteA.isEmpty()) {
            return;
        }
        UUID designe = a.getVerificationAssigneeId() != null
                ? a.getVerificationAssigneeId()
                : c.getVerificationAssigneeId();
        if (designe == null || !designe.equals(restreinteA.get())) {
            throw new AccessDeniedException(
                    "Only the designated verifier of this action may judge its effectiveness");
        }
    }

    public void deleteAction(UUID capaId, UUID actionId) {
        CapaCase c = loadCase(capaId);
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.REJECTED) {
            throw new CapaStateException("Cannot delete actions on a " + c.getStatus() + " CAPA");
        }
        CapaAction a = actionRepository.findByIdAndCapaId(actionId, capaId)
                .orElseThrow(() -> new CapaActionNotFoundException(actionId));
        // La contrainte de base efface en cascade la pièce rattachée à l'action,
        // mais pas le binaire dans le stockage objet — il resterait orphelin,
        // invisible et facturé. Surtout, une preuve d'audit disparaîtrait sans
        // que rien ne le consigne. Refus explicite : on retire la preuve
        // d'abord, ce qui laisse une trace (§11.5), puis l'action.
        if (evidenceRepository.countByTenantIdAndActionId(c.getTenantId(), actionId) > 0) {
            throw new CapaStateException(
                    "This action carries an evidence file: remove it before deleting the action");
        }
        actionRepository.delete(a);
    }

    // --- helpers ---

    /** Borne du libellé, alignée sur la colonne (255) — refusée, jamais tronquée en silence. */
    private static final int ACTION_TITLE_MAX = 255;

    /**
     * Un libellé d'action doit rester lisible : ni vide, ni fait d'espaces, ni
     * plus long que sa colonne. Tronquer serait pire que refuser — l'utilisateur
     * croirait avoir enregistré ce qu'il a tapé.
     */
    private static String requireUsableTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new CapaValidationException("Action title must not be blank");
        }
        String trimmed = title.strip();
        if (trimmed.length() > ACTION_TITLE_MAX) {
            throw new CapaValidationException(
                    "Action title exceeds " + ACTION_TITLE_MAX + " characters");
        }
        return trimmed;
    }

    /**
     * Nom du porteur : mis à blanc plutôt que conservé vide, pour que l'écran
     * ait une seule chose à tester. Une chaîne d'espaces afficherait une colonne
     * « Responsable » vide sans le repère « — » qui dit qu'elle l'est.
     */
    private static String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > ACTION_TITLE_MAX) {
            throw new CapaValidationException(
                    "Assignee name exceeds " + ACTION_TITLE_MAX + " characters");
        }
        return trimmed;
    }

    /**
     * Écart d'origine du dossier, s'il y en a un.
     *
     * <p>Deux chemins, dans cet ordre : le lien réel porté par la NC
     * ({@code capa_case_id}, posé à l'escalade), puis à défaut la référence
     * saisie à la main dans le dossier. Le second n'est qu'un repli — une
     * référence tapée peut désigner un écart inexistant, auquel cas on ne
     * montre rien plutôt qu'un nom inventé.
     */
    private CapaDto.LinkedNonConformity resolveSourceNc(CapaCase c) {
        var linked = ncRepository.findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(
                c.getTenantId(), c.getId());
        if (linked.isEmpty() && estEcart(c.getSourceType())
                && c.getSourceRef() != null && !c.getSourceRef().isBlank()) {
            linked = ncRepository.findByTenantIdAndReference(c.getTenantId(), c.getSourceRef().strip());
        }
        return linked
                .map(nc -> new CapaDto.LinkedNonConformity(nc.getId(), nc.getReference(), nc.getTitle()))
                .orElse(null);
    }

    /** Les deux sources qui désignent un écart du registre : interne et externe. */
    private static boolean estEcart(CapaSourceType source) {
        return source == CapaSourceType.NON_CONFORMITY
                || source == CapaSourceType.EXTERNAL_NON_CONFORMITY;
    }

    /**
     * Les sources dont la référence peut NOMMER un écart du registre.
     *
     * <p>{@code COMPLAINT} s'y ajoute : la fenêtre de création fait choisir, pour
     * « réclamation client », un écart EXTERNE encore ouvert — c'est là que les
     * réclamations sont instruites, le registre des réclamations n'ayant aucun
     * écran de saisie. Sans cela le dossier restait orphelin : la fiche de
     * l'écart n'y menait pas, son rapport 8D ne le voyait pas, et sa clôture ne
     * l'attendait pas.
     *
     * <p>Plus large que {@link #estEcart}, qui dit ce qu'un dossier EST une fois
     * sa source arrêtée : une référence qui ne désigne aucun écart laisse la
     * source telle qu'elle a été choisie.
     */
    private static boolean citeUnEcart(CapaSourceType source) {
        return estEcart(source) || source == CapaSourceType.COMPLAINT;
    }

    private CapaCase loadCase(UUID id) {
        UUID tenantId = requireTenantId();
        // Hors de portée, le dossier n'existe pas : ni lecture ni action (ADR 0081).
        Optional<UUID> seulement = CapaScope.restriction(scope);
        return caseRepository.findByIdAndTenantId(id, tenantId)
                .filter(c -> CapaScope.sees(seulement, c))
                .orElseThrow(() -> new CapaNotFoundException(id));
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private CapaDto.CaseResponse toResponse(CapaCase c) {
        return toResponse(c, true);
    }

    /**
     * @param detailed résout l'écart d'origine, les motifs de blocage ET le
     *        dernier verdict d'efficacité — soit une requête de plus par
     *        dossier chacun. La liste paginée s'en passe : vingt dossiers
     *        vaudraient soixante requêtes pour des informations que la liste
     *        n'affiche pas. Le plan de travail de l'efficacité, lui, les obtient
     *        en une seule requête pour tout le tableau (voir
     *        {@code EffectivenessReviewPort#findLastPerCase}).
     */
    private CapaDto.CaseResponse toResponse(CapaCase c, boolean detailed) {
        return new CapaDto.CaseResponse(
                c.getId(), c.getTenantId(), c.getTitle(), c.getDescription(),
                c.getType(), c.getCriticity(), c.getStatus(),
                c.getSourceType(), c.getSourceRef(), c.getOwnerId(), c.getOwnerName(),
                c.getRootCauseId(), c.getDueDate(),
                c.getResolvedAt(), c.getClosedAt(),
                c.getEffectivenessVerified(), c.getEffectivenessVerifiedAt(),
                c.getEffectivenessCriterion(), c.getEffectivenessComment(),
                detailed ? lastVerdict(c) : null,
                c.getVerificationRequired(), c.getVerificationAssigneeId(),
                c.getVerificationAssigneeName(), c.getVerificationInstructions(),
                c.getCreatedAt(), c.getUpdatedAt(),
                toActionResponses(c),
                detailed ? resolveSourceNc(c) : null,
                detailed ? closureBlockers(c) : null);
    }

    /**
     * Les actions du dossier, chacune avec son dernier verdict, en UNE requete
     * d'historique pour tout le dossier (ADR 0132).
     *
     * <p>Un dossier de huit actions se projetterait sinon en huit requetes, et
     * la fiche d'un ecart en affiche trois dossiers.
     */
    private List<CapaDto.ActionResponse> toActionResponses(CapaCase c) {
        List<CapaAction> actions = c.getActions();
        if (actions.isEmpty()) {
            return List.of();
        }
        Map<UUID, EffectivenessReview> jugements =
                effectivenessReviews.findLastPerActionOfCase(c.getTenantId(), c.getId());
        return actions.stream()
                .map(a -> toActionResponse(a, jugements.get(a.getId())))
                .toList();
    }

    /**
     * Les actions de ce dossier qui RETIENNENT sa cloture, chacune avec ce
     * qu'elle attend.
     *
     * <p>Le decompte d'un motif de blocage dit combien de dossiers resistent ;
     * il ne dit pas ce qu'il faut aller faire. Un dossier porte dix actions, et
     * la personne devait les ouvrir une a une pour trouver celle qu'on attend
     * -- un travail d'instruction rendu a l'utilisateur alors que le serveur
     * tient deja la reponse.
     *
     * <p>Les DEUX MEMES regles que {@link #closureBlockers}, lues ici action par
     * action au lieu d'etre comptees : pas {@code DONE}, ou faite sans que son
     * effet soit verifie (ADR 0132). Deux lectures de la meme regle finiraient
     * par diverger ; celles-ci partagent leurs predicats.
     *
     * <p>Un dossier REGLE -- clos ou ecarte -- ne retient plus rien : la liste
     * est vide sans interroger les jugements d'efficacite.
     */
    private List<CapaDto.BlockingAction> actionsQuiBloquent(CapaCase c) {
        if (CAPA_SETTLED_STATUSES.contains(c.getStatus())) {
            return List.of();
        }
        List<CapaAction> actions = c.getActions();
        if (actions.isEmpty()) {
            return List.of();
        }
        List<CapaAction> aFaire = actionsNonExecutees(c);
        List<CapaAction> aVerifier = actionsNonVerifiees(c, actions).stream()
                .filter(a -> a.getStatus() == CapaActionStatus.DONE)
                .toList();
        return Stream.concat(
                        aFaire.stream().map(a -> new CapaDto.BlockingAction(
                                a.getId(), a.getTitle(), CapaDto.BlockingReason.TO_DO)),
                        aVerifier.stream().map(a -> new CapaDto.BlockingAction(
                                a.getId(), a.getTitle(), CapaDto.BlockingReason.TO_VERIFY)))
                .toList();
    }

    /**
     * Les actions de ce dossier qui restent A EXECUTER : pas encore {@code DONE}.
     *
     * <p>La seule lecture qui vaille pour la RESOLUTION d'un ecart — « ce qui
     * devait etre fait l'a-t-il ete ? » — et le volet {@code TO_DO} de la
     * cloture. Une seule methode pour les deux, parce que deux lectures de la
     * meme question finiraient par diverger.
     *
     * <p>Un dossier REGLE — clos ou ecarte — ne retient plus rien : ce qui est
     * clos l'a ete sur l'etat d'alors.
     */
    private List<CapaAction> actionsNonExecutees(CapaCase c) {
        if (CAPA_SETTLED_STATUSES.contains(c.getStatus())) {
            return List.of();
        }
        return c.getActions().stream()
                .filter(a -> a.getStatus() != CapaActionStatus.DONE)
                .toList();
    }

    /**
     * Les actions qui ont PROMIS une verification et ne l'ont pas obtenue.
     *
     * <p>Trois cas comptent comme non verifiees : jamais jugee, jugee PARTIEL,
     * jugee INEFFICACE. Les deux derniers ont ete regardes, et ce qu'on a vu ne
     * permet pas de clore -- c'est exactement ce que le verdict du dossier dit
     * deja de lui-meme (ADR 0108).
     *
     * <p>Une action qui n'a rien promis n'est pas comptee : son statut DONE
     * suffit, et c'est la l'objet du code ACTIONS_NOT_DONE.
     */
    private long verificationsDActionEnAttente(CapaCase c) {
        return actionsNonVerifiees(c, c.getActions()).size();
    }

    /**
     * Les memes, NOMMEES : la liste dont {@link #verificationsDActionEnAttente}
     * ne rend que la taille, et dont {@link #actionsQuiBloquent} a besoin pour
     * dire laquelle on attend.
     */
    private List<CapaAction> actionsNonVerifiees(CapaCase c, List<CapaAction> actions) {
        List<CapaAction> promises = actions.stream()
                .filter(a -> Boolean.TRUE.equals(a.getVerificationRequired()))
                .toList();
        if (promises.isEmpty()) {
            return List.of();
        }
        Map<UUID, EffectivenessReview> jugements =
                effectivenessReviews.findLastPerActionOfCase(c.getTenantId(), c.getId());
        return promises.stream()
                .filter(a -> {
                    EffectivenessReview dernier = jugements.get(a.getId());
                    return dernier == null || !dernier.verdict().closesCase();
                })
                .toList();
    }

    /**
     * Le dernier verdict d'efficacité du dossier, ou {@code null} s'il n'a
     * jamais été évalué (ADR 0108).
     *
     * <p>Lu depuis l'historique plutôt que recopié sur le dossier. Une colonne
     * de plus sur {@code capa_cases} serait plus rapide, et se désynchroniserait
     * le premier jour où une évaluation serait enregistrée par un autre chemin :
     * il y aurait alors deux réponses à la même question, dont une fausse.
     */
    private EffectivenessVerdict lastVerdict(CapaCase c) {
        return effectivenessReviews.findLastByCase(c.getTenantId(), c.getId())
                .map(EffectivenessReview::verdict)
                .orElse(null);
    }

    /**
     * L'évaluation d'efficacité D'UNE ACTION : son critère, et la décision de
     * vérifier son effet (ADR 0131).
     *
     * <p><b>Pourquoi sur l'action.</b> Le dossier porte les siennes, posées à
     * l'ouverture, et les garde : ce sont elles qui gouvernent sa clôture
     * (ADR 0073) et son verdict (ADR 0108). Mais un dossier porte plusieurs
     * actions de natures différentes (ADR 0115), et « recalibrer la presse » ne
     * se mesure pas à la même chose que « former les opérateurs de nuit ». La
     * question n'étant posée qu'une fois, les actions suivantes s'écrivaient
     * sans qu'on sache jamais si leur effet serait vérifié.
     *
     * <p>Trois refus, les mêmes que sur le dossier :
     * <ul>
     *   <li>un ENDIGUEMENT ne promet aucun effet et n'a donc rien à mesurer
     *       (ISO 9001 §10.2) : critère et vérification y sont refusés, au lieu
     *       d'être acceptés puis ignorés ;</li>
     *   <li>désigner un vérificateur ou écrire des consignes suppose que la
     *       vérification est EXIGÉE ;</li>
     *   <li>une vérification exigée doit être confiée à quelqu'un — sans nom,
     *       elle n'aurait lieu que si quelqu'un y pense.</li>
     * </ul>
     *
     * <p>Le critère, lui, se VERROUILLE dès qu'il est posé : le réécrire plus
     * tard, c'est déplacer la cible entre deux mesures (ADR 0108). Le même
     * texte réécrit à l'identique passe, lui : un PATCH qui renvoie le champ
     * inchangé ne change rien et n'a pas à être refusé.
     */
    private void appliquerEfficaciteDeLAction(CapaAction a, CapaDto.ActionRequest request) {
        boolean endiguement = a.getActionType() == CapaActionType.CONTAINMENT;

        if (endiguement
                && (nonVide(request.effectivenessCriterion())
                    || Boolean.TRUE.equals(request.verificationRequired())
                    || request.verificationAssigneeId() != null
                    || nonVide(request.verificationInstructions()))) {
            throw new CapaValidationException(
                    "A containment action has no effect to measure: "
                            + "no effectiveness criterion, no verification");
        }

        String critere = nettoyer(request.effectivenessCriterion());
        if (critere != null) {
            String pose = a.getEffectivenessCriterion();
            if (pose != null && !pose.equals(critere)) {
                throw new CapaValidationException(
                        "The effectiveness criterion of an action cannot be rewritten");
            }
            a.setEffectivenessCriterion(critere);
        }

        if (Boolean.FALSE.equals(request.verificationRequired())) {
            a.setVerificationRequired(false);
            a.setVerificationAssigneeId(null);
            a.setVerificationAssigneeName(null);
            a.setVerificationInstructions(null);
            return;
        }

        boolean exigeeApres = Boolean.TRUE.equals(request.verificationRequired())
                || (request.verificationRequired() == null
                    && Boolean.TRUE.equals(a.getVerificationRequired()));

        if (!exigeeApres
                && (request.verificationAssigneeId() != null
                    || nonVide(request.verificationInstructions()))) {
            throw new CapaValidationException(
                    "A verifier or verification instructions require verificationRequired=true");
        }

        if (Boolean.TRUE.equals(request.verificationRequired())) {
            a.setVerificationRequired(true);
        }
        if (request.verificationAssigneeId() != null) {
            a.setVerificationAssigneeId(request.verificationAssigneeId());
        }
        if (request.verificationAssigneeName() != null) {
            a.setVerificationAssigneeName(nettoyer(request.verificationAssigneeName()));
        }
        if (request.verificationInstructions() != null) {
            a.setVerificationInstructions(nettoyer(request.verificationInstructions()));
        }

        if (exigeeApres && a.getVerificationAssigneeId() == null) {
            throw new CapaValidationException(
                    "A required verification must be assigned to someone");
        }
    }

    private CapaDto.ActionResponse toActionResponse(CapaAction a) {
        // Sans l'historique sous la main, on le lit pour cette seule action :
        // c'est le cas d'un ajout ou d'une mise a jour, ou l'appelant rend UNE
        // action et non un dossier entier.
        return toActionResponse(a, derniereEvaluationDe(a));
    }

    /**
     * @param jugement la derniere evaluation de cette action, ou {@code null}
     *        si elle n'a jamais ete jugee. Passe en argument, et non relu ici :
     *        un dossier de huit actions se projetterait en huit requetes.
     */
    private CapaDto.ActionResponse toActionResponse(CapaAction a, EffectivenessReview jugement) {
        return new CapaDto.ActionResponse(
                a.getId(), a.getCapa().getId(), a.getTitle(), a.getDescription(),
                a.getStatus(), a.getActionType(), a.getAssigneeId(), a.getAssigneeName(),
                a.getDecidedOn(), a.getDueDate(), a.getCompletedAt(),
                a.getEffectivenessCriterion(), a.getVerificationRequired(),
                a.getVerificationAssigneeId(), a.getVerificationAssigneeName(),
                a.getVerificationInstructions(),
                jugement == null ? null : jugement.verdict(),
                jugement == null ? null : jugement.reviewedAt(),
                jugement == null ? null : jugement.reviewerName(),
                a.getCreatedAt(), a.getUpdatedAt());
    }

    /** La derniere evaluation d'UNE action, pour la rendre seule. */
    private EffectivenessReview derniereEvaluationDe(CapaAction a) {
        return effectivenessReviews.findByAction(a.getCapa().getTenantId(), a.getId())
                .stream().findFirst().orElse(null);
    }

    /**
     * Énumère ce qui s'oppose encore à la clôture — avant le clic, pas après.
     *
     * <p>La liste couvre TOUT le chemin restant, y compris ce qui bloque la
     * résolution qui précède la clôture : l'utilisateur veut savoir ce qui le
     * sépare de la fin, pas ce qui bloque l'étape suivante prise isolément.
     *
     * <p>Un dossier déjà clos ou rejeté ne renvoie rien : plus rien ne s'y
     * oppose, et afficher des obstacles sur un dossier terminé donnerait à
     * croire qu'il faudrait encore agir.
     */
    private List<CapaDto.ClosureBlocker> closureBlockers(CapaCase c) {
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.REJECTED) {
            return List.of();
        }
        List<CapaDto.ClosureBlocker> blockers = new ArrayList<>();
        List<CapaAction> actions = c.getActions();

        if (actions.isEmpty()) {
            blockers.add(new CapaDto.ClosureBlocker(ClosureBlockerCode.NO_ACTION, 0));
        } else {
            long notDone = actions.stream().filter(a -> a.getStatus() != CapaActionStatus.DONE).count();
            if (notDone > 0) {
                blockers.add(new CapaDto.ClosureBlocker(ClosureBlockerCode.ACTIONS_NOT_DONE, notDone));
            }
            // SEULEMENT SUR LE CHEMIN DU VERDICT (ADR 0137). « N'a fait
            // qu'endiguer » s'oppose a la declaration d'une efficacite
            // demontree, pas a la fermeture elle-meme : depuis l'ADR 0135 le
            // dossier d'endiguement ne contient que de l'endiguement, et
            // l'annoncer comme obstacle eteignait le seul bouton qui lui
            // restait -- il ne se fermait alors jamais.
            if (isContainmentOnly(actions) && verificationPromise(c)) {
                blockers.add(new CapaDto.ClosureBlocker(
                        ClosureBlockerCode.CONTAINMENT_ONLY, actions.size()));
            }
        }

        long stillOpen = ncRepository.countByTenantIdAndCapaCaseIdAndStatusNotIn(
                c.getTenantId(), c.getId(), NC_SETTLED_STATUSES);
        if (stillOpen > 0) {
            blockers.add(new CapaDto.ClosureBlocker(
                    ClosureBlockerCode.OPEN_NON_CONFORMITIES, stillOpen));
        }
        // Les verifications promises action par action (ADR 0132). Une action
        // « faite » dit que le geste a eu lieu, pas qu'il a produit son effet :
        // clore sans cela inscrirait au registre une cause traitee sur la foi
        // d'une promesse que personne n'est alle controler.
        long aVerifier = verificationsDActionEnAttente(c);
        if (aVerifier > 0) {
            blockers.add(new CapaDto.ClosureBlocker(
                    ClosureBlockerCode.ACTION_VERIFICATIONS_PENDING, aVerifier));
        }
        // Le retour du KPI sous la cible, pour un dossier ne d'un ecart
        // d'objectif qualite (ADR 0133). ANNONCE ici et TENU dans
        // verifyEffectiveness : une liste qui n'engage pas le serveur ne vaut
        // rien, et un refus que l'ecran n'a pas annonce surprend au clic.
        //
        // Le decompte est le nombre de periodes qu'il RESTE a tenir : c'est ce
        // que le verificateur a besoin de savoir, et non le nombre deja acquis.
        objectiveRecovery.forCase(c.getTenantId(), c.getId())
                .filter(r -> !r.achieved())
                .ifPresent(r -> blockers.add(new CapaDto.ClosureBlocker(
                        ClosureBlockerCode.OBJECTIVE_RECOVERY_PENDING,
                        Math.max(0, r.requiredPeriods() - r.onTargetPeriods()))));
        return List.copyOf(blockers);
    }

    /**
     * Vrai quand le dossier ne porte QUE des mesures d'endiguement.
     *
     * <p>Un dossier sans action ne relève pas de ce cas : il a son propre motif
     * ({@link ClosureBlockerCode#NO_ACTION}), et un {@code allMatch} sur une
     * liste vide répondrait « oui » à une question qui ne se pose pas.
     */
    static boolean isContainmentOnly(List<CapaAction> actions) {
        return !actions.isEmpty()
                && actions.stream().allMatch(a -> a.getActionType() == CapaActionType.CONTAINMENT);
    }
}
