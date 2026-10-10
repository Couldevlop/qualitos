package com.openlab.qualitos.quality.smi.infrastructure;

import com.openlab.qualitos.quality.audit.AuditService;
import com.openlab.qualitos.quality.calibration.CalibrationEquipmentRepository;
import com.openlab.qualitos.quality.calibration.CalibrationPlanRepository;
import com.openlab.qualitos.quality.capa.CapaActionRepository;
import com.openlab.qualitos.quality.change.ChangeRequestRepository;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterService;
import com.openlab.qualitos.quality.smi.application.SmiDashboardService;
import com.openlab.qualitos.quality.standards.StandardsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Câblage du tableau de bord SMI : un service sans Spring, des adaptateurs vers chaque module. */
@Configuration
public class SmiBeanConfiguration {

    @Bean
    @SuppressWarnings("java:S107") // une source par module relu
    public SmiDashboardService smiDashboardService(StandardsService standards,
                                                   CapaActionRepository capaActions,
                                                   RiskRegisterService riskRegister,
                                                   AuditService audits,
                                                   CalibrationPlanRepository calibrationPlans,
                                                   CalibrationEquipmentRepository calibrationEquipments,
                                                   ChangeRequestRepository changes,
                                                   Clock clock) {
        return new SmiDashboardService(
                new SmiAdapters.Standards(standards),
                new SmiAdapters.Actions(capaActions),
                new SmiAdapters.Risks(riskRegister),
                new SmiAdapters.Audits(audits),
                new SmiAdapters.Deadlines(calibrationPlans, calibrationEquipments, changes),
                clock);
    }
}
