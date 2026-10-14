package com.openlab.qualitos.quality.audit;

/**
 * Un rapport ne s'émet pas incomplet : sans synthèse ni conclusion, ce qui serait
 * remis ne dirait rien de l'audit. Distinct d'un état incompatible (409) — ici,
 * c'est le contenu qui manque, et l'utilisateur sait quoi compléter.
 */
public class AuditReportIncompleteException extends RuntimeException {
    public AuditReportIncompleteException(String message) { super(message); }
}
