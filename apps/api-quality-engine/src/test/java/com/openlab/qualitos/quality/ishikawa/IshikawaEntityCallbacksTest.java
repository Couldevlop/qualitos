package com.openlab.qualitos.quality.ishikawa;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Couvre les callbacks @PrePersist / @PreUpdate des entités JPA Ishikawa.
 *
 * Les services applicatifs sont mockés avec Mockito, ce qui empêche Hibernate
 * d'invoquer ces callbacks ; on les déclenche ici par réflexion pour valider
 * le comportement de l'entité (valeurs par défaut, mise à jour des timestamps).
 */
class IshikawaEntityCallbacksTest {

    @Test
    void diagramPrePersist_setsTimestampsAndDefaults() throws Exception {
        IshikawaDiagram diagram = new IshikawaDiagram();
        diagram.setTenantId(UUID.randomUUID());
        diagram.setProblemStatement("Pb");
        diagram.setOwnerId(UUID.randomUUID());

        invoke(diagram, "prePersist");

        assertThat(diagram.getCreatedAt()).isNotNull();
        assertThat(diagram.getUpdatedAt()).isNotNull();
        assertThat(diagram.getStatus()).isEqualTo(IshikawaStatus.DRAFT);
        assertThat(diagram.getMode()).isEqualTo(IshikawaMode.SIX_M);
    }

    @Test
    void diagramPrePersist_preservesExplicitStatusAndMode() throws Exception {
        IshikawaDiagram diagram = new IshikawaDiagram();
        diagram.setTenantId(UUID.randomUUID());
        diagram.setProblemStatement("Pb");
        diagram.setOwnerId(UUID.randomUUID());
        diagram.setStatus(IshikawaStatus.IN_REVIEW);
        diagram.setMode(IshikawaMode.EIGHT_M);

        invoke(diagram, "prePersist");

        assertThat(diagram.getStatus()).isEqualTo(IshikawaStatus.IN_REVIEW);
        assertThat(diagram.getMode()).isEqualTo(IshikawaMode.EIGHT_M);
    }

    @Test
    void diagramPreUpdate_refreshesUpdatedAt() throws Exception {
        IshikawaDiagram diagram = new IshikawaDiagram();
        diagram.setCreatedAt(Instant.now().minusSeconds(60));
        diagram.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = diagram.getUpdatedAt();

        Thread.sleep(5);
        invoke(diagram, "preUpdate");

        assertThat(diagram.getUpdatedAt()).isAfter(before);
    }

    @Test
    void causePrePersist_setsTimestamps() throws Exception {
        IshikawaCause cause = new IshikawaCause();
        cause.setCategory(CauseCategory.METHODS);
        cause.setLabel("c");

        invoke(cause, "prePersist");

        assertThat(cause.getCreatedAt()).isNotNull();
        assertThat(cause.getUpdatedAt()).isNotNull();
    }

    @Test
    void causePreUpdate_refreshesUpdatedAt() throws Exception {
        IshikawaCause cause = new IshikawaCause();
        cause.setCreatedAt(Instant.now().minusSeconds(60));
        cause.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = cause.getUpdatedAt();

        Thread.sleep(5);
        invoke(cause, "preUpdate");

        assertThat(cause.getUpdatedAt()).isAfter(before);
    }

    private static void invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}
