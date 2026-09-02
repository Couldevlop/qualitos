package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.billing.Money;

/**
 * La mise en forme d'un montant sur une pièce destinée au client.
 *
 * <p>Les montants vivent en centimes entiers : 9900 est un nombre juste et un
 * affichage faux. Imprimer « 9900 EUR » sur une facture, ou « 99 EUR » pour
 * 9900 centimes, est le genre d'erreur qui survit à toute la chaîne de tests
 * parce qu'aucune assertion sur des entiers ne la voit.
 *
 * <p>Virgule décimale et espace insécable fin : c'est la convention française,
 * celle de l'adresse de facturation par défaut. Le format n'est PAS localisé
 * par {@code Locale} implicite — une facture ne doit pas changer de
 * ponctuation selon la machine qui l'imprime, sans quoi deux exemplaires du
 * même document diffèrent.
 */
public final class InvoiceAmounts {

    private InvoiceAmounts() {}

    /** Par exemple {@code 9900 EUR} devient {@code "99,00 EUR"}. */
    public static String format(Money amount) {
        long cents = amount.cents();
        return (cents / 100) + "," + String.format("%02d", cents % 100) + " " + amount.currency();
    }
}
