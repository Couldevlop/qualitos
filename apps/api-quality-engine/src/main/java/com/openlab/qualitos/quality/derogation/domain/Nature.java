package com.openlab.qualitos.quality.derogation.domain;

/**
 * Section 2 — la nature de la demande : quatre choix fermés et leur précision.
 *
 * <p>Les valeurs par défaut sont celles de la maquette, et un choix absent
 * retombe dessus plutôt que de laisser un trou : une demande sans nature ne se
 * lit pas.
 */
public record Nature(
        ShipmentStage shipmentStage,
        DeviationOrigin deviationOrigin,
        Occurrence occurrence,
        DurationKind durationKind,
        String durationDetail
) {
    public Nature {
        shipmentStage = shipmentStage == null ? ShipmentStage.TO_SHIP : shipmentStage;
        deviationOrigin = deviationOrigin == null ? DeviationOrigin.PRODUCT : deviationOrigin;
        occurrence = occurrence == null ? Occurrence.FIRST : occurrence;
        durationKind = durationKind == null ? DurationKind.TEMPORARY : durationKind;
        durationDetail = Texte.net(durationDetail);
    }

    /** La nature d'une demande qui vient d'être ouverte. */
    public static Nature parDefaut() {
        return new Nature(null, null, null, null, null);
    }
}
