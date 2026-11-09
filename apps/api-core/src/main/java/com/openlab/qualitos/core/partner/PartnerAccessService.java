package com.openlab.qualitos.core.partner;

import com.openlab.qualitos.core.platformaudit.PlatformAuditAction;
import com.openlab.qualitos.core.platformaudit.PlatformAuditService;
import com.openlab.qualitos.core.tenant.Tenant;
import com.openlab.qualitos.core.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Le côté client et le côté consultant du domaine partenaire : accorder,
 * reprendre, et vérifier qu'un accès est ouvert.
 *
 * <p><b>La règle que ce service existe pour tenir</b> (spécification D2) : un
 * consultant n'entre jamais chez un client sans accord enregistré <b>de ce
 * client</b>. L'éditeur propose ; seul le client décide, pour une durée qu'il
 * fixe, et il reprend quand il veut.
 *
 * <p><b>Le client vient du jeton, jamais du corps</b> ({@code CLAUDE.md}
 * §18.2.2). Les méthodes côté client reçoivent le {@code tenantId} résolu par
 * le filtre, et vérifient que l'accord visé appartient bien à ce client avant
 * d'y toucher : sans ce contrôle, l'identifiant d'un accord suffirait à
 * accorder un accès chez le voisin. La table est par ailleurs sous politique de
 * cloisonnement en base, mais {@code api-core} n'active pas encore la session
 * RLS (ADR 0086, « reste à faire ») — le contrôle applicatif est donc
 * aujourd'hui la barrière réelle, et il est écrit comme telle.
 *
 * <p><b>Trois conditions pour qu'un accès soit ouvert</b>, et elles se vérifient
 * à chaque fois, pas une fois pour toutes : le cabinet est actif, le consultant
 * est actif, l'accord est accordé et dans ses dates. Une seule qui tombe ferme
 * l'accès immédiatement — sans travail de fond à faire tourner, sans ligne à
 * réécrire en base, donc sans fenêtre pendant laquelle un accès périmé reste
 * ouvert parce qu'un traitement nocturne n'a pas tourné.
 */
@Service
public class PartnerAccessService {

    private static final Logger log = LoggerFactory.getLogger(PartnerAccessService.class);

    /** La nature de l'objet visé, telle qu'elle apparaît dans la piste d'audit. */
    private static final String TARGET_TYPE = "partner_engagement";

    private final PartnerFirmRepository firms;
    private final PartnerConsultantRepository consultants;
    private final PartnerEngagementRepository engagements;
    private final TenantRepository tenants;
    private final Clock clock;
    private final PlatformAuditService auditTrail;

    public PartnerAccessService(PartnerFirmRepository firms, PartnerConsultantRepository consultants,
                                PartnerEngagementRepository engagements, TenantRepository tenants,
                                Clock clock, PlatformAuditService auditTrail) {
        this.auditTrail = auditTrail;
        this.firms = firms;
        this.consultants = consultants;
        this.engagements = engagements;
        this.tenants = tenants;
        this.clock = clock;
    }

    // ---------- côté client : ce que l'administrateur du client voit et décide ----------

    /** Les accords qui concernent CE client, décidés ou non. */
    @Transactional(readOnly = true)
    public List<PartnerDto.EngagementResponse> listForTenant(UUID tenantId) {
        List<PartnerEngagement> lignes = engagements.findByTenantIdOrderByProposedAtDesc(tenantId);
        String nomClient = tenants.findById(tenantId).map(Tenant::getName).orElse(null);
        Map<UUID, String> cabinets = firmNames(lignes);
        LocalDate today = today();
        return lignes.stream()
                .map(e -> PartnerDto.EngagementResponse.from(
                        e, nomClient, cabinets.get(e.getFirmId()), today))
                .toList();
    }

    /**
     * Le client accorde : portée, terme, objet.
     *
     * <p>Il peut restreindre ce que l'éditeur a proposé — proposition
     * d'intervention, accord en lecture seule. L'inverse est vrai aussi : c'est
     * SA décision, pas une contresignature de celle de l'éditeur.
     */
    @Transactional
    public PartnerDto.EngagementResponse grant(UUID tenantId, UUID engagementId,
                                               PartnerDto.GrantRequest request, UUID actor) {
        PartnerEngagement accord = require(tenantId, engagementId);
        if (accord.getStatus() != EngagementStatus.PROPOSED) {
            throw new PartnerExceptions.EngagementNotDecidable(accord.getStatus());
        }
        accord.setScope(request.scope());
        accord.setStatus(EngagementStatus.GRANTED);
        accord.setStartsOn(today());
        accord.setEndsOn(request.endsOn());
        if (request.purpose() != null && !request.purpose().isBlank()) {
            accord.setPurpose(request.purpose().strip());
        }
        accord.setDecidedBy(actor);
        accord.setDecidedAt(Instant.now(clock));
        log.info("partner.engagement.granted engagement_id={} tenant_id={} firm_id={} scope={} "
                        + "ends_on={} actor={}",
                engagementId, tenantId, accord.getFirmId(), accord.getScope(),
                accord.getEndsOn(), actor);
        // L'acte le plus sensible de tout api-core : un tiers obtient acces aux
        // donnees qualite d'un client. C'est la premiere ligne qu'une autorite
        // cherchera, et la portee accordee compte autant que la date.
        auditTrail.record(PlatformAuditAction.PARTNER_ENGAGEMENT_GRANTED, TARGET_TYPE,
                engagementId, tenantId,
                "Acces accorde au cabinet " + accord.getFirmId() + " : portee "
                        + accord.getScope() + ", terme "
                        + (accord.getEndsOn() == null ? "sans terme" : accord.getEndsOn()),
                "{\"firmId\":\"" + accord.getFirmId() + "\",\"scope\":\""
                        + accord.getScope() + "\",\"endsOn\":"
                        + (accord.getEndsOn() == null ? "null" : "\"" + accord.getEndsOn() + "\"")
                        + "}");
        return describe(engagements.save(accord));
    }

    /** Le client refuse d'emblée. */
    @Transactional
    public PartnerDto.EngagementResponse decline(UUID tenantId, UUID engagementId, UUID actor) {
        PartnerEngagement accord = require(tenantId, engagementId);
        if (accord.getStatus() != EngagementStatus.PROPOSED) {
            throw new PartnerExceptions.EngagementNotDecidable(accord.getStatus());
        }
        accord.setStatus(EngagementStatus.DECLINED);
        accord.setDecidedBy(actor);
        accord.setDecidedAt(Instant.now(clock));
        log.info("partner.engagement.declined engagement_id={} tenant_id={} actor={}",
                engagementId, tenantId, actor);
        auditTrail.record(PlatformAuditAction.PARTNER_ENGAGEMENT_DECLINED, TARGET_TYPE,
                engagementId, tenantId,
                "Mission refusee : cabinet " + accord.getFirmId(),
                "{\"firmId\":\"" + accord.getFirmId() + "\"}");
        return describe(engagements.save(accord));
    }

    /**
     * Le client reprend ce qu'il avait accordé.
     *
     * <p>Révoquer est possible à tout instant, y compris sur un accord dont le
     * terme est déjà passé : la ligne dit alors que l'accès a été REPRIS et non
     * qu'il s'est éteint tout seul, et c'est une différence qu'un audit doit
     * pouvoir lire dix-huit mois plus tard.
     */
    @Transactional
    public PartnerDto.EngagementResponse revoke(UUID tenantId, UUID engagementId, UUID actor) {
        PartnerEngagement accord = require(tenantId, engagementId);
        if (accord.getStatus() != EngagementStatus.GRANTED) {
            throw new PartnerExceptions.EngagementNotDecidable(accord.getStatus());
        }
        accord.setStatus(EngagementStatus.REVOKED);
        accord.setRevokedBy(actor);
        accord.setRevokedAt(Instant.now(clock));
        log.info("partner.engagement.revoked engagement_id={} tenant_id={} firm_id={} actor={}",
                engagementId, tenantId, accord.getFirmId(), actor);
        auditTrail.record(PlatformAuditAction.PARTNER_ENGAGEMENT_REVOKED, TARGET_TYPE,
                engagementId, tenantId,
                "Acces retire au cabinet " + accord.getFirmId(),
                "{\"firmId\":\"" + accord.getFirmId() + "\"}");
        return describe(engagements.save(accord));
    }

    // ---------- côté consultant : chez qui puis-je entrer, aujourd'hui ----------

    /**
     * Les clients où ce consultant a un accès ouvert aujourd'hui.
     *
     * <p>Une personne dont le compte n'est rattaché à aucun cabinet n'a pas un
     * portefeuille vide par accident : elle n'en a pas, et la liste vide le dit
     * sans lever — se connecter avec un rôle mais sans fiche n'est pas une
     * panne, c'est un rattachement qui reste à faire.
     */
    @Transactional(readOnly = true)
    public List<PartnerDto.EngagementResponse> portfolioOf(UUID accountId) {
        Optional<PartnerConsultant> fiche = consultant(accountId);
        if (fiche.isEmpty() || !fiche.get().isActive()) {
            return List.of();
        }
        PartnerFirm cabinet = firms.findById(fiche.get().getFirmId()).orElse(null);
        if (cabinet == null || !cabinet.isActive()) {
            return List.of();
        }
        LocalDate today = today();
        List<PartnerEngagement> accords = engagements.findGrantedForFirm(cabinet.getId()).stream()
                .filter(e -> e.opensAccessOn(today))
                .toList();
        Map<UUID, String> noms = tenantNames(accords);
        return accords.stream()
                .map(e -> PartnerDto.EngagementResponse.from(
                        e, noms.get(e.getTenantId()), cabinet.getName(), today))
                .toList();
    }

    /**
     * La garde : ce consultant peut-il agir chez ce client, maintenant ?
     *
     * <p>C'est le seul point par lequel un accès de cabinet doit passer. Il ne
     * répond pas « oui / non » mais lève avec la raison : « votre cabinet est
     * suspendu » et « votre mission s'est terminée le 12 » appellent deux
     * gestes différents, et un refus muet les confond.
     *
     * @param write vrai si le geste écrit : un accord en lecture ne l'autorise pas
     */
    @Transactional(readOnly = true)
    public PartnerEngagement requireAccess(UUID accountId, UUID tenantId, boolean write) {
        PartnerConsultant fiche = consultant(accountId)
                .orElseThrow(() -> new PartnerExceptions.AccessNotGranted(
                        "Votre compte n'est rattaché à aucun cabinet."));
        if (!fiche.isActive()) {
            throw new PartnerExceptions.AccessNotGranted("Votre accès a été désactivé par votre cabinet.");
        }
        PartnerFirm cabinet = firms.findById(fiche.getFirmId())
                .orElseThrow(() -> new PartnerExceptions.AccessNotGranted("Cabinet introuvable."));
        if (!cabinet.isActive()) {
            throw new PartnerExceptions.AccessNotGranted("Votre cabinet est suspendu.");
        }
        PartnerEngagement accord = engagements.findLive(tenantId, cabinet.getId())
                .orElseThrow(() -> new PartnerExceptions.AccessNotGranted(
                        "Ce client n'a accordé aucun accès à votre cabinet."));
        if (!accord.opensAccessOn(today())) {
            throw new PartnerExceptions.AccessNotGranted(
                    "L'accord de ce client n'est pas en vigueur aujourd'hui.");
        }
        if (write && accord.getScope() != EngagementScope.INTERVENE) {
            throw new PartnerExceptions.AccessNotGranted(
                    "Ce client vous a accordé la lecture, pas l'intervention.");
        }
        // Un accès de cabinet est un accès de tiers : il se journalise
        // nominativement, à chaque fois, même en lecture.
        log.info("partner.access.used account_id={} consultant_id={} firm_id={} tenant_id={} write={}",
                accountId, fiche.getId(), cabinet.getId(), tenantId, write);
        return accord;
    }

    /**
     * Rattache le compte d'une personne à sa fiche de consultant, à sa première
     * connexion, par l'adresse de courriel que le cabinet a inscrite.
     *
     * @return la fiche rattachée, ou rien si aucune ne correspond
     */
    @Transactional
    public Optional<PartnerConsultant> linkAccount(UUID accountId, String email) {
        if (accountId == null || email == null || email.isBlank()) {
            return Optional.empty();
        }
        Optional<PartnerConsultant> deja = consultants.findByAccountId(accountId);
        if (deja.isPresent()) {
            return deja;
        }
        Optional<PartnerConsultant> fiche =
                consultants.findFirstByAccountIdIsNullAndEmailIgnoreCase(email.strip());
        fiche.ifPresent(c -> {
            c.setAccountId(accountId);
            consultants.save(c);
            log.info("partner.consultant.linked consultant_id={} account_id={}", c.getId(), accountId);
        });
        return fiche;
    }

    // ---------- outils ----------

    private Optional<PartnerConsultant> consultant(UUID accountId) {
        return accountId == null ? Optional.empty() : consultants.findByAccountId(accountId);
    }

    /**
     * L'accord visé, et il doit appartenir au client du jeton.
     *
     * <p>Rendre « introuvable » plutôt que « interdit » quand il appartient à un
     * autre client : répondre 403 confirmerait à l'appelant que cet identifiant
     * existe ailleurs, ce qui est déjà une information.
     */
    private PartnerEngagement require(UUID tenantId, UUID engagementId) {
        PartnerEngagement accord = engagements.findById(engagementId)
                .orElseThrow(() -> new PartnerExceptions.EngagementNotFound(engagementId));
        if (!accord.getTenantId().equals(tenantId)) {
            throw new PartnerExceptions.EngagementNotFound(engagementId);
        }
        return accord;
    }

    private PartnerDto.EngagementResponse describe(PartnerEngagement accord) {
        String client = tenants.findById(accord.getTenantId()).map(Tenant::getName).orElse(null);
        String cabinet = firms.findById(accord.getFirmId()).map(PartnerFirm::getName).orElse(null);
        return PartnerDto.EngagementResponse.from(accord, client, cabinet, today());
    }

    private Map<UUID, String> firmNames(List<PartnerEngagement> lignes) {
        List<UUID> ids = lignes.stream().map(PartnerEngagement::getFirmId).distinct().toList();
        Map<UUID, String> noms = new HashMap<>();
        firms.findAllById(ids).forEach(f -> noms.put(f.getId(), f.getName()));
        return noms;
    }

    private Map<UUID, String> tenantNames(List<PartnerEngagement> lignes) {
        List<UUID> ids = lignes.stream().map(PartnerEngagement::getTenantId).distinct().toList();
        Map<UUID, String> noms = new HashMap<>();
        tenants.findAllById(ids).forEach(t -> noms.put(t.getId(), t.getName()));
        return noms;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
