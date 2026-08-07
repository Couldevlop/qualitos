package com.openlab.qualitos.quality.ishikawa;

import java.util.UUID;

public class IshikawaActionNotFoundException extends RuntimeException {
    public IshikawaActionNotFoundException(UUID id) {
        super("Ishikawa action not found: " + id);
    }
}
