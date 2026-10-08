package com.openlab.qualitos.quality.smi.domain;

import java.util.List;
import java.util.Optional;

/**
 * Les sept chapitres communs de la structure-cadre des normes de systèmes de
 * management (ISO, Annexe SL / « HLS »), et les modules de QualitOS où se
 * fabriquent leurs preuves.
 *
 * <p>C'est la colonne « Module QualitOS » de la matrice des exigences : elle ne
 * dépend ni de la norme ni du secteur — le chapitre 10 se prouve par les NC et
 * les CAPA, que la norme soit l'ISO 9001 ou l'ISO 45001. Les modules sont des
 * codes ; l'écran les nomme dans sa langue.
 */
public enum HlsChapter {

    CONTEXT("4", List.of("PROCESS_MAP", "RISK_REGISTER")),
    LEADERSHIP("5", List.of("DOCUMENTS", "POLICY")),
    PLANNING("6", List.of("RISK_REGISTER", "OBJECTIVES")),
    SUPPORT("7", List.of("DOCUMENTS", "TRAINING", "CALIBRATION")),
    OPERATION("8", List.of("APQP", "SPC", "CHANGES")),
    PERFORMANCE("9", List.of("AUDITS", "KPI", "MANAGEMENT_REVIEW")),
    IMPROVEMENT("10", List.of("NC", "CAPA"));

    private final String code;
    private final List<String> modules;

    HlsChapter(String code, List<String> modules) {
        this.code = code;
        this.modules = modules;
    }

    public String code() {
        return code;
    }

    public List<String> modules() {
        return modules;
    }

    public static Optional<HlsChapter> ofCode(String code) {
        for (HlsChapter c : values()) {
            if (c.code.equals(code)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
