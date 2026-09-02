package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.billing.BillingProfileDto;

/**
 * Port sortant : une facture et l'identité de son client donnent un PDF.
 *
 * <p>Une interface, et non un appel direct à PDFBox depuis
 * {@link InvoiceService} : les règles d'émission et d'envoi se testent alors
 * sans produire un seul octet de PDF, et le rendu se teste sans base ni
 * abonnement.
 *
 * <p>Le profil de facturation est un PARAMÈTRE et non un champ de la facture :
 * l'identité du client vit dans son profil, qui peut évoluer, tandis que la
 * facture, elle, est figée. Les recopier tous deux dans la pièce aurait
 * doublé une information déjà stockée ailleurs ; le rendu les rapproche au
 * moment où il en a besoin.
 */
public interface InvoiceRenderPort {

    byte[] render(Invoice invoice, BillingProfileDto.View profile);
}
