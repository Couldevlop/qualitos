package com.openlab.qualitos.quality.ishikawa;

import java.util.UUID;

public class IshikawaDiagramNotFoundException extends RuntimeException {

    public IshikawaDiagramNotFoundException(UUID id) {
        super("Ishikawa diagram not found: " + id);
    }
}
