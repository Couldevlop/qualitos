package com.openlab.qualitos.quality.complaints;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class ComplaintServiceTest {

    @Mock ComplaintRepository complaintRepo;
    @Mock ComplaintResponseRepository responseRepo;
    ComplaintService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID CMP = UUID.randomUUID();
    static final UUID RESP = UUID.randomUUID();
    static final UUID SUPPLIER = UUID.randomUUID();
    static final LocalDate TODAY = LocalDate.parse("2026-05-15");
    static final Clock CLOCK = Clock.fixed(
            TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new ComplaintService(complaintRepo, responseRepo, CLOCK);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    // ---- CRUD ----

    @Test
    void create_defaults() {
        when(complaintRepo.findByTenantIdAndCode(TENANT, "C-1")).thenReturn(Optional.empty());
        when(complaintRepo.save(any())).thenAnswer(inv -> {
            Complaint c = inv.getArgument(0);
            c.setId(CMP); c.setCreatedAt(Instant.now()); c.setUpdatedAt(Instant.now());
            return c;
        });
        ComplaintDto.ComplaintResponse out = service.create(new ComplaintDto.CreateComplaintRequest(
                "C-1", ComplaintChannel.EMAIL, "Alice", "alice@example.test", null,
                "Slow delivery", "Order ABC delayed", null, null, null, null, USER, null));
        assertThat(out.status()).isEqualTo(ComplaintStatus.RECEIVED);
        assertThat(out.severity()).isEqualTo(ComplaintSeverity.MEDIUM);
        assertThat(out.category()).isEqualTo(ComplaintCategory.OTHER);
        assertThat(out.receivedAt()).isNotNull();
    }

    @Test
    void create_duplicateCode_throws() {
        when(complaintRepo.findByTenantIdAndCode(TENANT, "dup"))
                .thenReturn(Optional.of(complaint(ComplaintStatus.RECEIVED)));
        assertThatThrownBy(() -> service.create(new ComplaintDto.CreateComplaintRequest(
                "dup", ComplaintChannel.EMAIL, null, null, null,
                "t", null, null, null, null, null, USER, null)))
                .isInstanceOf(ComplaintStateException.class);
    }

    @Test
    void create_noTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.create(new ComplaintDto.CreateComplaintRequest(
                "C-9", ComplaintChannel.EMAIL, null, null, null,
                "t", null, null, null, null, null, USER, null)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void get_crossTenant_appearsNotFound() {
        Complaint c = complaint(ComplaintStatus.RECEIVED);
        c.setTenantId(UUID.randomUUID());
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.get(CMP))
                .isInstanceOf(ComplaintNotFoundException.class);
    }

    @Test
    void update_terminal_rejected() {
        Complaint c = complaint(ComplaintStatus.CLOSED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.update(CMP, new ComplaintDto.UpdateComplaintRequest(
                "x", null, null, null, null, null, null, null, null)))
                .isInstanceOf(ComplaintStateException.class);
    }

    @Test
    void update_appliesPatches() {
        Complaint c = complaint(ComplaintStatus.UNDER_INVESTIGATION);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.update(CMP, new ComplaintDto.UpdateComplaintRequest(
                "Alice Doe", "alice2@example.test", "ORD-99",
                "New subject", "new desc", ComplaintSeverity.HIGH,
                ComplaintCategory.DELIVERY, SUPPLIER, USER));
        assertThat(c.getCustomerName()).isEqualTo("Alice Doe");
        assertThat(c.getSeverity()).isEqualTo(ComplaintSeverity.HIGH);
        assertThat(c.getCategory()).isEqualTo(ComplaintCategory.DELIVERY);
        assertThat(c.getSupplierId()).isEqualTo(SUPPLIER);
    }

    @Test
    void delete_received_cascades() {
        Complaint c = complaint(ComplaintStatus.RECEIVED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        service.delete(CMP);
        verify(responseRepo).deleteByComplaintId(CMP);
        verify(complaintRepo).delete(c);
    }

    @Test
    void delete_inProgress_rejected() {
        Complaint c = complaint(ComplaintStatus.RESPONDED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.delete(CMP))
                .isInstanceOf(ComplaintStateException.class);
    }

    @Test
    void list_filterPaths() {
        when(complaintRepo.findByTenantIdAndStatus(eq(TENANT), eq(ComplaintStatus.RESPONDED), any()))
                .thenReturn(new PageImpl<>(List.of(complaint(ComplaintStatus.RESPONDED))));
        assertThat(service.list(ComplaintStatus.RESPONDED, null, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(complaintRepo.findByTenantIdAndCategory(eq(TENANT), eq(ComplaintCategory.QUALITY), any()))
                .thenReturn(new PageImpl<>(List.of(complaint(ComplaintStatus.RECEIVED))));
        assertThat(service.list(null, ComplaintCategory.QUALITY, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(complaintRepo.findByTenantIdAndSupplierId(eq(TENANT), eq(SUPPLIER), any()))
                .thenReturn(new PageImpl<>(List.of(complaint(ComplaintStatus.RECEIVED))));
        assertThat(service.list(null, null, SUPPLIER, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(complaintRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(complaint(ComplaintStatus.RECEIVED))));
        assertThat(service.list(null, null, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    // ---- Workflow ----

    @Test
    void assign_movesReceivedToInvestigation() {
        Complaint c = complaint(ComplaintStatus.RECEIVED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ComplaintDto.ComplaintResponse out = service.assign(CMP,
                new ComplaintDto.AssignRequest(USER));
        assertThat(out.assignedToUserId()).isEqualTo(USER);
        assertThat(out.status()).isEqualTo(ComplaintStatus.UNDER_INVESTIGATION);
    }

    @Test
    void assign_existingInvestigation_keepsStatus() {
        Complaint c = complaint(ComplaintStatus.UNDER_INVESTIGATION);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ComplaintDto.ComplaintResponse out = service.assign(CMP,
                new ComplaintDto.AssignRequest(USER));
        assertThat(out.status()).isEqualTo(ComplaintStatus.UNDER_INVESTIGATION);
    }

    @Test
    void assign_terminal_rejected() {
        Complaint c = complaint(ComplaintStatus.REJECTED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.assign(CMP, new ComplaintDto.AssignRequest(USER)))
                .isInstanceOf(ComplaintStateException.class);
    }

    @Test
    void reject_fromReceived_ok() {
        Complaint c = complaint(ComplaintStatus.RECEIVED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ComplaintDto.ComplaintResponse out = service.reject(CMP,
                new ComplaintDto.RejectRequest("spam"));
        assertThat(out.status()).isEqualTo(ComplaintStatus.REJECTED);
        assertThat(out.rejectionReason()).isEqualTo("spam");
        assertThat(out.closedAt()).isNotNull();
    }

    @Test
    void reject_fromResolved_rejected() {
        Complaint c = complaint(ComplaintStatus.RESOLVED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.reject(CMP, new ComplaintDto.RejectRequest("x")))
                .isInstanceOf(ComplaintStateException.class);
    }

    @Test
    void resolve_withoutFirstResponse_rejected() {
        Complaint c = complaint(ComplaintStatus.UNDER_INVESTIGATION);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.resolve(CMP, null))
                .isInstanceOf(ComplaintStateException.class)
                .hasMessageContaining("response must be sent");
    }

    @Test
    void resolve_withFirstResponse_ok() {
        Complaint c = complaint(ComplaintStatus.UNDER_INVESTIGATION);
        c.setFirstResponseAt(Instant.now(CLOCK).minusSeconds(3600));
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID capa = UUID.randomUUID();
        ComplaintDto.ComplaintResponse out = service.resolve(CMP,
                new ComplaintDto.ResolveRequest(capa));
        assertThat(out.status()).isEqualTo(ComplaintStatus.RESOLVED);
        assertThat(out.resolvedAt()).isNotNull();
        assertThat(out.capaCaseId()).isEqualTo(capa);
    }

    @Test
    void resolve_fromResponded_ok() {
        Complaint c = complaint(ComplaintStatus.RESPONDED);
        c.setFirstResponseAt(Instant.now(CLOCK).minusSeconds(60));
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.resolve(CMP, null).status()).isEqualTo(ComplaintStatus.RESOLVED);
    }

    @Test
    void resolve_fromReceived_rejected() {
        Complaint c = complaint(ComplaintStatus.RECEIVED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.resolve(CMP, null))
                .isInstanceOf(ComplaintStateException.class);
    }

    @Test
    void close_fromResolved_ok() {
        Complaint c = complaint(ComplaintStatus.RESOLVED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.close(CMP).status()).isEqualTo(ComplaintStatus.CLOSED);
    }

    @Test
    void close_fromReceived_rejected() {
        Complaint c = complaint(ComplaintStatus.RECEIVED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.close(CMP))
                .isInstanceOf(ComplaintStateException.class);
    }

    @Test
    void reopen_fromClosed_resetsTimestamps() {
        Complaint c = complaint(ComplaintStatus.CLOSED);
        c.setResolvedAt(Instant.now(CLOCK));
        c.setClosedAt(Instant.now(CLOCK));
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ComplaintDto.ComplaintResponse out = service.reopen(CMP);
        assertThat(out.status()).isEqualTo(ComplaintStatus.UNDER_INVESTIGATION);
        assertThat(out.resolvedAt()).isNull();
        assertThat(out.closedAt()).isNull();
    }

    @Test
    void reopen_fromReceived_rejected() {
        Complaint c = complaint(ComplaintStatus.RECEIVED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.reopen(CMP))
                .isInstanceOf(ComplaintStateException.class);
    }

    @Test
    void setSatisfaction_resolved_ok() {
        Complaint c = complaint(ComplaintStatus.RESOLVED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.setSatisfaction(CMP, new ComplaintDto.SatisfactionRequest(9))
                .satisfactionScore()).isEqualTo(9);
    }

    @Test
    void setSatisfaction_unresolved_rejected() {
        Complaint c = complaint(ComplaintStatus.UNDER_INVESTIGATION);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.setSatisfaction(CMP,
                new ComplaintDto.SatisfactionRequest(8)))
                .isInstanceOf(ComplaintStateException.class);
    }

    // ---- Responses ----

    @Test
    void addResponse_externalFromUnderInvestigation_movesToResponded() {
        Complaint c = complaint(ComplaintStatus.UNDER_INVESTIGATION);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(responseRepo.save(any())).thenAnswer(inv -> {
            ComplaintResponse r = inv.getArgument(0);
            r.setId(RESP); r.setCreatedAt(Instant.now(CLOCK));
            return r;
        });
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.addResponse(CMP, new ComplaintDto.AddResponseRequest(
                USER, ComplaintChannel.EMAIL, "Bonjour, nous avons reçu votre message.", false));
        assertThat(c.getStatus()).isEqualTo(ComplaintStatus.RESPONDED);
        assertThat(c.getFirstResponseAt()).isNotNull();
    }

    @Test
    void addResponse_externalFromReceived_alsoMovesToResponded() {
        Complaint c = complaint(ComplaintStatus.RECEIVED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(responseRepo.save(any())).thenAnswer(inv -> {
            ComplaintResponse r = inv.getArgument(0);
            r.setId(RESP); r.setCreatedAt(Instant.now(CLOCK));
            return r;
        });
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.addResponse(CMP, new ComplaintDto.AddResponseRequest(
                USER, null, "body", false));
        assertThat(c.getStatus()).isEqualTo(ComplaintStatus.RESPONDED);
    }

    @Test
    void addResponse_internalNote_doesNotChangeStatus() {
        Complaint c = complaint(ComplaintStatus.UNDER_INVESTIGATION);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(responseRepo.save(any())).thenAnswer(inv -> {
            ComplaintResponse r = inv.getArgument(0);
            r.setId(RESP); r.setCreatedAt(Instant.now(CLOCK));
            return r;
        });
        service.addResponse(CMP, new ComplaintDto.AddResponseRequest(
                USER, null, "private note", true));
        assertThat(c.getStatus()).isEqualTo(ComplaintStatus.UNDER_INVESTIGATION);
        assertThat(c.getFirstResponseAt()).isNull();
        verify(complaintRepo, never()).save(any());
    }

    @Test
    void addResponse_terminal_rejected() {
        Complaint c = complaint(ComplaintStatus.CLOSED);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.addResponse(CMP,
                new ComplaintDto.AddResponseRequest(USER, null, "body", false)))
                .isInstanceOf(ComplaintStateException.class);
    }

    @Test
    void addResponse_secondExternal_doesNotOverwriteFirstResponseAt() {
        Complaint c = complaint(ComplaintStatus.RESPONDED);
        Instant first = Instant.parse("2026-05-10T10:00:00Z");
        c.setFirstResponseAt(first);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(responseRepo.save(any())).thenAnswer(inv -> {
            ComplaintResponse r = inv.getArgument(0);
            r.setId(RESP); r.setCreatedAt(Instant.now(CLOCK));
            return r;
        });
        when(complaintRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.addResponse(CMP, new ComplaintDto.AddResponseRequest(
                USER, null, "follow-up", false));
        assertThat(c.getFirstResponseAt()).isEqualTo(first);
    }

    @Test
    void deleteResponse_externalImmutable() {
        Complaint c = complaint(ComplaintStatus.UNDER_INVESTIGATION);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        ComplaintResponse r = response(false);
        when(responseRepo.findById(RESP)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.deleteResponse(CMP, RESP))
                .isInstanceOf(ComplaintStateException.class);
    }

    @Test
    void deleteResponse_internalNote_ok() {
        Complaint c = complaint(ComplaintStatus.UNDER_INVESTIGATION);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        ComplaintResponse r = response(true);
        when(responseRepo.findById(RESP)).thenReturn(Optional.of(r));
        service.deleteResponse(CMP, RESP);
        verify(responseRepo).delete(r);
    }

    @Test
    void deleteResponse_crossComplaint_appearsNotFound() {
        Complaint c = complaint(ComplaintStatus.UNDER_INVESTIGATION);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        ComplaintResponse r = response(true);
        r.setComplaintId(UUID.randomUUID());
        when(responseRepo.findById(RESP)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.deleteResponse(CMP, RESP))
                .isInstanceOf(ComplaintResponseNotFoundException.class);
    }

    @Test
    void listResponses_returnsPage() {
        Complaint c = complaint(ComplaintStatus.UNDER_INVESTIGATION);
        when(complaintRepo.findById(CMP)).thenReturn(Optional.of(c));
        when(responseRepo.findByComplaintIdOrderBySentAtAsc(eq(CMP), any()))
                .thenReturn(new PageImpl<>(List.of(response(false))));
        assertThat(service.listResponses(CMP, PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    // ---- Statistics ----

    @Test
    void statistics_aggregatesCounts() {
        @SuppressWarnings("unchecked")
        Page<Complaint> totalPage = (Page<Complaint>) mock(Page.class);
        when(totalPage.getTotalElements()).thenReturn(42L);
        when(complaintRepo.findByTenantId(eq(TENANT), any(Pageable.class))).thenReturn(totalPage);
        when(complaintRepo.countByTenantIdAndStatus(eq(TENANT), eq(ComplaintStatus.RECEIVED))).thenReturn(3L);
        when(complaintRepo.countByTenantIdAndStatus(eq(TENANT), eq(ComplaintStatus.UNDER_INVESTIGATION))).thenReturn(5L);
        when(complaintRepo.countByTenantIdAndStatus(eq(TENANT), eq(ComplaintStatus.RESPONDED))).thenReturn(8L);
        when(complaintRepo.countByTenantIdAndStatus(eq(TENANT), eq(ComplaintStatus.RESOLVED))).thenReturn(12L);
        when(complaintRepo.countByTenantIdAndStatus(eq(TENANT), eq(ComplaintStatus.CLOSED))).thenReturn(10L);
        when(complaintRepo.countByTenantIdAndStatus(eq(TENANT), eq(ComplaintStatus.REJECTED))).thenReturn(4L);
        when(complaintRepo.countByTenantIdAndCategory(eq(TENANT), any())).thenReturn(0L);
        ComplaintDto.ComplaintStatistics stats = service.statistics();
        assertThat(stats.total()).isEqualTo(42L);
        assertThat(stats.resolved()).isEqualTo(12L);
        assertThat(stats.received()).isEqualTo(3L);
    }

    // ---- helpers ----

    private Complaint complaint(ComplaintStatus status) {
        Complaint c = new Complaint();
        c.setId(CMP); c.setTenantId(TENANT);
        c.setCode("C-1"); c.setChannel(ComplaintChannel.EMAIL);
        c.setSubject("Slow"); c.setSeverity(ComplaintSeverity.MEDIUM);
        c.setCategory(ComplaintCategory.DELIVERY);
        c.setStatus(status);
        c.setReceivedAt(Instant.now(CLOCK).minusSeconds(86400));
        c.setCreatedBy(USER);
        c.setCreatedAt(Instant.now(CLOCK).minusSeconds(86400));
        c.setUpdatedAt(Instant.now(CLOCK));
        return c;
    }

    private ComplaintResponse response(boolean internal) {
        ComplaintResponse r = new ComplaintResponse();
        r.setId(RESP); r.setTenantId(TENANT); r.setComplaintId(CMP);
        r.setAuthorUserId(USER); r.setChannel(ComplaintChannel.EMAIL);
        r.setBody("hello"); r.setInternalNote(internal);
        r.setSentAt(Instant.now(CLOCK)); r.setCreatedAt(Instant.now(CLOCK));
        return r;
    }
}
