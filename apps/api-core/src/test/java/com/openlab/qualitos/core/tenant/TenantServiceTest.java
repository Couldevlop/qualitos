package com.openlab.qualitos.core.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantService")
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    private Tenant sampleTenant;

    @BeforeEach
    void setUp() {
        sampleTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .slug("acme-corp")
                .name("ACME Corporation")
                .plan(Tenant.Plan.PRO)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Returns page of tenants mapped to DTOs")
        void returnsPageOfDtos() {
            Pageable pageable = PageRequest.of(0, 20);
            given(tenantRepository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(sampleTenant)));

            Page<TenantDto.Response> result = tenantService.findAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).slug()).isEqualTo("acme-corp");
        }

        @Test
        @DisplayName("Returns empty page when no tenants exist")
        void returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            given(tenantRepository.findAll(pageable))
                    .willReturn(Page.empty());

            Page<TenantDto.Response> result = tenantService.findAll(pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Returns tenant DTO when found")
        void returnsDtoWhenFound() {
            UUID id = sampleTenant.getId();
            given(tenantRepository.findById(id)).willReturn(Optional.of(sampleTenant));

            TenantDto.Response result = tenantService.findById(id);

            assertThat(result.id()).isEqualTo(id);
            assertThat(result.slug()).isEqualTo("acme-corp");
            assertThat(result.plan()).isEqualTo(Tenant.Plan.PRO);
        }

        @Test
        @DisplayName("Throws TenantNotFoundException when not found")
        void throwsWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            given(tenantRepository.findById(unknownId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.findById(unknownId))
                    .isInstanceOf(TenantNotFoundException.class)
                    .hasMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("findBySlug")
    class FindBySlug {

        @Test
        @DisplayName("Returns tenant DTO when slug matches")
        void returnsDtoWhenFound() {
            given(tenantRepository.findBySlug("acme-corp")).willReturn(Optional.of(sampleTenant));

            TenantDto.Response result = tenantService.findBySlug("acme-corp");

            assertThat(result.slug()).isEqualTo("acme-corp");
        }

        @Test
        @DisplayName("Throws TenantNotFoundException when slug not found")
        void throwsWhenNotFound() {
            given(tenantRepository.findBySlug("unknown")).willReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.findBySlug("unknown"))
                    .isInstanceOf(TenantNotFoundException.class)
                    .hasMessageContaining("unknown");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Creates and returns new tenant with default STARTER plan when plan is null")
        void createsWithDefaultPlan() {
            TenantDto.CreateRequest request = new TenantDto.CreateRequest(
                    "new-tenant", "New Tenant SAS", null);

            given(tenantRepository.existsBySlug("new-tenant")).willReturn(false);
            given(tenantRepository.save(any(Tenant.class))).willAnswer(inv -> {
                Tenant t = inv.getArgument(0);
                t = Tenant.builder()
                        .id(UUID.randomUUID())
                        .slug(t.getSlug())
                        .name(t.getName())
                        .plan(t.getPlan())
                        .active(t.isActive())
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                return t;
            });

            TenantDto.Response result = tenantService.create(request);

            assertThat(result.slug()).isEqualTo("new-tenant");
            assertThat(result.plan()).isEqualTo(Tenant.Plan.STARTER);
            assertThat(result.active()).isTrue();
        }

        @Test
        @DisplayName("Creates tenant with provided plan")
        void createsWithProvidedPlan() {
            TenantDto.CreateRequest request = new TenantDto.CreateRequest(
                    "enterprise-client", "Enterprise Client", Tenant.Plan.ENTERPRISE);

            given(tenantRepository.existsBySlug("enterprise-client")).willReturn(false);
            given(tenantRepository.save(any(Tenant.class))).willAnswer(inv -> {
                Tenant t = inv.getArgument(0);
                return Tenant.builder()
                        .id(UUID.randomUUID())
                        .slug(t.getSlug())
                        .name(t.getName())
                        .plan(t.getPlan())
                        .active(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
            });

            TenantDto.Response result = tenantService.create(request);

            assertThat(result.plan()).isEqualTo(Tenant.Plan.ENTERPRISE);
        }

        @Test
        @DisplayName("Throws TenantAlreadyExistsException when slug already taken")
        void throwsWhenSlugExists() {
            TenantDto.CreateRequest request = new TenantDto.CreateRequest(
                    "acme-corp", "Duplicate", null);
            given(tenantRepository.existsBySlug("acme-corp")).willReturn(true);

            assertThatThrownBy(() -> tenantService.create(request))
                    .isInstanceOf(TenantAlreadyExistsException.class)
                    .hasMessageContaining("acme-corp");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Updates name, plan, and active status")
        void updatesAllFields() {
            UUID id = sampleTenant.getId();
            TenantDto.UpdateRequest request = new TenantDto.UpdateRequest(
                    "Updated Name", Tenant.Plan.ENTERPRISE, false);

            given(tenantRepository.findById(id)).willReturn(Optional.of(sampleTenant));
            given(tenantRepository.save(any(Tenant.class))).willAnswer(inv -> inv.getArgument(0));

            TenantDto.Response result = tenantService.update(id, request);

            assertThat(result.name()).isEqualTo("Updated Name");
            assertThat(result.plan()).isEqualTo(Tenant.Plan.ENTERPRISE);
            assertThat(result.active()).isFalse();
        }

        @Test
        @DisplayName("Does not change plan when update request plan is null")
        void keepsPlanWhenNull() {
            UUID id = sampleTenant.getId();
            TenantDto.UpdateRequest request = new TenantDto.UpdateRequest(
                    "New Name", null, null);

            given(tenantRepository.findById(id)).willReturn(Optional.of(sampleTenant));
            given(tenantRepository.save(any(Tenant.class))).willAnswer(inv -> inv.getArgument(0));

            TenantDto.Response result = tenantService.update(id, request);

            assertThat(result.plan()).isEqualTo(Tenant.Plan.PRO);
        }

        @Test
        @DisplayName("Throws TenantNotFoundException when tenant not found")
        void throwsWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            given(tenantRepository.findById(unknownId)).willReturn(Optional.empty());

            TenantDto.UpdateRequest request = new TenantDto.UpdateRequest("Name", null, null);
            assertThatThrownBy(() -> tenantService.update(unknownId, request))
                    .isInstanceOf(TenantNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deactivate")
    class Deactivate {

        @Test
        @DisplayName("Sets active to false on tenant")
        void deactivatesTenant() {
            UUID id = sampleTenant.getId();
            given(tenantRepository.findById(id)).willReturn(Optional.of(sampleTenant));
            given(tenantRepository.save(any(Tenant.class))).willAnswer(inv -> inv.getArgument(0));

            tenantService.deactivate(id);

            ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
            verify(tenantRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("Throws TenantNotFoundException when tenant not found")
        void throwsWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            given(tenantRepository.findById(unknownId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.deactivate(unknownId))
                    .isInstanceOf(TenantNotFoundException.class);
        }
    }
}
