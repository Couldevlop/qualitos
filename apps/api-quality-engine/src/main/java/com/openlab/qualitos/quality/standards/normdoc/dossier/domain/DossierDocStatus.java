package com.openlab.qualitos.quality.standards.normdoc.dossier.domain;

/**
 * Statut de génération d'UN document cible au sein du dossier (suivi de
 * progression par document, §8.8). Indépendant du cycle de validation humaine
 * du {@code NormativeDocument} lié (qui porte BROUILLON_IA → APPROUVE) : ce
 * statut-ci ne décrit que l'étape de génération IA en lot.
 */
public enum DossierDocStatus {
    /** Planifié, génération pas encore tentée. */
    EN_ATTENTE,
    /** Génération IA en cours. */
    EN_GENERATION,
    /** Document généré (brouillon IA persisté, lié par {@code normDocId}). */
    GENERE,
    /** Génération échouée (timeout / erreur IA) — relançable. */
    ECHEC,
    /** Réutilisé : une pièce équivalente déjà approuvée a été proposée (§8.9). */
    REUTILISE
}
