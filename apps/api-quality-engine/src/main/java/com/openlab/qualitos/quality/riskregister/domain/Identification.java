package com.openlab.qualitos.quality.riskregister.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Ce qui identifie une fiche, risque ou opportunité : la moitié commune des
 * deux formulaires.
 *
 * <p>Processus, site et propriétaire sont du texte : la plateforme n'a pas
 * encore de référentiel des processus ni des sites, et l'annuaire n'est lisible
 * que des administrateurs. L'écran propose les valeurs déjà employées, pour que
 * « Production » ne devienne pas « production » trois lignes plus bas.
 */
public record Identification(
        String title,
        RegisterType type,
        String process,
        String site,
        String owner,
        RegisterOrigin origin,
        String originRef,
        Set<RegisterRequirement> requirements) {

    public static final int TITLE_MAX = 255;
    public static final int PROCESS_MAX = 120;
    public static final int SITE_MAX = 120;
    public static final int OWNER_MAX = 150;
    public static final int ORIGIN_REF_MAX = 120;

    /** Valide et normalise pour un registre donné ; rend une copie propre. */
    public Identification validated(boolean forRisk) {
        String intitule = Texts.required("title", title, TITLE_MAX, "L'intitulé est obligatoire.");
        if (type == null) {
            throw new RegisterValidationException("type", "Le type est obligatoire.");
        }
        String processus = Texts.required("process", process, PROCESS_MAX, "Le processus est obligatoire.");
        String lieu = Texts.optional("site", site, SITE_MAX);
        String proprietaire = Texts.required("owner", owner, OWNER_MAX, "Le propriétaire est obligatoire.");
        RegisterOrigin source = origin == null ? RegisterOrigin.DIRECT : origin;
        if (!forRisk && !source.forOpportunity()) {
            throw new RegisterValidationException("origin",
                    "Cette origine ne vaut que pour un risque.");
        }
        String reference = Texts.optional("originRef", originRef, ORIGIN_REF_MAX);
        return new Identification(intitule, type, processus, lieu, proprietaire, source, reference,
                exigences(requirements, forRisk));
    }

    private static Set<RegisterRequirement> exigences(Collection<RegisterRequirement> choisies,
                                                      boolean forRisk) {
        Set<RegisterRequirement> retenues = EnumSet.noneOf(RegisterRequirement.class);
        if (choisies == null) {
            return retenues;
        }
        for (RegisterRequirement r : choisies) {
            if (r == null) {
                continue;
            }
            if (forRisk ? !r.forRisk() : !r.forOpportunity()) {
                throw new RegisterValidationException("requirements",
                        "Exigence non applicable à ce registre : " + r);
            }
            retenues.add(r);
        }
        return retenues;
    }
}
