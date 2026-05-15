package com.openlab.qualitos.quality.calibration;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalibrationServiceTest {

    @Mock CalibrationEquipmentRepository equipmentRepo;
    @Mock CalibrationPlanRepository planRepo;
    @Mock CalibrationRecordRepository recordRepo;
    @Mock MsaStudyRepository msaRepo;
    CalibrationService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID EQ = UUID.randomUUID();
    static final UUID IOT = UUID.randomUUID();
    static final LocalDate TODAY = LocalDate.parse("2026-05-15");
    static final Clock CLOCK = Clock.fixed(
            TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new CalibrationService(equipmentRepo, planRepo, recordRepo, msaRepo, CLOCK);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    // ----- Equipment -----

    @Test
    void create_defaultsActive() {
        when(equipmentRepo.findByTenantIdAndCode(TENANT, "EQ-1")).thenReturn(Optional.empty());
        when(equipmentRepo.save(any())).thenAnswer(inv -> {
            CalibrationEquipment e = inv.getArgument(0);
            e.setId(EQ); e.setCreatedAt(Instant.now()); e.setUpdatedAt(Instant.now());
            return e;
        });
        CalibrationDto.EquipmentResponse out = service.createEquipment(
                new CalibrationDto.CreateEquipmentRequest(
                        "EQ-1", "Caliper", "Mitutoyo", "CD-15CPX", "SN-1",
                        "Lab", true, IOT, null, USER));
        assertThat(out.status()).isEqualTo(EquipmentStatus.ACTIVE);
        assertThat(out.critical()).isTrue();
        assertThat(out.iotDeviceId()).isEqualTo(IOT);
    }

    @Test
    void create_duplicateCode_throws() {
        when(equipmentRepo.findByTenantIdAndCode(TENANT, "dup"))
                .thenReturn(Optional.of(equipment(EquipmentStatus.ACTIVE)));
        assertThatThrownBy(() -> service.createEquipment(new CalibrationDto.CreateEquipmentRequest(
                "dup", "x", null, null, null, null, false, null, null, USER)))
                .isInstanceOf(CalibrationStateException.class);
    }

    @Test
    void create_noTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.createEquipment(new CalibrationDto.CreateEquipmentRequest(
                "EQ-9", "x", null, null, null, null, false, null, null, USER)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void get_crossTenant_appearsNotFound() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        e.setTenantId(UUID.randomUUID());
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.getEquipment(EQ))
                .isInstanceOf(CalibrationEquipmentNotFoundException.class);
    }

    @Test
    void update_retired_rejected() {
        CalibrationEquipment e = equipment(EquipmentStatus.RETIRED);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.updateEquipment(EQ, new CalibrationDto.UpdateEquipmentRequest(
                "x", null, null, null, null, null, null, null)))
                .isInstanceOf(CalibrationStateException.class);
    }

    @Test
    void update_appliesPatches() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(equipmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.updateEquipment(EQ, new CalibrationDto.UpdateEquipmentRequest(
                "Caliper v2", "Mitutoyo", "CD-20", "SN-2", "Lab A", true, IOT, USER));
        assertThat(e.getName()).isEqualTo("Caliper v2");
        assertThat(e.isCritical()).isTrue();
        assertThat(e.getIotDeviceId()).isEqualTo(IOT);
    }

    @Test
    void setStatus_critical_outOfService_to_active_withoutPass_rejected() {
        CalibrationEquipment e = equipment(EquipmentStatus.OUT_OF_SERVICE);
        e.setCritical(true);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(recordRepo.findByEquipmentIdOrderByPerformedOnDesc(eq(EQ), any()))
                .thenReturn(new PageImpl<>(List.of(record(CalibrationResult.FAIL))));
        assertThatThrownBy(() -> service.setEquipmentStatus(EQ, EquipmentStatus.ACTIVE))
                .isInstanceOf(CalibrationStateException.class)
                .hasMessageContaining("PASS");
    }

    @Test
    void setStatus_critical_outOfService_to_active_withPass_ok() {
        CalibrationEquipment e = equipment(EquipmentStatus.OUT_OF_SERVICE);
        e.setCritical(true);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(recordRepo.findByEquipmentIdOrderByPerformedOnDesc(eq(EQ), any()))
                .thenReturn(new PageImpl<>(List.of(record(CalibrationResult.PASS))));
        when(equipmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.setEquipmentStatus(EQ, EquipmentStatus.ACTIVE).status())
                .isEqualTo(EquipmentStatus.ACTIVE);
    }

    @Test
    void setStatus_nonCritical_outOfService_to_active_alwaysOk() {
        CalibrationEquipment e = equipment(EquipmentStatus.OUT_OF_SERVICE);
        e.setCritical(false);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(equipmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.setEquipmentStatus(EQ, EquipmentStatus.ACTIVE).status())
                .isEqualTo(EquipmentStatus.ACTIVE);
        verify(recordRepo, never()).findByEquipmentIdOrderByPerformedOnDesc(any(), any());
    }

    @Test
    void setStatus_fromRetired_rejected() {
        CalibrationEquipment e = equipment(EquipmentStatus.RETIRED);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.setEquipmentStatus(EQ, EquipmentStatus.ACTIVE))
                .isInstanceOf(CalibrationStateException.class);
    }

    @Test
    void setStatus_nullTarget_rejected() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.setEquipmentStatus(EQ, null))
                .isInstanceOf(CalibrationStateException.class);
    }

    @Test
    void delete_cascades() {
        CalibrationEquipment e = equipment(EquipmentStatus.RETIRED);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        service.deleteEquipment(EQ);
        verify(recordRepo).deleteByEquipmentId(EQ);
        verify(msaRepo).deleteByEquipmentId(EQ);
        verify(planRepo).deleteByEquipmentId(EQ);
        verify(equipmentRepo).delete(e);
    }

    @Test
    void list_filtered_or_all() {
        when(equipmentRepo.findByTenantIdAndStatus(eq(TENANT), eq(EquipmentStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(equipment(EquipmentStatus.ACTIVE))));
        assertThat(service.listEquipment(EquipmentStatus.ACTIVE, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(equipmentRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(equipment(EquipmentStatus.ACTIVE))));
        assertThat(service.listEquipment(null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    // ----- Plan -----

    @Test
    void upsertPlan_new_takesFirstDueOn() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(planRepo.findByEquipmentId(EQ)).thenReturn(Optional.empty());
        when(planRepo.save(any())).thenAnswer(inv -> {
            CalibrationPlan p = inv.getArgument(0);
            p.setId(UUID.randomUUID()); p.setCreatedAt(Instant.now()); p.setUpdatedAt(Instant.now());
            return p;
        });
        LocalDate due = LocalDate.parse("2026-09-01");
        CalibrationDto.PlanResponse out = service.upsertPlan(EQ,
                new CalibrationDto.UpsertPlanRequest(12, "PROC-001", "±0.1mm", "COFRAC", due));
        assertThat(out.frequencyMonths()).isEqualTo(12);
        assertThat(out.nextDueOn()).isEqualTo(due);
    }

    @Test
    void upsertPlan_existing_keepsNextDueOnFromRecords() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        CalibrationPlan existing = new CalibrationPlan();
        existing.setId(UUID.randomUUID()); existing.setTenantId(TENANT);
        existing.setEquipmentId(EQ); existing.setFrequencyMonths(6);
        existing.setNextDueOn(LocalDate.parse("2026-12-01"));
        when(planRepo.findByEquipmentId(EQ)).thenReturn(Optional.of(existing));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CalibrationDto.PlanResponse out = service.upsertPlan(EQ,
                new CalibrationDto.UpsertPlanRequest(
                        18, "PROC-002", "±0.05mm", "DAKKS",
                        LocalDate.parse("2026-01-01")));
        assertThat(out.frequencyMonths()).isEqualTo(18);
        // firstDueOn ignoré sur update
        assertThat(out.nextDueOn()).isEqualTo(LocalDate.parse("2026-12-01"));
    }

    @Test
    void upsertPlan_retired_rejected() {
        CalibrationEquipment e = equipment(EquipmentStatus.RETIRED);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.upsertPlan(EQ, new CalibrationDto.UpsertPlanRequest(
                12, null, null, null, LocalDate.parse("2026-09-01"))))
                .isInstanceOf(CalibrationStateException.class);
    }

    @Test
    void deletePlan_critical_rejected() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        e.setCritical(true);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(planRepo.findByEquipmentId(EQ)).thenReturn(Optional.of(new CalibrationPlan()));
        assertThatThrownBy(() -> service.deletePlan(EQ))
                .isInstanceOf(CalibrationStateException.class);
    }

    @Test
    void deletePlan_nonCritical_ok() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(planRepo.findByEquipmentId(EQ)).thenReturn(Optional.of(new CalibrationPlan()));
        service.deletePlan(EQ);
        verify(planRepo).deleteByEquipmentId(EQ);
    }

    @Test
    void deletePlan_missing_404() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(planRepo.findByEquipmentId(EQ)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deletePlan(EQ))
                .isInstanceOf(CalibrationChildNotFoundException.class);
    }

    @Test
    void overdue_defaultsToToday() {
        when(planRepo.findByTenantIdAndNextDueOnBefore(eq(TENANT), eq(TODAY), any()))
                .thenReturn(new PageImpl<>(List.of(planEntity(LocalDate.parse("2026-01-01")))));
        assertThat(service.overdue(null, PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    @Test
    void overdue_customCutoff() {
        LocalDate cutoff = LocalDate.parse("2026-12-31");
        when(planRepo.findByTenantIdAndNextDueOnBefore(eq(TENANT), eq(cutoff), any()))
                .thenReturn(new PageImpl<>(List.of(planEntity(LocalDate.parse("2026-06-01")))));
        assertThat(service.overdue(cutoff, PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    // ----- Records -----

    @Test
    void addRecord_recomputesPlanNextDueOn() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(recordRepo.save(any())).thenAnswer(inv -> {
            CalibrationRecord r = inv.getArgument(0);
            r.setId(UUID.randomUUID()); r.setCreatedAt(Instant.now()); return r;
        });
        CalibrationPlan p = planEntity(LocalDate.parse("2026-06-01"));
        when(planRepo.findByEquipmentId(EQ)).thenReturn(Optional.of(p));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.addRecord(EQ, new CalibrationDto.CreateRecordRequest(
                LocalDate.parse("2026-05-01"), USER, "AcmeLab",
                CalibrationResult.PASS, "values...", "CERT-12", null));
        // performedOn 2026-05-01 + frequencyMonths 12 = 2027-05-01
        assertThat(p.getNextDueOn()).isEqualTo(LocalDate.parse("2027-05-01"));
        assertThat(p.getLastCalibratedOn()).isEqualTo(LocalDate.parse("2026-05-01"));
    }

    @Test
    void addRecord_overrideUsedWhenProvided() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(recordRepo.save(any())).thenAnswer(inv -> {
            CalibrationRecord r = inv.getArgument(0);
            r.setId(UUID.randomUUID()); r.setCreatedAt(Instant.now()); return r;
        });
        CalibrationPlan p = planEntity(LocalDate.parse("2026-06-01"));
        when(planRepo.findByEquipmentId(EQ)).thenReturn(Optional.of(p));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LocalDate override = LocalDate.parse("2026-09-15");
        service.addRecord(EQ, new CalibrationDto.CreateRecordRequest(
                LocalDate.parse("2026-05-01"), USER, null,
                CalibrationResult.CONDITIONAL, null, null, override));
        assertThat(p.getNextDueOn()).isEqualTo(override);
    }

    @Test
    void addRecord_failOnCritical_forcesOutOfService() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        e.setCritical(true);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(recordRepo.save(any())).thenAnswer(inv -> {
            CalibrationRecord r = inv.getArgument(0);
            r.setId(UUID.randomUUID()); r.setCreatedAt(Instant.now()); return r;
        });
        when(planRepo.findByEquipmentId(EQ)).thenReturn(Optional.empty());
        when(equipmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.addRecord(EQ, new CalibrationDto.CreateRecordRequest(
                LocalDate.parse("2026-05-01"), null, null,
                CalibrationResult.FAIL, null, null, null));
        ArgumentCaptor<CalibrationEquipment> cap = ArgumentCaptor.forClass(CalibrationEquipment.class);
        verify(equipmentRepo).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(EquipmentStatus.OUT_OF_SERVICE);
    }

    @Test
    void addRecord_failOnNonCritical_keepsStatus() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        e.setCritical(false);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(recordRepo.save(any())).thenAnswer(inv -> {
            CalibrationRecord r = inv.getArgument(0);
            r.setId(UUID.randomUUID()); r.setCreatedAt(Instant.now()); return r;
        });
        when(planRepo.findByEquipmentId(EQ)).thenReturn(Optional.empty());
        service.addRecord(EQ, new CalibrationDto.CreateRecordRequest(
                LocalDate.parse("2026-05-01"), null, null,
                CalibrationResult.FAIL, null, null, null));
        verify(equipmentRepo, never()).save(any());
    }

    @Test
    void addRecord_nonActive_rejected() {
        CalibrationEquipment e = equipment(EquipmentStatus.OUT_OF_SERVICE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.addRecord(EQ, new CalibrationDto.CreateRecordRequest(
                LocalDate.parse("2026-05-01"), null, null,
                CalibrationResult.PASS, null, null, null)))
                .isInstanceOf(CalibrationStateException.class);
    }

    @Test
    void listRecords_paginated() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(recordRepo.findByEquipmentIdOrderByPerformedOnDesc(eq(EQ), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record(CalibrationResult.PASS))));
        assertThat(service.listRecords(EQ, PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    // ----- MSA -----

    @Test
    void addMsa_persists() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(msaRepo.save(any())).thenAnswer(inv -> {
            MsaStudy m = inv.getArgument(0);
            m.setId(UUID.randomUUID()); m.setCreatedAt(Instant.now()); return m;
        });
        CalibrationDto.MsaResponse out = service.addMsa(EQ, new CalibrationDto.CreateMsaRequest(
                MsaType.GAGE_R_R, LocalDate.parse("2026-04-01"),
                new BigDecimal("8.50"), new BigDecimal("10.00"),
                MsaResult.PASS, "notes", USER));
        assertThat(out.type()).isEqualTo(MsaType.GAGE_R_R);
        assertThat(out.result()).isEqualTo(MsaResult.PASS);
    }

    @Test
    void addMsa_retired_rejected() {
        CalibrationEquipment e = equipment(EquipmentStatus.RETIRED);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.addMsa(EQ, new CalibrationDto.CreateMsaRequest(
                MsaType.BIAS, LocalDate.parse("2026-04-01"),
                BigDecimal.ONE, null, MsaResult.FAIL, null, USER)))
                .isInstanceOf(CalibrationStateException.class);
    }

    @Test
    void listMsa_paginated() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(msaRepo.findByEquipmentIdOrderByPerformedOnDesc(eq(EQ), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new MsaStudy())));
        assertThat(service.listMsa(EQ, PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    // ----- Summary -----

    @Test
    void summary_aggregates() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        e.setCritical(true);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        CalibrationPlan p = planEntity(LocalDate.parse("2026-01-01"));
        p.setLastCalibratedOn(LocalDate.parse("2025-01-01"));
        when(planRepo.findByEquipmentId(EQ)).thenReturn(Optional.of(p));
        Page<CalibrationRecord> lastPage = new PageImpl<>(List.of(record(CalibrationResult.PASS)));
        when(recordRepo.findByEquipmentIdOrderByPerformedOnDesc(eq(EQ), any(Pageable.class)))
                .thenReturn(lastPage);
        when(recordRepo.countByEquipmentIdAndResult(EQ, CalibrationResult.PASS)).thenReturn(5L);
        when(recordRepo.countByEquipmentIdAndResult(EQ, CalibrationResult.FAIL)).thenReturn(1L);
        when(recordRepo.countByEquipmentIdAndResult(EQ, CalibrationResult.CONDITIONAL)).thenReturn(2L);
        when(msaRepo.countByEquipmentIdAndResult(EQ, MsaResult.PASS)).thenReturn(3L);
        when(msaRepo.countByEquipmentIdAndResult(EQ, MsaResult.FAIL)).thenReturn(0L);
        CalibrationDto.EquipmentSummary s = service.summary(EQ);
        assertThat(s.passRecords()).isEqualTo(5L);
        assertThat(s.failRecords()).isOne();
        assertThat(s.conditionalRecords()).isEqualTo(2L);
        assertThat(s.msaPass()).isEqualTo(3L);
        assertThat(s.overdue()).isTrue();
        assertThat(s.lastResult()).isEqualTo(CalibrationResult.PASS);
        assertThat(s.lastCalibratedOn()).isEqualTo(LocalDate.parse("2025-01-01"));
    }

    @Test
    void summary_noPlan_noLastResult_safe() {
        CalibrationEquipment e = equipment(EquipmentStatus.ACTIVE);
        when(equipmentRepo.findById(EQ)).thenReturn(Optional.of(e));
        when(planRepo.findByEquipmentId(EQ)).thenReturn(Optional.empty());
        when(recordRepo.findByEquipmentIdOrderByPerformedOnDesc(eq(EQ), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        CalibrationDto.EquipmentSummary s = service.summary(EQ);
        assertThat(s.nextDueOn()).isNull();
        assertThat(s.lastResult()).isNull();
        assertThat(s.overdue()).isFalse();
    }

    // ----- helpers -----

    private CalibrationEquipment equipment(EquipmentStatus status) {
        CalibrationEquipment e = new CalibrationEquipment();
        e.setId(EQ); e.setTenantId(TENANT);
        e.setCode("EQ-1"); e.setName("Caliper");
        e.setStatus(status); e.setCritical(false);
        e.setCreatedBy(USER);
        e.setCreatedAt(Instant.now()); e.setUpdatedAt(Instant.now());
        return e;
    }

    private CalibrationPlan planEntity(LocalDate nextDue) {
        CalibrationPlan p = new CalibrationPlan();
        p.setId(UUID.randomUUID()); p.setTenantId(TENANT);
        p.setEquipmentId(EQ); p.setFrequencyMonths(12);
        p.setNextDueOn(nextDue);
        p.setCreatedAt(Instant.now()); p.setUpdatedAt(Instant.now());
        return p;
    }

    private CalibrationRecord record(CalibrationResult result) {
        CalibrationRecord r = new CalibrationRecord();
        r.setId(UUID.randomUUID()); r.setTenantId(TENANT);
        r.setEquipmentId(EQ); r.setPerformedOn(LocalDate.parse("2026-05-01"));
        r.setResult(result); r.setCreatedAt(Instant.now());
        return r;
    }
}
