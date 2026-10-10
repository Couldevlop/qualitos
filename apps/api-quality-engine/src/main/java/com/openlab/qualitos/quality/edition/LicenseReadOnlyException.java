package com.openlab.qualitos.quality.edition;

import com.openlab.qualitos.licensing.domain.LicenseStatus;

/** Une écriture refusée parce que la licence on-premise ne l'autorise plus (ADR 0082). */
public class LicenseReadOnlyException extends RuntimeException {

    private final LicenseStatus status;

    public LicenseReadOnlyException(LicenseStatus status, String reason) {
        super(reason == null || reason.isBlank()
                ? "La licence de cette installation n'autorise plus les modifications (" + status + ")."
                : reason);
        this.status = status;
    }

    public LicenseStatus getStatus() {
        return status;
    }
}
