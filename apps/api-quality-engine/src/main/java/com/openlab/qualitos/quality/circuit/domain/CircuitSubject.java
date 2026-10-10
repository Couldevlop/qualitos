package com.openlab.qualitos.quality.circuit.domain;

import com.openlab.qualitos.quality.authz.domain.Permission;

/**
 * Ce qu'un circuit de validation fait approuver.
 *
 * <p>Une valeur par type d'objet ; le code est ce que stocke la base et ce que
 * l'écran nomme. Un nouveau type d'objet s'ajoute ici, puis se branche dans son
 * service (soumission → départ du circuit, approbation → décision).
 */
public enum CircuitSubject {

    /** Une version de document soumise à revue. */
    DOCUMENT_VERSION("document-version", Permission.DOCUMENT_APPROVE);

    private final String code;
    private final Permission decidePermission;

    CircuitSubject(String code, Permission decidePermission) {
        this.code = code;
        this.decidePermission = decidePermission;
    }

    public String code() {
        return code;
    }

    /** Le droit qu'exige l'écran de décision : le rôle d'une étape doit l'avoir. */
    public Permission decidePermission() {
        return decidePermission;
    }

    public static CircuitSubject fromCode(String code) {
        for (CircuitSubject s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new CircuitException(CircuitException.Reason.UNKNOWN_SUBJECT, "Type d'objet inconnu : " + code);
    }
}
