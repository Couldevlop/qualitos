package com.openlab.qualitos.quality.dmaic.resolution.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSettingsTest.assertRefus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La fiche se vérifie contre le paramétrage du client au moment où on
 * l'écrit, et garde les réponses que le catalogue ne pose plus.
 */
class ResolutionSheetTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PROJET = UUID.randomUUID();
    static final Instant T0 = Instant.parse("2026-10-24T08:00:00Z");
    static final Person ALICE = new Person(UUID.randomUUID(), "Alice Martin");
    static final Person BRUNO = new Person(UUID.randomUUID(), "Bruno Petit");
    static final ResolutionSettings DEFAUTS = ResolutionSettings.platformDefaults();

    static ResolutionContent saisie(Map<String, String> answers) {
        return new ResolutionContent("  Acme  ", " CL-42 ", ALICE, List.of(BRUNO),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), 2, "v10", "  Fissure au montage ",
                answers, "Mesures", "Tri à 100 %", "Outil usé", "Changement d'outil", "Contrôle horaire",
                List.of(new WhyChain(WhyBranch.OCCURRENCE, " Outil usé ", List.of(" Pas de suivi ", " "), true,
                        " Compteur ")),
                List.of(new ActionLine(LocalDate.of(2026, 10, 2), " Changer l'outil ", BRUNO, ActionStatus.CLOSED,
                        LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 4), 100, " fait "))
                , null);
    }

    static ResolutionSheet fiche() {
        return ResolutionSheet.blank(TENANT, PROJET, ALICE.accountId(), T0);
    }

    @Test
    void uneFicheViergeEstVide() {
        ResolutionSheet f = fiche();

        assertThat(f.getId()).isNull();
        assertThat(f.getTenantId()).isEqualTo(TENANT);
        assertThat(f.getProjectId()).isEqualTo(PROJET);
        assertThat(f.getCreatedBy()).isEqualTo(ALICE.accountId());
        assertThat(f.getCreatedAt()).isEqualTo(T0);
        assertThat(f.getContent()).isEqualTo(ResolutionContent.empty());
    }

    @Test
    void laSaisieEstNettoyeeEtVerifiee() {
        ResolutionSheet f = fiche();
        Instant t1 = T0.plusSeconds(60);

        f.revise(saisie(Map.of("DEF-01", "  Rayure ", "DEF-04", "YES", "DEF-12", "UNKNOWN", "MES-03", "  ")),
                DEFAUTS, t1);

        ResolutionContent c = f.getContent();
        assertThat(c.clientName()).isEqualTo("Acme");
        assertThat(c.claimNumber()).isEqualTo("CL-42");
        assertThat(c.customerSymptoms()).isEqualTo("Fissure au montage");
        assertThat(c.failureTypeCode()).isEqualTo("V10");
        assertThat(c.severity()).isEqualTo(2);
        assertThat(c.answers()).containsOnly(Map.entry("DEF-01", "Rayure"), Map.entry("DEF-04", "YES"),
                Map.entry("DEF-12", "UNKNOWN"));
        assertThat(c.whys().get(0).cause()).isEqualTo("Outil usé");
        assertThat(c.whys().get(0).whys()).containsExactly("Pas de suivi");
        assertThat(c.whys().get(0).action()).isEqualTo("Compteur");
        assertThat(c.actions().get(0).action()).isEqualTo("Changer l'outil");
        assertThat(c.actions().get(0).notes()).isEqualTo("fait");
        assertThat(f.getUpdatedAt()).isEqualTo(t1);
    }

    @Test
    void unCodeDeDefaillanceInconnuEstRefuse() {
        ResolutionContent inconnu = new ResolutionContent(null, null, null, null, null, null, null, "V999",
                null, null, null, null, null, null, null, null, null, null);
        assertRefus(() -> fiche().revise(inconnu, DEFAUTS, T0), "failureTypeCode");
        assertRefus(() -> fiche().revise(new ResolutionContent(null, null, null, null, null, null, null,
                "V".repeat(21), null, null, null, null, null, null, null, null, null, null), DEFAUTS, T0),
                "failureTypeCode");
    }

    @Test
    void lesReglesDeLEnTeteSontAppliquees() {
        ResolutionContent base = valide();
        assertRefus(() -> fiche().revise(with(base, b -> b.closedOn = LocalDate.of(2026, 9, 1)), DEFAUTS, T0),
                "closedOn");
        assertRefus(() -> fiche().revise(with(base, b -> b.severity = 7), DEFAUTS, T0), "severity");
        assertRefus(() -> fiche().revise(with(base, b -> b.clientName = "x".repeat(201)), DEFAUTS, T0),
                "clientName");
        assertRefus(() -> fiche().revise(with(base, b -> b.owner = new Person(null, "Sans compte")), DEFAUTS, T0),
                "problemOwner");
        assertRefus(() -> fiche().revise(with(base, b -> b.owner = new Person(UUID.randomUUID(), " ")), DEFAUTS, T0),
                "problemOwner");
    }

    @Test
    void unProprietaireVideVautPersonne() {
        ResolutionSheet f = fiche();
        f.revise(with(valide(), b -> b.owner = new Person(null, " ")), DEFAUTS, T0);
        assertThat(f.getContent().problemOwner()).isNull();

        f.revise(with(valide(), b -> b.owner = null), DEFAUTS, T0);
        assertThat(f.getContent().problemOwner()).isNull();
    }

    @Test
    void lEquipeEstVerifiee() {
        List<Person> grande = new ArrayList<>();
        for (int i = 0; i <= ResolutionSheet.TEAM_MAX; i++) {
            grande.add(new Person(UUID.randomUUID(), "P" + i));
        }
        assertRefus(() -> fiche().revise(with(valide(), b -> b.team = grande), DEFAUTS, T0), "team");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.team = List.of(BRUNO, BRUNO)), DEFAUTS, T0), "team");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.team = List.of(new Person(null, ""))), DEFAUTS, T0),
                "team");
    }

    @Test
    void lesReponsesSontVerifieesSelonLeurType() {
        assertRefus(() -> fiche().revise(with(valide(), b -> b.answers = Map.of("XX-99", "oui")), DEFAUTS, T0),
                "answers");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.answers = Map.of("DEF-04", "NA")), DEFAUTS, T0),
                "answers.DEF-04");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.answers = Map.of("DEF-01", "x".repeat(2001))),
                DEFAUTS, T0), "answers.DEF-01");
        Map<String, String> avecNull = new HashMap<>();
        avecNull.put("DEF-01", null);
        ResolutionSheet f = fiche();
        f.revise(with(valide(), b -> b.answers = avecNull), DEFAUTS, T0);
        assertThat(f.getContent().answers()).isEmpty();
    }

    @Test
    void lesReponsesAuxQuestionsRetireesSontConservees() {
        ResolutionSheet f = fiche();
        f.revise(with(valide(), b -> b.answers = Map.of("DEF-01", "Rayure", "MES-02", "Cpk 1,33")), DEFAUTS, T0);
        ResolutionSettings sansDef01 = ResolutionSettings.configured(
                DEFAUTS.questions().stream().filter(q -> !q.code().equals("DEF-01")).toList(),
                DEFAUTS.failureTypes(), DEFAUTS.severities());

        f.revise(with(valide(), b -> b.answers = Map.of("MES-02", "Cpk 1,67")), sansDef01, T0);

        assertThat(f.getContent().answers()).containsOnly(Map.entry("DEF-01", "Rayure"),
                Map.entry("MES-02", "Cpk 1,67"));
    }

    @Test
    void lesCinqPourquoiSontVerifies() {
        List<WhyChain> trop = Collections.nCopies(ResolutionSheet.WHYS_MAX + 1,
                new WhyChain(WhyBranch.DETECTION, "c", List.of(), false, null));
        assertRefus(() -> fiche().revise(with(valide(), b -> b.whys = trop), DEFAUTS, T0), "whys");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.whys = Collections.singletonList(null)), DEFAUTS, T0),
                "whys");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.whys = List.of(
                new WhyChain(null, "c", List.of(), false, null))), DEFAUTS, T0), "whys");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.whys = List.of(
                new WhyChain(WhyBranch.SYSTEMIC, " ", List.of(), false, null))), DEFAUTS, T0), "whys");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.whys = List.of(
                new WhyChain(WhyBranch.SYSTEMIC, "c", Arrays.asList("1", "2", "3", "4", "5", "6"), false, null))),
                DEFAUTS, T0), "whys");
        ResolutionSheet f = fiche();
        f.revise(with(valide(), b -> b.whys = List.of(new WhyChain(WhyBranch.SYSTEMIC, "c",
                Arrays.asList("1", null, "2"), false, null))), DEFAUTS, T0);
        assertThat(f.getContent().whys().get(0).whys()).containsExactly("1", "2");
    }

    @Test
    void lePlanDActionsEstVerifie() {
        ActionLine bonne = new ActionLine(null, "Faire", null, ActionStatus.OPEN, null, null, 0, null);
        List<ActionLine> trop = Collections.nCopies(ResolutionSheet.ACTIONS_MAX + 1, bonne);
        assertRefus(() -> fiche().revise(with(valide(), b -> b.actions = trop), DEFAUTS, T0), "actions");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.actions = Collections.singletonList(null)), DEFAUTS, T0),
                "actions");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.actions = List.of(
                new ActionLine(null, "Faire", null, null, null, null, 0, null))), DEFAUTS, T0), "actions");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.actions = List.of(
                new ActionLine(null, " ", null, ActionStatus.OPEN, null, null, 0, null))), DEFAUTS, T0), "actions");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.actions = List.of(
                new ActionLine(null, "Faire", null, ActionStatus.OPEN, null, null, 101, null))), DEFAUTS, T0),
                "actions");
        assertRefus(() -> fiche().revise(with(valide(), b -> b.actions = List.of(
                new ActionLine(null, "Faire", null, ActionStatus.OPEN, null, null, -1, null))), DEFAUTS, T0),
                "actions");
    }

    @Test
    void unContenuAbsentEstUneErreurDeProgrammation() {
        assertThatThrownBy(() -> fiche().revise(null, DEFAUTS, T0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ResolutionSheet(null, null, PROJET, ResolutionContent.empty(), null, T0, T0))
                .isInstanceOf(NullPointerException.class);
    }

    // ---- fabrique de saisies ----

    static ResolutionContent valide() {
        return new ResolutionContent("Acme", "CL-1", ALICE, List.of(BRUNO), LocalDate.of(2026, 10, 1), null, 1,
                "V10", null, Map.of(), null, null, null, null, null, List.of(), List.of(), UUID.randomUUID());
    }

    /** Une copie modifiable d'une saisie, pour n'en changer qu'un champ. */
    static final class Builder {
        String clientName;
        Person owner;
        List<Person> team;
        LocalDate closedOn;
        Integer severity;
        Map<String, String> answers;
        List<WhyChain> whys;
        List<ActionLine> actions;
    }

    static ResolutionContent with(ResolutionContent c, java.util.function.Consumer<Builder> change) {
        Builder b = new Builder();
        b.clientName = c.clientName();
        b.owner = c.problemOwner();
        b.team = c.team();
        b.closedOn = c.closedOn();
        b.severity = c.severity();
        b.answers = c.answers();
        b.whys = c.whys();
        b.actions = c.actions();
        change.accept(b);
        return new ResolutionContent(b.clientName, c.claimNumber(), b.owner, b.team, c.openedOn(), b.closedOn,
                b.severity, c.failureTypeCode(), c.customerSymptoms(), b.answers, c.measureSummary(),
                c.containmentMeasures(), c.rootCause(), c.retainedSolutions(), c.surveillancePlan(), b.whys,
                b.actions, c.ishikawaId());
    }
}
