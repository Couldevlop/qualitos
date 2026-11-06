package com.openlab.qualitos.quality.dmaic;

import java.util.List;

/**
 * Un refus que l'utilisateur doit pouvoir LIRE : la phase n'est pas complète, ou
 * le projet ne peut pas être clos, et on sait exactement pourquoi.
 *
 * <p><b>Pourquoi une exception à part.</b> {@link DmaicStateException} porte
 * aussi des refus de machine à états rédigés pour le développeur (« Only ACTIVE
 * projects can advance phase »). Ces messages-là n'ont rien à faire sous les yeux
 * d'un responsable qualité. Le contrôle d'accès aux messages est donc posé par le
 * <b>type</b> de l'exception, pas par une inspection de la chaîne : seul ce qui
 * est levé ici est déclaré destiné à l'utilisateur, et c'est la seule chose que
 * l'écran a le droit d'afficher.
 *
 * <p>Sans ce canal, l'écran recevait un 409 nu et le traduisait par « État
 * incompatible — rechargez la page » : un conseil faux, puisque recharger ne
 * solde aucune action, et une perte de l'information utile — <i>quelles</i>
 * réponses manquent, <i>quelles</i> actions ne sont pas à 100 %.
 */
public class DmaicGateException extends DmaicStateException {

    /** Les points qui bloquent, un par ligne, rédigés pour l'utilisateur. */
    private final transient List<String> reasons;

    public DmaicGateException(String message, List<String> reasons) {
        super(message);
        this.reasons = List.copyOf(reasons);
    }

    public List<String> reasons() {
        return reasons;
    }
}
