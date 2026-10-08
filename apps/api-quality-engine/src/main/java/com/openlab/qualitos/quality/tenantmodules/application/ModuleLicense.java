package com.openlab.qualitos.quality.tenantmodules.application;

/**
 * Ce que la licence dit des modules (ADR 0082).
 *
 * <p>En SaaS, rien : les modules s'ouvrent par activation, au gré des
 * abonnements. En on-premise, la licence OUVRE d'office les modules qu'elle
 * couvre ; le client peut en fermer, jamais en ouvrir un qu'elle ne couvre pas.
 */
public interface ModuleLicense {

    /** L'édition SaaS : aucune licence, les activations décident. */
    ModuleLicense NONE = new ModuleLicense() {
        @Override
        public boolean governs() {
            return false;
        }

        @Override
        public boolean allows(String moduleCode) {
            return true;
        }
    };

    /** Vrai quand la licence ouvre d'office les modules qu'elle couvre. */
    boolean governs();

    /** Vrai si la licence couvre ce module (toujours vrai sans licence). */
    boolean allows(String moduleCode);
}
