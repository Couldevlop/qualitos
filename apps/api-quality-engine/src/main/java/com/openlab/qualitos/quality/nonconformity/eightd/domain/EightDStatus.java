package com.openlab.qualitos.quality.nonconformity.eightd.domain;

/**
 * Les deux états d'un rapport 8D.
 *
 * <p>Un seul passage, dans un seul sens. {@code DRAFT} se remanie autant qu'on
 * veut ; {@code ISSUED} ne se touche plus, parce qu'il porte une empreinte
 * signée et ancrée : un 8D qui change après son émission n'est plus un 8D, c'est
 * une page web.
 */
public enum EightDStatus {

    /** En préparation : les trois disciplines saisies se remplissent encore. */
    DRAFT,

    /** Émis à la clôture : contenu figé, empreinte signée, ancrage posé. */
    ISSUED
}
