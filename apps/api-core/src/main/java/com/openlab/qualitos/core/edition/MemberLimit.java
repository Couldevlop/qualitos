package com.openlab.qualitos.core.edition;

import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.License;

/**
 * Le nombre d'utilisateurs actifs que couvre la licence (ADR 0082).
 *
 * <p>Vérifié à chaque fois qu'un membre devient actif : invitation, création,
 * réactivation. Désactiver un compte libère une place. Sans plafond (SaaS, ou
 * licence « sans limite »), rien n'est vérifié.
 */
@FunctionalInterface
public interface MemberLimit {

    MemberLimit NONE = activeMembers -> { };

    /**
     * @param activeMembers les membres actifs AVANT l'arrivée du nouveau
     * @throws EditionExceptions.MemberLimitReached si la place manque
     */
    void ensureRoomForOneMore(long activeMembers);

    /** Le plafond de la licence on-premise ; aucun en SaaS. */
    static MemberLimit of(Licensing licensing) {
        if (!licensing.isOnPrem()) {
            return NONE;
        }
        return activeMembers -> licensing.state().licenseOpt()
                .filter(l -> !l.unlimitedUsers())
                .map(License::maxUsers)
                .filter(max -> activeMembers >= max)
                .ifPresent(max -> {
                    throw new EditionExceptions.MemberLimitReached(max);
                });
    }
}
