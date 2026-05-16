package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.quality.blockchain.application.AnchoringService;
import com.openlab.qualitos.quality.blockchain.domain.AnchorablesPort;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BlockchainBeanConfiguration {

    @Bean
    public AnchoringService anchoringService(AnchorablesPort anchorables,
                                             BlockchainAnchorPort blockchain,
                                             Clock clock) {
        return new AnchoringService(anchorables, blockchain, clock);
    }
}
