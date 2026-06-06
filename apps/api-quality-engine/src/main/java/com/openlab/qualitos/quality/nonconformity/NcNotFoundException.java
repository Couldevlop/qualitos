package com.openlab.qualitos.quality.nonconformity;

import java.util.UUID;

public class NcNotFoundException extends RuntimeException {
    public NcNotFoundException(UUID id) { super("Non-conformity not found: " + id); }
}
