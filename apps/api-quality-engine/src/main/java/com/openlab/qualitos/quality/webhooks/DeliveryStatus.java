package com.openlab.qualitos.quality.webhooks;

public enum DeliveryStatus {
    PENDING,
    SUCCESS,
    FAILED,
    RETRYING,
    /** Dead-letter — abandonnée après épuisement des tentatives. */
    DEAD_LETTER
}
