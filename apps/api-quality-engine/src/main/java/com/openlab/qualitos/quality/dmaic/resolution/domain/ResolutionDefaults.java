package com.openlab.qualitos.quality.dmaic.resolution.domain;

import java.util.List;

import static com.openlab.qualitos.quality.dmaic.resolution.domain.AnswerType.TEXT;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.AnswerType.YES_NO;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.AnswerType.YES_NO_NA;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.AnswerType.YES_NO_UNKNOWN;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic.HOW_MANY;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic.WHAT;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic.WHEN;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic.WHERE;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic.WHO;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic.WHY;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.SheetSection.ANALYZE;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.SheetSection.CONTAIN;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.SheetSection.CONTROL;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.SheetSection.DEFINE;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.SheetSection.IMPROVE;
import static com.openlab.qualitos.quality.dmaic.resolution.domain.SheetSection.MEASURE;

/**
 * Le paramétrage de la plateforme, repris du classeur « DMAIC Résolution de
 * Problème » : ses questions, dans son ordre, avec la liste de réponses que le
 * classeur attache à chacune ; ses types de défaillance V01 à V122 ; ses trois
 * niveaux de sévérité.
 *
 * <p>Un client qui n'a rien réglé travaille avec ces valeurs-là : la fiche
 * s'utilise dès le premier jour. Les fautes de frappe du classeur ont été
 * corrigées (« Quend », « preformance », « totamment »…), le fond est intact.
 */
public final class ResolutionDefaults {

    private ResolutionDefaults() {}

    public static final List<SheetQuestion> QUESTIONS = List.of(
            q("DEF-01", DEFINE, WHAT, "Quel est le problème et comment a-t-il été identifié ?", TEXT),
            q("DEF-02", DEFINE, WHAT, "Quel est l'impact du problème ?", TEXT),
            q("DEF-03", DEFINE, WHAT, "Quel est le problème (n° de pièce, de lot, etc.) ?", TEXT),
            q("DEF-04", DEFINE, WHAT, "Avons-nous des preuves matérielles du problème ?", YES_NO),
            q("DEF-05", DEFINE, WHO, "Qui est concerné par le problème ?", TEXT),
            q("DEF-06", DEFINE, WHO, "Qui a observé le problème en premier ?", TEXT),
            q("DEF-07", DEFINE, WHO, "À qui le problème a-t-il été signalé ?", TEXT),
            q("DEF-08", DEFINE, WHY, "Pourquoi est-ce un problème ?", TEXT),
            q("DEF-09", DEFINE, WHERE, "Où le problème s'est-il produit ?", TEXT),
            q("DEF-10", DEFINE, WHEN, "Quand le problème a-t-il été constaté en premier ?", TEXT),
            q("DEF-11", DEFINE, WHEN, "Quand est-il apparu depuis ?", TEXT),
            q("DEF-12", DEFINE, WHEN, "Le problème est-il survenu précédemment ?", YES_NO_UNKNOWN),
            q("DEF-13", DEFINE, WHEN,
                    "Le problème est-il survenu précédemment sur d'autres produits ou services ?", YES_NO_UNKNOWN),
            q("DEF-14", DEFINE, HOW_MANY,
                    "Quelle est la quantité ? Exemples : combien de produits, de pièces, etc.", TEXT),

            q("MES-01", MEASURE, null, "Avez-vous vérifié le moyen de mesure et les données ?", YES_NO),
            q("MES-02", MEASURE, null, "Quelle est la performance souhaitée ?", TEXT),
            q("MES-03", MEASURE, null, "Quel est l'écart de performance ?", TEXT),

            q("CON-01", CONTAIN, null,
                    "Avons-nous identifié tous les produits suspects (pas seulement le produit défectueux "
                    + "en cours d'analyse) ?", YES_NO_NA),
            q("CON-02", CONTAIN, null,
                    "Est-il possible que nous soyons toujours en train de produire ce problème dans notre usine ?",
                    YES_NO_NA),
            q("CON-03", CONTAIN, null, "Avons-nous isolé tous les clients de ce problème ?", TEXT),
            q("CON-04", CONTAIN, null, "Quel est le plan d'action temporaire pour contenir le problème ?", TEXT),
            q("CON-05", CONTAIN, null, "Les mesures conservatoires permettent-elles de contenir le problème ?",
                    YES_NO),
            q("CON-06", CONTAIN, null, "Avons-nous validé les mesures conservatoires avec le client ?", YES_NO_NA),

            q("ANA-01", ANALYZE, null,
                    "Quels outils ont été utilisés pour déterminer la (les) cause(s) racine(s) ? "
                    + "(exemples : 5 pourquoi, diagramme d'Ishikawa)", TEXT),
            q("ANA-02", ANALYZE, null,
                    "Vous êtes-vous affranchi de « l'erreur humaine » pour trouver la cause racine ?", TEXT),
            q("ANA-03", ANALYZE, null, "L'analyse des données confirme-t-elle la cause racine ?", YES_NO),

            q("AME-01", IMPROVE, null,
                    "Quels outils ont été utilisés pour réfléchir et choisir les solutions (par exemple : "
                    + "critères de sélection, matrice Impact / Effort) ?", TEXT),
            q("AME-02", IMPROVE, null,
                    "Avez-vous consulté les bases de données traitant des problèmes similaires pour voir "
                    + "comment ils ont été résolus ?", YES_NO),
            q("AME-03", IMPROVE, null, "L'analyse des données confirme-t-elle les solutions retenues ?", YES_NO),
            q("AME-04", IMPROVE, null, "Décrivez les meilleures solutions sélectionnées.", TEXT),
            q("AME-05", IMPROVE, null,
                    "Les solutions sélectionnées répondent-elles aux critères de mise en pratique, de faisabilité "
                    + "et de rentabilité ?", YES_NO),
            q("AME-06", IMPROVE, null,
                    "Les solutions peuvent-elles être mises en place dans le temps imparti et satisfaire "
                    + "les exigences client ?", YES_NO),
            q("AME-07", IMPROVE, null, "Les solutions permettent-elles d'éviter la récurrence du problème ?",
                    YES_NO),
            q("AME-08", IMPROVE, null,
                    "Un plan d'action adéquat a-t-il été créé pour mettre en place les solutions (exemples : "
                    + "diagramme de Gantt, diagramme de PERT, etc.) ?", YES_NO),
            q("AME-09", IMPROVE, null, "Toutes les actions du plan d'action ont-elles été complétées et documentées ?",
                    YES_NO),
            q("AME-10", IMPROVE, null,
                    "Toutes les actions ont-elles été totalement mises en place ? Comment le savez-vous ?", TEXT),

            q("CTL-01", CONTROL, null,
                    "Avez-vous mis en place des vérifications physiques ou administratives, ou des contrôles visuels ?",
                    YES_NO),
            q("CTL-02", CONTROL, null,
                    "Les procédures ou leurs modifications ont-elles été documentées, et les modifications "
                    + "correspondantes du système qualité ont-elles été faites ?", YES_NO),
            q("CTL-03", CONTROL, null,
                    "La formation qui accompagne les nouvelles solutions a-t-elle été développée et dispensée, "
                    + "et les enregistrements prouvent-ils son efficacité ?", YES_NO),
            q("CTL-04", CONTROL, null, "Un plan de surveillance a-t-il été développé ?", YES_NO),
            q("CTL-05", CONTROL, null,
                    "Le plan de surveillance élimine-t-il ou minimise-t-il la récurrence, et comment est-ce mesuré ?",
                    TEXT),
            q("CTL-06", CONTROL, null, "L'efficacité des solutions a-t-elle été vérifiée ?", YES_NO),
            q("CTL-07", CONTROL, null,
                    "Des audits ont-ils été menés pour évaluer l'utilisation et l'efficacité de la solution, "
                    + "et s'assurer que les gains sont maintenus ?", YES_NO),
            q("CTL-08", CONTROL, null,
                    "Les résultats ont-ils été mis à profit pour prévenir le problème sur des opérations ou "
                    + "des produits similaires ?", YES_NO_NA),
            q("CTL-09", CONTROL, null, "Une copie du DMAIC a-t-elle été partagée avec le client ?", YES_NO_NA),
            q("CTL-10", CONTROL, null,
                    "Le client est-il satisfait de l'analyse des causes, des actions correctives et du délai "
                    + "de résolution ?", YES_NO_NA),
            q("CTL-11", CONTROL, null, "Avez-vous amélioré la satisfaction client ?", YES_NO_NA),
            q("CTL-12", CONTROL, null, "La direction a-t-elle reconnu les efforts des membres de l'équipe ?",
                    YES_NO));

    public static final List<FailureType> FAILURE_TYPES = List.of(
            f("V01", "Machining - Dimensional, size, radius, undercut, thread form, wrong location, etc."),
            f("V02", "Machining - Geometric, concentricity, runout, roundness, flatness, surface finish, etc."),
            f("V03", "Machining - Burrs, sharp edges, chamfering"),
            f("V05", "Machining - Operational error, broken tool, clamping damage, etc."),
            f("V10", "Defect - Linear, cracks, tears, seams, broken, etc."),
            f("V11", "Defect - Non linear, inclusions, porosity, shrink, void, etc."),
            f("V12", "Defect - Geometric, excess material, missing feature, warp, overgrind, thin walls, size, etc."),
            f("V13", "Defect - Identification, markings, heat numbers, serial numbers, alloy ID, tags, "
                    + "wrong material, etc."),
            f("V14", "Defect - Surface problems, rust, scale, surface finish (non-machined), grind marks, etc."),
            f("V15", "Defect - Painting"),
            f("V16", "Defect - Plating"),
            f("V17", "Defect - Other coating"),
            f("V18", "Defect - Incorrect part"),
            f("V20", "Defect - Metallurgical, hard, soft, improper heat treatment, etc."),
            f("V30", "Field failure, corrosion, fatigue, fracture, vibration, etc."),
            f("V31", "Seal failure"),
            f("V40", "Test failure, balance, leak, efficiency, bearing noise, temperature, hardness, NDE, "
                    + "vibrations, etc."),
            f("V41", "Test failure, failed pressure check"),
            f("V50", "Electrical failure, coil defect, insulation, winding, etc."),
            f("V60", "Documentation, material certificates, incorrect, missing, incomplete, etc."),
            f("V65", "Design error, specification, drawing incorrect, engineering, etc."),
            f("V66", "Order entry / technical support error"),
            f("V67", "Purchasing error, order not cancelled, order quantity incorrect, etc."),
            f("V70", "Handling / processing, damaged in processing"),
            f("V71", "Handling / processing, missing operation, assembly error, improper or inadequate processing"),
            f("V72", "Handling / processing, lost, missing part or component"),
            f("V73", "Handling / processing, traceability, improper marking, etc."),
            f("V75", "Welding, quality, PWHT, buildup, etc."),
            f("V79", "Warehouse error, pulled wrong part, quantity, etc."),
            f("V80", "Shipping, damage, packaging, address, quantity, etc."),
            f("V95", "Audit finding, customer, quality, internal, etc."),
            f("V96", "Safety defects"),
            f("V97", "Environmental defect"),
            f("V100", "Bad potting - Conformal coating"),
            f("V101", "Baked on sand - Sand inclusions"),
            f("V102", "Bill of material"),
            f("V103", "Change order issue"),
            f("V104", "Casting core shift"),
            f("V105", "Customer furnished equipment"),
            f("V106", "Cycle count"),
            f("V107", "Test - Failed hydro - Assembled"),
            f("V108", "Test - Failed hydro - Component"),
            f("V109", "Test - Failed seat leak"),
            f("V110", "Test - Fails function"),
            f("V111", "Gas porosity - Pits - Voids - Cracks"),
            f("V112", "Insufficient material"),
            f("V113", "Kitting error"),
            f("V114", "Pack slip - PO quantity"),
            f("V115", "Paint or blast issue"),
            f("V116", "Pattern not to print"),
            f("V117", "Performance - Valve data sheet consistency"),
            f("V118", "Procedure / work instruction / policy error"),
            f("V119", "Profiler issue"),
            f("V120", "Receiving error"),
            f("V121", "Rerouting error"),
            f("V122", "Late delivery / OTD"));

    public static final List<SeverityLevel> SEVERITIES = List.of(
            new SeverityLevel(1, "Sévère"),
            new SeverityLevel(2, "Modérée"),
            new SeverityLevel(3, "Mineure"));

    private static SheetQuestion q(String code, SheetSection section, DefineTopic topic, String label,
                                   AnswerType type) {
        return new SheetQuestion(code, section, topic, label, type);
    }

    private static FailureType f(String code, String label) {
        return new FailureType(code, label);
    }
}
