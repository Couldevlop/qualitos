package com.openlab.qualitos.quality.derogation.application;

import com.openlab.qualitos.quality.derogation.domain.Derogation;
import com.openlab.qualitos.quality.derogation.domain.DerogationLanguage;

/**
 * Le document joint au courriel du client.
 *
 * <p>Il est <b>conforme à l'interface d'édition</b> : il reprend la fiche telle
 * qu'elle se présente à l'écran, ses six sections dans leur ordre, et rien
 * d'autre. Ce n'est donc ni un extrait, ni une mise en page inventée pour
 * l'occasion — ce que le client lit est ce que l'initiateur a rempli.
 *
 * <p>Il n'est <b>pas scellé</b> : ni signature cryptographique, ni ancrage. La
 * pièce opposable est le PDF que le client renvoie signé de son côté.
 */
public interface DerogationPdfPort {

    byte[] render(Derogation dossier, DerogationLanguage langue);
}
