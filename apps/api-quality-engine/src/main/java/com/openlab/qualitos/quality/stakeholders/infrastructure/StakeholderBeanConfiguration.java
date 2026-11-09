package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterService;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage Spring du registre des parties intéressées.
 *
 * <p>Le {@code Clock} vient de la configuration commune, unique et
 * {@code @Primary} : en déclarer un second ici rendrait l'injection ambiguë.
 */
@Configuration
public class StakeholderBeanConfiguration {

    @Bean
    public StakeholderService stakeholderService(StakeholderJpaRepository stakeholders,
                                                 CapaService capaService,
                                                 RiskRegisterService riskRegister,
                                                 AiGatewayClient ai,
                                                 ObjectMapper mapper,
                                                 Clock clock) {
        return new StakeholderService(
                new StakeholderRepositoryAdapter(stakeholders),
                new JwtStakeholderContext(),
                new CapaStakeholderGateway(capaService),
                new RiskStakeholderGateway(riskRegister),
                new AiStakeholderAssistant(ai, mapper),
                clock);
    }
}
