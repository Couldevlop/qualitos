package com.openlab.qualitos.quality.derogation.domain;

/**
 * Le traitement que le domaine fait subir à toute chaîne qu'on lui confie.
 *
 * <p>Une seule règle, posée une seule fois : on retire les blancs de bord, et
 * une chaîne qui ne contient que du blanc vaut {@code null}. Sans cela, un
 * champ laissé vide par un formulaire arriverait tantôt {@code null}, tantôt
 * {@code ""}, tantôt {@code " "} — et le contrôle de complétude dirait oui à un
 * dossier vide parce qu'une espace y traîne.
 */
final class Texte {

    private Texte() {
    }

    static String net(String valeur) {
        if (valeur == null) {
            return null;
        }
        String taille = valeur.strip();
        return taille.isEmpty() ? null : taille;
    }

    static boolean vide(String valeur) {
        return net(valeur) == null;
    }
}
