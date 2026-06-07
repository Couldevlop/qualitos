package com.openlab.qualitos.quality.nonconformity;

import java.util.UUID;

public class NcPhotoNotFoundException extends RuntimeException {
    public NcPhotoNotFoundException(UUID id) { super("Non-conformity photo not found: " + id); }
}
