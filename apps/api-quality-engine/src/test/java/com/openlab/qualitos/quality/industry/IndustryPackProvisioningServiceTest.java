package com.openlab.qualitos.quality.industry;

import com.openlab.qualitos.quality.kpi.KpiDefinition;
import com.openlab.qualitos.quality.kpi.KpiDefinitionRepository;
import com.openlab.qualitos.quality.kpi.KpiDirection;
import com.openlab.qualitos.quality.kpi.KpiFrequency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndustryPackProvisioningServiceTest {

    @Mock KpiDefinitionRepository kpiRepo;

    IndustryPackProvisioningService service() {
        return new IndustryPackProvisioningService(kpiRepo);
    }

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    /** Manifeste riche minimal (2 KPIs) sérialisé tel qu'en base (camelCase via Jackson). */
    private static final String RICH_JSON = """
            {
              "code":"banking","name":"Banking","version":"3.0.0",
              "richKpis":[
                {"kpiId":"bank_a","name":"KPI A","category":"risk","unit":"%",
                 "formula":"AVG(x)","target":">= 95","thresholdWarning":"85 - 95",
                 "thresholdCritical":"< 85","refreshFrequency":"monthly",
                 "explainability":"explication A"},
                {"kpiId":"bank_b","name":"KPI B","category":"loss","unit":"EUR",
                 "formula":"SUM(y)","target":"< 2,5","refreshFrequency":"realtime"}
              ]
            }
            """;

    /** Manifeste plat (aucun richKpis). */
    private static final String FLAT_JSON = """
            {"code":"manufacturing","name":"Manufacturing","version":"1.0.0",
             "kpis":["oee","fpy"]}
            """;

    @Test
    void richPack_createsAllKpis() {
        when(kpiRepo.findByTenantIdAndCode(eq(TENANT), any())).thenReturn(Optional.empty());
        when(kpiRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IndustryPackProvisioningService.ProvisioningResult r =
                service().provision(TENANT, USER, RICH_JSON);

        assertThat(r.kpisCreated()).isEqualTo(2);
        assertThat(r.kpisSkipped()).isZero();
        assertThat(r.warnings()).isEmpty();
        verify(kpiRepo, times(2)).save(any());
    }

    @Test
    void reActivation_allSkipped() {
        // Tous les codes existent déjà chez le tenant ⇒ skip, jamais d'écrasement.
        when(kpiRepo.findByTenantIdAndCode(eq(TENANT), any()))
                .thenReturn(Optional.of(new KpiDefinition()));

        IndustryPackProvisioningService.ProvisioningResult r =
                service().provision(TENANT, USER, RICH_JSON);

        assertThat(r.kpisCreated()).isZero();
        assertThat(r.kpisSkipped()).isEqualTo(2);
        assertThat(r.warnings()).hasSize(2);
        verify(kpiRepo, never()).save(any());
    }

    @Test
    void flatPack_provisionsNothing() {
        IndustryPackProvisioningService.ProvisioningResult r =
                service().provision(TENANT, USER, FLAT_JSON);

        assertThat(r.kpisCreated()).isZero();
        assertThat(r.kpisSkipped()).isZero();
        assertThat(r.warnings()).isEmpty();
        verifyNoInteractions(kpiRepo);
    }

    @Test
    void preExistingCode_collisionSkippedWithWarning() {
        // bank_a déjà présent (collision), bank_b libre.
        when(kpiRepo.findByTenantIdAndCode(TENANT, "bank_a"))
                .thenReturn(Optional.of(new KpiDefinition()));
        when(kpiRepo.findByTenantIdAndCode(TENANT, "bank_b")).thenReturn(Optional.empty());
        when(kpiRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IndustryPackProvisioningService.ProvisioningResult r =
                service().provision(TENANT, USER, RICH_JSON);

        assertThat(r.kpisCreated()).isEqualTo(1);
        assertThat(r.kpisSkipped()).isEqualTo(1);
        assertThat(r.warnings()).anyMatch(w -> w.contains("bank_a"));
    }

    @Test
    void saveFailure_isResilient_warningNoRethrow() {
        when(kpiRepo.findByTenantIdAndCode(eq(TENANT), any())).thenReturn(Optional.empty());
        when(kpiRepo.save(any())).thenThrow(new RuntimeException("db down"));

        IndustryPackProvisioningService.ProvisioningResult r =
                service().provision(TENANT, USER, RICH_JSON);

        assertThat(r.kpisCreated()).isZero();
        assertThat(r.kpisSkipped()).isEqualTo(2);
        assertThat(r.warnings()).allMatch(w -> w.contains("db down"));
    }

    @Test
    void unreadableManifest_returnsWarningNoFailure() {
        IndustryPackProvisioningService.ProvisioningResult r =
                service().provision(TENANT, USER, "not-json");

        assertThat(r.kpisCreated()).isZero();
        assertThat(r.warnings()).hasSize(1);
        verifyNoInteractions(kpiRepo);
    }

    @Test
    void mapping_thresholdsAndDirectionAndFrequency() {
        when(kpiRepo.findByTenantIdAndCode(eq(TENANT), any())).thenReturn(Optional.empty());
        ArgumentCaptor<KpiDefinition> cap = ArgumentCaptor.forClass(KpiDefinition.class);
        when(kpiRepo.save(cap.capture())).thenAnswer(inv -> inv.getArgument(0));

        service().provision(TENANT, USER, RICH_JSON);

        KpiDefinition a = cap.getAllValues().stream()
                .filter(d -> d.getCode().equals("bank_a")).findFirst().orElseThrow();
        assertThat(a.getTargetValue()).isEqualByComparingTo("95");        // '>= 95'
        assertThat(a.getThresholdWarning()).isEqualByComparingTo("85");   // '85 - 95'
        assertThat(a.getThresholdCritical()).isEqualByComparingTo("85");  // '< 85'
        assertThat(a.getDirection()).isEqualTo(KpiDirection.HIGHER_IS_BETTER);
        assertThat(a.getFrequency()).isEqualTo(KpiFrequency.MONTHLY);
        assertThat(a.getDescription()).isEqualTo("AVG(x) — explication A");
        assertThat(a.getOwnerUserId()).isEqualTo(USER);
        assertThat(a.getCreatedBy()).isEqualTo(USER);

        KpiDefinition b = cap.getAllValues().stream()
                .filter(d -> d.getCode().equals("bank_b")).findFirst().orElseThrow();
        assertThat(b.getTargetValue()).isEqualByComparingTo("2.5");       // '< 2,5' virgule FR
        assertThat(b.getDirection()).isEqualTo(KpiDirection.LOWER_IS_BETTER);
        assertThat(b.getFrequency()).isEqualTo(KpiFrequency.REALTIME);    // 'realtime'
        assertThat(b.getThresholdWarning()).isNull();
    }

    // ---- parsing / mapping unitaires (sans repo) ----

    @Test
    void parseFirstNumber_handlesPercentSpaceCommaAndText() {
        assertThat(IndustryPackProvisioningService.parseFirstNumber(">= 95 %"))
                .isEqualByComparingTo("95");
        assertThat(IndustryPackProvisioningService.parseFirstNumber("< 2,5"))
                .isEqualByComparingTo("2.5");
        assertThat(IndustryPackProvisioningService.parseFirstNumber("0,5"))
                .isEqualByComparingTo("0.5");
        assertThat(IndustryPackProvisioningService.parseFirstNumber("80% budget"))
                .isEqualByComparingTo("80");
        assertThat(IndustryPackProvisioningService.parseFirstNumber("texte sans nombre")).isNull();
        assertThat(IndustryPackProvisioningService.parseFirstNumber("any miss")).isNull();
        assertThat(IndustryPackProvisioningService.parseFirstNumber(null)).isNull();
        assertThat(IndustryPackProvisioningService.parseFirstNumber("")).isNull();
    }

    @Test
    void deduceDirection_fromTargetOperator() {
        assertThat(IndustryPackProvisioningService.deduceDirection(">= 95"))
                .isEqualTo(KpiDirection.HIGHER_IS_BETTER);
        assertThat(IndustryPackProvisioningService.deduceDirection("> 5"))
                .isEqualTo(KpiDirection.HIGHER_IS_BETTER);
        assertThat(IndustryPackProvisioningService.deduceDirection("< 2.5"))
                .isEqualTo(KpiDirection.LOWER_IS_BETTER);
        assertThat(IndustryPackProvisioningService.deduceDirection("<= budget"))
                .isEqualTo(KpiDirection.LOWER_IS_BETTER);
        assertThat(IndustryPackProvisioningService.deduceDirection("100"))
                .isEqualTo(KpiDirection.HIGHER_IS_BETTER); // défaut
        assertThat(IndustryPackProvisioningService.deduceDirection(null))
                .isEqualTo(KpiDirection.HIGHER_IS_BETTER);
    }

    @Test
    void mapFrequency_recognisesPackVocabulary() {
        assertThat(IndustryPackProvisioningService.mapFrequency("realtime")).isEqualTo(KpiFrequency.REALTIME);
        assertThat(IndustryPackProvisioningService.mapFrequency("per-event")).isEqualTo(KpiFrequency.REALTIME);
        assertThat(IndustryPackProvisioningService.mapFrequency("weekly")).isEqualTo(KpiFrequency.WEEKLY);
        assertThat(IndustryPackProvisioningService.mapFrequency("monthly")).isEqualTo(KpiFrequency.MONTHLY);
        assertThat(IndustryPackProvisioningService.mapFrequency("annual")).isEqualTo(KpiFrequency.YEARLY);
        assertThat(IndustryPackProvisioningService.mapFrequency("quarterly")).isEqualTo(KpiFrequency.QUARTERLY);
        assertThat(IndustryPackProvisioningService.mapFrequency("whatever")).isEqualTo(KpiFrequency.MONTHLY);
        assertThat(IndustryPackProvisioningService.mapFrequency(null)).isEqualTo(KpiFrequency.MONTHLY);
    }

    @Test
    void buildDescription_truncatesAndHandlesNulls() {
        assertThat(IndustryPackProvisioningService.buildDescription("f", "e")).isEqualTo("f — e");
        assertThat(IndustryPackProvisioningService.buildDescription("f", null)).isEqualTo("f");
        assertThat(IndustryPackProvisioningService.buildDescription(null, "e")).isEqualTo("e");
        assertThat(IndustryPackProvisioningService.buildDescription(null, null)).isNull();
        String huge = "x".repeat(5000);
        assertThat(IndustryPackProvisioningService.buildDescription(huge, "e")).hasSize(2000);
    }

    @SuppressWarnings("unused")
    private static BigDecimal bd(String s) { return new BigDecimal(s); }
}
