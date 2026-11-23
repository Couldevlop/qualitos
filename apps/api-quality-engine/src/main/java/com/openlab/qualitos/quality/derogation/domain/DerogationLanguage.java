package com.openlab.qualitos.quality.derogation.domain;

import java.util.Locale;

/**
 * La langue du courrier envoyé au client : français ou anglais.
 *
 * <p><b>Deux seulement</b>, là où la plateforme en sert six. C'est le besoin, pas
 * une limite technique : une dérogation part à un client industriel, et ces deux
 * langues couvrent ce qui a été demandé.
 *
 * <p>Elle se choisit à l'envoi et se <b>retient sur le dossier</b>. Le PDF
 * n'étant pas scellé il peut être réémis, et sans cela un second envoi partirait
 * dans une autre langue sans que personne l'ait décidé — même garde-fou que
 * l'ADR 0129 pour le rapport 8D, qui fige la langue à l'émission plutôt que de
 * traduire à la relecture.
 */
public enum DerogationLanguage {

    FR("fr"),
    EN("en");

    private final String code;

    DerogationLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** La langue d'un code stocké ; {@code null} et l'inconnu donnent le français. */
    public static DerogationLanguage of(String code) {
        if (code == null) {
            return FR;
        }
        return EN.code.equalsIgnoreCase(code.trim()) ? EN : FR;
    }

    public Locale locale() {
        return this == EN ? Locale.ENGLISH : Locale.FRENCH;
    }
}
