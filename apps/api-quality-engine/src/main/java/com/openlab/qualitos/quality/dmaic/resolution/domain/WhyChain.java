package com.openlab.qualitos.quality.dmaic.resolution.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Une chaîne de « pourquoi » : une cause de départ, jusqu'à cinq pourquoi
 * successifs, et l'action qui en découle.
 *
 * @param rootCause vrai quand la chaîne aboutit à une cause racine (« oui » du classeur)
 */
public record WhyChain(WhyBranch branch, String cause, List<String> whys, boolean rootCause, String action) {

    public static final int MAX_WHYS = 5;

    /** Un pourquoi laissé vide arrive nul : la fiche l'écarte au nettoyage. */
    public WhyChain {
        whys = whys == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(whys));
    }
}
