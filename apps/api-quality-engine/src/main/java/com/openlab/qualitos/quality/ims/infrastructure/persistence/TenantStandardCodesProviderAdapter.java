package com.openlab.qualitos.quality.ims.infrastructure.persistence;

import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.ims.domain.port.TenantStandardCodesProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

/**
 * Adapter qui lit les codes des normes adoptées par le tenant courant.
 *
 * Sécurité OWASP A01 : le tenant_id provient EXCLUSIVEMENT du
 * {@link TenantContext} alimenté par le {@code TenantJwtFilter} —
 * jamais d'une entrée utilisateur (URL, body, header custom).
 *
 * Sécurité OWASP A03 : requête JPQL avec paramètres positionnels (jamais
 * de concaténation de chaînes).
 */
@Component
public class TenantStandardCodesProviderAdapter implements TenantStandardCodesProvider {

    private final EntityManager em;

    @Autowired
    public TenantStandardCodesProviderAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<String> findAdoptedStandardCodes() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            return List.of();
        }
        UUID tenantId;
        try {
            tenantId = UUID.fromString(tenantIdStr);
        } catch (IllegalArgumentException ex) {
            return List.of();
        }

        return em.createQuery(
                "SELECT ts.standard.code FROM TenantStandard ts WHERE ts.tenantId = :tid",
                String.class)
            .setParameter("tid", tenantId)
            .getResultList();
    }
}
