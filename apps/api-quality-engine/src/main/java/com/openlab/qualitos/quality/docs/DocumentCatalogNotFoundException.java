package com.openlab.qualitos.quality.docs;

import java.util.UUID;

/** Un dossier ou un processus introuvable chez ce client. */
public class DocumentCatalogNotFoundException extends RuntimeException {
    public DocumentCatalogNotFoundException(String what, UUID id) { super(what + " not found: " + id); }
}
