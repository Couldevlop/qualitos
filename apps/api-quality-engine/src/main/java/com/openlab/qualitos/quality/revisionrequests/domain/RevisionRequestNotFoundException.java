package com.openlab.qualitos.quality.revisionrequests.domain;

import java.util.UUID;

/** 404. Une demande d'un autre tenant lève la meme chose : un 403 confirmerait son existence. */
public class RevisionRequestNotFoundException extends RuntimeException {

    public RevisionRequestNotFoundException(UUID id) {
        super("Revision request not found: " + id);
    }
}
