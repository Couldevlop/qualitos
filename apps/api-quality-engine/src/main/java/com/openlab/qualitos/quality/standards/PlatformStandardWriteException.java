package com.openlab.qualitos.quality.standards;

/**
 * Tentative d'écriture sur une norme de la plateforme (403).
 *
 * <p>Le catalogue livré se maintient par migrations, jamais par l'API d'un
 * tenant — fût-il super-admin : une ISO 9001 modifiée par un tenant deviendrait
 * une ISO 9001 différente pour tous les autres.
 *
 * <p>403 et non 404, contrairement au référentiel d'un AUTRE tenant : la norme
 * plateforme est visible de tous, son existence n'est donc pas un secret — la
 * masquer derrière un 404 laisserait croire à une faute de frappe.
 */
public class PlatformStandardWriteException extends RuntimeException {
    public PlatformStandardWriteException() {
        super("Une norme de la plateforme ne se modifie pas depuis cette API");
    }
}
