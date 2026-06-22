package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierReuseLookup;
import com.openlab.qualitos.quality.standards.normdoc.infrastructure.NormDocJpaEntity;
import com.openlab.qualitos.quality.standards.normdoc.infrastructure.NormDocJpaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Adapter du port {@link DossierReuseLookup} : réutilisation transversale (§8.9).
 * Recherche, parmi les documents normatifs APPROUVÉS du tenant courant (issu du
 * JWT) sur d'AUTRES normes, des pièces équivalentes (même {@link NormDocKind})
 * réutilisables. Le filtrage par tenant garantit l'isolation (OWASP A01).
 */
@Component
public class NormDocReuseLookupAdapter implements DossierReuseLookup {

    private static final int MAX_CANDIDATES = 5;

    private final NormDocJpaRepository normDocs;
    private final NormDocTenantProvider tenantProvider;

    public NormDocReuseLookupAdapter(
            NormDocJpaRepository normDocs,
            @Qualifier("normDocTenantContextProvider") NormDocTenantProvider tenantProvider) {
        this.normDocs = normDocs;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public List<ReusableDoc> findApprovedByKind(NormDocKind kind, UUID excludeStandardId) {
        UUID tenantId = tenantProvider.requireTenantId();
        return normDocs.findByTenantIdAndKindAndStatusAndStandardIdNot(
                        tenantId, kind, NormDocStatus.APPROUVE, excludeStandardId,
                        PageRequest.of(0, MAX_CANDIDATES, Sort.by("updatedAt").descending()))
                .stream()
                .map(this::toRef)
                .toList();
    }

    private ReusableDoc toRef(NormDocJpaEntity e) {
        return new ReusableDoc(e.getId(), e.getStandardId(), e.getStandardCode(),
                e.getTitle(), e.getKind());
    }
}
