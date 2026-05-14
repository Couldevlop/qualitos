package com.openlab.qualitos.quality.ishikawa;

import java.util.UUID;

public class IshikawaCauseNotFoundException extends RuntimeException {

    public IshikawaCauseNotFoundException(UUID id) {
        super("Ishikawa cause not found: " + id);
    }
}
