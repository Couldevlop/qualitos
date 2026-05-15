package com.openlab.qualitos.quality.itsm;

/**
 * ITSM providers supportés en V1 (CLAUDE.md §13.3).
 *
 * Extension future : ZENDESK, FRESHSERVICE, OTRS, etc.
 * Chaque provider est implémenté par un bean qui satisfait {@link ItsmProviderClient}.
 */
public enum ItsmProvider {
    SERVICENOW,
    JIRA_SM
}
