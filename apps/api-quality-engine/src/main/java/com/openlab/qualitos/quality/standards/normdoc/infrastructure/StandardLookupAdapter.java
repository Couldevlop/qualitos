package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.standards.Standard;
import com.openlab.qualitos.quality.standards.StandardRepository;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocStandardLookup;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter du port {@link NormDocStandardLookup} sur le catalogue normatif JPA.
 *
 * <p>Le catalogue mêle désormais des normes de plateforme et des référentiels
 * appartenant à des tenants (V108) : cet adaptateur ne tourne jamais hors d'une
 * requête (génération de document normatif — {@code NormDocService.generate},
 * {@code DossierService.start}/{@code retryFailed} — appelées uniquement depuis
 * des endpoints authentifiés), le tenant est donc toujours disponible via
 * {@link NormDocTenantProvider} et DOIT filtrer la lecture : sans ce filtre, un
 * tenant pourrait générer un document à partir du référentiel privé d'un autre
 * tenant.
 */
@Component
public class StandardLookupAdapter implements NormDocStandardLookup {

    private final StandardRepository standards;
    private final NormDocTenantProvider tenantProvider;

    public StandardLookupAdapter(StandardRepository standards,
                                  @Qualifier("normDocTenantContextProvider") NormDocTenantProvider tenantProvider) {
        this.standards = standards;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public Optional<StandardRef> findById(UUID standardId) {
        return standards.findVisibleById(standardId, tenantProvider.requireTenantId())
                .map(this::toRef);
    }

    private StandardRef toRef(Standard s) {
        return new StandardRef(s.getId(), s.getCode(), s.getFullName());
    }
}
