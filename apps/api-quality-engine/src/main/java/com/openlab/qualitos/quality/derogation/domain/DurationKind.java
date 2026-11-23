package com.openlab.qualitos.quality.derogation.domain;

/** « Temporaire » ou « Permanente » (section 2). */
public enum DurationKind {

    TEMPORARY,

    /**
     * La dérogation ne couvre pas un lot mais un état durable.
     *
     * <p>C'est alors la spécification qu'il faudrait changer, et le dossier le
     * dit sans le décider.
     */
    PERMANENT
}
