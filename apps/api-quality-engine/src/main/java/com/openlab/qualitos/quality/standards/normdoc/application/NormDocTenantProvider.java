package com.openlab.qualitos.quality.standards.normdoc.application;

import java.util.UUID;

/** Port : tenant courant (issu du JWT, jamais du body). */
public interface NormDocTenantProvider {
    UUID requireTenantId();
}
