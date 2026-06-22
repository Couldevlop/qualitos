package com.openlab.qualitos.quality.standards.normdoc.dossier.application;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;

import java.util.List;
import java.util.UUID;

/**
 * Port — réutilisation transversale (§8.9 / IMS) : recherche, parmi les documents
 * normatifs DÉJÀ APPROUVÉS du tenant (sur d'autres normes déjà couvertes), une
 * pièce équivalente (même {@link NormDocKind}) qui pourrait être réutilisée
 * plutôt que régénérée. Le tenant est issu du JWT côté adapter.
 */
public interface DossierReuseLookup {

    /**
     * @param kind             type de pièce recherché.
     * @param excludeStandardId norme du dossier en cours (à exclure des candidats).
     * @return candidats réutilisables, du plus récent au plus ancien.
     */
    List<ReusableDoc> findApprovedByKind(NormDocKind kind, UUID excludeStandardId);

    /** Référence d'un document approuvé réutilisable. */
    record ReusableDoc(UUID normDocId, UUID standardId, String standardCode,
                       String title, NormDocKind kind) {}
}
