package com.openlab.qualitos.quality.config;

/**
 * Le tenant n'a pas souscrit le module dont dépend l'écriture demandée.
 *
 * <p>Exception <b>distincte</b> d'un refus de droits, et c'est tout l'intérêt :
 * les deux se règlent à des endroits opposés. Un manque de droits se traite avec
 * l'administrateur du tenant ; un module non souscrit se traite avec l'éditeur.
 * Les confondre envoie l'utilisateur frapper à la mauvaise porte — le même
 * travers que 401 rendu comme 403.
 */
public class ModuleNotEnabledException extends RuntimeException {

    private final String moduleCode;

    public ModuleNotEnabledException(String moduleCode) {
        super("Module not enabled: " + moduleCode);
        this.moduleCode = moduleCode;
    }

    /**
     * Le code du module manquant.
     *
     * <p>Exposé dans la réponse : ce n'est pas une information interne mais le
     * nom d'une ligne du catalogue public, celle que l'administrateur doit
     * activer. Le taire obligerait à deviner lequel des vingt modules manque.
     */
    public String getModuleCode() {
        return moduleCode;
    }
}
