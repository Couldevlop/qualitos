package com.openlab.qualitos.core.tenant;

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

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
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
        log.info("Tenant created: slug={}, id={}", saved.getSlug(), saved.getId());
        return TenantDto.Response.from(saved);
    }

    @Transactional
    public TenantDto.Response update(UUID id, TenantDto.UpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));

        tenant.setName(request.name());

        if (request.plan() != null) {
            tenant.setPlan(request.plan());
        }
        if (request.active() != null) {
            tenant.setActive(request.active());
        }

        Tenant saved = tenantRepository.save(tenant);
        log.info("Tenant updated: id={}", saved.getId());
        return TenantDto.Response.from(saved);
    }

    @Transactional
    public void deactivate(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        tenant.setActive(false);
        tenantRepository.save(tenant);
        log.info("Tenant deactivated: id={}", id);
    }
}
