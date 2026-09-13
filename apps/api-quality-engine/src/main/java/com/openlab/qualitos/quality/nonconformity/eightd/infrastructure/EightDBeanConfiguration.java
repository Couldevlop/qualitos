package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDPdfRenderPort;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSealPort;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDService;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSnapshotAssembler;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSourcePort;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDSupportPorts;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReportRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

/**
 * Câble le cas d'usage 8D. Le service est un objet Java nu, sans annotation : c'est
 * ce qui lui permet d'être construit dans un test unitaire avec dix doublures et
 * sans contexte Spring.
 */
@Configuration
public class EightDBeanConfiguration {

    @Bean
    public EightDSnapshotAssembler eightDSnapshotAssembler() {
        return new EightDSnapshotAssembler();
    }

    @Bean
    public EightDService eightDService(EightDReportRepository repository,
                                      EightDSourcePort sources,
                                      EightDSnapshotAssembler assembler,
                                      EightDPdfRenderPort renderer,
                                      EightDSealPort seals,
                                      EightDSupportPorts.SnapshotCodec codec,
                                      EightDSupportPorts.VerifyUrlBuilder verifyUrlBuilder,
                                      EightDSupportPorts.AuditPort audit,
                                      EightDSupportPorts.TenantProvider tenants,
                                      EightDSupportPorts.ActorProvider actors,
                                      Clock clock) {
        return new EightDService(repository, sources, assembler, renderer, seals, codec,
                verifyUrlBuilder, audit, tenants, actors, new SecureRandom(), clock);
    }
}
