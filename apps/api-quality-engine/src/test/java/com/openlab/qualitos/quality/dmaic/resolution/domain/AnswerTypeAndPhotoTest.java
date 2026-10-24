package com.openlab.qualitos.quality.dmaic.resolution.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSettingsTest.assertRefus;
import static org.assertj.core.api.Assertions.assertThat;

/** Les listes fermées du classeur, et les règles des six emplacements photo. */
class AnswerTypeAndPhotoTest {

    @Test
    void chaqueListeFermeeNAdmetQueSesCodes() {
        assertThat(AnswerType.YES_NO.accepts("YES")).isTrue();
        assertThat(AnswerType.YES_NO.accepts("NA")).isFalse();
        assertThat(AnswerType.YES_NO_NA.accepts("NA")).isTrue();
        assertThat(AnswerType.YES_NO_NA.accepts("UNKNOWN")).isFalse();
        assertThat(AnswerType.YES_NO_UNKNOWN.accepts("UNKNOWN")).isTrue();
        assertThat(AnswerType.YES_NO_UNKNOWN.accepts("Oui")).isFalse();
        assertThat(AnswerType.YES_NO_UNKNOWN.choices()).containsExactlyInAnyOrder("YES", "NO", "UNKNOWN");
        assertThat(AnswerType.TEXT.choices()).isEmpty();
    }

    @Test
    void uneReponseLibreEstBornee() {
        assertThat(AnswerType.TEXT.accepts("x".repeat(AnswerType.TEXT_MAX))).isTrue();
        assertThat(AnswerType.TEXT.accepts("x".repeat(AnswerType.TEXT_MAX + 1))).isFalse();
    }

    @Test
    void lesEmplacementsVontDeUnASix() {
        SheetPhoto.checkSlot(1);
        SheetPhoto.checkSlot(6);
        assertRefus(() -> SheetPhoto.checkSlot(0), "slot");
        assertRefus(() -> SheetPhoto.checkSlot(7), "slot");
    }

    @Test
    void laRemarqueEstNettoyeeEtBornee() {
        assertThat(SheetPhoto.cleanRemark(null)).isNull();
        assertThat(SheetPhoto.cleanRemark("  ")).isNull();
        assertThat(SheetPhoto.cleanRemark("  Fissure visible ")).isEqualTo("Fissure visible");
        assertRefus(() -> SheetPhoto.cleanRemark("x".repeat(SheetPhoto.REMARK_MAX + 1)), "remark");

        SheetPhoto p = new SheetPhoto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2, "k", "image/png",
                10, null, null, Instant.EPOCH);
        SheetPhoto commentee = p.withRemark(" Vue de dessus ");
        assertThat(commentee.remark()).isEqualTo("Vue de dessus");
        assertThat(commentee.slot()).isEqualTo(2);
        assertThat(commentee.objectKey()).isEqualTo("k");
    }

    @Test
    void lesFormatsDuClasseurSontAdmis() {
        assertThat(SheetPhoto.ALLOWED_TYPES).containsKeys("image/jpeg", "image/gif", "image/png", "image/webp");
    }
}
