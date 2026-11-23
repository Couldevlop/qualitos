package com.openlab.qualitos.quality.derogation.domain;

/**
 * Section 4 — cause racine, confinement, action corrective, et les renvois.
 *
 * <p>C'est ce que le client veut savoir après « pourquoi c'est acceptable » :
 * pourquoi c'est arrivé, et ce qui fait que ça n'arrivera plus. Deux de ces
 * champs — la cause et l'action corrective — comptent parmi les six exigés à la
 * demande de signature.
 *
 * @param capaReference    « Réf. 8D / CAPA et date prévue » : un texte libre, et
 *                         non une clé — la maquette l'écrit ainsi
 * @param attachmentsNote  « Pièces jointes » : une ligne de texte citant
 *                         « mesures, photos, rapport 8D ». Un dépôt de fichiers
 *                         réels est un chantier à part, non demandé.
 */
public record Analysis(
        String rootCause,
        String containment,
        String correctiveAction,
        String capaReference,
        String attachmentsNote
) {
    public Analysis {
        rootCause = Texte.net(rootCause);
        containment = Texte.net(containment);
        correctiveAction = Texte.net(correctiveAction);
        capaReference = Texte.net(capaReference);
        attachmentsNote = Texte.net(attachmentsNote);
    }

    /** L'analyse d'une demande qui vient d'être ouverte : rien encore. */
    public static Analysis vide() {
        return new Analysis(null, null, null, null, null);
    }
}
