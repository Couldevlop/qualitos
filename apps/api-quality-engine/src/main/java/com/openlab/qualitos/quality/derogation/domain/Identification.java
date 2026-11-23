package com.openlab.qualitos.quality.derogation.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Section 1 — qui, quelle pièce, combien.
 *
 * <p>Un objet de valeur et non huit champs épars : les huit répondent à la même
 * question — de quoi parle cette demande — et se remplacent ensemble. Le client
 * est un texte libre, la plateforme n'ayant pas de registre de clients.
 *
 * @param partReference « Réf. pièce / désignation » : c'est ce champ que la
 *                      colonne « produit » du registre affiche et que son filtre
 *                      interroge
 */
public record Identification(
        String customerName,
        String customerQualityContact,
        String customerOrderNo,
        String partReference,
        String partIndex,
        String batchNumber,
        BigDecimal quantity,
        LocalDate requestDate
) {
    public Identification {
        customerName = Texte.net(customerName);
        customerQualityContact = Texte.net(customerQualityContact);
        customerOrderNo = Texte.net(customerOrderNo);
        partReference = Texte.net(partReference);
        partIndex = Texte.net(partIndex);
        batchNumber = Texte.net(batchNumber);
    }
}
