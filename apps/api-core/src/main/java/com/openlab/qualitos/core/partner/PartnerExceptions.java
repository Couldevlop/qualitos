package com.openlab.qualitos.core.partner;

import java.util.UUID;

/**
 * Ce que le domaine partenaire refuse, et le mot qu'il met dessus.
 *
 * <p>Chaque refus porte sa raison en clair : un « 409 » nu oblige celui qui le
 * recoit a deviner, et il devine mal.
 */
public final class PartnerExceptions {

    private PartnerExceptions() {
    }

    /** Le cabinet n'existe pas. */
    public static class FirmNotFound extends RuntimeException {
        public FirmNotFound(UUID id) {
            super("Cabinet introuvable : " + id);
        }
    }

    /** Le consultant n'existe pas, ou n'appartient pas a ce cabinet. */
    public static class ConsultantNotFound extends RuntimeException {
        public ConsultantNotFound(UUID id) {
            super("Consultant introuvable : " + id);
        }
    }

    /** L'accord n'existe pas, ou ne concerne pas ce client. */
    public static class EngagementNotFound extends RuntimeException {
        public EngagementNotFound(UUID id) {
            super("Accord introuvable : " + id);
        }
    }

    /** Deux cabinets ne peuvent pas porter le meme identifiant d'URL. */
    public static class FirmAlreadyExists extends RuntimeException {
        public FirmAlreadyExists(String slug) {
            super("Un cabinet porte deja l'identifiant : " + slug);
        }
    }

    /** Le meme courriel est deja inscrit dans ce cabinet. */
    public static class ConsultantAlreadyExists extends RuntimeException {
        public ConsultantAlreadyExists(String email) {
            super("Ce cabinet compte deja un consultant avec l'adresse : " + email);
        }
    }

    /**
     * Un accord vivant existe deja entre ce client et ce cabinet.
     *
     * <p>En proposer un second rendrait indecidable lequel borne l'acces.
     */
    public static class EngagementAlreadyLive extends RuntimeException {
        public EngagementAlreadyLive() {
            super("Un accord est deja en cours entre ce client et ce cabinet : "
                    + "reprenez-le ou laissez-le expirer avant d'en proposer un autre.");
        }
    }

    /** L'accord n'est pas dans un etat qui permet ce geste. */
    public static class EngagementNotDecidable extends RuntimeException {
        public EngagementNotDecidable(EngagementStatus status) {
            super("Cet accord est " + status + " : il ne peut plus etre accorde ni refuse.");
        }
    }

    /** On a demande un acces que l'accord n'ouvre pas. */
    public static class AccessNotGranted extends RuntimeException {
        public AccessNotGranted(String detail) {
            super(detail);
        }
    }
}
