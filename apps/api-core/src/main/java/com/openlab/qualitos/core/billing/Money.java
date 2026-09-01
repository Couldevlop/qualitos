package com.openlab.qualitos.core.billing;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Un montant : un nombre ENTIER de centièmes, et sa devise.
 *
 * <p>Entier et non décimal : {@code 0.1 + 0.2} ne fait pas {@code 0.3} en
 * virgule flottante, et sur une facture cet écart devient un litige avec le
 * client. On compte en centimes, on divise à l'affichage seulement.
 *
 * <p>La devise voyage AVEC le montant. Un montant sans devise n'est pas un
 * montant, et additionner des euros à des dollars donne un nombre qui ne veut
 * rien dire — d'où le refus, plutôt qu'une conversion implicite dont personne
 * ne connaîtrait le taux.
 */
public record Money(long cents, String currency) {

    private static final Pattern ISO_4217 = Pattern.compile("^[A-Z]{3}$");

    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("Un montant ne peut pas etre negatif : " + cents);
        }
        Objects.requireNonNull(currency, "devise obligatoire");
        if (!ISO_4217.matcher(currency).matches()) {
            throw new IllegalArgumentException("Devise hors ISO 4217 : " + currency);
        }
    }

    public static Money of(long cents, String currency) {
        return new Money(cents, currency);
    }

    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "devises differentes : " + currency + " et " + other.currency);
        }
        return new Money(cents + other.cents, currency);
    }

    public Money times(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantite negative : " + quantity);
        }
        return new Money(cents * quantity, currency);
    }
}
