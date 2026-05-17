package com.openlab.qualitos.quality.aieudb.domain;

/**
 * Statuts d'un enregistrement EUDB (AI Act Art. 49 / 71).
 *
 *   DRAFT → SUBMITTED → REGISTERED → UPDATED (peut boucler)
 *   DRAFT|SUBMITTED → REJECTED
 *   REGISTERED|UPDATED → RETIRED
 */
public enum EudbRegistrationStatus {
    DRAFT,
    SUBMITTED,
    REGISTERED,
    UPDATED,
    REJECTED,
    RETIRED
}
