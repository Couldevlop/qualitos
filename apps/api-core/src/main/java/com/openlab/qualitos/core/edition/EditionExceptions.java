package com.openlab.qualitos.core.edition;

import com.openlab.qualitos.licensing.domain.LicenseStatus;

/** Les refus liés à l'édition et à la licence (ADR 0082). */
public final class EditionExceptions {

    private EditionExceptions() {}

    /** Un point d'entrée de la plateforme éditeur, appelé dans une installation on-premise : 404. */
    public static class NotInThisEdition extends RuntimeException {
        public NotInThisEdition() {
            super("Cette fonction n'existe pas dans l'édition on-premise.");
        }
    }

    /** Une écriture refusée parce que la licence ne l'autorise plus : 403. */
    public static class LicenseReadOnly extends RuntimeException {

        private final LicenseStatus status;

        public LicenseReadOnly(LicenseStatus status, String reason) {
            super(reason == null || reason.isBlank()
                    ? "La licence de cette installation n'autorise plus les modifications (" + status + ")."
                    : reason);
            this.status = status;
        }

        public LicenseStatus getStatus() {
            return status;
        }
    }

    /** Le plafond d'utilisateurs de la licence est atteint : 409. */
    public static class MemberLimitReached extends RuntimeException {

        private final int limit;

        public MemberLimitReached(int limit) {
            super("La licence couvre " + limit + " utilisateurs actifs : désactivez un compte ou étendez la licence.");
            this.limit = limit;
        }

        public int getLimit() {
            return limit;
        }
    }
}
