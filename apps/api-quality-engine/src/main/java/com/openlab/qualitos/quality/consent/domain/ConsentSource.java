package com.openlab.qualitos.quality.consent.domain;

/** Canal par lequel le consentement a été collecté — utile pour la preuve. */
public enum ConsentSource {
    WEB_FORM,
    MOBILE_APP,
    EMAIL,
    PAPER,
    PHONE,
    API,
    IMPORT,
    OTHER
}
