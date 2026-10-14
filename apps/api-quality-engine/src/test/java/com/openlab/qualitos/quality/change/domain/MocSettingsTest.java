package com.openlab.qualitos.quality.change.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.openlab.qualitos.quality.change.domain.MocClassification.CRITICAL;
import static com.openlab.qualitos.quality.change.domain.MocClassification.MAJOR;
import static com.openlab.qualitos.quality.change.domain.MocClassification.MINOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Le paramétrage MOC : valeurs livrées et règles de ce que l'administrateur enregistre. */
class MocSettingsTest {

    static final UUID JEAN = UUID.randomUUID();
    static final ValidationRole.RoleHolder TITULAIRE = new ValidationRole.RoleHolder(JEAN, "j.dupont@acme.test");

    @Test
    void lesValeursLivreesSontCellesDeLaMaquette() {
        MocSettings d = MocSettings.defaults();
        assertThat(d.thresholds()).isEqualTo(MocThresholds.DEFAULT);
        assertThat(d.weights()).isEqualTo(MocWeights.defaults());
        assertThat(d.reminderDays()).isEqualTo(3);
        assertThat(d.requiredRoles(MINOR)).extracting(ValidationRole::label)
                .containsExactly("Responsable de service", "HSE");
        assertThat(d.requiredRoles(MAJOR)).extracting(ValidationRole::label)
                .containsExactly("Responsable de service", "HSE", "Directeur technique");
        assertThat(d.requiredRoles(CRITICAL)).extracting(ValidationRole::label)
                .containsExactly("Responsable de service", "HSE", "Directeur technique", "Directeur de site");
        assertThat(d.requiredRoles(MocClassification.NOT_ASSESSED)).isEmpty();
        assertThat(d.roles()).allSatisfy(r -> assertThat(r.holders()).isEmpty());
    }

    @Test
    void unParametrageCompletEstAccepte() {
        MocSettings s = avec(List.of(role("Responsable", EnumSet.of(MINOR, MAJOR, CRITICAL), TITULAIRE)));
        assertThat(s.validated()).isSameAs(s);
    }

    @Test
    void chaqueClassificationExigeUnRole() {
        assertThatThrownBy(() -> avec(List.of(role("Responsable", EnumSet.of(MINOR, MAJOR)))).validated())
                .isInstanceOf(MocException.class)
                .satisfies(e -> {
                    assertThat(((MocException) e).code()).isEqualTo("moc.settings.required-empty");
                    assertThat(((MocException) e).details()).containsEntry("classification", "CRITICAL");
                });
    }

    @Test
    void lesLibellesSontNonVidesBornesEtUniquesSansCasse() {
        assertThatThrownBy(() -> avec(List.of(role("  ", EnumSet.allOf(MocClassification.class)))).validated())
                .isInstanceOf(MocException.class);
        assertThatThrownBy(() -> avec(List.of(role("x".repeat(101), EnumSet.of(MINOR, MAJOR, CRITICAL))))
                .validated()).isInstanceOf(MocException.class)
                .extracting(e -> ((MocException) e).code()).isEqualTo("moc.settings.role-label");
        assertThatThrownBy(() -> avec(List.of(role("HSE", EnumSet.of(MINOR, MAJOR, CRITICAL)),
                role("hse", EnumSet.of(MINOR)))).validated())
                .isInstanceOf(MocException.class)
                .extracting(e -> ((MocException) e).code()).isEqualTo("moc.settings.role-duplicate");
    }

    @Test
    void unRoleNEstRequisQuePourUneClasseEvaluee() {
        assertThatThrownBy(() -> avec(List.of(role("HSE",
                EnumSet.of(MINOR, MAJOR, CRITICAL, MocClassification.NOT_ASSESSED)))).validated())
                .isInstanceOf(MocException.class)
                .extracting(e -> ((MocException) e).code()).isEqualTo("moc.settings.role-required");
    }

    @Test
    void ilFautEntreUnEtVingtRoles() {
        assertThatThrownBy(() -> avec(List.of()).validated()).isInstanceOf(MocException.class)
                .extracting(e -> ((MocException) e).code()).isEqualTo("moc.settings.roles-count");
        List<ValidationRole> trop = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            trop.add(role("R" + i, EnumSet.of(MINOR, MAJOR, CRITICAL)));
        }
        assertThatThrownBy(() -> avec(trop).validated()).isInstanceOf(MocException.class);
    }

    @Test
    void unMemeRoleNeFigurePasDeuxFois() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> avec(List.of(
                new ValidationRole(id, "A", EnumSet.of(MINOR, MAJOR, CRITICAL), List.of()),
                new ValidationRole(id, "B", EnumSet.of(MINOR), List.of()))).validated())
                .isInstanceOf(MocException.class)
                .extracting(e -> ((MocException) e).code()).isEqualTo("moc.settings.role-id");
    }

    @Test
    void lesTitulairesSontDesMembresDistinctsEtBornes() {
        assertThatThrownBy(() -> avec(List.of(role("HSE", EnumSet.of(MINOR, MAJOR, CRITICAL), TITULAIRE, TITULAIRE)))
                .validated()).isInstanceOf(MocException.class)
                .extracting(e -> ((MocException) e).code()).isEqualTo("moc.settings.holder-duplicate");
        assertThatThrownBy(() -> avec(List.of(role("HSE", EnumSet.of(MINOR, MAJOR, CRITICAL),
                new ValidationRole.RoleHolder(null, "x")))).validated())
                .isInstanceOf(MocException.class);
        assertThatThrownBy(() -> avec(List.of(role("HSE", EnumSet.of(MINOR, MAJOR, CRITICAL),
                new ValidationRole.RoleHolder(UUID.randomUUID(), " ")))).validated())
                .isInstanceOf(MocException.class);
        ValidationRole.RoleHolder[] vingtEtUn = new ValidationRole.RoleHolder[21];
        for (int i = 0; i < 21; i++) {
            vingtEtUn[i] = new ValidationRole.RoleHolder(UUID.randomUUID(), "u" + i + "@acme.test");
        }
        assertThatThrownBy(() -> avec(List.of(role("HSE", EnumSet.of(MINOR, MAJOR, CRITICAL), vingtEtUn)))
                .validated()).isInstanceOf(MocException.class)
                .extracting(e -> ((MocException) e).code()).isEqualTo("moc.settings.holders-count");
    }

    @Test
    void leDelaiDeRappelVaDeUnATrenteJours() {
        assertThatThrownBy(() -> new MocSettings(MocThresholds.DEFAULT, MocWeights.defaults(), 0, List.of()))
                .isInstanceOf(MocException.class)
                .extracting(e -> ((MocException) e).code()).isEqualTo("moc.settings.reminder-days");
        assertThatThrownBy(() -> new MocSettings(MocThresholds.DEFAULT, MocWeights.defaults(), 31, List.of()))
                .isInstanceOf(MocException.class);
        assertThat(new MocSettings(MocThresholds.DEFAULT, MocWeights.defaults(), 30, null).roles()).isEmpty();
    }

    @Test
    void unRoleSaitQuiLeTient() {
        ValidationRole r = role("HSE", EnumSet.of(MINOR), TITULAIRE);
        assertThat(r.heldBy(JEAN)).isTrue();
        assertThat(r.heldBy(UUID.randomUUID())).isFalse();
        assertThat(r.heldBy(null)).isFalse();
        assertThat(new ValidationRole(null, "X", null, null).requiredFor()).isEmpty();
        assertThat(new ValidationRole(null, "X", null, null).holders()).isEmpty();
    }

    @Test
    void uneExceptionPorteSaNatureEtSonCode() {
        MocException e = MocException.conflict("moc.x", "m");
        assertThat(e.kind()).isEqualTo(MocException.Kind.CONFLICT);
        assertThat(e.details()).isEmpty();
        assertThat(MocException.invalid("moc.y", "m").kind()).isEqualTo(MocException.Kind.INVALID);
    }

    static MocSettings avec(List<ValidationRole> roles) {
        return new MocSettings(MocThresholds.DEFAULT, MocWeights.defaults(), 3, roles);
    }

    static ValidationRole role(String label, Set<MocClassification> requis, ValidationRole.RoleHolder... titulaires) {
        return new ValidationRole(null, label, requis, List.of(titulaires));
    }
}
