package com.openlab.qualitos.core.billing.invoice;

import java.util.UUID;

/**
 * La facture demandée n'existe pas. Traduite en 404 par
 * {@code GlobalExceptionHandler}, comme les autres « introuvable » du module :
 * un identifiant inconnu est une erreur de l'appelant, pas une panne.
 */
public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(UUID invoiceId) {
        super("Facture introuvable : " + invoiceId);
    }
}
