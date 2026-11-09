package com.openlab.qualitos.core.tenant;

import com.openlab.qualitos.core.platformaudit.PlatformAuditAction;
import com.openlab.qualitos.core.platformaudit.PlatformAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    /** La nature de l'objet visé, telle qu'elle apparaît dans la piste d'audit. */
    private static final String TARGET_TYPE = "tenant";

    private final TenantRepository tenantRepository;
    private final PlatformAuditService auditTrail;

    public TenantService(TenantRepository tenantRepository, PlatformAuditService auditTrail) {
        this.tenantRepository = tenantRepository;
        this.auditTrail = auditTrail;
    }

    public Page<TenantDto.Response> findAll(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(TenantDto.Response::from);
    }

    public TenantDto.Response findById(UUID id) {
        return tenantRepository.findById(id)
                .map(TenantDto.Response::from)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    public TenantDto.Response findBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .map(TenantDto.Response::from)
                .orElseThrow(() -> new TenantNotFoundException(slug));
    }

    @Transactional
    public TenantDto.Response create(TenantDto.CreateRequest request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new TenantAlreadyExistsException(request.slug());
        }

        Tenant tenant = Tenant.builder()
                .slug(request.slug())
                .name(request.name())
                .plan(request.plan() != null ? request.plan() : Tenant.Plan.STARTER)
                .active(true)
                .build();

        Tenant saved = tenantRepository.save(tenant);
        // La ligne de journal applicatif ci-dessous sert l'exploitation : elle
        // est utile, effaçable, et ne prouve rien. L'acte, lui, est consigné
        // dans la piste d'audit de la plateforme, en ajout seul et chaînée.
        log.info("Tenant created: slug={}, id={}", saved.getSlug(), saved.getId());
        auditTrail.record(PlatformAuditAction.TENANT_CREATED, TARGET_TYPE,
                saved.getId(), saved.getId(),
                "Client cree : " + saved.getName() + " (" + saved.getSlug()
                        + "), palier " + saved.getPlan(),
                "{\"slug\":\"" + saved.getSlug() + "\",\"plan\":\"" + saved.getPlan() + "\"}");
        return TenantDto.Response.from(saved);
    }

    @Transactional
    public TenantDto.Response update(UUID id, TenantDto.UpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));

        // Relevé AVANT mutation : un journal qui ne dit que l'état d'arrivée
        // laisse l'auditeur incapable de voir ce qui a changé.
        String avant = tenant.getName() + "/" + tenant.getPlan() + "/"
                + (tenant.isActive() ? "actif" : "inactif");
        tenant.setName(request.name());

        if (request.plan() != null) {
            tenant.setPlan(request.plan());
        }
        if (request.active() != null) {
            tenant.setActive(request.active());
        }

        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant updated: id={}", saved.getId());
        String apres = saved.getName() + "/" + saved.getPlan() + "/"
                + (saved.isActive() ? "actif" : "inactif");
        auditTrail.record(PlatformAuditAction.TENANT_UPDATED, TARGET_TYPE,
                saved.getId(), saved.getId(),
                "Client modifie : " + avant + " -> " + apres,
                "{\"before\":\"" + avant + "\",\"after\":\"" + apres + "\"}");
        return TenantDto.Response.from(saved);
    }

    @Transactional
    public void deactivate(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        tenant.setActive(false);
        tenantRepository.save(tenant);
        log.info("Tenant deactivated: id={}", id);
        auditTrail.record(PlatformAuditAction.TENANT_DEACTIVATED, TARGET_TYPE, id, id,
                "Client desactive : " + tenant.getName() + " (" + tenant.getSlug() + ")",
                null);
    }
}
