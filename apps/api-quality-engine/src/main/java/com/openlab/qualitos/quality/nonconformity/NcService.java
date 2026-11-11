package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.dmaic.DmaicDto;
import com.openlab.qualitos.quality.dmaic.DmaicService;
import com.openlab.qualitos.quality.pdca.PdcaDto;
import com.openlab.qualitos.quality.pdca.PdcaService;
import com.openlab.qualitos.quality.nonconformity.subcategory.NcSubcategoryCatalog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Module Non-Conformités (§4.3) : saisie terrain, workflow de traitement et
 * escalade vers une CAPA. Multi-tenant strict (tenant issu du JWT, jamais du body).
 */
@Service
@Transactional
public class NcService {

    /** Borne de l'intitulé d'un cycle PDCA comme d'un projet DMAIC, en base. */
    private static final int TITRE_MAX = 255;
    /** Borne de l'énoncé repris : large, mais la colonne n'est pas sans fond. */
    private static final int ENONCE_MAX = 4000;

    private final NonConformityRepository repository;
    /** L'équipe en charge de chaque écart — la première discipline du 8D (D1). */
    private final NcTeamMemberRepository teamMembers;
    /**
     * Ouverture du dossier CAPA par le module CAPA lui-même (ADR 0088) : le
     * dossier naît avec son action, son journal et ses notifications, comme
     * depuis n'importe quel autre écran.
     */
    private final CapaService capaService;
    /**
     * Les deux méthodes par lesquelles un écart se traite (§3.6). Le cycle et le
     * projet naissent par leur propre module : ils y gagnent leur numérotation,
     * leurs étapes initiales et leur journal, comme depuis n'importe quel écran.
     */
    private final PdcaService pdcaService;
    private final DmaicService dmaicService;
    private final ApplicationEventPublisher events;
    /** Sans « voir toutes les NC », on ne voit que celles qu'on a déclarées (ADR 0081). */
    private final RecordScope scope;
    /** Le catalogue qui dit si une nature d'écart existe, et sous quelle catégorie. */
    private final NcSubcategoryCatalog subcategories;

    public NcService(NonConformityRepository repository, NcTeamMemberRepository teamMembers,
                     CapaService capaService,
                     PdcaService pdcaService, DmaicService dmaicService,
                     ApplicationEventPublisher events, RecordScope scope,
                     NcSubcategoryCatalog subcategories) {
        this.repository = repository;
        this.teamMembers = teamMembers;
        this.capaService = capaService;
        this.pdcaService = pdcaService;
        this.dmaicService = dmaicService;
        this.events = events;
        this.scope = scope;
        this.subcategories = subcategories;
    }

    /**
     * Liste filtrée du tenant courant.
     *
     * <p>Une spécification, et non une cascade de branches : à trois filtres il
     * fallait déjà huit combinaisons écrites à la main, et l'ajout de l'origine
     * en aurait demandé seize. Chaque critère nul est simplement ignoré.
     */
    @Transactional(readOnly = true)
    public Page<NcDto.Response> findAll(NcStatus status, NcSeverity severity, NcCategory category,
                                        String subcategoryCode, NcOrigin origin, UUID productId,
                                        Pageable pageable) {
        UUID tenantId = requireTenantId();
        Optional<UUID> seulement = NcScope.restriction(scope);
        Specification<NonConformity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Le tenant vient du jeton et n'est jamais optionnel (§18.2 #2).
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (status != null)   predicates.add(cb.equal(root.get("status"), status));
            if (severity != null) predicates.add(cb.equal(root.get("severity"), severity));
            if (category != null) predicates.add(cb.equal(root.get("category"), category));
            if (subcategoryCode != null && !subcategoryCode.isBlank()) {
                predicates.add(cb.equal(root.get("subcategoryCode"), subcategoryCode.strip()));
            }
            if (origin != null)   predicates.add(cb.equal(root.get("origin"), origin));
            // Filtré en base avec le tenant, jamais après coup en Java : trier une
            // page déjà découpée donnerait des pages incomplètes.
            if (productId != null) predicates.add(cb.equal(root.get("productId"), productId));
            seulement.ifPresent(moi -> predicates.add(cb.equal(root.get("reporterId"), moi)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<NonConformity> page = repository.findAll(spec, pageable);
        // Une requête pour toute la page : une par ligne ferait dix allers-retours
        // pour afficher dix écarts.
        Map<UUID, List<NcDto.TeamMemberRef>> equipes = equipesDe(tenantId,
                page.getContent().stream().map(NonConformity::getId).toList());
        return page.map(nc -> toResponse(nc, equipes.getOrDefault(nc.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public NcDto.Response findById(UUID id) {
        return toResponse(load(id));
    }

    /** Nombre de tentatives sur collision de référence concurrente (BUG #4). */
    private static final int REFERENCE_RETRY_ATTEMPTS = 3;

    public NcDto.Response create(NcDto.CreateRequest request) {
        UUID tenantId = requireTenantId();
        // H2 (OWASP A01) : le rapporteur est dérivé du JWT (sub), jamais d'un champ de
        // body falsifiable. On retombe sur le body uniquement hors contexte authentifié
        // (tests unitaires de service) ; en production le principal est toujours présent.
        UUID reporterId = CurrentUser.userId().orElse(request.reporterId());

        NonConformity nc = new NonConformity();
        nc.setTenantId(tenantId);
        nc.setTitle(request.title());
        nc.setDescription(request.description());
        nc.setCategory(request.category());
        // Vérifié ici, pas dans l'écran : l'API est publique et la file
        // hors-ligne du mobile rejoue par le même chemin.
        nc.setSubcategoryCode(subcategories.resolve(request.subcategoryCode(), request.category()));
        nc.setSeverity(request.severity());
        nc.setStatus(NcStatus.OPEN);
        nc.setOrigin(NcOrigin.orDefault(request.origin()));
        nc.setDetectedAt(request.detectedAt());
        nc.setZone(request.zone());
        nc.setGeoLat(request.geoLat());
        nc.setGeoLng(request.geoLng());
        nc.setPhotoUrls(request.photoUrls());
        nc.setReporterId(reporterId);
        // Le NOM suit la même règle que l'identifiant : il vient du jeton. Il est
        // recopié une fois pour toutes parce qu'un signalement doit rester
        // attribuable même quand le compte disparaît de l'annuaire.
        nc.setReporterName(CurrentUser.displayName().orElse(null));
        nc.setProductId(request.productId());
        nc.setFmeaItemId(request.fmeaItemId());

        // BUG #4 — Race sur la génération de référence : deux créations concurrentes
        // peuvent calculer la même référence et violer la contrainte d'unicité
        // (tenant_id, reference). On retente quelques fois en régénérant la référence ;
        // au-delà on laisse remonter → 409 via le GlobalExceptionHandler.
        DataIntegrityViolationException last = null;
        for (int attempt = 0; attempt < REFERENCE_RETRY_ATTEMPTS; attempt++) {
            nc.setReference(generateReference(tenantId));
            try {
                NonConformity saved = repository.saveAndFlush(nc);
                // L'équipe après l'écart : la ligne fille a besoin de son identifiant.
                remplacerEquipe(tenantId, saved.getId(), request.teamMembers());
                announceCreation(saved);
                return toResponse(saved);
            } catch (DataIntegrityViolationException ex) {
                last = ex;
                // Référence reprise entre temps : on régénère et on retente.
            }
        }
        throw last;
    }

    /**
     * Annonce la création aux abonnés. L'événement porte le tenant de l'ENTITÉ :
     * son consommateur s'exécute après le commit, hors du contexte d'exécution
     * qui l'a produit.
     */
    private void announceCreation(NonConformity saved) {
        events.publishEvent(new NcCreatedEvent(
                saved.getTenantId(), saved.getId(), saved.getProductId(), saved.getFmeaItemId(),
                saved.getTitle(), saved.getDescription(), saved.getDetectedAt()));
    }

    public NcDto.Response update(UUID id, NcDto.UpdateRequest request) {
        NonConformity nc = load(id);
        // REJECTED est terminal comme les deux autres : rouvrir la saisie d'un
        // constat écarté laisserait réécrire ce que le client a lu.
        if (nc.getStatus() == NcStatus.CLOSED
                || nc.getStatus() == NcStatus.CANCELLED
                || nc.getStatus() == NcStatus.REJECTED) {
            throw new NcStateException("Cannot modify a " + nc.getStatus() + " non-conformity");
        }
        if (request.origin() != null) nc.setOrigin(request.origin());
        if (request.productId() != null) nc.setProductId(request.productId());
        if (request.fmeaItemId() != null) nc.setFmeaItemId(request.fmeaItemId());
        if (request.title() != null) nc.setTitle(request.title());
        if (request.description() != null) nc.setDescription(request.description());
        if (request.category() != null) nc.setCategory(request.category());
        // La nature se revérifie contre la catégorie EFFECTIVE après coup : changer
        // la catégorie sans changer la nature laisserait un classement incohérent.
        if (request.subcategoryCode() != null) {
            nc.setSubcategoryCode(request.subcategoryCode().isBlank()
                    ? null
                    : subcategories.resolve(request.subcategoryCode(), nc.getCategory()));
        } else if (request.category() != null && nc.getSubcategoryCode() != null) {
            nc.setSubcategoryCode(subcategories.resolve(nc.getSubcategoryCode(), nc.getCategory()));
        }
        if (request.severity() != null) nc.setSeverity(request.severity());
        if (request.zone() != null) nc.setZone(request.zone());
        if (request.geoLat() != null) nc.setGeoLat(request.geoLat());
        if (request.geoLng() != null) nc.setGeoLng(request.geoLng());
        if (request.photoUrls() != null) nc.setPhotoUrls(request.photoUrls());
        // L'équipe se corrige : quelqu'un part, quelqu'un arrive. Absente de la
        // requête, elle reste telle quelle — la mise à jour est partielle.
        if (request.teamMembers() != null) {
            remplacerEquipe(nc.getTenantId(), nc.getId(), request.teamMembers());
        }
        return toResponse(repository.save(nc));
    }

    // --- workflow ---

    public NcDto.Response startAnalysis(UUID id, NcDto.StartAnalysisRequest request) {
        NonConformity nc = load(id);
        if (nc.getStatus() != NcStatus.OPEN) {
            throw new NcStateException("Only OPEN non-conformities can move to analysis");
        }
        nc.setStatus(NcStatus.UNDER_ANALYSIS);
        if (request != null && request.rootCause() != null) {
            nc.setRootCause(request.rootCause());
        }
        return toResponse(repository.save(nc));
    }

    public NcDto.Response defineAction(UUID id) {
        NonConformity nc = load(id);
        if (nc.getStatus() != NcStatus.UNDER_ANALYSIS) {
            throw new NcStateException("Action can only be defined from UNDER_ANALYSIS");
        }
        nc.setStatus(NcStatus.ACTION_DEFINED);
        return toResponse(repository.save(nc));
    }

    public NcDto.Response resolve(UUID id, NcDto.ResolveRequest request) {
        NonConformity nc = load(id);
        if (nc.getStatus() != NcStatus.ACTION_DEFINED) {
            throw new NcStateException("Only ACTION_DEFINED non-conformities can be resolved");
        }
        nc.setStatus(NcStatus.RESOLVED);
        nc.setResolutionNote(request.resolutionNote());
        nc.setResolvedAt(Instant.now());
        return toResponse(repository.save(nc));
    }

    public NcDto.Response close(UUID id) {
        NonConformity nc = load(id);
        if (nc.getStatus() != NcStatus.RESOLVED) {
            throw new NcStateException("Only RESOLVED non-conformities can be closed");
        }
        nc.setStatus(NcStatus.CLOSED);
        nc.setClosedAt(Instant.now());
        return toResponse(repository.save(nc));
    }

    public NcDto.Response cancel(UUID id) {
        NonConformity nc = load(id);
        if (nc.getStatus() != NcStatus.OPEN && nc.getStatus() != NcStatus.UNDER_ANALYSIS) {
            throw new NcStateException("Only OPEN or UNDER_ANALYSIS non-conformities can be cancelled");
        }
        nc.setStatus(NcStatus.CANCELLED);
        return toResponse(repository.save(nc));
    }

    /**
     * Écarte une réclamation examinée et jugée non fondée.
     *
     * <p>Réservé aux écarts signalés du DEHORS. Un constat qu'on a fait soi-même
     * ne se rejette pas : on le résout, on le clôt, ou on l'annule. Le refus est
     * porté ici et non par l'écran seul, faute de quoi un appel direct à l'API
     * contournerait la règle.
     *
     * <p>Depuis {@code OPEN} ou {@code UNDER_ANALYSIS} seulement : une réclamation
     * déjà résolue ou clôturée a reçu une réponse, et la reprendre en rejet
     * réécrirait l'histoire que le client a lue.
     */
    public NcDto.Response reject(UUID id, NcDto.RejectRequest request) {
        NonConformity nc = load(id);
        if (nc.getOrigin() != NcOrigin.EXTERNAL) {
            throw new NcStateException(
                    "Only an EXTERNAL non-conformity can be rejected: an internal finding is"
                    + " resolved, closed, or cancelled");
        }
        if (nc.getStatus() != NcStatus.OPEN && nc.getStatus() != NcStatus.UNDER_ANALYSIS) {
            throw new NcStateException(
                    "Only OPEN or UNDER_ANALYSIS non-conformities can be rejected");
        }
        nc.setStatus(NcStatus.REJECTED);
        nc.setRejectionReason(request.reason().trim());
        nc.setRejectedAt(Instant.now());
        return toResponse(repository.save(nc));
    }

    /**
     * Ouvre la CAPA de cette NC (§4.3, ADR 0088) : le dossier ET son action
     * unique, saisis dans la fenêtre commune, puis pose {@code capa_case_id}.
     *
     * <p>La source n'est pas lue dans le corps : c'est cette NC, par sa
     * référence. Ne change pas le statut de la NC. Une seconde ouverture est
     * refusée — une NC a une CAPA, qui porte une action.
     */
    public NcDto.Response escalateToCapa(UUID id, CapaDto.OpenCaseRequest request) {
        // Sous verrou : un double clic ou deux onglets ne doivent pas ouvrir deux
        // dossiers dont le second écraserait le lien du premier.
        NonConformity nc = loadForUpdate(id);
        if (nc.getCapaCaseId() != null) {
            throw new NcStateException("Non-conformity already escalated to CAPA " + nc.getCapaCaseId());
        }
        CapaDto.CaseResponse capa = capaService.openCase(
                request, CapaSourceType.NON_CONFORMITY, nc.getReference());
        nc.setCapaCaseId(capa.id());
        return toResponse(repository.save(nc));
    }

    /**
     * Ouvre, depuis cet écart, la méthode qui le traitera (§3.6) : un cycle PDCA
     * ou un projet DMAIC.
     *
     * <p>C'est le chemin qui manquait. Un écart partait en CAPA — une action
     * corrective et sa vérification d'efficacité — mais la CAPA ne couvre pas
     * tout : un écart qui révèle un procédé instable demande des mesures et une
     * phase de maîtrise, un écart qui révèle une pratique à corriger demande un
     * cycle. Ces deux chemins existaient, sans porte depuis l'écart.
     *
     * <p>La gravité propose la méthode ({@link NcMethod#fromSeverity}) ; la
     * requête peut en imposer une autre. Le cycle ou le projet cite l'écart dans
     * son intitulé et dans son énoncé : ouvert seul, il ne saurait plus d'où il
     * vient, et la trace qui relie la cause à son traitement serait perdue.
     *
     * <p>Une seule conversion par écart, et ce n'est pas une limite technique :
     * deux dossiers ouverts sur le même écart le traiteraient deux fois, et
     * chacun compterait dans les indicateurs. Un écart écarté
     * ({@link NcStatus#REJECTED}) ne se convertit pas non plus — on a jugé qu'il
     * n'y avait rien à traiter.
     *
     * @return la fiche de l'écart, portant désormais le lien vers ce qui le traite
     * @throws NcStateException si l'écart est déjà converti, ou a été écarté
     */
    public NcDto.Response convert(UUID id, NcDto.ConvertRequest request) {
        // Sous verrou : deux onglets ne doivent pas ouvrir deux cycles dont le
        // second effacerait le lien du premier.
        NonConformity nc = loadForUpdate(id);
        if (nc.getPdcaCycleId() != null) {
            throw new NcStateException(
                    "Non-conformity already converted to PDCA cycle " + nc.getPdcaCycleId());
        }
        if (nc.getDmaicProjectId() != null) {
            throw new NcStateException(
                    "Non-conformity already converted to DMAIC project " + nc.getDmaicProjectId());
        }
        if (nc.getStatus() == NcStatus.REJECTED) {
            throw new NcStateException("A rejected non-conformity has nothing left to treat");
        }

        NcMethod method = request.method() != null
                ? request.method()
                : NcMethod.fromSeverity(nc.getSeverity());
        if (method == NcMethod.DMAIC) {
            DmaicDto.ProjectResponse projet = dmaicService.createProject(
                    new DmaicDto.CreateProjectRequest(
                            tronque("DMAIC — " + nc.getTitle(), TITRE_MAX),
                            enonce(nc),
                            "Supprimer la cause de l'écart " + nc.getReference()
                                    + " et maintenir le procédé dans ses limites.",
                            request.pilotId(), null, request.dueDate(),
                            null, null, null, null, null));
            nc.setDmaicProjectId(projet.id());
        } else {
            PdcaDto.CycleResponse cycle = pdcaService.createCycle(
                    new PdcaDto.CreateCycleRequest(
                            tronque("PDCA — " + nc.getTitle(), TITRE_MAX),
                            enonce(nc), request.pilotId()));
            nc.setPdcaCycleId(cycle.id());
        }
        return toResponse(repository.save(nc));
    }

    /**
     * Ce que le cycle ou le projet reprend de l'écart : sa référence, sa gravité
     * et sa description.
     *
     * <p>La description n'est pas recopiée au hasard : c'est elle que le pilote
     * lit en ouvrant le dossier, et la lui faire ressaisir produirait deux
     * énoncés du même problème, qui divergeraient à la première correction.
     */
    private static String enonce(NonConformity nc) {
        StringBuilder texte = new StringBuilder()
                .append("Issu de la non-conformité ").append(nc.getReference())
                .append(" (gravité ").append(nc.getSeverity()).append(") : ")
                .append(nc.getTitle()).append('.');
        if (nc.getDescription() != null && !nc.getDescription().isBlank()) {
            texte.append(' ').append(nc.getDescription().strip());
        }
        if (nc.getZone() != null && !nc.getZone().isBlank()) {
            texte.append(" Zone : ").append(nc.getZone().strip()).append('.');
        }
        return tronque(texte.toString(), ENONCE_MAX);
    }

    /** Les intitulés sont bornés en base ; un titre d'écart long ne doit pas faire échouer la conversion. */
    private static String tronque(String valeur, int max) {
        if (valeur == null) {
            return null;
        }
        String v = valeur.strip();
        return v.length() <= max ? v : v.substring(0, max - 1).strip() + "…";
    }

    // --- helpers ---

    /** NC-{année}-{séquence par tenant}, avec garde anti-collision. */
    private String generateReference(UUID tenantId) {
        String prefix = "NC-" + Instant.now().atZone(ZoneOffset.UTC).getYear() + "-";
        long next = repository.countByTenantIdAndReferenceStartingWith(tenantId, prefix) + 1;
        String reference;
        do {
            reference = prefix + String.format("%04d", next);
            next++;
        } while (repository.existsByTenantIdAndReference(tenantId, reference));
        return reference;
    }

    private NonConformity loadForUpdate(UUID id) {
        UUID tenantId = requireTenantId();
        Optional<UUID> seulement = NcScope.restriction(scope);
        return repository.findForUpdateByIdAndTenantId(id, tenantId)
                .filter(nc -> NcScope.sees(seulement, nc))
                .orElseThrow(() -> new NcNotFoundException(id));
    }

    private NonConformity load(UUID id) {
        UUID tenantId = requireTenantId();
        // Hors de portée, la fiche n'existe pas : ni lecture ni action (ADR 0081).
        Optional<UUID> seulement = NcScope.restriction(scope);
        return repository.findByIdAndTenantId(id, tenantId)
                .filter(nc -> NcScope.sees(seulement, nc))
                .orElseThrow(() -> new NcNotFoundException(id));
    }

    private UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    private NcDto.Response toResponse(NonConformity nc) {
        return toResponse(nc, equipeDe(nc));
    }

    private NcDto.Response toResponse(NonConformity nc, List<NcDto.TeamMemberRef> equipe) {
        return new NcDto.Response(
                nc.getId(), nc.getTenantId(), nc.getReference(), nc.getTitle(), nc.getDescription(),
                nc.getCategory(), nc.getSubcategoryCode(), nc.getSeverity(), nc.getStatus(),
                nc.getOrigin(), nc.getDetectedAt(),
                nc.getZone(), nc.getGeoLat(), nc.getGeoLng(), nc.getPhotoUrls(),
                nc.getReporterId(), nc.getReporterName(), nc.getProductId(), nc.getFmeaItemId(),
                nc.getCapaCaseId(), nc.getPdcaCycleId(), nc.getDmaicProjectId(),
                nc.getRootCause(), nc.getResolutionNote(),
                nc.getResolvedAt(), nc.getClosedAt(),
                nc.getRejectionReason(), nc.getRejectedAt(),
                nc.getCreatedAt(), nc.getUpdatedAt(),
                equipe);
    }

    // ---------- L'équipe en charge (D1 du 8D) ----------

    /** L'équipe d'un écart, dans son ordre de saisie. */
    private List<NcDto.TeamMemberRef> equipeDe(NonConformity nc) {
        return teamMembers.findByTenantIdAndNcIdOrderByPositionAsc(nc.getTenantId(), nc.getId())
                .stream()
                .map(m -> new NcDto.TeamMemberRef(m.getAccountId(), m.getDisplayName()))
                .toList();
    }

    /** Les équipes de plusieurs écarts, en une requête, rangées par écart. */
    private Map<UUID, List<NcDto.TeamMemberRef>> equipesDe(UUID tenantId, List<UUID> ncIds) {
        if (ncIds.isEmpty()) {
            return Map.of();
        }
        return teamMembers.findByTenantIdAndNcIdInOrderByPositionAsc(tenantId, ncIds).stream()
                .collect(Collectors.groupingBy(NcTeamMember::getNcId, LinkedHashMap::new,
                        Collectors.mapping(
                                m -> new NcDto.TeamMemberRef(m.getAccountId(), m.getDisplayName()),
                                Collectors.toList())));
    }

    /**
     * Écrit l'équipe d'un écart, en remplaçant celle qui s'y trouvait.
     *
     * <p>Remplacement complet et non fusion : l'écran envoie la liste telle
     * qu'elle doit être, et fusionner rendrait impossible de retirer quelqu'un.
     * L'ordre reçu est conservé — le premier nommé pilote.
     *
     * <p>Les doublons d'une même personne sont écartés : la contrainte d'unicité
     * de la base refuserait la deuxième ligne, et un 400 pour un double-clic dans
     * un sélecteur n'apprendrait rien à personne.
     */
    private void remplacerEquipe(UUID tenantId, UUID ncId, List<NcDto.TeamMemberRef> membres) {
        teamMembers.deleteByTenantIdAndNcId(tenantId, ncId);
        teamMembers.flush();
        Set<String> deja = new LinkedHashSet<>();
        int rang = 0;
        for (NcDto.TeamMemberRef membre : membres) {
            String compte = membre.accountId().strip();
            if (compte.isEmpty() || !deja.add(compte)) {
                continue;
            }
            NcTeamMember ligne = new NcTeamMember();
            ligne.setTenantId(tenantId);
            ligne.setNcId(ncId);
            ligne.setAccountId(compte);
            ligne.setDisplayName(membre.displayName().strip());
            ligne.setPosition(rang++);
            teamMembers.save(ligne);
        }
    }

    // ---------- Statistiques ----------

    /**
     * Dénombrements par statut, pour les tuiles d'en-tête d'une liste de NC.
     *
     * <p>Le périmètre suit l'ÉCRAN : la liste interne compte les NC internes,
     * l'externe les externes. Un total toutes origines confondues au-dessus
     * d'une liste filtrée serait un chiffre juste au mauvais endroit — donc un
     * chiffre faux pour qui le lit.
     *
     * @param origin l'origine à compter, ou {@code null} pour les deux
     */
    @Transactional(readOnly = true)
    public NcDto.NcStatistics statistics(NcOrigin origin) {
        UUID tenantId = requireTenantId();
        Optional<UUID> seulement = NcScope.restriction(scope);
        if (seulement.isPresent()) {
            return statisticsOf(tenantId, origin, seulement.get());
        }
        return new NcDto.NcStatistics(
                tenantId,
                origin,
                origin == null
                        ? repository.countByTenantId(tenantId)
                        : repository.countByTenantIdAndOrigin(tenantId, origin),
                compte(tenantId, origin, NcStatus.OPEN),
                compte(tenantId, origin, NcStatus.UNDER_ANALYSIS),
                compte(tenantId, origin, NcStatus.ACTION_DEFINED),
                compte(tenantId, origin, NcStatus.RESOLVED),
                compte(tenantId, origin, NcStatus.CLOSED),
                compte(tenantId, origin, NcStatus.CANCELLED),
                compte(tenantId, origin, NcStatus.REJECTED));
    }

    /**
     * Les tuiles comptent le même périmètre que le tableau (ADR 0081) : sans
     * « voir toutes les NC », seulement celles que l'utilisateur a déclarées.
     */
    private NcDto.NcStatistics statisticsOf(UUID tenantId, NcOrigin origin, UUID moi) {
        Function<NcStatus, Long> compter = status -> repository.count((root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("tenantId"), tenantId));
            p.add(cb.equal(root.get("reporterId"), moi));
            if (origin != null) p.add(cb.equal(root.get("origin"), origin));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            return cb.and(p.toArray(new Predicate[0]));
        });
        return new NcDto.NcStatistics(tenantId, origin, compter.apply(null),
                compter.apply(NcStatus.OPEN), compter.apply(NcStatus.UNDER_ANALYSIS),
                compter.apply(NcStatus.ACTION_DEFINED), compter.apply(NcStatus.RESOLVED),
                compter.apply(NcStatus.CLOSED), compter.apply(NcStatus.CANCELLED), compter.apply(NcStatus.REJECTED));
    }

    /** Un statut, dans le périmètre demandé. */
    private long compte(UUID tenantId, NcOrigin origin, NcStatus status) {
        return origin == null
                ? repository.countByTenantIdAndStatus(tenantId, status)
                : repository.countByTenantIdAndOriginAndStatus(tenantId, origin, status);
    }
}
