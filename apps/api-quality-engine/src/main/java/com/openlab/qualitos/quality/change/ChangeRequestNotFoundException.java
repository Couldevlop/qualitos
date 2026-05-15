package com.openlab.qualitos.quality.change;

import java.util.UUID;

public class ChangeRequestNotFoundException extends RuntimeException {
    public ChangeRequestNotFoundException(UUID id) { super("Change request not found: " + id); }
}
