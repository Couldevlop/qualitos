package com.openlab.qualitos.core.config;

import com.openlab.qualitos.core.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Aspect Spring AOP qui active le filtre Hibernate {@code tenantFilter}
 * avant chaque appel de repository, et le désactive après.
 *
 * <p>Cela garantit que toutes les requêtes JPA sur les entités annotées
 * {@code @Filter(name = "tenantFilter")} sont automatiquement filtrées
 * par le tenant courant (extrait du JWT via TenantContext).
 *
 * <p>Si aucun tenant n'est dans le contexte (endpoints publics), le filtre
 * n'est pas activé — la RLS PostgreSQL constitue la deuxième couche de défense.
 */
@Aspect
@Component
public class TenantHibernateFilterInterceptor {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..))")
    public Object applyTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        String tenantId = TenantContext.getTenantId();
        Session session = entityManager.unwrap(Session.class);

        boolean filterEnabled = false;

        if (tenantId != null) {
            Filter filter = session.enableFilter(TenantConfig.FilterNames.TENANT_FILTER);
            filter.setParameter(TenantConfig.FilterNames.TENANT_FILTER_PARAM, tenantId);
            filterEnabled = true;
        }

        try {
            return joinPoint.proceed();
        } finally {
            if (filterEnabled) {
                session.disableFilter(TenantConfig.FilterNames.TENANT_FILTER);
            }
        }
    }
}
