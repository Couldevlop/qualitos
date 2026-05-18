package com.openlab.qualitos.quality.marketplace.application;

import com.openlab.qualitos.quality.marketplace.domain.MarketplacePack;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackNotFoundException;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackRepository;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarketplacePackServiceTest {

    @Mock MarketplacePackRepository repo;
    @Mock SuperAdminProvider superAdmin;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID ADMIN = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    MarketplacePackService service;

    @BeforeEach
    void setup() {
        service = new MarketplacePackService(repo, superAdmin, CLOCK);
        when(superAdmin.requireSuperAdminId()).thenReturn(ADMIN);
        when(repo.existsByPackIdAndVersion(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            MarketplacePack p = inv.getArgument(0);
            if (p.getId() == null) p.assignId(ID);
            return p;
        });
    }

    @Test
    void list_returnsVerifiedOnly() {
        MarketplacePack verified = sample(true);
        when(repo.findVerified(null)).thenReturn(List.of(verified));
        assertThat(service.listVerified(null)).hasSize(1);
    }

    @Test
    void list_filtersBySector() {
        when(repo.findVerified("healthcare")).thenReturn(List.of(sample(true)));
        assertThat(service.listVerified("healthcare")).hasSize(1);
    }

    @Test
    void register_succeeds() {
        var view = service.register(new MarketplacePackDto.RegisterRequest(
                "iso", "1.0", "Pub", "Title", "d", "healthcare", 0, "EUR",
                "https://x.com/y.zip", "deadbeef".repeat(8)));
        assertThat(view.id()).isEqualTo(ID);
        assertThat(view.verified()).isFalse();
    }

    @Test
    void register_duplicate_throws() {
        when(repo.existsByPackIdAndVersion("iso", "1.0")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new MarketplacePackDto.RegisterRequest(
                "iso", "1.0", "Pub", "T", null, "s", 0, "EUR",
                "https://x.com/y.zip", "deadbeef".repeat(8))))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void verify_setsAdmin() {
        MarketplacePack p = sample(false);
        p.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(p));
        var view = service.verify(ID);
        assertThat(view.verified()).isTrue();
        assertThat(view.verifiedBy()).isEqualTo(ADMIN);
    }

    @Test
    void verify_unknown_throws() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verify(ID))
            .isInstanceOf(MarketplacePackNotFoundException.class);
    }

    @Test
    void get_returnsView() {
        MarketplacePack p = sample(true);
        p.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(p));
        assertThat(service.get(ID).id()).isEqualTo(ID);
    }

    @Test
    void get_unknown_throws() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
            .isInstanceOf(MarketplacePackNotFoundException.class);
    }

    private static MarketplacePack sample(boolean verified) {
        MarketplacePack p = MarketplacePack.register(
                "iso", "1.0", "Pub", "Title", null, "healthcare",
                0, "EUR", "https://x.com/y.zip", "deadbeef".repeat(8), NOW);
        if (verified) {
            p.verify(UUID.randomUUID(), NOW);
        }
        return p;
    }
}
