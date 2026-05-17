package com.openlab.qualitos.quality.ims.domain.model;

public enum RelationType {
    /** Mêmes exigences (HLS Annexe SL). */
    EQUIVALENT,
    /** La source couvre la cible. */
    COVERS,
    /** Liens partiels / overlap. */
    RELATED,
    /** Cite mais ne couvre pas. */
    REFERENCES
}
