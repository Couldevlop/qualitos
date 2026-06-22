package com.openlab.qualitos.quality.standards.normdoc.dossier.domain;

/**
 * État d'un dossier documentaire multi-documents (Standards Hub §8.8 —
 * génération avancée en lot). Liste fermée :
 *
 * <ul>
 *   <li>{@link #GENERATION_EN_COURS} : la génération IA en lot est lancée, des
 *       documents sont encore en attente / en cours de rédaction.</li>
 *   <li>{@link #GENERE} : tous les documents cibles ont été générés (chacun en
 *       brouillon IA) ; le dossier attend la validation humaine de ses pièces.</li>
 *   <li>{@link #FINALISE} : tous les documents liés sont approuvés et signés ;
 *       le dossier porte une empreinte SHA-256 signée ML-DSA + ancrage
 *       blockchain (preuve d'intégrité audit-ready).</li>
 * </ul>
 */
public enum DossierStatus {
    GENERATION_EN_COURS,
    GENERE,
    FINALISE
}
