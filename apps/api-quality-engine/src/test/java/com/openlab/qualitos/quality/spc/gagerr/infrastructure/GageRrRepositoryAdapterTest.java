package com.openlab.qualitos.quality.spc.gagerr.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.spc.capability.domain.SpcNotFoundException;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrDecision;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrFixtures;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrStudy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'adaptateur sur un vrai mapping JPA (contexte complet, H2 en mode
 * PostgreSQL) : grille et opérateurs aller-retour en JSON, verdict relu, et la
 * frontière entre clients.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class GageRrRepositoryAdapterTest {

    @Autowired GageRrStudyJpaRepository jpa;
    @Autowired ObjectMapper objectMapper;

    GageRrRepositoryAdapter studies;
    UUID tenant;

    @BeforeEach
    void setup() {
        studies = new GageRrRepositoryAdapter(jpa, objectMapper);
        tenant = UUID.randomUUID();
    }

    GageRrStudy etude(UUID t) {
        return GageRrStudy.record(t, GageRrFixtures.workbookDetails(), UUID.randomUUID(),
                Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    void uneEtudeSEnregistreEtSeRelitAChampsEgaux() {
        GageRrStudy enregistree = studies.save(etude(tenant));

        GageRrStudy relue = studies.findByIdAndTenant(enregistree.getId(), tenant).orElseThrow();
        assertThat(relue.getPartReference()).isEqualTo("Arbre AX-210");
        assertThat(relue.getResponsibleName()).isEqualTo("Jeanne Leroy");
        assertThat(relue.getOperators()).containsExactlyElementsOf(GageRrFixtures.operators());
        assertThat(relue.getMeasurements()).hasSize(3);
        assertThat(relue.getMeasurements().get(0).get(0).get(0)).isEqualTo(9.955);
        assertThat(relue.getMeasurements().get(2).get(9).get(2)).isEqualTo(10.077);
        assertThat(relue.getGrrPercent()).isEqualTo(21.0);
        assertThat(relue.getAnovaGrrPercent()).isEqualTo(21.16);
        assertThat(relue.getNdc()).isEqualTo(6);
        assertThat(relue.getDecision()).isEqualTo(GageRrDecision.CONDITIONAL);
        assertThat(relue.getSigmaMultiplier()).isEqualTo(5.15);
        assertThat(relue.calculation().ndc()).isEqualTo(6);
    }

    @Test
    void uneCorrectionReecritLaMemeLigne() {
        GageRrStudy enregistree = studies.save(etude(tenant));
        enregistree.revise(GageRrFixtures.details(GageRrFixtures.workbookGrid(), GageRrFixtures.operators(), 6.0),
                Instant.now());

        GageRrStudy corrigee = studies.save(enregistree);

        assertThat(corrigee.getId()).isEqualTo(enregistree.getId());
        assertThat(corrigee.getSigmaMultiplier()).isEqualTo(6.0);
        assertThat(studies.findByTenant(tenant)).hasSize(1);
    }

    @Test
    void lesEtudesDUnAutreClientNeSeVoientNiNeSEcrivent() {
        GageRrStudy chezMoi = studies.save(etude(tenant));
        UUID autre = UUID.randomUUID();
        studies.save(etude(autre));

        assertThat(studies.findByTenant(tenant)).extracting(GageRrStudy::getId).containsExactly(chezMoi.getId());
        assertThat(studies.findByIdAndTenant(chezMoi.getId(), autre)).isEmpty();

        GageRrStudy usurpee = new GageRrStudy(chezMoi.getId(), autre, "X", null, null, 9.9, 10.1, 3, 10, 3, 5.15,
                chezMoi.getStudyDate(), null, null, chezMoi.getOperators(), chezMoi.getMeasurements(), 1, 1, 1,
                GageRrDecision.ACCEPTABLE, 1, 1, null, Instant.now(), Instant.now());
        assertThatThrownBy(() -> studies.save(usurpee)).isInstanceOf(SpcNotFoundException.class);
        studies.delete(usurpee);
        assertThat(studies.findByIdAndTenant(chezMoi.getId(), tenant)).isPresent();
    }

    @Test
    void supprimerRetireLEtude() {
        GageRrStudy e = studies.save(etude(tenant));
        studies.delete(e);
        assertThat(studies.findByIdAndTenant(e.getId(), tenant)).isEmpty();
    }

    @Test
    void uneGrilleIllisibleEstUneErreurDeStockage() {
        GageRrStudy e = studies.save(etude(tenant));
        GageRrStudyJpaEntity ligne = jpa.findById(e.getId()).orElseThrow();
        ligne.setMeasurements("pas du json");
        jpa.save(ligne);

        assertThatThrownBy(() -> studies.findByIdAndTenant(e.getId(), tenant))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void uneEcritureImpossibleEstUneErreurDeStockage() throws Exception {
        ObjectMapper casse = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
                throw new com.fasterxml.jackson.core.JsonGenerationException("casse", (com.fasterxml.jackson.core.JsonGenerator) null);
            }
        };
        GageRrRepositoryAdapter adaptateur = new GageRrRepositoryAdapter(jpa, casse);
        assertThatThrownBy(() -> adaptateur.save(etude(tenant))).isInstanceOf(IllegalStateException.class);
    }
}
