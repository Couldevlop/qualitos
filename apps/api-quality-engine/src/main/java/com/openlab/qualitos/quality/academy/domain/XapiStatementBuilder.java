package com.openlab.qualitos.quality.academy.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Construit des statements xAPI (Experience API / Tin Can) pour un cours et la
 * complétion d'un apprenant (§19.3, « xAPI (statements) »).
 *
 * <p>Émet des verbes ADL standard : {@code experienced} (cours suivi) et
 * {@code completed} / {@code passed|failed} (complétion). L'IRI de l'activité
 * est dérivée du code de cours. Aucun appel réseau : on produit le JSON pur que
 * le LMS (LRS) externe ingérera.</p>
 */
public final class XapiStatementBuilder {

    public static final String VERB_COMPLETED = "http://adlnet.gov/expapi/verbs/completed";
    public static final String VERB_PASSED = "http://adlnet.gov/expapi/verbs/passed";
    public static final String VERB_FAILED = "http://adlnet.gov/expapi/verbs/failed";
    private static final String ACTIVITY_BASE = "https://qualitos.io/xapi/academy/course/";

    private final ObjectMapper mapper;

    public XapiStatementBuilder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Construit la liste de statements (JSON array) pour une complétion de cours.
     *
     * @param actorId      identifiant de l'apprenant (account name, anonymisé : UUID)
     * @param courseCode   code du cours (devient l'IRI d'activité)
     * @param courseTitle  titre lisible
     * @param score        score final 0-100
     * @param passed       réussite (verbe passed) ou échec (failed)
     * @param completedAt  horodatage de complétion
     * @return tableau JSON de statements xAPI
     */
    public String completionStatements(UUID actorId, String courseCode, String courseTitle,
                                       int score, boolean passed, Instant completedAt) {
        ArrayNode arr = mapper.createArrayNode();
        arr.add(statement(actorId, passed ? VERB_PASSED : VERB_FAILED,
                passed ? "passed" : "failed", courseCode, courseTitle, score, passed, completedAt));
        arr.add(statement(actorId, VERB_COMPLETED, "completed",
                courseCode, courseTitle, score, passed, completedAt));
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arr);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize xAPI statements", e);
        }
    }

    private ObjectNode statement(UUID actorId, String verbId, String verbDisplay,
                                 String courseCode, String courseTitle,
                                 int score, boolean success, Instant when) {
        ObjectNode st = mapper.createObjectNode();
        st.put("id", UUID.randomUUID().toString());
        st.put("timestamp", when.toString());

        ObjectNode actor = st.putObject("actor");
        actor.put("objectType", "Agent");
        ObjectNode account = actor.putObject("account");
        account.put("homePage", "https://qualitos.io");
        account.put("name", actorId.toString());

        ObjectNode verb = st.putObject("verb");
        verb.put("id", verbId);
        verb.putObject("display").put("en-US", verbDisplay);

        ObjectNode object = st.putObject("object");
        object.put("objectType", "Activity");
        object.put("id", ACTIVITY_BASE + courseCode);
        ObjectNode definition = object.putObject("definition");
        definition.put("type", "http://adlnet.gov/expapi/activities/course");
        definition.putObject("name").put("en-US", courseTitle);

        ObjectNode result = st.putObject("result");
        result.put("completion", true);
        result.put("success", success);
        ObjectNode scoreNode = result.putObject("score");
        scoreNode.put("scaled", Math.max(0.0, Math.min(1.0, score / 100.0)));
        scoreNode.put("raw", score);
        scoreNode.put("min", 0);
        scoreNode.put("max", 100);
        return st;
    }
}
