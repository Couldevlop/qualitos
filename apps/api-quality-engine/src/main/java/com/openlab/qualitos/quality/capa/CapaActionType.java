package com.openlab.qualitos.quality.capa;

/**
 * Nature d'une action d'un dossier CAPA (§4.2).
 *
 * <p>Le TYPE DU DOSSIER ({@link CapaType}) dit pourquoi le dossier a été ouvert.
 * Celui-ci dit ce que chaque action fait réellement, et les deux ne se déduisent
 * pas l'un de l'autre : un dossier correctif porte presque toujours, en plus de
 * ses actions correctives, une ou deux mesures prises le jour même pour arrêter
 * l'hémorragie.
 *
 * <p>La distinction n'est pas cosmétique. Elle sépare ce qui <b>arrête l'effet</b>
 * de ce qui <b>supprime la cause</b> — la distinction même que porte l'ISO 9001
 * §10.2 et l'étape D3 de la méthode 8D. Sans elle, un dossier où l'on a trié le
 * lot suspect et rien d'autre se lit exactement comme un dossier où l'on a
 * corrigé le réglage de la presse : les deux affichent « toutes les actions
 * faites », et le second seul empêche la récidive.
 */
public enum CapaActionType {

    /**
     * Mesure d'endiguement : elle contient l'effet sans toucher à la cause
     * (trier un lot, arrêter une ligne, informer un client, remettre en conformité
     * l'existant). Temporaire par nature — on la lève quand la cause est traitée.
     */
    CONTAINMENT,

    /** Action qui supprime la cause d'un écart survenu. */
    CORRECTIVE,

    /** Action qui supprime la cause d'un écart qui n'est pas encore survenu. */
    PREVENTIVE
}
