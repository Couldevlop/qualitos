package com.openlab.qualitos.quality.standards.normdoc.dossier.domain;

import java.util.List;

/**
 * Port — fournit le plan documentaire (pièces + sections) à générer pour une
 * adoption de norme. Industry-agnostic : le plan est dérivé de la trame de
 * système de management (HLS) + d'un éventuel sous-ensemble demandé par
 * l'utilisateur (clés de pièces) ; aucune logique sectorielle codée en dur ici
 * (CLAUDE.md §18.2 #9, l'adaptation passe par Industry Packs).
 */
public interface DossierPlanProvider {

    /**
     * @param requestedDocumentKeys clés des pièces souhaitées (vide = plan complet
     *                              par défaut : Manuel + Politique + procédures clés).
     * @return le plan documentaire ordonné.
     */
    DossierPlan planFor(List<String> requestedDocumentKeys);

    /** Clés disponibles du catalogue de pièces (pour l'UI de sélection). */
    List<DossierDocument> catalog();
}
