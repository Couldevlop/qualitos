package com.openlab.qualitos.quality.docs;

/**
 * Où en est un document, au-delà de son archivage (ADR 0094).
 *
 * <ul>
 *   <li>{@code DRAFT} : aucune version n'a jamais été publiée ;</li>
 *   <li>{@code IN_FORCE} : une version publiée fait foi, rien n'est en préparation ;</li>
 *   <li>{@code IN_REVISION} : une version publiée fait foi et la suivante se prépare.</li>
 * </ul>
 */
public enum DocumentLifecycle {
    DRAFT, IN_FORCE, IN_REVISION
}
