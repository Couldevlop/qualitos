package com.openlab.qualitos.quality.revisionrequests.domain;

/** 409. Une décision sur une demande déjà décidée. */
public class RevisionRequestStateException extends RuntimeException {

    public RevisionRequestStateException(String message) {
        super(message);
    }
}
