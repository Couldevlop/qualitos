package com.openlab.qualitos.quality.derogation.domain;

import java.util.List;

/**
 * La fiche n'est pas complète pour le geste demandé.
 *
 * <p>Elle porte la LISTE des champs qui manquent, et non un refus sec : la
 * maquette affiche « Champs obligatoires manquants : client, réf. pièce,
 * quantité, justification, cause, action corrective », et c'est cette phrase-là
 * qui permet de corriger sans deviner.
 */
public class DerogationValidationException extends RuntimeException {

    private final List<String> missingFields;

    public DerogationValidationException(String message) {
        this(message, List.of());
    }

    public DerogationValidationException(String message, List<String> missingFields) {
        super(message);
        this.missingFields = List.copyOf(missingFields);
    }

    public List<String> getMissingFields() {
        return missingFields;
    }
}
