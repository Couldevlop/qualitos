package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaEvidence;
import com.openlab.qualitos.quality.capa.CapaEvidenceRepository;
import com.openlab.qualitos.quality.capa.effectiveness.application.EffectivenessReviewPort;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview;
import com.openlab.qualitos.quality.derogation.application.DirectoryPort;
import com.openlab.qualitos.quality.derogation.application.DirectoryUnavailableException;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlan;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanRepository;
import com.openlab.qualitos.quality.fivewhys.FiveWhysAnalysis;
import com.openlab.qualitos.quality.fivewhys.FiveWhysAnalysisRepository;
import com.openlab.qualitos.quality.fivewhys.FiveWhysStep;
import com.openlab.qualitos.quality.fivewhys.FiveWhysStepRepository;
import com.openlab.qualitos.quality.ishikawa.IshikawaCause;
import com.openlab.qualitos.quality.ishikawa.IshikawaDiagram;
import com.openlab.qualitos.quality.ishikawa.IshikawaDiagramRepository;
import com.openlab.qualitos.quality.nonconformity.NcNotFoundException;
import com.openlab.qualitos.quality.nonconformity.NcPhotoRepository;
import com.openlab.qualitos.quality.nonconformity.NcTeamMember;
import com.openlab.qualitos.quality.nonconformity.NcTeamMemberRepository;
import com.openlab.qualitos.quality.nonconformity.NcScope;
import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSourcePort;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSources;
import com.openlab.qualitos.quality.product.domain.ProductLookup;
import com.openlab.qualitos.quality.risk.FmeaItem;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rassemble, pour une non-conformité, ce que les autres modules savent déjà.
 *
 * <p>C'est l'adaptateur qui connaît les chemins de liaison — et ils ne se
 * ressemblent pas : l'Ishikawa porte un {@code ncId}, les 5 pourquoi une
 * association JPA, la CAPA est désignée par la NC, le mode de défaillance PFMEA
 * aussi, et les plans de surveillance se trouvent par le PRODUIT. Le cas d'usage
 * ignore tout cela, et c'est pour ça qu'il se teste sans base.
 *
 * <p>Lecture seule et dans une seule transaction : les collections paresseuses
 * (causes d'un Ishikawa, actions d'une CAPA) doivent être parcourues tant que la
 * session est ouverte.
 *
 * <p>Toutes les requêtes portent le tenant. Une non-conformité d'un autre tenant
 * est « introuvable », sans nuance : distinguer les deux cas ferait de ce point
 * d'entrée un oracle d'existence (OWASP A01).
 */
@Component
public class EightDSourceAdapter implements EightDSourcePort {

    private final NonConformityRepository ncs;
    private final NcPhotoRepository photos;
    /** L'equipe nommee sur la fiche : la source de D1. */
    private final NcTeamMemberRepository teamMembers;
    /**
     * L'annuaire, pour l'adresse de chaque membre du D1.
     *
     * <p>La fiche ne retient que le nom et le compte : l'adresse se relit ici,
     * et se fige comme le reste a l'emission. Une panne de l'annuaire ne doit
     * PAS empecher d'assembler un rapport -- l'adresse est une commodite, le
     * rapport est une preuve -- d'ou la case laissee vide plutot que l'erreur.
     */
    private final DirectoryPort annuaire;
    /**
     * Les verdicts d'efficacite portes action par action (ADR 0131).
     *
     * <p>Le rapport disait qu'un geste avait ete fait sans dire s'il avait
     * servi : l'evaluation vivait dans la CAPA et n'arrivait pas jusqu'au
     * document remis au client.
     */
    private final EffectivenessReviewPort efficacite;
    private final IshikawaDiagramRepository ishikawas;
    private final FiveWhysAnalysisRepository fiveWhys;
    private final FiveWhysStepRepository fiveWhysSteps;
    private final CapaCaseRepository capas;
    private final CapaEvidenceRepository capaEvidences;
    private final FmeaItemRepository fmeaItems;
    private final ControlPlanRepository controlPlans;
    /**
     * Le produit visé, pour l'écrire au D2 par son code et sa désignation.
     *
     * <p>{@link ProductLookup} et non le dépôt complet : ce rapport lit, il
     * n'écrit ni ne supprime de produit, et hériter du droit de le faire
     * laisserait croire à un futur lecteur que c'est permis ici.
     */
    private final ProductLookup produits;
    /** Le 8D d'une NC hors de portée n'existe pas (ADR 0081). */
    private final RecordScope scope;

    public EightDSourceAdapter(NonConformityRepository ncs,
                               NcPhotoRepository photos,
                               NcTeamMemberRepository teamMembers,
                               DirectoryPort annuaire,
                               EffectivenessReviewPort efficacite,
                               IshikawaDiagramRepository ishikawas,
                               FiveWhysAnalysisRepository fiveWhys,
                               FiveWhysStepRepository fiveWhysSteps,
                               CapaCaseRepository capas,
                               CapaEvidenceRepository capaEvidences,
                               FmeaItemRepository fmeaItems,
                               ControlPlanRepository controlPlans,
                               ProductLookup produits,
                               RecordScope scope) {
        this.ncs = ncs;
        this.photos = photos;
        this.teamMembers = teamMembers;
        this.annuaire = annuaire;
        this.efficacite = efficacite;
        this.ishikawas = ishikawas;
        this.fiveWhys = fiveWhys;
        this.fiveWhysSteps = fiveWhysSteps;
        this.capas = capas;
        this.capaEvidences = capaEvidences;
        this.fmeaItems = fmeaItems;
        this.controlPlans = controlPlans;
        this.produits = produits;
        this.scope = scope;
    }

    @Override
    @Transactional(readOnly = true)
    public EightDSources collect(UUID tenantId, UUID ncId) {
        NonConformity nc = ncs.findByIdAndTenantId(ncId, tenantId)
                .filter(n -> NcScope.sees(NcScope.restriction(scope), n))
                .orElseThrow(() -> new NcNotFoundException(ncId));

        return new EightDSources(
                enNc(tenantId, nc),
                enEquipe(tenantId, ncId),
                enIshikawas(tenantId, ncId),
                enFiveWhys(tenantId, ncId),
                enCapas(tenantId, nc),
                enFmea(tenantId, nc),
                enSurveillance(tenantId, nc));
    }

    /**
     * L'équipe nommée sur la fiche, dans son ordre de saisie (D1).
     *
     * <p>LE NOM FIGE D'ABORD, l'annuaire ENSUITE. Le nom retenu à la saisie
     * prime : un rapport 8D relu trois ans plus tard porte les noms de
     * l'époque, et un membre renommé depuis ne réécrit pas un document émis.
     *
     * <p>Mais ce qui a été figé n'est pas toujours un nom : les écarts ouverts
     * avant que le pilote soit amorcé depuis l'annuaire portent l'ADRESSE de
     * celui qui les a saisis, et le D1 affichait donc
     * « waopron.coulibaly@qualitos.fr » sous « Nom et prénom ». Quand le nom
     * figé est une adresse — ou manque —, on va donc chercher le vrai nom dans
     * l'annuaire : mieux vaut le nom d'aujourd'hui que l'adresse de toujours.
     *
     * <p>La FONCTION, elle, se relit toujours : elle n'est figée nulle part, et
     * c'est une coordonnée d'organisation, pas une trace de décision.
     */
    private List<EightDSources.Member> enEquipe(UUID tenantId, UUID ncId) {
        List<EightDSources.Member> membres = new ArrayList<>();
        for (NcTeamMember membre : teamMembers.findByTenantIdAndNcIdOrderByPositionAsc(
                tenantId, ncId)) {
            UUID compte = compteDe(membre.getAccountId());
            String fige = membre.getDisplayName();
            String nom = estUnNom(fige) ? fige : nomAnnuaire(compte, fige);
            membres.add(new EightDSources.Member(
                    nom, adresseDe(membre.getAccountId()), fonctionDe(compte)));
        }
        return membres;
    }

    /**
     * Vrai quand ce qu'on a figé ressemble à un nom, et non à une adresse.
     *
     * <p>L'arobase suffit à les distinguer : aucun nom de personne n'en porte,
     * et toute adresse en porte une. Une règle plus savante se tromperait plus
     * souvent qu'elle ne rattraperait de cas.
     */
    private static boolean estUnNom(String fige) {
        return fige != null && !fige.isBlank() && !fige.contains("@");
    }

    /** Le nom de l'annuaire, ou ce qu'on avait figé si l'annuaire ne sait rien. */
    private String nomAnnuaire(UUID compte, String defaut) {
        if (compte == null) {
            return defaut;
        }
        try {
            return annuaire.displayNameOf(compte).filter(EightDSourceAdapter::estUnNom)
                    .orElse(defaut);
        } catch (DirectoryUnavailableException injoignable) {
            return defaut;
        }
    }

    /** La fonction du compte, ou {@code null} : ni l'absence ni la panne ne lèvent ici. */
    private String fonctionDe(UUID compte) {
        if (compte == null) {
            return null;
        }
        try {
            return annuaire.fonctionOf(compte).orElse(null);
        } catch (DirectoryUnavailableException injoignable) {
            return null;
        }
    }

    /** L'identifiant de compte, ou {@code null} quand ce n'en est pas un. */
    private static UUID compteDe(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(accountId);
        } catch (IllegalArgumentException pasUnUuid) {
            return null;
        }
    }

    /** L'adresse du compte, ou {@code null} : ni l'absence ni la panne ne lèvent ici. */
    private String adresseDe(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return null;
        }
        try {
            return annuaire.emailOf(UUID.fromString(accountId)).orElse(null);
        } catch (IllegalArgumentException pasUnUuid) {
            return null;
        } catch (DirectoryUnavailableException injoignable) {
            return null;
        }
    }

    private EightDSources.Nc enNc(UUID tenantId, NonConformity nc) {
        int nbPhotos = photos.findByTenantIdAndNcIdOrderByCreatedAtAsc(tenantId, nc.getId()).size();
        return new EightDSources.Nc(
                nc.getReference(), nc.getTitle(), nc.getDescription(),
                nom(nc.getCategory()), nom(nc.getSeverity()), nom(nc.getOrigin()),
                nc.getStatus(), nc.getDetectedAt(), nc.getClosedAt(),
                nc.getZone(), nc.getReporterName(), nbPhotos,
                nc.getRootCause(), nc.getResolutionNote(),
                nc.getCauseAnalysisComment(),
                nc.getCustomerName(), nc.getBatchNumber(), libelleProduit(nc.getProductId()));
    }

    /**
     * Le produit visé, écrit comme la fiche l'écrit : « code — désignation ».
     *
     * <p>Un identifiant ne dit rien au lecteur du rapport, et un produit effacé
     * du référentiel depuis ne laisse pas de ligne : mieux vaut taire le produit
     * que montrer un UUID.
     */
    private String libelleProduit(UUID productId) {
        if (productId == null) {
            return null;
        }
        return produits.findById(productId)
                .map(p -> p.getCode() + " — " + p.getDesignation())
                .orElse(null);
    }

    private List<EightDSources.CauseTree> enIshikawas(UUID tenantId, UUID ncId) {
        List<EightDSources.CauseTree> arbres = new ArrayList<>();
        for (IshikawaDiagram diagram : ishikawas.findByTenantIdAndNcIdOrderByCreatedAtDesc(tenantId, ncId)) {
            List<EightDSources.Cause> causes = new ArrayList<>();
            for (IshikawaCause cause : diagram.getCauses()) {
                causes.add(new EightDSources.Cause(
                        nom(cause.getCategory()), cause.getLabel(), cause.getDescription(),
                        cause.getRootCauseScore()));
            }
            arbres.add(new EightDSources.CauseTree(
                    diagram.getProblemStatement(), nom(diagram.getStatus()), causes));
        }
        return arbres;
    }

    private List<EightDSources.WhysChain> enFiveWhys(UUID tenantId, UUID ncId) {
        List<EightDSources.WhysChain> chaines = new ArrayList<>();
        for (FiveWhysAnalysis analyse
                : fiveWhys.findByNonConformityIdAndTenantIdOrderByCreatedAtDesc(ncId, tenantId)) {
            List<String> reponses = new ArrayList<>();
            for (FiveWhysStep etape
                    : fiveWhysSteps.findByAnalysisIdAndTenantIdOrderByPositionAsc(analyse.getId(), tenantId)) {
                reponses.add(etape.getAnswer());
            }
            chaines.add(new EightDSources.WhysChain(
                    analyse.getProblem(), analyse.getRootCause(), reponses));
        }
        return chaines;
    }

    /**
     * TOUS les dossiers qui traitent cet écart, du plus ancien au plus récent.
     *
     * <p>Deux chemins mènent à un dossier, et il faut les deux : le lien porté
     * par la fiche ({@code capa_case_id}, posé par le premier dossier ouvert
     * depuis l'écart) et la RÉFÉRENCE citée en source par les dossiers ouverts
     * depuis la liste — un second dossier ne remplace pas le lien du premier
     * (ADR 0121). C'est exactement l'ensemble que la clôture de l'écart attend.
     * N'en lire qu'un faisait disparaître du rapport remis au client des actions
     * réellement menées, et le D5 contredisait alors le refus de clôture.
     *
     * <p>Les deux natures de source sont interrogées : un constat interne et une
     * réclamation client portent la même référence d'écart, pas le même type.
     *
     * <p><b>Pas de portée CAPA ici, et c'est délibéré.</b> Le droit de lire ce
     * rapport se décide sur l'ÉCART, en amont ({@code NcScope} — un écart hors
     * de portée est « introuvable » et rien n'est collecté). Filtrer ensuite les
     * dossiers lecteur par lecteur donnerait DEUX rapports différents pour le
     * même écart : l'instantané est signé et son empreinte ancrée, un contenu
     * qui dépend de qui le demande ne se vérifie plus. Un 8D est le document
     * de l'écart, pas la vue personnelle de celui qui l'imprime.
     */
    private List<EightDSources.Capa> enCapas(UUID tenantId, NonConformity nc) {
        Map<UUID, CapaCase> dossiers = new LinkedHashMap<>();
        if (nc.getCapaCaseId() != null) {
            // Lien rompu (dossier supprimé) : traité comme une absence, pas comme
            // une erreur — le rapport doit rester émissible, et il le dira.
            capas.findByIdAndTenantId(nc.getCapaCaseId(), tenantId)
                    .ifPresent(c -> dossiers.put(c.getId(), c));
        }
        if (nc.getReference() != null && !nc.getReference().isBlank()) {
            for (CapaSourceType origine : List.of(CapaSourceType.NON_CONFORMITY,
                                                  CapaSourceType.EXTERNAL_NON_CONFORMITY)) {
                for (CapaCase c : capas.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                        tenantId, origine, nc.getReference().strip())) {
                    dossiers.putIfAbsent(c.getId(), c);
                }
            }
        }
        List<EightDSources.Capa> sources = new ArrayList<>();
        for (CapaCase capa : dossiers.values()) {
            sources.add(enCapa(tenantId, capa));
        }
        return sources;
    }

    private EightDSources.Capa enCapa(UUID tenantId, CapaCase capa) {
        // LES PIECES EN UNE PASSE, et non une requete par action : le D6 les
        // nomme toutes, et un dossier de vingt actions aurait fait vingt
        // allers-retours pour lire vingt noms de fichiers.
        Map<UUID, List<String>> parAction = new LinkedHashMap<>();
        for (CapaEvidence piece : capaEvidences.findActionLevel(tenantId, capa.getId())) {
            parAction.computeIfAbsent(piece.getActionId(), k -> new ArrayList<>())
                    .add(nomDe(piece));
        }
        List<String> duDossier = new ArrayList<>();
        for (CapaEvidence piece : capaEvidences.findCaseLevel(tenantId, capa.getId())) {
            duDossier.add(nomDe(piece));
        }
        Map<UUID, EffectivenessReview> verdicts =
                efficacite.findLastPerActionOfCase(tenantId, capa.getId());
        // LE VERDICT DU DOSSIER VAUT POUR SON ACTION UNIQUE (ADR 0143). Un
        // jugement porte depuis l'ecran d'efficacite est une evaluation DU
        // DOSSIER : sans ce repli, il n'atteignait aucune discipline, et le
        // rapport taisait un verdict pourtant rendu. Reserve au dossier d'UNE
        // SEULE action (ADR 0135) : sur un dossier ancien qui en porte
        // plusieurs, le meme verdict se repeterait sur chaque ligne en
        // pretendant juger chacune.
        //
        // C'est le DERNIER jugement du dossier, action comprise : sur un
        // dossier d'une action, prendre celui de l'action aurait affiche un
        // « partiellement » depasse sous un « efficace » rendu depuis.
        EffectivenessReview duDossierSeul = capa.getActions().size() == 1
                ? efficacite.findLastByCase(tenantId, capa.getId()).orElse(null)
                : null;
        List<EightDSources.Action> actions = new ArrayList<>();
        for (CapaAction action : capa.getActions()) {
            List<String> pieces = parAction.getOrDefault(action.getId(), List.of());
            EffectivenessReview jugement = duDossierSeul != null
                    ? duDossierSeul
                    : verdicts.get(action.getId());
            actions.add(new EightDSources.Action(
                    action.getTitle(), action.getDescription(),
                    nom(action.getActionType()), nom(action.getStatus()),
                    // LE RESPONSABLE SE NOMME. La colonne portait l'adresse
                    // de courriel figee a l'affectation : une adresse
                    // personnelle dans un document remis au client, la ou le
                    // lecteur attend un nom. L'annuaire le sait ; le nom fige
                    // ne sert que s'il en est un, ou si l'annuaire se tait.
                    nomAnnuaire(action.getAssigneeId(),
                            estUnNom(action.getAssigneeName()) ? action.getAssigneeName() : null),
                    action.getDecidedOn(), action.getDueDate(),
                    action.getCompletedAt(),
                    pieces,
                    jugement == null ? null : jugement.verdict().name(),
                    jugement == null ? null : jugement.reviewedAt()));
        }
        return new EightDSources.Capa(
                capa.getTitle(), nom(capa.getType()), nom(capa.getCriticity()), nom(capa.getStatus()),
                capa.getDueDate(), capa.getClosedAt(),
                capa.getEffectivenessVerified(), capa.getEffectivenessVerifiedAt(),
                duDossier,
                actions);
    }

    /** Le nom du fichier tel qu'il a ete depose, ou sa cle a defaut. */
    private static String nomDe(CapaEvidence piece) {
        String nom = piece.getOriginalFilename();
        return nom == null || nom.isBlank() ? piece.getObjectKey() : nom;
    }

    private EightDSources.Fmea enFmea(UUID tenantId, NonConformity nc) {
        if (nc.getFmeaItemId() == null) {
            return null;
        }
        FmeaItem item = fmeaItems.findByIdAndTenantId(nc.getFmeaItemId(), tenantId).orElse(null);
        if (item == null) {
            return null;
        }
        return new EightDSources.Fmea(
                item.getFailureMode(), item.getFailureEffect(), item.getFailureCause(),
                item.getCurrentControls(), item.getRpn(), item.getRpnAfter(),
                nom(item.getActionPriority()), item.getRecommendedAction(), item.getActionsTaken());
    }

    private List<EightDSources.Surveillance> enSurveillance(UUID tenantId, NonConformity nc) {
        if (nc.getProductId() == null) {
            return List.of();
        }
        List<EightDSources.Surveillance> plans = new ArrayList<>();
        for (ControlPlan plan : controlPlans.findByProduct(tenantId, nc.getProductId())) {
            plans.add(new EightDSources.Surveillance(
                    plan.getCode(), plan.getRevision(), nom(plan.getPhase()), nom(plan.getStatus()),
                    controlPlans.linesOf(plan.getId()).size(), plan.getSealSha256()));
        }
        return plans;
    }

    private static String nom(Enum<?> valeur) {
        return valeur == null ? null : valeur.name();
    }
}
