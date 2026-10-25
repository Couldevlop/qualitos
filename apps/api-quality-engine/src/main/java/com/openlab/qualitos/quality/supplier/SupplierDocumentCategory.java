package com.openlab.qualitos.quality.supplier;

/**
 * Nature d'un document fournisseur. Elle se lit dans la fiche NC imprimée :
 * l'auditeur cherche le 8D du fournisseur et les preuves de CAPA, pas « un PDF ».
 */
public enum SupplierDocumentCategory {
    /** Rapport 8D remis par le fournisseur. */
    EIGHT_D,
    /** Preuve d'une action corrective ou préventive. */
    CAPA_EVIDENCE,
    /** Le certificat lui-même, ou son annexe. */
    CERTIFICATE,
    OTHER
}
