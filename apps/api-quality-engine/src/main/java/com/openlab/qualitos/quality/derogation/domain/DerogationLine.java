package com.openlab.qualitos.quality.derogation.domain;

import java.math.BigDecimal;

/**
 * Une ligne d'écart : ce que le client exige, ce qu'on a réellement, et combien
 * de pièces sont concernées (section 3).
 *
 * <p>Un dossier en porte <b>plusieurs</b> — la maquette ouvre deux lignes vides
 * et donne un bouton « + Ajouter une ligne ». La colonne « défaut » du registre
 * affiche la <b>première</b> ; les autres se lisent dans la fiche.
 *
 * @param position le rang dans le tableau, à partir de 1 : c'est lui qui dit
 *                 laquelle est la première
 */
public record DerogationLine(
        int position,
        String specification,
        String actualValue,
        BigDecimal nonConformingQty
) {
    public DerogationLine {
        specification = Texte.net(specification);
        actualValue = Texte.net(actualValue);
    }

    /**
     * La ligne dit-elle quelque chose ?
     *
     * <p>La maquette ouvre deux lignes VIDES : les retenir telles quelles
     * remplirait le registre de défauts sans texte, et la colonne « défaut »
     * afficherait du blanc sur des dossiers qui en portent un plus bas.
     */
    public boolean isBlank() {
        return specification == null && actualValue == null && nonConformingQty == null;
    }

    /** « Cote 12,4 attendue → 12,9 mesurée » : la ligne dite en une phrase. */
    public String summary() {
        if (specification == null) {
            return actualValue;
        }
        return actualValue == null ? specification : specification + " → " + actualValue;
    }

    /** La même ligne à un autre rang, l'ordre étant refait à chaque saisie. */
    public DerogationLine at(int rang) {
        return new DerogationLine(rang, specification, actualValue, nonConformingQty);
    }
}
