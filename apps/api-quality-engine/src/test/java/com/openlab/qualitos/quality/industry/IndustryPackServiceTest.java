package com.openlab.qualitos.quality.industry;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndustryPackServiceTest {

    @Mock IndustryPackRepository packRepo;
    @Mock TenantIndustryPackActivationRepository activationRepo;
    @InjectMocks IndustryPackService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() { TenantContext.setTenantId(TENANT.toString()); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void listAll_paginates() {
        when(packRepo.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(pack("manufacturing"))));
        assertThat(service.listAll(PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    @Test
    void getByCode_found() {
        when(packRepo.findByCode("it-itsm")).thenReturn(Optional.of(pack("it-itsm")));
        assertThat(service.getByCode("it-itsm").code()).isEqualTo("it-itsm");
    }

    @Test
    void getByCode_missing_throws404() {
        when(packRepo.findByCode("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByCode("nope"))
                .isInstanceOf(IndustryPackNotFoundException.class);
    }

    @Test
    void activate_newPack_persists() {
        when(packRepo.findByCode("manufacturing")).thenReturn(Optional.of(pack("manufacturing")));
        when(activationRepo.findByTenantIdAndPackCodeAndStatus(TENANT, "manufacturing", ActivationStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(activationRepo.save(any())).thenAnswer(inv -> {
            TenantIndustryPackActivation a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        IndustryPackDto.ActivationResponse out = service.activate("manufacturing",
                new IndustryPackDto.ActivateRequest(USER, "{\"override\":1}"));
        assertThat(out.status()).isEqualTo(ActivationStatus.ACTIVE);
        assertThat(out.tenantId()).isEqualTo(TENANT);
        assertThat(out.activatedBy()).isEqualTo(USER);
    }

    @Test
    void activate_alreadyActive_idempotent() {
        when(packRepo.findByCode("it-itsm")).thenReturn(Optional.of(pack("it-itsm")));
        TenantIndustryPackActivation existing = activation("it-itsm", ActivationStatus.ACTIVE);
        when(activationRepo.findByTenantIdAndPackCodeAndStatus(TENANT, "it-itsm", ActivationStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

        IndustryPackDto.ActivationResponse out = service.activate("it-itsm",
                new IndustryPackDto.ActivateRequest(USER, null));

        assertThat(out.id()).isEqualTo(existing.getId());
        verify(activationRepo, never()).save(any());
    }

    @Test
    void activate_packNotInCatalog_throws() {
        when(packRepo.findByCode("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.activate("ghost",
                new IndustryPackDto.ActivateRequest(USER, null)))
                .isInstanceOf(IndustryPackNotFoundException.class);
        verify(activationRepo, never()).save(any());
    }

    @Test
    void activate_withoutTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.activate("manufacturing",
                new IndustryPackDto.ActivateRequest(USER, null)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void deactivate_flipsStatusAndStamps() {
        TenantIndustryPackActivation a = activation("manufacturing", ActivationStatus.ACTIVE);
        when(activationRepo.findByTenantIdAndPackCodeAndStatus(TENANT, "manufacturing", ActivationStatus.ACTIVE))
                .thenReturn(Optional.of(a));
        when(activationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IndustryPackDto.ActivationResponse out = service.deactivate("manufacturing", USER);

        assertThat(out.status()).isEqualTo(ActivationStatus.DEACTIVATED);
        assertThat(out.deactivatedBy()).isEqualTo(USER);
        assertThat(out.deactivatedAt()).isNotNull();
    }

    @Test
    void deactivate_noActiveActivation_throws() {
        when(activationRepo.findByTenantIdAndPackCodeAndStatus(TENANT, "manufacturing", ActivationStatus.ACTIVE))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deactivate("manufacturing", USER))
                .isInstanceOf(IndustryPackNotFoundException.class);
    }

    @Test
    void myActiveActivations_returnsList() {
        when(activationRepo.findByTenantIdAndStatus(TENANT, ActivationStatus.ACTIVE))
                .thenReturn(List.of(activation("manufacturing", ActivationStatus.ACTIVE),
                        activation("it-itsm", ActivationStatus.ACTIVE)));
        assertThat(service.myActiveActivations()).hasSize(2);
    }

    @Test
    void myActivationHistory_returnsAll() {
        when(activationRepo.findByTenantIdOrderByActivatedAtDesc(TENANT))
                .thenReturn(List.of(
                        activation("manufacturing", ActivationStatus.DEACTIVATED),
                        activation("it-itsm", ActivationStatus.ACTIVE)));
        assertThat(service.myActivationHistory()).hasSize(2);
    }

    @Test
    void packResponse_emptyTags_returnsEmptyList() {
        IndustryPack p = pack("x");
        p.setTagsCsv(null);
        when(packRepo.findByCode("x")).thenReturn(Optional.of(p));
        assertThat(service.getByCode("x").tags()).isEmpty();
    }

    @Test
    void packResponse_csvTags_splitsTrimmed() {
        IndustryPack p = pack("x");
        p.setTagsCsv("a, b , c");
        when(packRepo.findByCode("x")).thenReturn(Optional.of(p));
        assertThat(service.getByCode("x").tags()).containsExactly("a", "b", "c");
    }

    private IndustryPack pack(String code) {
        IndustryPack p = new IndustryPack();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setName("Pack " + code);
        p.setVersion("1.0.0");
        p.setLocale("fr-FR");
        p.setTagsCsv("foo,bar");
        p.setManifestJson("{}");
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private TenantIndustryPackActivation activation(String code, ActivationStatus status) {
        TenantIndustryPackActivation a = new TenantIndustryPackActivation();
        a.setId(UUID.randomUUID());
        a.setTenantId(TENANT);
        a.setPackCode(code);
        a.setStatus(status);
        a.setActivatedBy(USER);
        a.setActivatedAt(Instant.now());
        return a;
    }
}
