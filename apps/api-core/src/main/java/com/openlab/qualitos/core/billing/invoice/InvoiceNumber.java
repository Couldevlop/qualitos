package com.openlab.qualitos.core.billing.invoice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * La numérotation des factures : {@code FA-<exercice>-<rang>}.
 *
 * <p><b>Continue et sans trou, par exercice.</b> Ce n'est pas une coquetterie :
 * une numérotation à trous est un motif de rejet en contrôle fiscal, parce que
 * rien ne distingue un numéro sauté d'une facture détruite. D'où deux gestes
 * seulement — {@link #first(int)} et {@link #next(String)} — et aucun moyen de
 * fabriquer un numéro arbitraire.
 *
 * <p><b>Le rang déborde vers le haut, il ne repart pas à zéro.</b> Quatre
 * chiffres suffisent à un éditeur qui émet moins de dix mille factures par an ;
 * le jour où ce n'est plus vrai, {@code FA-2031-9999} est suivi de
 * {@code FA-2031-10000}, jamais de {@code FA-2031-0000}. Une remise à zéro
 * réutiliserait un numéro déjà attribué — deux pièces comptables portant la
 * même référence, ce qui est pire que le débordement de format qu'on aurait
 * voulu éviter. La contrainte SQL {@code chk_invoice_number} accepte le même
 * élargissement ({@code [0-9]{4,}}).
 *
 * <p><b>Ce que cette classe ne fait pas :</b> garantir l'unicité. Deux
 * émissions concurrentes peuvent lire le même « dernier numéro » et calculer
 * le même suivant. C'est {@code uk_invoice_number} qui les départage, en base ;
 * la seconde échoue et sera rejouée. Confier cette garantie au calcul en
 * mémoire reviendrait à supposer un seul processus — ce que deux répliques
 * démentent.
 */
public final class InvoiceNumber {

    private static final String PREFIX = "FA";
    private static final int RANK_WIDTH = 4;
    private static final Pattern FORMAT = Pattern.compile("^FA-([0-9]{4})-([0-9]{4,})$");

    private InvoiceNumber() {}

    /** Le premier numéro d'un exercice. Chaque exercice repart de 1. */
    public static String first(int fiscalYear) {
        requireFiscalYear(fiscalYear);
        return format(fiscalYear, 1);
    }

    /**
     * Le numéro qui suit immédiatement celui donné, dans le MÊME exercice.
     *
     * <p>Changer d'exercice passe par {@link #first(int)} : incrémenter au
     * travers d'un 31 décembre produirait {@code FA-2026-0413} en janvier 2027,
     * et l'exercice porté par le numéro ne correspondrait plus à celui de la
     * facture.
     */
    public static String next(String previous) {
        Matcher matcher = FORMAT.matcher(requireText(previous));
        if (!matcher.matches()) {
            // Un numéro illisible ne se « répare » pas en repartant de 1 : cela
            // réattribuerait des numéros déjà émis. On refuse, et quelqu'un
            // regarde ce qu'il y a en base.
            throw new IllegalArgumentException(
                    "Numero de facture illisible, la suite ne peut pas etre calculee : " + previous);
        }
        int fiscalYear = Integer.parseInt(matcher.group(1));
        long rank = Long.parseLong(matcher.group(2));
        return format(fiscalYear, rank + 1);
    }

    /** L'exercice porté par un numéro — la source de vérité, c'est le numéro. */
    public static int fiscalYearOf(String number) {
        Matcher matcher = FORMAT.matcher(requireText(number));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Numero de facture illisible : " + number);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static String format(int fiscalYear, long rank) {
        // %04d pose le format minimal ; au-delà de 9999, le rang s'écrit sur
        // autant de chiffres qu'il en faut.
        return PREFIX + "-" + fiscalYear + "-" + String.format("%0" + RANK_WIDTH + "d", rank);
    }

    private static void requireFiscalYear(int fiscalYear) {
        // Même fenêtre que chk_invoice_year : un exercice à trois chiffres
        // casserait le format, et un exercice à cinq aussi.
        if (fiscalYear < 2000 || fiscalYear > 2999) {
            throw new IllegalArgumentException("Exercice hors plage : " + fiscalYear);
        }
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Numero de facture absent");
        }
        return value;
    }
}
