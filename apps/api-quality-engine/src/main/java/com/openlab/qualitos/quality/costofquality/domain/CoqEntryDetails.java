package com.openlab.qualitos.quality.costofquality.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ce qu'on saisit dans la fenêtre d'une ligne.
 *
 * <p>Les quatre derniers champs ne valent que pour une ligne de contrôle de
 * pièces ; ils sont ignorés — et effacés — sur toute autre ligne, pour qu'une
 * ligne de formation ne garde pas un numéro de lot resté d'une saisie
 * antérieure.
 */
public record CoqEntryDetails(
        BigDecimal amount,
        String responsible,
        LocalDate imputationDate,
        String comment,
        String partReference,
        Integer partQuantity,
        String lot,
        LocalDate receivedOrMadeOn) {
}
