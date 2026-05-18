package com.openlab.qualitos.quality.marketplace.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketplacePackTest {

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final UUID ADMIN = UUID.randomUUID();

    @Test
    void register_setsDefaults() {
        MarketplacePack p = MarketplacePack.register(
                "iso-13485-startup", "1.0", "Acme Consulting",
                "Pack ISO 13485 startup MedTech", "desc", "healthcare",
                12000, "EUR",
                "https://packs.example.com/iso-13485-startup-1.0.zip",
                "deadbeef".repeat(8),
                NOW);
        assertThat(p.isVerified()).isFalse();
        assertThat(p.getPriceCents()).isEqualTo(12000);
        assertThat(p.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void verify_setsAdminAndTimestamp() {
        MarketplacePack p = MarketplacePack.register(
                "iso-9001-base", "1.0", "Pub", "Title", null, "industry",
                0, "EUR", "https://x.com/y.zip", "deadbeef".repeat(8), NOW);
        p.verify(ADMIN, NOW.plusSeconds(60));
        assertThat(p.isVerified()).isTrue();
        assertThat(p.getVerifiedBy()).isEqualTo(ADMIN);
        assertThat(p.getVerifiedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void verify_twice_throws() {
        MarketplacePack p = MarketplacePack.register(
                "iso-9001-base", "1.0", "Pub", "Title", null, "industry",
                0, "EUR", "https://x.com/y.zip", "deadbeef".repeat(8), NOW);
        p.verify(ADMIN, NOW);
        assertThatThrownBy(() -> p.verify(ADMIN, NOW))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void verify_withoutSignature_throws() {
        MarketplacePack p = MarketplacePack.register(
                "iso-9001-base", "1.0", "Pub", "Title", null, "industry",
                0, "EUR", "https://x.com/y.zip", null, NOW);
        assertThatThrownBy(() -> p.verify(ADMIN, NOW))
            .isInstanceOf(MarketplacePackStateException.class);
    }

    @Test
    void invalidPackId_throws() {
        assertThatThrownBy(() -> MarketplacePack.register(
                "Invalid_ID", "1.0", "Pub", "T", null, "s", 0, "EUR",
                "https://x.com/y.zip", "deadbeef".repeat(8), NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownCurrency_throws() {
        assertThatThrownBy(() -> MarketplacePack.register(
                "iso", "1.0", "Pub", "T", null, "s", 0, "XYZ",
                "https://x.com/y.zip", "deadbeef".repeat(8), NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void httpManifest_throws() {
        assertThatThrownBy(() -> MarketplacePack.register(
                "iso", "1.0", "Pub", "T", null, "s", 0, "EUR",
                "http://x.com/y.zip", "deadbeef".repeat(8), NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativePrice_throws() {
        assertThatThrownBy(() -> MarketplacePack.register(
                "iso", "1.0", "Pub", "T", null, "s", -1, "EUR",
                "https://x.com/y.zip", "deadbeef".repeat(8), NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
