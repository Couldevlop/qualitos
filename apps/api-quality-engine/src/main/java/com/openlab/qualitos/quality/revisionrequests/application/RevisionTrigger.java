package com.openlab.qualitos.quality.revisionrequests.application;

import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;

import java.util.List;

/**
 * Une source de propositions de revision.
 *
 * <p>Le point d'extension du dispositif : brancher une alerte SPC ou une anomalie
 * ML plus tard, c'est ajouter une implementation et un test — le moteur, lui, ne
 * bouge pas. Sans ce contrat, chaque nouvelle source ajouterait un {@code if} dans
 * le service, et le service finirait par savoir tout faire.
 */
public interface RevisionTrigger<T> {

    /** Les demandes que ce declencheur estime justifiees. Jamais null, souvent vide. */
    List<RevisionRequest> propose(T event);
}
