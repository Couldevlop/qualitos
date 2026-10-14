package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.MocClassification;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeEntityCallbacksTest {

    @Test
    void uneNouvelleDemandeEstUnBrouillonPermanentNonEvalue() throws Exception {
        ChangeRequest c = new ChangeRequest();
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(ChangeRequestStatus.DRAFT);
        assertThat(c.getType()).isEqualTo(ChangeRequestType.PERMANENT);
        assertThat(c.getClassification()).isEqualTo(MocClassification.NOT_ASSESSED);
        assertThat(c.getCreatedAt()).isNotNull();
        assertThat(c.getUpdatedAt()).isNotNull();
    }

    @Test
    void lesValeursPoseesSontGardees() throws Exception {
        ChangeRequest c = new ChangeRequest();
        c.setStatus(ChangeRequestStatus.IN_ANALYSIS);
        c.setType(ChangeRequestType.TEMPORARY);
        c.setClassification(MocClassification.MAJOR);
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(ChangeRequestStatus.IN_ANALYSIS);
        assertThat(c.getType()).isEqualTo(ChangeRequestType.TEMPORARY);
        assertThat(c.getClassification()).isEqualTo(MocClassification.MAJOR);
    }

    @Test
    void laMiseAJourRafraichitLaDate() throws Exception {
        ChangeRequest c = new ChangeRequest();
        c.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = c.getUpdatedAt();
        invoke(c, "preUpdate");
        assertThat(c.getUpdatedAt()).isAfter(before);
    }

    @Test
    void seulsBrouillonEtAnalyseSontOuverts() {
        assertThat(ChangeRequestStatus.DRAFT.open()).isTrue();
        assertThat(ChangeRequestStatus.IN_ANALYSIS.open()).isTrue();
        assertThat(ChangeRequestStatus.IN_VALIDATION.open()).isFalse();
        assertThat(ChangeRequestStatus.APPROVED.open()).isFalse();
        assertThat(ChangeRequestStatus.REJECTED.open()).isFalse();
    }

    private static void invoke(Object target, String name) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(target);
    }
}
