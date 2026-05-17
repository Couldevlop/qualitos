package com.openlab.qualitos.quality.ims.application;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.ims.application.usecase.GetCoverageMatrixUseCase;
import com.openlab.qualitos.quality.ims.domain.model.ClauseMapping;
import com.openlab.qualitos.quality.ims.domain.model.ClauseRef;
import com.openlab.qualitos.quality.ims.domain.model.CoverageMatrix;
import com.openlab.qualitos.quality.ims.domain.model.RelationType;
import com.openlab.qualitos.quality.ims.domain.port.ClauseMappingRepository;
import com.openlab.qualitos.quality.ims.domain.port.TenantStandardCodesProvider;
import com.openlab.qualitos.quality.ims.domain.service.CoverageMatrixDomainService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCoverageMatrixUseCaseTest {

    @Mock ClauseMappingRepository mappingRepo;
    @Mock TenantStandardCodesProvider tenantStandardCodes;

    GetCoverageMatrixUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCoverageMatrixUseCase(mappingRepo, tenantStandardCodes, new CoverageMatrixDomainService());
        TenantContext.setTenantId("11111111-1111-1111-1111-111111111111");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void execute_failsWhenTenantContextMissing() {
        TenantContext.clear();
        assertThatThrownBy(() -> useCase.execute(List.of()))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void execute_usesAdoptedCodesWhenRequestedListIsNull() {
        when(tenantStandardCodes.findAdoptedStandardCodes())
                .thenReturn(List.of("iso-9001", "iso-14001"));
        when(mappingRepo.findMappingsBetween(any()))
                .thenReturn(List.of(map("iso-9001", "4.1", "iso-14001", "4.1")));

        CoverageMatrix r = useCase.execute(null);
        assertThat(r.standardCodes()).containsExactly("iso-9001", "iso-14001");
        assertThat(r.totalMappings()).isEqualTo(1);
        verify(tenantStandardCodes).findAdoptedStandardCodes();
    }

    @Test
    void execute_usesProvidedCodesWhenNonEmpty() {
        when(mappingRepo.findMappingsBetween(any()))
                .thenReturn(List.of(map("iso-9001", "4.1", "iso-14001", "4.1")));
        CoverageMatrix r = useCase.execute(List.of("iso-9001", "iso-14001"));
        assertThat(r.standardCodes()).containsExactly("iso-9001", "iso-14001");
        verify(tenantStandardCodes, never()).findAdoptedStandardCodes();
    }

    @Test
    void execute_emptyTenantAdoptionsReturnsEmptyMatrix() {
        when(tenantStandardCodes.findAdoptedStandardCodes()).thenReturn(List.of());
        CoverageMatrix r = useCase.execute(null);
        assertThat(r.cells()).isEmpty();
        assertThat(r.totalMappings()).isZero();
    }

    private static ClauseMapping map(String srcStd, String srcCl, String tgtStd, String tgtCl) {
        return new ClauseMapping(
                new ClauseRef(srcStd, srcCl),
                new ClauseRef(tgtStd, tgtCl),
                RelationType.EQUIVALENT, 100, null);
    }
}
