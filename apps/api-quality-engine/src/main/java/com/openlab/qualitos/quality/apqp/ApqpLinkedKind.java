package com.openlab.qualitos.quality.apqp;

/**
 * Le module visé par un livrable de genre {@code MODULE_LINK}.
 *
 * <p>Ces quatre-là seulement : ce sont ceux dont l'enregistrement porte un
 * identifiant stable et vérifiable dans ce même service. Renvoyer vers un module
 * qu'on ne peut pas interroger reviendrait à accepter des liens morts — et un
 * lien mort est pire qu'une absence de lien, puisqu'il affirme qu'une preuve
 * existe.
 */
public enum ApqpLinkedKind { FMEA, CONTROL_PLAN, PDCA, CAPA }
