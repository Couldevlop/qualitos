package com.openlab.qualitos.quality.controlplan.infrastructure;

import com.openlab.qualitos.quality.controlplan.domain.CharacteristicType;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlan;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanLine;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanPhase;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanStatus;
import com.openlab.qualitos.quality.risk.CharacteristicClass;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'aller-retour domaine → entité → domaine. Un champ oublié dans le mapper ne
 * casse aucun test de service — il se contente de faire disparaître silencieusement
 * une tolérance ou un plan de réaction entre deux redémarrages.
 */
class ControlPlanMapperTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID PLAN = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    @Test
    void aPlanSurvivesTheRoundTrip() {
        ControlPlan source = ControlPlan.rehydrate(PLAN, TENANT, PRODUCT,
                ControlPlanPhase.PRE_LAUNCH, "CP-4471", 4, ControlPlanStatus.ACTIVE,
                USER, USER, NOW, USER, NOW, NOW.plusSeconds(60));

        ControlPlan back = ControlPlanMapper.toDomain(ControlPlanMapper.toEntity(source, null));

        assertThat(back.getId()).isEqualTo(PLAN);
        assertThat(back.getTenantId()).isEqualTo(TENANT);
        assertThat(back.getProductId()).isEqualTo(PRODUCT);
        assertThat(back.getPhase()).isEqualTo(ControlPlanPhase.PRE_LAUNCH);
        assertThat(back.getCode()).isEqualTo("CP-4471");
        assertThat(back.getRevision()).isEqualTo(4);
        assertThat(back.getStatus()).isEqualTo(ControlPlanStatus.ACTIVE);
        assertThat(back.getOwnerUserId()).isEqualTo(USER);
        assertThat(back.getApprovedBy()).isEqualTo(USER);
        assertThat(back.getApprovedAt()).isEqualTo(NOW);
        assertThat(back.getCreatedBy()).isEqualTo(USER);
        assertThat(back.getCreatedAt()).isEqualTo(NOW);
        assertThat(back.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void aFreshPlanWithoutAnIdKeepsTheGeneratedOneOfTheEntity() {
        ControlPlan source = ControlPlan.create(TENANT, PRODUCT, ControlPlanPhase.PROTOTYPE,
                "CP-1", USER, NOW);

        ControlPlanJpaEntity entity = ControlPlanMapper.toEntity(source, null);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getStatus()).isEqualTo("DRAFT");
        assertThat(entity.getRevision()).isEqualTo(1);
    }

    @Test
    void anExistingEntityIsUpdatedInPlaceRatherThanReplaced() {
        ControlPlanJpaEntity existing = new ControlPlanJpaEntity();
        existing.setId(PLAN);
        ControlPlan source = ControlPlan.rehydrate(PLAN, TENANT, PRODUCT,
                ControlPlanPhase.PRODUCTION, "CP-2", 2, ControlPlanStatus.DRAFT,
                null, null, null, USER, NOW, NOW);

        ControlPlanJpaEntity target = ControlPlanMapper.toEntity(source, existing);

        assertThat(target).isSameAs(existing);
        assertThat(target.getCode()).isEqualTo("CP-2");
    }

    @Test
    void aLineSurvivesTheRoundTripWithAllItsAiagColumns() {
        UUID lineId = UUID.randomUUID();
        UUID operation = UUID.randomUUID();
        UUID fmeaItem = UUID.randomUUID();
        ControlPlanLine source = ControlPlanLine.rehydrate(lineId, TENANT, PLAN, 30,
                "Diamètre alésage", CharacteristicType.PRODUCT);
        source.describe(operation, "Tour CN 3", "12", CharacteristicClass.SAFETY, "Ø 20 ±0,05",
                new BigDecimal("19.950000"), new BigDecimal("20.050000"), "mm",
                "Micromètre", 5, "1 pièce / heure", "Carte X-R", "Tri à 100 %");
        source.justifiedBy(fmeaItem);

        ControlPlanLine back = ControlPlanMapper.toDomain(ControlPlanMapper.toEntity(source, null));

        assertThat(back.getId()).isEqualTo(lineId);
        assertThat(back.getTenantId()).isEqualTo(TENANT);
        assertThat(back.getPlanId()).isEqualTo(PLAN);
        assertThat(back.getSequenceNo()).isEqualTo(30);
        assertThat(back.getOperationId()).isEqualTo(operation);
        assertThat(back.getMachine()).isEqualTo("Tour CN 3");
        assertThat(back.getCharacteristicNo()).isEqualTo("12");
        assertThat(back.getCharacteristicLabel()).isEqualTo("Diamètre alésage");
        assertThat(back.getCharacteristicType()).isEqualTo(CharacteristicType.PRODUCT);
        assertThat(back.getSpecialClass()).isEqualTo(CharacteristicClass.SAFETY);
        assertThat(back.getSpecification()).isEqualTo("Ø 20 ±0,05");
        assertThat(back.getToleranceLower()).isEqualByComparingTo("19.95");
        assertThat(back.getToleranceUpper()).isEqualByComparingTo("20.05");
        assertThat(back.getUnit()).isEqualTo("mm");
        assertThat(back.getMeasurementTechnique()).isEqualTo("Micromètre");
        assertThat(back.getSampleSize()).isEqualTo(5);
        assertThat(back.getSampleFrequency()).isEqualTo("1 pièce / heure");
        assertThat(back.getControlMethod()).isEqualTo("Carte X-R");
        assertThat(back.getReactionPlan()).isEqualTo("Tri à 100 %");
        assertThat(back.getFmeaItemId()).isEqualTo(fmeaItem);
    }

    @Test
    void aLineWhoseSpecialClassIsAbsentInDatabaseFallsBackToStandard() {
        ControlPlanLineJpaEntity entity = new ControlPlanLineJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(TENANT);
        entity.setPlanId(PLAN);
        entity.setSequenceNo(10);
        entity.setCharacteristicLabel("Cote");
        entity.setCharacteristicType("PROCESS");
        entity.setSpecialClass(null);

        ControlPlanLine back = ControlPlanMapper.toDomain(entity);

        assertThat(back.getSpecialClass()).isEqualTo(CharacteristicClass.STANDARD);
        assertThat(back.getCharacteristicType()).isEqualTo(CharacteristicType.PROCESS);
    }

    @Test
    void anExistingLineEntityIsUpdatedInPlace() {
        ControlPlanLineJpaEntity existing = new ControlPlanLineJpaEntity();
        ControlPlanLine source = ControlPlanLine.create(TENANT, PLAN, 10, "Cote",
                CharacteristicType.PRODUCT);

        ControlPlanLineJpaEntity target = ControlPlanMapper.toEntity(source, existing);

        assertThat(target).isSameAs(existing);
        assertThat(target.getId()).isNull();
        assertThat(target.getSpecialClass()).isEqualTo("STANDARD");
    }
}
