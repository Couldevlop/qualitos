package com.openlab.qualitos.core.partner;

import com.openlab.qualitos.core.tenant.Tenant;
import com.openlab.qualitos.core.tenant.TenantNotFoundException;
import com.openlab.qualitos.core.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Le côté éditeur du domaine partenaire : constituer les cabinets, leurs
 * équipes, et leur portefeuille.
 *
 * <p><b>Ce que l'éditeur ne fait pas.</b> Il PROPOSE un client au portefeuille
 * d'un cabinet ; il ne lui donne pas l'accès. L'accord appartient au client, et
 * c'est {@link PartnerAccessService} qui le recueille. Mettre la décision ici
 * aurait fait du portefeuille un droit d'entrée — exactement la confusion que
 * la spécification (D2) interdit.
 */
@Service
public class PartnerFirmService {

    private static final Logger log = LoggerFactory.getLogger(PartnerFirmService.class);

    private final PartnerFirmRepository firms;
    private final PartnerConsultantRepository consultants;
    private final PartnerEngagementRepository engagements;
    private final TenantRepository tenants;
    private final Clock clock;

    public PartnerFirmService(PartnerFirmRepository firms, PartnerConsultantRepository consultants,
                              PartnerEngagementRepository engagements, TenantRepository tenants,
                              Clock clock) {
        this.firms = firms;
        this.consultants = consultants;
        this.engagements = engagements;
        this.tenants = tenants;
        this.clock = clock;
    }

    // ---------- les cabinets ----------

    @Transactional
    public PartnerDto.FirmResponse create(PartnerDto.FirmRequest request) {
        if (firms.existsBySlug(request.slug())) {
            throw new PartnerExceptions.FirmAlreadyExists(request.slug());
        }
        PartnerFirm firm = firms.save(PartnerFirm.builder()
                .slug(request.slug())
                .name(request.name())
                .registrationNumber(blankToNull(request.registrationNumber()))
                .contactEmail(blankToNull(request.contactEmail()))
                .contactPhone(blankToNull(request.contactPhone()))
                .active(true)
                .build());
        log.info("partner.firm.created firm_id={} slug={}", firm.getId(), firm.getSlug());
        return describe(firm);
    }

    @Transactional(readOnly = true)
    public Page<PartnerDto.FirmResponse> list(Pageable pageable) {
        return firms.findAllByOrderByNameAsc(pageable).map(this::describe);
    }

    @Transactional(readOnly = true)
    public PartnerDto.FirmResponse get(UUID firmId) {
        return describe(require(firmId));
    }

    @Transactional
    public PartnerDto.FirmResponse update(UUID firmId, PartnerDto.FirmRequest request) {
        PartnerFirm firm = require(firmId);
        if (!firm.getSlug().equals(request.slug()) && firms.existsBySlug(request.slug())) {
            throw new PartnerExceptions.FirmAlreadyExists(request.slug());
        }
        firm.setSlug(request.slug());
        firm.setName(request.name());
        firm.setRegistrationNumber(blankToNull(request.registrationNumber()));
        firm.setContactEmail(blankToNull(request.contactEmail()));
        firm.setContactPhone(blankToNull(request.contactPhone()));
        log.info("partner.firm.updated firm_id={}", firmId);
        return describe(firms.save(firm));
    }

    /**
     * Suspend ou réactive un cabinet.
     *
     * <p>La suspension ne touche PAS aux accords : elle coupe l'accès de tous
     * ses consultants tant qu'elle dure, et le jour où elle est levée, les
     * missions en cours reprennent telles qu'elles étaient. Supprimer les
     * accords ferait d'une mesure conservatoire une décision irréversible, que
     * seul le client pourrait défaire — accord par accord.
     */
    @Transactional
    public PartnerDto.FirmResponse setActive(UUID firmId, boolean active) {
        PartnerFirm firm = require(firmId);
        firm.setActive(active);
        log.info("partner.firm.{} firm_id={}", active ? "reactivated" : "suspended", firmId);
        return describe(firms.save(firm));
    }

    // ---------- les consultants ----------

    @Transactional(readOnly = true)
    public List<PartnerDto.ConsultantResponse> listConsultants(UUID firmId) {
        require(firmId);
        return consultants.findByFirmIdOrderByDisplayNameAsc(firmId).stream()
                .map(PartnerDto.ConsultantResponse::from)
                .toList();
    }

    @Transactional
    public PartnerDto.ConsultantResponse addConsultant(UUID firmId,
                                                       PartnerDto.ConsultantRequest request) {
        require(firmId);
        String email = request.email().strip();
        if (consultants.existsByFirmIdAndEmailIgnoreCase(firmId, email)) {
            throw new PartnerExceptions.ConsultantAlreadyExists(email);
        }
        PartnerConsultant saved = consultants.save(PartnerConsultant.builder()
                .firmId(firmId)
                .displayName(request.displayName().strip())
                .email(email)
                .active(true)
                .build());
        log.info("partner.consultant.added firm_id={} consultant_id={}", firmId, saved.getId());
        return PartnerDto.ConsultantResponse.from(saved);
    }

    /**
     * Désactive ou réactive un consultant.
     *
     * <p>C'est le geste qui compte au départ d'une personne : il lui retire d'un
     * coup l'accès à TOUS les clients du portefeuille. Le faire client par
     * client ferait dépendre la fin de son accès de la vigilance de chaque
     * administrateur pris séparément, c'est-à-dire d'un oubli.
     */
    @Transactional
    public PartnerDto.ConsultantResponse setConsultantActive(UUID firmId, UUID consultantId,
                                                             boolean active) {
        PartnerConsultant consultant = requireConsultant(firmId, consultantId);
        consultant.setActive(active);
        log.info("partner.consultant.{} consultant_id={}",
                active ? "reactivated" : "deactivated", consultantId);
        return PartnerDto.ConsultantResponse.from(consultants.save(consultant));
    }

    // ---------- le portefeuille ----------

    /**
     * Propose un client au portefeuille d'un cabinet. Rien n'est ouvert : la
     * balle passe au client.
     */
    @Transactional
    public PartnerDto.EngagementResponse propose(UUID firmId, PartnerDto.ProposeRequest request,
                                                 UUID actor) {
        PartnerFirm firm = require(firmId);
        Tenant tenant = tenants.findById(request.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(request.tenantId()));
        if (engagements.findLive(tenant.getId(), firmId).isPresent()) {
            throw new PartnerExceptions.EngagementAlreadyLive();
        }
        PartnerEngagement saved = engagements.save(PartnerEngagement.builder()
                .tenantId(tenant.getId())
                .firmId(firmId)
                .scope(request.scope())
                .status(EngagementStatus.PROPOSED)
                .purpose(blankToNull(request.purpose()))
                .proposedBy(actor)
                .proposedAt(Instant.now(clock))
                .build());
        log.info("partner.engagement.proposed engagement_id={} firm_id={} tenant_id={} actor={}",
                saved.getId(), firmId, tenant.getId(), actor);
        return PartnerDto.EngagementResponse.from(saved, tenant.getName(), firm.getName(), today());
    }

    @Transactional(readOnly = true)
    public List<PartnerDto.EngagementResponse> portfolio(UUID firmId) {
        PartnerFirm firm = require(firmId);
        List<PartnerEngagement> lignes = engagements.findByFirmIdOrderByProposedAtDesc(firmId);
        Map<UUID, String> noms = tenantNames(lignes);
        LocalDate today = today();
        return lignes.stream()
                .map(e -> PartnerDto.EngagementResponse.from(
                        e, noms.get(e.getTenantId()), firm.getName(), today))
                .toList();
    }

    // ---------- outils ----------

    private PartnerFirm require(UUID firmId) {
        return firms.findById(firmId).orElseThrow(() -> new PartnerExceptions.FirmNotFound(firmId));
    }

    private PartnerConsultant requireConsultant(UUID firmId, UUID consultantId) {
        PartnerConsultant consultant = consultants.findById(consultantId)
                .orElseThrow(() -> new PartnerExceptions.ConsultantNotFound(consultantId));
        // Le cabinet du chemin doit être celui du consultant : sans cette
        // vérification, l'identifiant d'un consultant suffirait à le désactiver
        // depuis l'écran d'un autre cabinet.
        if (!consultant.getFirmId().equals(firmId)) {
            throw new PartnerExceptions.ConsultantNotFound(consultantId);
        }
        return consultant;
    }

    private PartnerDto.FirmResponse describe(PartnerFirm firm) {
        return PartnerDto.FirmResponse.from(firm,
                consultants.countByFirmIdAndActiveTrue(firm.getId()),
                engagements.countByFirmIdAndStatus(firm.getId(), EngagementStatus.GRANTED));
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
