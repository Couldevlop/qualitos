package com.openlab.qualitos.core.platformaudit;

/**
 * Qui a agi : une personne, ou la machine.
 *
 * <p>La distinction n'est pas décorative. Devant un auditeur, « le tarif a
 * changé » et « quelqu'un a changé le tarif » ne sont pas la même phrase :
 * la seconde engage une personne. La base refuse donc un acte humain sans
 * identité, et un acte machine qui s'en inventerait une
 * ({@code ck_platform_audit_human_is_named}, V12).
 *
 * <p>Pas de valeur {@code EXTERNAL}, contrairement au journal du moteur de
 * qualité : la plateforme n'a pas d'acteur extérieur. Un consultant de
 * cabinet qui agit chez un client est un {@code USER} — il a un compte, un
 * {@code sub}, et ses actes doivent lui être imputés nommément.
 */
public enum PlatformActorType {

    /** Une personne authentifiée, identifiée par le {@code sub} de son jeton. */
    USER,

    /** La plateforme elle-même : provisionnement, réconciliation, migration. */
    SYSTEM,

    /** Une tâche planifiée : renouvellement, relance, vérification de chaîne. */
    SCHEDULER
}
