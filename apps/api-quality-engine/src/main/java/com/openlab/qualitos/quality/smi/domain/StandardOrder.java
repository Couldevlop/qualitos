package com.openlab.qualitos.quality.smi.domain;

import java.util.Comparator;
import java.util.Locale;

/**
 * L'ordre dans lequel le SMI range les normes : celui qu'un lecteur attend.
 *
 * <p>Comparés comme du texte, {@code iso-14001} et {@code iso-45001} passent
 * avant {@code iso-9001} (« 1 » et « 4 » précèdent « 9 ») : le tableau de bord
 * d'un client 9001 + 14001 ouvrait sur la norme qu'il venait d'ajouter. Ici,
 * les suites de chiffres se comparent par leur valeur — ISO 9001, puis
 * ISO 14001, puis ISO 45001 — et le reste du code, lettre à lettre.
 */
public final class StandardOrder {

    /** Ordre naturel des codes ; {@code null} en dernier. */
    public static final Comparator<String> BY_CODE =
            Comparator.nullsLast(StandardOrder::compare);

    private StandardOrder() {}

    static int compare(String a, String b) {
        String x = a.toLowerCase(Locale.ROOT);
        String y = b.toLowerCase(Locale.ROOT);
        int i = 0;
        int j = 0;
        while (i < x.length() && j < y.length()) {
            char cx = x.charAt(i);
            char cy = y.charAt(j);
            if (Character.isDigit(cx) && Character.isDigit(cy)) {
                int finX = finDesChiffres(x, i);
                int finY = finDesChiffres(y, j);
                int parValeur = compareNombres(x.substring(i, finX), y.substring(j, finY));
                if (parValeur != 0) {
                    return parValeur;
                }
                i = finX;
                j = finY;
            } else {
                if (cx != cy) {
                    return Character.compare(cx, cy);
                }
                i++;
                j++;
            }
        }
        return Integer.compare(x.length() - i, y.length() - j);
    }

    private static int finDesChiffres(String s, int debut) {
        int k = debut;
        while (k < s.length() && Character.isDigit(s.charAt(k))) {
            k++;
        }
        return k;
    }

    /** Sans conversion numérique : un code de vingt chiffres ne déborde pas. */
    private static int compareNombres(String a, String b) {
        String x = a.replaceFirst("^0+(?=.)", "");
        String y = b.replaceFirst("^0+(?=.)", "");
        if (x.length() != y.length()) {
            return Integer.compare(x.length(), y.length());
        }
        return x.compareTo(y);
    }
}
